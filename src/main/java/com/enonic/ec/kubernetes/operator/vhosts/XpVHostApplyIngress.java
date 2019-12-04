package com.enonic.ec.kubernetes.operator.vhosts;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.immutables.value.Value;

import com.google.common.collect.ImmutableMap;

import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.client.KubernetesClient;

import com.enonic.ec.kubernetes.common.Configuration;
import com.enonic.ec.kubernetes.common.Diff;
import com.enonic.ec.kubernetes.common.commands.CombinedCommandBuilder;
import com.enonic.ec.kubernetes.common.commands.ImmutableCombinedCommand;
import com.enonic.ec.kubernetes.crd.vhost.diff.DiffSpec;
import com.enonic.ec.kubernetes.crd.vhost.spec.Spec;
import com.enonic.ec.kubernetes.crd.vhost.spec.SpecCertificate;
import com.enonic.ec.kubernetes.crd.vhost.spec.SpecCertificateLetEncryptType;
import com.enonic.ec.kubernetes.crd.vhost.spec.SpecMapping;
import com.enonic.ec.kubernetes.operator.deployments.spec.ImmutableServiceSpecBuilder;
import com.enonic.ec.kubernetes.operator.kubectl.apply.ImmutableCommandApplyIngress;
import com.enonic.ec.kubernetes.operator.kubectl.apply.ImmutableCommandApplyService;
import com.enonic.ec.kubernetes.operator.kubectl.delete.ImmutableCommandDeleteService;
import com.enonic.ec.kubernetes.operator.vhosts.spec.ImmutableIngressSpec;

@Value.Immutable
public abstract class XpVHostApplyIngress
    extends Configuration
    implements CombinedCommandBuilder
{
    protected abstract KubernetesClient defaultClient();

    protected abstract OwnerReference ownerReference();

    protected abstract String namespace();

    protected abstract Map<String, String> defaultLabels(); // TODO: HANDLE

    protected abstract List<DiffSpec> diffs();

    @Override
    public void addCommands( final ImmutableCombinedCommand.Builder commandBuilder )
    {
        diffs().stream().filter( Diff::shouldAddOrModify ).forEach( d -> applyDiff( commandBuilder, d ) );
    }

    private void applyDiff( final ImmutableCombinedCommand.Builder commandBuilder, final DiffSpec diffSpec )
    {
        String vHostResourceName = vHostResourceName( diffSpec.newValue().get() );
        String host = diffSpec.newValue().get().host();

        boolean changeMappings = diffSpec.mappingsChanged().stream().anyMatch( Diff::shouldAddOrModifyOrRemove );

        if ( changeMappings )
        {
            // TODO: If only target was changed in mapping ignore
            diffSpec.mappingsChanged().stream().
                filter( Diff::shouldAddOrModify ).forEach( m -> {

                Map<String, String> serviceLabels = new HashMap<>( defaultLabels() );

                Map<String, String> serviceSelector = new HashMap<>();
                serviceSelector.put( cfgStr( "operator.deployment.xp.labels.pod.name" ), m.newValue().get().node() );

                commandBuilder.addCommand( ImmutableCommandApplyService.builder().
                    client( defaultClient() ).
                    ownerReference( ownerReference() ).
                    namespace( namespace() ).
                    name( mappingResourceName( vHostResourceName, host, m.newValue().get() ) ).
                    labels( serviceLabels ).
                    spec( ImmutableServiceSpecBuilder.builder().
                        selector( serviceSelector ).
                        putPorts( cfgStr( "operator.deployment.xp.port.main.name" ), cfgInt( "operator.deployment.xp.port.main.number" ) ).
                        build().
                        spec() ).
                    build() );
            } );

            diffSpec.mappingsChanged().stream().
                filter( Diff::shouldRemove ).forEach( m -> commandBuilder.addCommand( ImmutableCommandDeleteService.builder().
                client( defaultClient() ).
                namespace( namespace() ).
                name( mappingResourceName( vHostResourceName, host, m.oldValue().get() ) ).
                build() ) );

        }

        boolean changeCert = diffSpec.isNew() || diffSpec.certificateChanged();

        if ( changeMappings || changeCert )
        {
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
                diffSpec.newValue().get().mappings().stream().filter( m -> m.target().startsWith( "/admin" ) ).findAny();
            if ( adminPath.isPresent() )
            {
                cfgIfBool( "operator.extensions.ingress.adminStickySession.enabled", () -> ingressAnnotations.
                    put( "nginx.ingress.kubernetes.io/affinity", "cookie" ).
                    put( "nginx.ingress.kubernetes.io/session-cookie-name", "XPADMINCOOKIE" ).
                    put( "nginx.ingress.kubernetes.io/session-cookie-path", "/admin" ) );
            }

            cfgIfBool( "operator.extensions.ingress.externalDns.enabled", () -> ingressAnnotations.
                put( "external-dns.alpha.kubernetes.io/hostname", diffSpec.newValue().get().host() ). // TODO: Only allow subdomains
                put( "external-dns.alpha.kubernetes.io/ttl", cfgStr( "operator.extensions.ingress.externalDns.recordTTL" ) ) );

            ingressAnnotations.put( cfgStr( "dns.annotations.enable" ), diffSpec.newValue().get().host() );

            // Regular ingress
            ingressAnnotations.
                put( "kubernetes.io/ingress.class", "nginx" ).
                put( "ingress.kubernetes.io/rewrite-target", "/" ).
                put( "nginx.ingress.kubernetes.io/configuration-snippet", nginxConfigSnippet.toString() );

            SpecCertificate cert = diffSpec.newValue().get().certificate();
            if ( cert != null )
            {
                ingressAnnotations.put( "nginx.ingress.kubernetes.io/ssl-redirect", "true" );
                if ( cert.selfSigned() != null && cert.selfSigned() )
                {
                    ingressAnnotations.put( "cert-manager.io/cluster-issuer", cfgStr( "operator.certissuer.selfsigned" ) );
                }

                if ( cert.letsEncrypt() != null )
                {
                    if ( cert.letsEncrypt() == SpecCertificateLetEncryptType.STAGING )
                    {
                        ingressAnnotations.put( "cert-manager.io/cluster-issuer", cfgStr( "operator.certissuer.letsencrypt.staging" ) );
                    }
                    if ( cert.letsEncrypt() == SpecCertificateLetEncryptType.PROD )
                    {
                        ingressAnnotations.put( "cert-manager.io/cluster-issuer", cfgStr( "operator.certissuer.letsencrypt.prod" ) );
                    }
                }
            }

            commandBuilder.addCommand( ImmutableCommandApplyIngress.builder().
                client( defaultClient() ).
                ownerReference( ownerReference() ).
                namespace( namespace() ).
                name( vHostResourceName ).
                labels( defaultLabels() ).
                annotations( ingressAnnotations.build() ).
                spec( ImmutableIngressSpec.builder().
                    certificateSecretName( Optional.ofNullable( cert != null ? vHostResourceName : null ) ).
                    mappingResourceName( m -> mappingResourceName( vHostResourceName, host, m ) ).
                    vHostSpec( diffSpec.newValue().get() ).
                    build().
                    spec() ).
                build() );
        }
    }

    private String vHostResourceName( final Spec spec )
    {
        return spec.host().replace( "-", "--" ).replace( ".", "-" );
    }

    private String mappingResourceName( final String vHostResourceName, String host, final SpecMapping mapping )
    {
        return vHostResourceName + "-" + mapping.name( host );
    }
}
