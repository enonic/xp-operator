package com.enonic.cloud.operator.domain;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.networking.v1beta1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1beta1.IngressRule;
import io.fabric8.kubernetes.api.model.networking.v1beta1.IngressTLS;

import com.enonic.cloud.kubernetes.Clients;
import com.enonic.cloud.kubernetes.Searchers;
import com.enonic.cloud.kubernetes.commands.K8sLogHelper;
import com.enonic.cloud.kubernetes.model.v1alpha2.domain.Domain;
import com.enonic.cloud.operator.helpers.InformerEventHandler;

import static com.enonic.cloud.common.Configuration.cfgStr;

/**
 * This operator class updates ingress certificate configuration based on linked Domain
 */
@Singleton
public class OperatorIngressCertSync
    extends InformerEventHandler<Ingress>
{
    @Inject
    Clients clients;

    @Inject
    Searchers searchers;

    @Override
    protected void onNewAdd( final Ingress newResource )
    {
        handle( newResource );
    }

    @Override
    public void onUpdate( final Ingress oldResource, final Ingress newResource )
    {
        handle( newResource );
    }

    @Override
    public void onDelete( final Ingress ingress, final boolean b )
    {
        // Do nothing
    }

    public void handle( final Ingress ingress )
    {
        // Ignore if ingress is not relevant
        if ( !ingressRelevant( ingress ) )
        {
            return;
        }

        // Collect all hosts in ingress
        Set<String> hosts = ingressHosts( ingress );

        // Get domains with those hosts
        List<Domain> domains = searchers.domain().query().
            filter( d -> hosts.contains( d.getDomainSpec().getHost() ) ).
            list();

        // Sync Ingress with domain
        domains.forEach( domain -> syncIngress( domain, ingress ) );
    }

    public void syncIngress( final Domain domain, final Ingress ingress )
    {
        String host = domain.getDomainSpec().getHost();

        // Collect annotations
        Map<String, String> oldAnnotations = ingress.getMetadata().getAnnotations();
        Map<String, String> newAnnotations = oldAnnotations != null ? new HashMap<>( oldAnnotations ) : new HashMap<>();

        // Always remove cert-manager annotation by default
        newAnnotations.remove( "cert-manager.io/cluster-issuer" );

        // Collect TLS definitions
        List<IngressTLS> oldTLS = ingress.getSpec().getTls() != null ? ingress.getSpec().getTls() : new LinkedList<>();
        List<IngressTLS> newTLS = new LinkedList<>();

        // Add all TLS definitions that do not relate to this domain
        for ( IngressTLS tls : oldTLS )
        {
            if ( !tls.getHosts().contains( host ) )
            {
                newTLS.add( tls );
            }
        }

        // If domain has certificate, set that up on the ingress
        if ( domain.getDomainSpec().getDomainSpecCertificate() != null )
        {
            switch ( domain.getDomainSpec().getDomainSpecCertificate().getAuthority() )
            {
                case SELF_SIGNED:
                case LETS_ENCRYPT_STAGING:
                case LETS_ENCRYPT:
                case CLUSTER_ISSUER:
                    newAnnotations.put( "cert-manager.io/cluster-issuer", getClusterIssuer( domain ) );
                    newTLS.add( new IngressTLS( Arrays.asList( host ), domain.getMetadata().getName() + "-cert" ) );
                    break;
                case CUSTOM:
                    newTLS.add(
                        new IngressTLS( Arrays.asList( host ), domain.getDomainSpec().getDomainSpecCertificate().getIdentifier() ) );
                    break;
            }
        }

        // If changes are detected, update ingress
        if ( !Objects.equals( oldAnnotations, newAnnotations ) || !Objects.equals( oldTLS, newTLS ) )
        {
            K8sLogHelper.logDoneable( clients.k8s().network().ingresses().
                inNamespace( ingress.getMetadata().getNamespace() ).
                withName( ingress.getMetadata().getName() ).
                edit().
                editMetadata().
                withAnnotations( newAnnotations ).
                endMetadata().
                editSpec().
                withTls( newTLS ).
                endSpec() );
        }
    }


    private String getClusterIssuer( Domain resource )
    {
        switch ( resource.getDomainSpec().getDomainSpecCertificate().getAuthority() )
        {
            case SELF_SIGNED:
                return cfgStr( "operator.cert.issuer.selfSigned" );
            case LETS_ENCRYPT_STAGING:
                return cfgStr( "operator.cert.issuer.letsEncrypt.staging" );
            case LETS_ENCRYPT:
                return cfgStr( "operator.cert.issuer.letsEncrypt.prod" );
            case CLUSTER_ISSUER:
                return resource.getDomainSpec().getDomainSpecCertificate().getIdentifier();
            default:
                return null;
        }
    }

    public Set<String> ingressHosts( final Ingress ingress )
    {
        return ingress.getSpec().getRules().stream().map( IngressRule::getHost ).collect( Collectors.toSet() );
    }

    public boolean ingressRelevant( final Ingress ingress )
    {
        if ( ingress.getSpec().getRules() == null )
        {
            return false;
        }

        if ( ingress.getMetadata().getAnnotations() == null )
        {
            return false;
        }

        if ( !ingress.getMetadata().getAnnotations().
            getOrDefault( cfgStr( "operator.annotations.ingress.cert.manage" ), "false" ).equals( "true" ) )
        {
            return false;
        }
        return true;
    }
}
