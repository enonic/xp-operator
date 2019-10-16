package com.enonic.ec.kubernetes.operator.commands;

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
import com.enonic.ec.kubernetes.operator.commands.apply.ImmutableCommandApplyConfigMap;
import com.enonic.ec.kubernetes.operator.commands.apply.ImmutableCommandApplyNamespace;
import com.enonic.ec.kubernetes.operator.commands.apply.ImmutableCommandApplyPodDisruptionBudget;
import com.enonic.ec.kubernetes.operator.commands.apply.ImmutableCommandApplyService;
import com.enonic.ec.kubernetes.operator.commands.apply.ImmutableCommandApplyStatefulSet;
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
            createNonClusteredDeployment( namespaceName, resource().getSpec().fullAppName(), singleNode.get(),
                                          resource().getSpec().defaultLabels(), commandBuilder );
        }
        else
        {
            throw new Exception( "Cluster deployment not implemented" );
        }

        return commandBuilder.build().execute();
    }

    private void createNonClusteredDeployment( String namespace, String name, XpDeploymentResourceSpecNode node, Map<String, String> labels,
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
        commandBuilder.addCommand( ImmutableCommandApplyConfigMap.builder().
            client( defaultClient() ).
            ownerReference( ownerReference() ).
            name( name ).
            namespace( namespace ).
            labels( labels ).
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

        // Add stateful set
        commandBuilder.addCommand( ImmutableCommandApplyStatefulSet.builder().
            client( defaultClient() ).
            ownerReference( ownerReference() ).
            name( name ).
            namespace( namespace ).
            labels( labels ).
            spec( ImmutableStatefulSetSpecNonClusteredSpec.builder().
                podLabels( labels ).
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
