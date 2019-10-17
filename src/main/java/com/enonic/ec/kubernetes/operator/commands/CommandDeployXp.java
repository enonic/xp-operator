package com.enonic.ec.kubernetes.operator.commands;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.client.KubernetesClient;

import com.enonic.ec.kubernetes.common.commands.Command;
import com.enonic.ec.kubernetes.common.commands.ImmutableCombinedCommand;
import com.enonic.ec.kubernetes.deployment.xpdeployment.XpDeploymentResource;
import com.enonic.ec.kubernetes.deployment.xpdeployment.XpDeploymentResourceSpecNode;
import com.enonic.ec.kubernetes.operator.commands.apply.ImmutableCommandApplyConfigMap;
import com.enonic.ec.kubernetes.operator.commands.apply.ImmutableCommandApplyIngress;
import com.enonic.ec.kubernetes.operator.commands.apply.ImmutableCommandApplyIssuer;
import com.enonic.ec.kubernetes.operator.commands.apply.ImmutableCommandApplyNamespace;
import com.enonic.ec.kubernetes.operator.commands.apply.ImmutableCommandApplyPodDisruptionBudget;
import com.enonic.ec.kubernetes.operator.commands.apply.ImmutableCommandApplyService;
import com.enonic.ec.kubernetes.operator.commands.apply.ImmutableCommandApplyStatefulSet;
import com.enonic.ec.kubernetes.operator.commands.builders.ImmutableIngressSpecBuilder;
import com.enonic.ec.kubernetes.operator.commands.builders.ImmutableIssuerSpecBuilder;
import com.enonic.ec.kubernetes.operator.commands.builders.ImmutablePodDisruptionBudgetSpecBuilder;
import com.enonic.ec.kubernetes.operator.commands.builders.ImmutableServiceSpecBuilder;
import com.enonic.ec.kubernetes.operator.commands.builders.ImmutableStatefulSetSpecNonClusteredSpec;
import com.enonic.ec.kubernetes.operator.crd.certmanager.issuer.IssuerClientProducer;

