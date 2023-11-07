package com.enonic.kubernetes.operator.domain;

import com.enonic.kubernetes.client.v1.domain.Domain;
import com.enonic.kubernetes.client.v1.domain.DomainStatus;
import com.enonic.kubernetes.kubernetes.Clients;
import com.enonic.kubernetes.kubernetes.Informers;
import com.enonic.kubernetes.kubernetes.Searchers;
import com.enonic.kubernetes.kubernetes.commands.K8sLogHelper;
import com.enonic.kubernetes.operator.helpers.InformerEventHandler;

import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.IngressRule;
import io.fabric8.kubernetes.api.model.networking.v1.IngressTLS;
import io.quarkus.runtime.StartupEvent;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.enonic.kubernetes.common.Configuration.cfgHasKey;
import static com.enonic.kubernetes.common.Configuration.cfgStr;

/**
 * This operator class updates ingress certificate configuration based on linked Domain
 */
@ApplicationScoped
public class OperatorIngressCertSync
    extends InformerEventHandler<Ingress>
{
    @Inject
    Clients clients;

    @Inject
    Searchers searchers;

    @Inject
    Informers informers;

    void onStart( @Observes StartupEvent ev )
    {
        listen( informers.ingressInformer() );
    }

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

        // Sync ingress with relevant domains
        searchers.domain().stream().filter( d -> hosts.contains( d.getSpec().getHost() ) ).forEach( d -> syncIngress( d, ingress ) );
    }

    public void syncIngress( final Domain domain, final Ingress ingress )
    {
        String host = domain.getSpec().getHost();

        // Collect annotations
        Map<String, String> oldAnnotations = ingress.getMetadata().getAnnotations();
        Map<String, String> newAnnotations = oldAnnotations != null ? new HashMap<>( oldAnnotations ) : new HashMap<>();

        // Always remove cert-manager annotation by default
        newAnnotations.remove( "cert-manager.io/cluster-issuer" );

        // Collect TLS definitions
        List<IngressTLS> oldTLS = ingress.getSpec().getTls() != null ? ingress.getSpec().getTls() : List.of();
        List<IngressTLS> newTLS = new ArrayList<>();

        // Add all TLS definitions that do not relate to this domain
        for ( IngressTLS tls : oldTLS )
        {
            if ( !tls.getHosts().contains( host ) )
            {
                newTLS.add( tls );
            }
        }

        // If domain has certificate, set that up on the ingress
        if ( domain.getSpec().getDomainSpecCertificate() != null )
        {
            switch ( domain.getSpec().getDomainSpecCertificate().getAuthority() )
            {
                case SELF_SIGNED:
                case LETS_ENCRYPT_STAGING:
                case LETS_ENCRYPT:
                case CLUSTER_ISSUER:
                    newAnnotations.put( "cert-manager.io/cluster-issuer", getClusterIssuer( domain ) );
                    newTLS.add( new IngressTLS( Arrays.asList( host ), domain.getMetadata().getName() + "-cert" ) );
                    break;
                case DOMAIN_WILDCARD:
                    if ( cfgHasKey( "dns.lb.domain.wildcardCertificate" ) )
                    {
                        newAnnotations.put( "cert-manager.io/cluster-issuer", getClusterIssuer( domain ) );
                        newTLS.add( new IngressTLS( Arrays.asList( host ), cfgStr( "dns.lb.domain.wildcardCertificate" ) ) );
                    }
                    else
                    {
                        domain.getStatus()
                            .withState( DomainStatus.State.ERROR )
                            .withMessage( "wildcard certificate name has not been set." );
                    }
                    break;
                case CUSTOM:
                    newTLS.add( new IngressTLS( Arrays.asList( host ), domain.getSpec().getDomainSpecCertificate().getIdentifier() ) );
                    break;
            }
        }

        // If changes are detected, update ingress
        if ( !Objects.equals( oldAnnotations, newAnnotations ) || !Objects.equals( oldTLS, newTLS ) )
        {
            K8sLogHelper.logEdit( clients.k8s()
                                      .network()
                                      .v1()
                                      .ingresses()
                                      .inNamespace( ingress.getMetadata().getNamespace() )
                                      .withName( ingress.getMetadata().getName() ), i -> {
                i.getMetadata().setAnnotations( newAnnotations );
                i.getSpec().setTls( newTLS );
                return i;
            } );
        }
    }


    private String getClusterIssuer( Domain resource )
    {
        switch ( resource.getSpec().getDomainSpecCertificate().getAuthority() )
        {
            case SELF_SIGNED:
                return cfgStr( "operator.certIssuer.selfSigned" );
            case LETS_ENCRYPT_STAGING:
                return cfgStr( "operator.certIssuer.letsEncrypt.staging" );
            case LETS_ENCRYPT:
                return cfgStr( "operator.certIssuer.letsEncrypt.prod" );
            case DOMAIN_WILDCARD:
                return cfgStr( "operator.certIssuer.domainWildcard" );
            case CLUSTER_ISSUER:
                return resource.getSpec().getDomainSpecCertificate().getIdentifier();
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

        return ingress.getMetadata()
            .getAnnotations()
            .getOrDefault( cfgStr( "operator.charts.values.annotationKeys.ingressCertManage" ), "false" )
            .equals( "true" );
    }
}
