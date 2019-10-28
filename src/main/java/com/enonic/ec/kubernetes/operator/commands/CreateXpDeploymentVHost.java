package com.enonic.ec.kubernetes.operator.commands;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.client.KubernetesClient;

import com.enonic.ec.kubernetes.common.Configuration;
import com.enonic.ec.kubernetes.common.ImmutableTuple;
import com.enonic.ec.kubernetes.common.Tuple;
import com.enonic.ec.kubernetes.common.commands.ImmutableCombinedKubernetesCommand;
import com.enonic.ec.kubernetes.deployment.vhost.VHost;
import com.enonic.ec.kubernetes.deployment.vhost.VHostPath;
import com.enonic.ec.kubernetes.deployment.xpdeployment.XpDeploymentResource;
import com.enonic.ec.kubernetes.operator.commands.builders.spec.ImmutableIngressSpecBuilder;
import com.enonic.ec.kubernetes.operator.commands.builders.spec.ImmutableIssuerSpecBuilder;
import com.enonic.ec.kubernetes.operator.commands.builders.spec.ImmutableServiceSpecBuilder;
import com.enonic.ec.kubernetes.operator.commands.kubectl.apply.ImmutableCommandApplyIngress;
import com.enonic.ec.kubernetes.operator.commands.kubectl.apply.ImmutableCommandApplyIssuer;
import com.enonic.ec.kubernetes.operator.commands.kubectl.apply.ImmutableCommandApplyService;
import com.enonic.ec.kubernetes.operator.commands.kubectl.delete.ImmutableCommandDeleteService;
import com.enonic.ec.kubernetes.operator.commands.plan.XpVHostDeploymentPlan;
import com.enonic.ec.kubernetes.operator.crd.certmanager.issuer.IssuerClient;

@Value.Immutable
public abstract class CreateXpDeploymentVHost
    extends Configuration
    implements CommandBuilder
{
    protected abstract KubernetesClient defaultClient();

    protected abstract IssuerClient issuerClient();

    protected abstract OwnerReference ownerReference();

    protected abstract String namespace();

    protected abstract XpVHostDeploymentPlan vHostPlan();

    protected abstract XpDeploymentResource resource();

    protected abstract Map<String, String> defaultLabels();

    protected abstract Function<VHost, String> vHostResourceName();

    protected abstract Function<Tuple<VHost, VHostPath>, String> pathResourceName();

    @Override
    public void addCommands( ImmutableCombinedKubernetesCommand.Builder commandBuilder )
        throws Exception
    {
        String vHostResourceName = vHostResourceName().apply( vHostPlan().vhost() );

        if ( vHostPlan().changeIssuer() )
        {
            commandBuilder.addCommand( ImmutableCommandApplyIssuer.builder().
                client( issuerClient() ).
                ownerReference( ownerReference() ).
                namespace( namespace() ).
                name( vHostResourceName ).
                labels( defaultLabels() ).
                spec( ImmutableIssuerSpecBuilder.builder().
                    certificate( vHostPlan().vhost().certificate().get() ).
                    build().
                    spec() ).
                build() );
        }

        // TODO: Delete issuer

        if ( vHostPlan().changeIngress() )
        {
            for ( VHostPath path : vHostPlan().newPaths() )
            {
                Map<String, String> serviceLabels = new HashMap<>();
                serviceLabels.putAll( defaultLabels() );
                serviceLabels.putAll( path.getServiceSelectorLabels() );
                serviceLabels.putAll( path.getVHostLabels( vHostPlan().vhost() ) );
                commandBuilder.addCommand( ImmutableCommandApplyService.builder().
                    client( defaultClient() ).
                    ownerReference( ownerReference() ).
                    namespace( namespace() ).
                    name( pathResourceName().apply( ImmutableTuple.of( vHostPlan().vhost(), path ) ) ).
                    labels( serviceLabels ).
                    spec( ImmutableServiceSpecBuilder.builder().
                        selector( path.getServiceSelectorLabels() ).
                        putPorts( cfgStr( "operator.deployment.xp.port.main.name" ), cfgInt( "operator.deployment.xp.port.main.number" ) ).
                        build().
                        spec() ).
                    build() );
            }

            for ( VHostPath path : vHostPlan().pathsToDelete() )
            {
                commandBuilder.addCommand( ImmutableCommandDeleteService.builder().
                    client( defaultClient() ).
                    namespace( namespace() ).
                    name( pathResourceName().apply( ImmutableTuple.of( vHostPlan().vhost(), path ) ) ).
                    build() );
            }

            Map<String, String> serviceAnnotations = new HashMap<>();
            serviceAnnotations.put( "kubernetes.io/ingress.class", "nginx" );
            serviceAnnotations.put( "ingress.kubernetes.io/rewrite-target", "/" );
            serviceAnnotations.put( "nginx.ingress.kubernetes.io/configuration-snippet",
                                    "proxy_set_header l5d-dst-override $service_name.$namespace.svc.cluster.local:$service_port;\n" +
                                        "      proxy_hide_header l5d-remote-ip;\n" + "      proxy_hide_header l5d-server-id;" );

            if ( vHostPlan().vhost().certificate().isPresent() )
            {
                serviceAnnotations.put( "nginx.ingress.kubernetes.io/ssl-redirect", "true" );
                serviceAnnotations.put( "certmanager.k8s.io/issuer", vHostResourceName );
            }
            Optional<String> certSecret =
                vHostPlan().vhost().certificate().isPresent() ? Optional.of( vHostResourceName ) : Optional.empty();
            commandBuilder.addCommand( ImmutableCommandApplyIngress.builder().
                client( defaultClient() ).
                ownerReference( ownerReference() ).
                namespace( namespace() ).
                name( vHostResourceName ).
                labels( defaultLabels() ).
                annotations( serviceAnnotations ).
                spec( ImmutableIngressSpecBuilder.builder().
                    certificateSecretName( certSecret ).
                    vhost( vHostPlan().vhost() ).
                    resource( resource() ).
                    build().
                    spec() ).
                build() );
        }
    }
}