@Value.Immutable
public abstract class CommandDeployXp
    implements Command<Void>
{
    protected abstract KubernetesClient defaultClient();

    protected abstract IssuerClientProducer.IssuerClient issuerClient();

    protected abstract XpDeploymentResource resource();

    @Value.Derived
    protected OwnerReference ownerReference()
    {
        return new OwnerReference( resource().getApiVersion(), true, true, resource().getKind(), resource().getMetadata().getName(),
                                   resource().getMetadata().getUid() );
    }

    @Value.Derived
    protected String podImageName()
    {
        return "gbbirkisson/xp:" + resource().getSpec().xpVersion() + "-ubuntu"; // TODO: Fix
    }

    @Override
    public Void execute()
        throws Exception
    {
        String namespaceName = resource().getSpec().fullProjectName();

        ImmutableCombinedCommand.Builder commandBuilder = ImmutableCombinedCommand.builder();

        commandBuilder.addCommand( ImmutableCommandApplyNamespace.builder().
            client( defaultClient() ).
            ownerReference( ownerReference() ).
            name( namespaceName ).
            build() );

        // TODO: Create network policy

        Vhosts vhosts = ImmutableVhostBuilder.builder().
            nodes( resource().getSpec().nodes() ).
            certificates( resource().getSpec().vHostCertificates() ).
            build().
            execute();

        for ( XpDeploymentResourceSpecNode node : resource().getSpec().nodes() )
        {
            createDeployNodeCommands( commandBuilder, namespaceName, resource().getSpec().fullAppName(),
                                      resource().getSpec().defaultLabels(), node, vhosts );
        }

        for ( Vhost vhost : vhosts )
        {
            createDeployVhostCommands( commandBuilder, namespaceName, resource().getSpec().fullAppName(),
                                       resource().getSpec().defaultLabels(), vhost );
        }

        // TODO clean up old ingress, services and certs

        return commandBuilder.build().execute();
    }

    private void createDeployNodeCommands( final ImmutableCombinedCommand.Builder commandBuilder, final String namespaceName,
                                           final String fullAppName, final Map<String, String> defaultLabels,
                                           final XpDeploymentResourceSpecNode node, final Vhosts vhosts )
    {
        // TODO: Handle all nodetypes
        commandBuilder.addCommand( ImmutableCommandApplyPodDisruptionBudget.builder().
            client( defaultClient() ).
            ownerReference( ownerReference() ).
            namespace( namespaceName ).
            name( fullAppName ).
            labels( defaultLabels ).
            spec( ImmutablePodDisruptionBudgetSpecBuilder.builder().
                minAvailable( 0 ). // Since we only have 1 node we have to be able to turn it off during upgrades
                matchLabels( defaultLabels ).
                build().
                execute() ).
            build() );

        commandBuilder.addCommand( ImmutableCommandApplyConfigMap.builder().
            client( defaultClient() ).
            ownerReference( ownerReference() ).
            namespace( namespaceName ).
            name( fullAppName ).
            labels( defaultLabels ).
            data( node.config() ).
            build() );

        List<EnvVar> podEnv = new LinkedList<>();
        if ( node.env() != null )
        {
            for ( Map.Entry<String, String> e : node.env().entrySet() )
            {
                podEnv.add( new EnvVar( e.getKey(), e.getValue(), null ) );
            }
        }

        commandBuilder.addCommand( ImmutableCommandApplyStatefulSet.builder().
            client( defaultClient() ).
            ownerReference( ownerReference() ).
            namespace( namespaceName ).
            name( fullAppName ).
            labels( defaultLabels ).
            spec( ImmutableStatefulSetSpecNonClusteredSpec.builder().
                podLabels( defaultLabels ).
                extraPodLabels( node.extraLabels() ).
                replicas( resource().getSpec().enabled() ? node.replicas() : 0 ).
                podImage( podImageName() ).
                podEnv( podEnv ).
                podResources( Map.of( "cpu", node.resources().cpu(), "memory", node.resources().memory() ) ).
                repoDiskSize( node.resources().disks().get( "repo" ) ).
                snapshotDiskSize( node.resources().disks().get( "snapshots" ) ).
                serviceName( fullAppName ).
                configMapName( fullAppName ).
                build().
                execute() ).
            build() );

    }

    private void createDeployVhostCommands( final ImmutableCombinedCommand.Builder commandBuilder, final String namespaceName,
                                            final String fullAppName, final Map<String, String> defaultLabels, final Vhost vhost )
        throws Exception
    {
        if ( vhost.certificate() != null )
        {
            commandBuilder.addCommand( ImmutableCommandApplyIssuer.builder().
                client( issuerClient() ).
                ownerReference( ownerReference() ).
                namespace( namespaceName ).
                name( vhost.getVhostResourceName( fullAppName ) ).
                labels( defaultLabels ).
                spec( ImmutableIssuerSpecBuilder.builder().
                    certificate( vhost.certificate() ).
                    build().
                    execute() ).
                build() );
        }

        for ( VhostPath path : vhost.vhostPaths() )
        {
            Map<String, String> serviceLabels = new HashMap<>();
            serviceLabels.putAll( defaultLabels );
            serviceLabels.putAll( path.getVhostLabel() );
            commandBuilder.addCommand( ImmutableCommandApplyService.builder().
                client( defaultClient() ).
                ownerReference( ownerReference() ).
                namespace( namespaceName ).
                name( path.getPathResourceName( fullAppName, vhost.host() ) ).
                labels( serviceLabels ).
                spec( ImmutableServiceSpecBuilder.builder().
                    selector( path.getVhostLabel() ).
                    build().
                    execute() ).
                build() );
        }

        Map<String, String> serviceAnnotations = new HashMap<>();
        serviceAnnotations.put( "kubernetes.io/ingress.class", "nginx" );
        serviceAnnotations.put( "ingress.kubernetes.io/rewrite-target", "/" );
        serviceAnnotations.put( "nginx.ingress.kubernetes.io/configuration-snippet",
                                "proxy_set_header l5d-dst-override $service_name.$namespace.svc.cluster.local:$service_port;\n" +
                                    "      proxy_hide_header l5d-remote-ip;\n" + "      proxy_hide_header l5d-server-id;" );

        if ( vhost.certificate() != null )
        {
            serviceAnnotations.put( "nginx.ingress.kubernetes.io/ssl-redirect", "true" );
            serviceAnnotations.put( "certmanager.k8s.io/issuer", vhost.getVhostResourceName( fullAppName ) );
        }

        commandBuilder.addCommand( ImmutableCommandApplyIngress.builder().
            client( defaultClient() ).
            ownerReference( ownerReference() ).
            namespace( namespaceName ).
            name( vhost.getVhostResourceName( fullAppName ) ).
            labels( defaultLabels ).
            annotations( serviceAnnotations ).
            spec( ImmutableIngressSpecBuilder.builder().
                certificateSecretName( vhost.certificate() != null ? vhost.getVhostResourceName( fullAppName ) : null ).
                vhost( vhost ).
                appFullName( fullAppName ).
                build().
                execute() ).
            build() );

    }
}
