package com.enonic.ec.kubernetes.operator.deployments;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.client.KubernetesClient;

import com.enonic.ec.kubernetes.common.Configuration;
import com.enonic.ec.kubernetes.common.Diff;
import com.enonic.ec.kubernetes.common.commands.CombinedCommandBuilder;
import com.enonic.ec.kubernetes.common.commands.ImmutableCombinedKubernetesCommand;
import com.enonic.ec.kubernetes.crd.deployment.XpDeploymentNamingHelper;
import com.enonic.ec.kubernetes.crd.deployment.diff.DiffSpec;
import com.enonic.ec.kubernetes.crd.deployment.diff.DiffSpecNode;
import com.enonic.ec.kubernetes.crd.deployment.spec.Spec;
import com.enonic.ec.kubernetes.crd.deployment.spec.SpecNode;
import com.enonic.ec.kubernetes.crd.issuer.client.IssuerClient;
import com.enonic.ec.kubernetes.operator.deployments.config.ConfigBuilderNode;
import com.enonic.ec.kubernetes.operator.deployments.config.ImmutableConfigBuilderCluster;
import com.enonic.ec.kubernetes.operator.deployments.config.ImmutableConfigBuilderNonClustered;
import com.enonic.ec.kubernetes.operator.deployments.volumes.ImmutableVolumeBuilderNfs;
import com.enonic.ec.kubernetes.operator.deployments.volumes.ImmutableVolumeBuilderStd;
import com.enonic.ec.kubernetes.operator.deployments.volumes.VolumeBuilder;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@Value.Immutable
public abstract class CreateXpDeployment
    extends Configuration
    implements CombinedCommandBuilder
{
    protected abstract KubernetesClient defaultClient();

    protected abstract IssuerClient issuerClient();

    protected abstract String deploymentName();

    protected abstract Map<String, String> defaultLabels();

    protected abstract DiffSpec diffSpec();

    protected abstract OwnerReference ownerReference();

    protected abstract XpDeploymentNamingHelper namingHelper();

    @Value.Derived
    protected Spec spec()
    {
        return diffSpec().newValue().get();
    }

    @Override
    public void addCommands( ImmutableCombinedKubernetesCommand.Builder commandBuilder )
    {
        String deploymentName = deploymentName();
        String namespaceName = namingHelper().defaultNamespaceName();
        String serviceName = namingHelper().defaultResourceName();
        Map<String, String> defaultLabels = defaultLabels();

        boolean useNfs = cfgBool( "operator.deployment.xp.volume.shared.nfs.enabled" );

        VolumeBuilder volumeBuilder;
        Optional<String> defaultSharedStorageName;
        Optional<Quantity> defaultSharedStorageSize;

        if ( useNfs )
        {
            defaultSharedStorageName = Optional.empty();
            defaultSharedStorageSize = Optional.empty();
            volumeBuilder = ImmutableVolumeBuilderNfs.builder().
                deploymentName( deploymentName ).
                nfsServer( cfgStr( "operator.deployment.xp.volume.shared.nfs.server" ) ).
                nfsPath( cfgStr( "operator.deployment.xp.volume.shared.nfs.path" ) ).
                build();
        }
        else
        {
            defaultSharedStorageName = Optional.of( namingHelper().defaultResourceNameWithPreFix( "shared" ) );
            defaultSharedStorageSize = Optional.of( spec().nodesSharedDisk() );
            volumeBuilder = ImmutableVolumeBuilderStd.builder().
                deploymentName( deploymentName ).
                sharedStoragePVCName( defaultSharedStorageName.get() ).
                build();
        }

        boolean isClustered = spec().isClustered();

        Function<SpecNode, Integer> defaultMinimumAvailable;
        ConfigBuilderNode configBuilder;

        if ( isClustered )
        {
            defaultMinimumAvailable = ( n ) -> ( n.replicas() / 2 ) + 1;

            SpecNode masterNode = spec().nodes().values().stream().filter( SpecNode::isMasterNode ).findAny().get();
            int minimumMasterNodes = defaultMinimumAvailable.apply( masterNode );

            SpecNode dataNode = spec().nodes().values().stream().filter( SpecNode::isDataNode ).findAny().get();
            int minimumDataNodes = defaultMinimumAvailable.apply( dataNode );

            configBuilder = ImmutableConfigBuilderCluster.builder().
                baseConfig( diffSpec().newValue().get().nodesSharedConfig() ).
                namespace( namespaceName ).
                serviceName( serviceName ).
                clusterName( deploymentName() ).
                minimumMasterNodes( Math.max( 1, minimumMasterNodes ) ).
                minimumDataNodes( Math.max( 1, minimumDataNodes ) ).
                // TODO: XP is not happy with this, do some research
                // discoveryHosts( Arrays.asList( String.join( ".", serviceName, namespaceName, "svc.cluster.local" ) ) ).
                    discoveryHosts( getAllMasterNodeDNS( serviceName ) ).
                    build();

        }
        else
        {
            defaultMinimumAvailable = ( n ) -> 0; // This has to be 0 to be able to update XP
            configBuilder = ImmutableConfigBuilderNonClustered.builder().
                baseConfig( diffSpec().newValue().get().nodesSharedConfig() ).
                build();
        }

        if ( diffSpec().isNew() )
        {
            ImmutableCreateXpDeploymentBase.builder().
                defaultClient( defaultClient() ).
                ownerReference( ownerReference() ).
                namespace( namespaceName ).
                isClustered( isClustered ).
                serviceName( serviceName ).
                sharedStorageName( defaultSharedStorageName ).
                sharedStorageSize( defaultSharedStorageSize ).
                defaultLabels( defaultLabels ).
                build().
                addCommands( commandBuilder );
        }

        Predicate<DiffSpecNode> createOrUpdate = n -> diffSpec().versionChanged() || diffSpec().enabledChanged() || n.shouldAddOrModify();

        // Create / Update Nodes
        diffSpec().nodesChanged().stream().
            filter( createOrUpdate ).
            forEach( diffSpecNode -> ImmutableCreateXpDeploymentNode.builder().
                defaultClient( defaultClient() ).
                ownerReference( ownerReference() ).
                namespace( namespaceName ).
                serviceName( serviceName ).
                nodeShortName( diffSpecNode.name() ).
                nodeFullName( namingHelper().defaultResourceName( diffSpecNode.name() ) ).
                diffSpec( diffSpec() ).
                diffSpecNode( diffSpecNode ).
                defaultLabels( defaultLabels ).
                configBuilder( configBuilder ).
                deploymentName( deploymentName ).
                volumeBuilder( volumeBuilder ).
                minimumAvailable( defaultMinimumAvailable.apply( diffSpecNode.newValue().get() ) ).
                nodeSharedConfigChanged( diffSpec().nodeSharedConfigChanged() ).
                build().
                addCommands( commandBuilder ) );

        // Remove old nodes
        diffSpec().nodesChanged().stream().
            filter( Diff::shouldRemove ).
            forEach( diff -> ImmutableDeleteXpDeploymentNode.builder().
                defaultClient( defaultClient() ).
                namespace( namespaceName ).
                nodeName( namingHelper().defaultResourceName( diff.name() ) ).
                build().
                addCommands( commandBuilder ) );
    }

    private List<String> getAllMasterNodeDNS( String serviceName )
    {
        List<String> res = new LinkedList<>();
        for ( Map.Entry<String, SpecNode> node : spec().nodes().entrySet() )
        {
            if ( node.getValue().isMasterNode() )
            {
                String tmp = namingHelper().defaultResourceName( node.getKey() );
                for ( int i = 0; i < node.getValue().replicas(); i++ )
                {
                    res.add( tmp + "-" + i + "." + serviceName );
                }
            }
        }
        return res;
    }
}
