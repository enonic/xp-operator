package com.enonic.ec.kubernetes.operator.commands;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.client.KubernetesClient;

import com.enonic.ec.kubernetes.common.Configuration;
import com.enonic.ec.kubernetes.common.commands.ImmutableCombinedKubernetesCommand;
import com.enonic.ec.kubernetes.deployment.diff.Diff;
import com.enonic.ec.kubernetes.deployment.diff.DiffVHost;
import com.enonic.ec.kubernetes.operator.commands.builders.spec.ImmutableIngressSpecBuilder;
import com.enonic.ec.kubernetes.operator.commands.builders.spec.ImmutableIssuerSpecBuilder;
import com.enonic.ec.kubernetes.operator.commands.builders.spec.ImmutableServiceSpecBuilder;
import com.enonic.ec.kubernetes.operator.commands.kubectl.apply.ImmutableCommandApplyIngress;
import com.enonic.ec.kubernetes.operator.commands.kubectl.apply.ImmutableCommandApplyIssuer;
import com.enonic.ec.kubernetes.operator.commands.kubectl.apply.ImmutableCommandApplyService;
import com.enonic.ec.kubernetes.operator.commands.kubectl.delete.ImmutableCommandDeleteIssuer;
import com.enonic.ec.kubernetes.operator.commands.kubectl.delete.ImmutableCommandDeleteService;
import com.enonic.ec.kubernetes.operator.crd.certmanager.issuer.IssuerClient;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@Value.Immutable
public abstract class CreateXpDeploymentVHost
    extends Configuration
    implements CommandBuilder
{
    protected abstract KubernetesClient defaultClient();

    protected abstract IssuerClient issuerClient();

    protected abstract OwnerReference ownerReference();

    protected abstract String namespace();

    protected abstract DiffVHost diffVHost();

    protected abstract Map<String, String> defaultLabels();

    @Override
    public void addCommands( ImmutableCombinedKubernetesCommand.Builder commandBuilder )
    {
        String vHostResourceName = diffVHost().newValue().get().vHostResourceName();

        boolean hasCert = diffVHost().newValue().get().certificate().isPresent();
        boolean changeCert = diffVHost().shouldAddOrModify() && diffVHost().certificateChanged();

        if ( changeCert )
        {
            if ( hasCert )
            {
                commandBuilder.addCommand( ImmutableCommandApplyIssuer.builder().
                    client( issuerClient() ).
                    ownerReference( ownerReference() ).
                    namespace( namespace() ).
                    name( vHostResourceName ).
                    labels( defaultLabels() ).
                    spec( ImmutableIssuerSpecBuilder.builder().
                        certificate( diffVHost().newValue().get().certificate().get() ).
                        build().
                        spec() ).
                    build() );
            }
            else
            {
                commandBuilder.addCommand( ImmutableCommandDeleteIssuer.builder().
                    client( issuerClient() ).
                    namespace( namespace() ).
                    name( vHostResourceName ).
                    build() );
            }
        }

        boolean changePaths = diffVHost().pathsChanged().stream().anyMatch( Diff::shouldAddOrModifyOrRemove );

        if ( changePaths )
        {

            diffVHost().pathsChanged().stream().
                filter( Diff::shouldAddOrModify ).forEach( p -> {
                Map<String, String> serviceLabels = new HashMap<>();
                serviceLabels.putAll( defaultLabels() );
                serviceLabels.putAll( p.newValue().get().getServiceSelectorLabels() );
                serviceLabels.putAll( p.newValue().get().vHostLabels() );

                commandBuilder.addCommand( ImmutableCommandApplyService.builder().
                    client( defaultClient() ).
                    ownerReference( ownerReference() ).
                    namespace( namespace() ).
                    name( p.newValue().get().pathResourceName() ).
                    labels( serviceLabels ).
                    spec( ImmutableServiceSpecBuilder.builder().
                        selector( p.newValue().get().getServiceSelectorLabels() ).
                        putPorts( cfgStr( "operator.deployment.xp.port.main.name" ), cfgInt( "operator.deployment.xp.port.main.number" ) ).
                        build().
                        spec() ).
                    build() );
            } );

            diffVHost().pathsChanged().stream().
                filter( Diff::shouldRemove ).forEach( p -> commandBuilder.addCommand( ImmutableCommandDeleteService.builder().
                client( defaultClient() ).
                namespace( namespace() ).
                name( p.oldValue().get().pathResourceName() ).
                build() ) );

        }

        if ( changePaths || changeCert )
        {
            Map<String, String> serviceAnnotations = new HashMap<>();
            serviceAnnotations.put( "kubernetes.io/ingress.class", "nginx" );
            serviceAnnotations.put( "ingress.kubernetes.io/rewrite-target", "/" );
            serviceAnnotations.put( "nginx.ingress.kubernetes.io/configuration-snippet",
                                    "proxy_set_header l5d-dst-override $service_name.$namespace.svc.cluster.local:$service_port;\n" +
                                        "      proxy_hide_header l5d-remote-ip;\n" + "      proxy_hide_header l5d-server-id;" );

            if ( hasCert )
            {
                serviceAnnotations.put( "nginx.ingress.kubernetes.io/ssl-redirect", "true" );
                serviceAnnotations.put( "certmanager.k8s.io/issuer", vHostResourceName );
            }

            commandBuilder.addCommand( ImmutableCommandApplyIngress.builder().
                client( defaultClient() ).
                ownerReference( ownerReference() ).
                namespace( namespace() ).
                name( vHostResourceName ).
                labels( defaultLabels() ).
                annotations( serviceAnnotations ).
                spec( ImmutableIngressSpecBuilder.builder().
                    certificateSecretName( Optional.ofNullable( hasCert ? vHostResourceName : null ) ).
                    vhost( diffVHost().newValue().get() ).
                    build().
                    spec() ).
                build() );
        }
    }
}
