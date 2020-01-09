package com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7vhost.commands;

import java.util.Optional;

import org.immutables.value.Value;

import com.google.common.collect.ImmutableMap;

import io.fabric8.kubernetes.client.KubernetesClient;

import com.enonic.ec.kubernetes.operator.common.Configuration;
import com.enonic.ec.kubernetes.operator.common.commands.CombinedCommandBuilder;
import com.enonic.ec.kubernetes.operator.common.commands.ImmutableCombinedCommand;
import com.enonic.ec.kubernetes.operator.kubectl.apply.ImmutableCommandApplyIngress;
import com.enonic.ec.kubernetes.operator.kubectl.delete.ImmutableCommandDeleteIngress;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7vhost.commands.spec.ImmutableIngressSpec;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7vhost.crd.Xp7VHostResource;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7vhost.info.DiffXp7VHost;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7vhost.crd.spec.Xp7VHostSpecCertificate;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7vhost.crd.spec.Xp7VHostSpecCertificateAuthority;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7vhost.crd.spec.Xp7VHostSpecMapping;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.ResourceInfoNamespaced;

@Value.Immutable
public abstract class CommandXpVHostIngressApply
    extends Configuration
    implements CombinedCommandBuilder
{
    private static String getIngressName( Xp7VHostResource resource )
    {
        return resource.getMetadata().getName();
    }

    protected abstract KubernetesClient defaultClient();

    protected abstract ResourceInfoNamespaced<Xp7VHostResource, DiffXp7VHost> info();

    @Override
    public void addCommands( final ImmutableCombinedCommand.Builder commandBuilder )
    {
        boolean skipIngress = info().resource().getSpec().skipIngress();
        if ( info().diff().diffSpec().skipIngressChanged() && skipIngress )
        {
            deleteIngress( commandBuilder );
        }
        else if ( !skipIngress )
        {
            applyIngress( commandBuilder );
        }
    }

    protected void applyIngress( final ImmutableCombinedCommand.Builder commandBuilder )
    {
        String name = info().resource().getMetadata().getName();
        String host = info().resource().getSpec().host();

        ImmutableMap.Builder<String, String> ingressAnnotations = ImmutableMap.builder();
        StringBuilder nginxConfigSnippet = new StringBuilder();

        // Linkerd config
        cfgIfBool( "operator.extensions.linkerd.enabled", () -> nginxConfigSnippet.
            append( "proxy_set_header l5d-dst-override $service_name.$namespace.svc.cluster.local:$service_port;" ).append( "\n" ).
            append( "proxy_hide_header l5d-remote-ip;" ).append( "\n" ).
            append( "proxy_hide_header l5d-server-id;" ).append( "\n" ) );

        // Caching
        cfgIfBool( "operator.extensions.ingress.caching.enabled", () -> {
            nginxConfigSnippet.
                append( "proxy_cache static-cache;" ).append( "\n" ).
                append( "proxy_cache_valid 404 1m;" ).append( "\n" ).
                append( "proxy_cache_valid 301 1m;" ).append( "\n" ).
                append( "proxy_cache_valid 303 1m;" ).append( "\n" ).
                append( "proxy_cache_use_stale error timeout updating http_404 http_500 http_502 http_503 http_504;" ).append( "\n" ).
                append( "proxy_cache_bypass $http_x_purge;" ).append( "\n" ).
                append( "add_header X-Cache-Status $upstream_cache_status;" ).append( "\n" );

            ingressAnnotations.
                put( "nginx.ingress.kubernetes.io/proxy-buffering", "on" );
        } );

        // DDOS mitigation
        cfgIfBool( "operator.extensions.ingress.ddos.enabled", () -> ingressAnnotations.
            put( "nginx.ingress.kubernetes.io/limit-connections", "20" ).
            put( "nginx.ingress.kubernetes.io/limit-rpm", "240" ) );

        // Sticky session
        Optional<Xp7VHostSpecMapping> adminPath =
            info().resource().getSpec().mappings().stream().filter( m -> m.target().startsWith( "/admin" ) ).findAny();
        if ( adminPath.isPresent() )
        {
            cfgIfBool( "operator.extensions.ingress.adminStickySession.enabled", () -> ingressAnnotations.
                put( "nginx.ingress.kubernetes.io/affinity", "cookie" ).
                put( "nginx.ingress.kubernetes.io/session-cookie-name", "XPADMINCOOKIE" ).
                put( "nginx.ingress.kubernetes.io/session-cookie-path", "/admin" ) );
        }

        cfgIfBool( "operator.extensions.ingress.externalDns.enabled", () -> ingressAnnotations.
            put( "external-dns.alpha.kubernetes.io/hostname", host ).
            put( "external-dns.alpha.kubernetes.io/ttl", cfgStr( "operator.extensions.ingress.externalDns.recordTTL" ) ) );

        ingressAnnotations.put( cfgStr( "dns.annotations.enable" ), host );

        // Regular ingress
        ingressAnnotations.
            put( "kubernetes.io/ingress.class", "nginx" ).
            put( "ingress.kubernetes.io/rewrite-target", "/" ).
            put( "nginx.ingress.kubernetes.io/configuration-snippet", nginxConfigSnippet.toString() );

        Xp7VHostSpecCertificate cert = info().resource().getSpec().certificate();
        if ( cert != null )
        {
            ingressAnnotations.put( "nginx.ingress.kubernetes.io/ssl-redirect", "true" );
            ingressAnnotations.put( "cert-manager.io/cluster-issuer", getIssuer( cert.authority() ) );
        }

        commandBuilder.addCommand( ImmutableCommandApplyIngress.builder().
            client( defaultClient() ).
            ownerReference( info().ownerReference() ).
            namespace( info().deploymentInfo().namespaceName() ).
            name( getIngressName( info().resource() ) ).
            annotations( ingressAnnotations.build() ).
            spec( ImmutableIngressSpec.builder().
                certificateSecretName( Optional.ofNullable( cert != null ? name : null ) ).
                allService( info().resource().getMetadata().getNamespace() ).
                vHostSpec( info().resource().getSpec() ).
                build().
                spec() ).
            build() );
    }

    private void deleteIngress( final ImmutableCombinedCommand.Builder commandBuilder )
    {
        commandBuilder.addCommand( ImmutableCommandDeleteIngress.builder().
            client( defaultClient() ).
            namespace( info().deploymentInfo().namespaceName() ).
            name( getIngressName( info().resource() ) ).
            build() );
    }

    private String getIssuer( Xp7VHostSpecCertificateAuthority authority )
    {
        switch ( authority )
        {
            case SELF_SIGNED:
                return cfgStr( "operator.certissuer.selfsigned" );
            case LETS_ENCRYPT_STAGING:
                return cfgStr( "operator.certissuer.letsencrypt.staging" );
            case LETS_ENCRYPT_PROD:
                return cfgStr( "operator.certissuer.letsencrypt.prod" );
        }
        return "none";
    }
}
