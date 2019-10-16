package com.enonic.ec.kubernetes.operator.commands;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.client.KubernetesClient;

import com.enonic.ec.kubernetes.common.commands.Command;
import com.enonic.ec.kubernetes.common.commands.ImmutableCombinedCommand;
import com.enonic.ec.kubernetes.deployment.xpdeployment.NodeType;
import com.enonic.ec.kubernetes.deployment.xpdeployment.XpDeploymentResource;
import com.enonic.ec.kubernetes.deployment.xpdeployment.XpDeploymentResourceSpecNode;
import com.enonic.ec.kubernetes.deployment.xpdeployment.XpDeploymentResourceSpecVhost;
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

        Optional<XpDeploymentResourceSpecNode> singleNode =
            resource().getSpec().nodes().stream().filter( n -> n.type() == NodeType.STANDALONE ).findAny();

        if ( singleNode.isPresent() )
        {
            createNonClusteredDeployment( namespaceName, resource().getSpec().fullAppName(), singleNode.get(), resource().getSpec().vhost(),
                                          resource().getSpec().defaultLabels(), commandBuilder );
        }
        else
        {
            throw new Exception( "Cluster deployment not implemented" );
        }

        // Add service for each vhost
        if ( resource().getSpec().vhost() != null )
        {
            for ( Map.Entry<String, XpDeploymentResourceSpecVhost> e : resource().getSpec().vhost().entrySet() )
            {
                createVhostServices( commandBuilder, namespaceName, resource().getSpec().fullAppName(), e.getKey(), e.getValue(),
                                     resource().getSpec().defaultLabels() );
            }
        }

        return commandBuilder.build().execute();
    }

    private void createNonClusteredDeployment( String namespace, String name, XpDeploymentResourceSpecNode node,
                                               Map<String, XpDeploymentResourceSpecVhost> vHosts, Map<String, String> labels,
                                               ImmutableCombinedCommand.Builder commandBuilder )
    {
        // Add pod disruption budget
        commandBuilder.addCommand( ImmutableCommandApplyPodDisruptionBudget.builder().
            client( defaultClient() ).
            ownerReference( ownerReference() ).
            name( name ).
            namespace( namespace ).
            labels( labels ).
            spec( ImmutablePodDisruptionBudgetSpecBuilder.builder().
                minAvailable( 0 ). // Since we only have 1 node we have to be able to turn it off during upgrades
                matchLabels( labels ).
                build().
                execute() ).
            build() );

        // Add Config map
        Map<String, String> configMap = new HashMap<>();
        configMap.putAll( node.config() );

        boolean vHostEnabled = node.vhost() != null && node.vhost().size() > 0;
        if ( vHostEnabled )
        {
            StringBuilder builder = new StringBuilder( "enabled = true" ).append( "\n" );

            node.vhost().forEach( v -> {
                builder.append( vHosts.get( v ).config() );
                builder.append( "\n" );
            } );
            configMap.put( "com.enonic.xp.web.vhost.cfg", builder.toString() );
        }

        commandBuilder.addCommand( ImmutableCommandApplyConfigMap.builder().
            client( defaultClient() ).
            ownerReference( ownerReference() ).
            name( name ).
            namespace( namespace ).
            labels( labels ).
            data( configMap ).
            build() );

        List<EnvVar> podEnv = new LinkedList<>();
        if ( node.env() != null )
        {
            for ( Map.Entry<String, String> e : node.env().entrySet() )
            {
                podEnv.add( new EnvVar( e.getKey(), e.getValue(), null ) );
            }
        }

        // Add stateful set
        Map<String, String> podLabels = new HashMap<>();
        podLabels.putAll( labels );
        if ( vHostEnabled )
        {
            node.vhost().forEach( v -> {
                Map.Entry<String, String> e = vhostLabel( name, v );
                podLabels.put( e.getKey(), e.getValue() );
            } );
        }

        commandBuilder.addCommand( ImmutableCommandApplyStatefulSet.builder().
            client( defaultClient() ).
            ownerReference( ownerReference() ).
            name( name ).
            namespace( namespace ).
            labels( labels ).
            spec( ImmutableStatefulSetSpecNonClusteredSpec.builder().
                podLabels( podLabels ).
                replicas( resource().getSpec().enabled() ? node.replicas() : 0 ).
                podImage( podImageName() ).
                podEnv( podEnv ).
                podResources( Map.of( "cpu", node.resources().cpu(), "memory", node.resources().memory() ) ).
                repoDiskSize( node.resources().disks().get( "repo" ) ).
                snapshotDiskSize( node.resources().disks().get( "snapshots" ) ).
                serviceName( name ).
                configMapName( name ).
                build().
                execute() ).
            build() );

        // Add service
        commandBuilder.addCommand( ImmutableCommandApplyService.builder().
            client( defaultClient() ).
            ownerReference( ownerReference() ).
            name( name ).
            namespace( namespace ).
            labels( labels ).
            spec( ImmutableServiceSpecBuilder.builder().
                putAllSelector( labels ).
                build().
                execute() ).
            build() );
    }

    private static Map.Entry<String, String> vhostLabel( String appFullName, String vhostName )
    {
        return new AbstractMap.SimpleEntry<String, String>( "xp.vhost." + vhostName, appFullName );
    }

    private void createVhostServices( ImmutableCombinedCommand.Builder commandBuilder, String namespace, String appFullName,
                                      String vhostName, XpDeploymentResourceSpecVhost vhost, Map<String, String> labels )
        throws Exception
    {
        String vHostResourceName = appFullName + "-vhost-" + vhostName;
        Map.Entry<String, String> vhostSelector = vhostLabel( appFullName, vhostName );
        commandBuilder.addCommand( ImmutableCommandApplyService.builder().
            client( defaultClient() ).
            ownerReference( ownerReference() ).
            name( vHostResourceName ).
            namespace( namespace ).
            labels( labels ).
            spec( ImmutableServiceSpecBuilder.builder().
                putAllSelector( Map.of( vhostSelector.getKey(), vhostSelector.getValue() ) ).
                build().
                execute() ).
            build() );

        commandBuilder.addCommand( ImmutableCommandApplyIssuer.builder().
            client( issuerClient() ).
            ownerReference( ownerReference() ).
            name( vHostResourceName ).
            namespace( namespace ).
            labels( labels ).
            spec( ImmutableIssuerSpecBuilder.builder().
                certificate( vhost.certificate() ).
                build().
                execute() ).
            build() );

        commandBuilder.addCommand( ImmutableCommandApplyIngress.builder().
            client( defaultClient() ).
            ownerReference( ownerReference() ).
            name( vHostResourceName ).
            namespace( namespace ).
            labels( labels ).
            annotations( Map.of( "kubernetes.io/ingress.class", "nginx", "ingress.kubernetes.io/rewrite-target", "/",
                                 "nginx.ingress.kubernetes.io/configuration-snippet",
                                 "proxy_set_header l5d-dst-override $service_name.$namespace.svc.cluster.local:$service_port;\n" +
                                     "      proxy_hide_header l5d-remote-ip;\n" + "      proxy_hide_header l5d-server-id;",
                                 "nginx.ingress.kubernetes.io/ssl-redirect", "true", "certmanager.k8s.io/issuer", vHostResourceName ) ).
            spec( ImmutableIngressSpecBuilder.builder().
                certificateSecretName( vHostResourceName ).
                serviceName( vHostResourceName ).
                servicePort( 8080 ).
                vhost( vhost ).
                build().
                execute() ).
            build() );
    }

    private void createXpCluster( String namespace, ImmutableCombinedCommand.Builder commandBuilder )
    {

//
//        commandBuilder.add( CommandApplyService.newBuilder().
//            client( defaultClient ).
//            ownerReference( ownerReference ).
//            name( resource.getSpec().getFullAppName() ).
//            namespace( namespaceName ).
//            labels( resource.getSpec().getDefaultLabels() ).
//            spec( null ). // TODO: Fix
//            build() );
//
//        commandBuilder.add( CommandApplyIssuer.newBuilder().
//            client( issuerClient ).
//            ownerReference( ownerReference ).
//            name( resource.getSpec().getFullAppName() ).
//            namespace( namespaceName ).
//            labels( resource.getSpec().getDefaultLabels() ).
//            spec( null ). // TODO: Fix
//            build() );
//
//        commandBuilder.add( CommandApplyIngress.newBuilder().
//            client( defaultClient ).
//            ownerReference( ownerReference ).
//            name( resource.getSpec().getFullAppName() ).
//            namespace( namespaceName ).
//            labels( resource.getSpec().getDefaultLabels() ).
//            spec( null ). // TODO: Fix
//            build() );
    }
}
