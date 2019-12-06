package com.enonic.ec.kubernetes.operator.commands.vhosts;

import java.util.Optional;

import org.immutables.value.Value;

import com.google.common.collect.ImmutableMap;

import io.fabric8.kubernetes.client.KubernetesClient;

import com.enonic.ec.kubernetes.common.commands.ImmutableCombinedCommand;
import com.enonic.ec.kubernetes.kubectl.apply.ImmutableCommandApplyIngress;
import com.enonic.ec.kubernetes.operator.commands.vhosts.spec.ImmutableIngressSpec;
import com.enonic.ec.kubernetes.operator.crd.vhost.XpVHostResource;
import com.enonic.ec.kubernetes.operator.crd.vhost.spec.SpecCertificate;
import com.enonic.ec.kubernetes.operator.crd.vhost.spec.SpecCertificateAuthority;
import com.enonic.ec.kubernetes.operator.crd.vhost.spec.SpecMapping;

@Value.Immutable
public abstract class XpVHostApplyIngress
    extends XpVHostCommand
{
    protected abstract KubernetesClient defaultClient();

    protected abstract XpVHostResource resource();

    @Override
    public void addCommands( final ImmutableCombinedCommand.Builder commandBuilder )
    {
        if ( !resource().getSpec().createIngress() )
        {
            return;
        }

        String name = resource().getMetadata().getName();
        String host = resource().getSpec().host();

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
        Optional<SpecMapping> adminPath =
            resource().getSpec().mappings().stream().filter( m -> m.target().startsWith( "/admin" ) ).findAny();
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

        SpecCertificate cert = resource().getSpec().certificate();
        if ( cert != null )
        {
            ingressAnnotations.put( "nginx.ingress.kubernetes.io/ssl-redirect", "true" );
            ingressAnnotations.put( "cert-manager.io/cluster-issuer", getIssuer( cert.authority() ) );
        }

        commandBuilder.addCommand( ImmutableCommandApplyIngress.builder().
            client( defaultClient() ).
            canSkipOwnerReference( true ).
            namespace( resource().getMetadata().getNamespace() ).
            name( getIngressName( resource() ) ).
            annotations( ingressAnnotations.build() ).
            spec( ImmutableIngressSpec.builder().
                certificateSecretName( Optional.ofNullable( cert != null ? name : null ) ).
                allService( resource().getMetadata().getNamespace() ).
                vHostSpec( resource().getSpec() ).
                build().
                spec() ).
            build() );
    }

    private String getIssuer( SpecCertificateAuthority authority )
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
