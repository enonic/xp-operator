package com.enonic.ec.kubernetes.operator.commands;

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
import com.enonic.ec.kubernetes.common.commands.ImmutableCombinedKubernetesCommand;
import com.enonic.ec.kubernetes.crd.deployment.ImmutableXpDeploymentNamingHelper;
import com.enonic.ec.kubernetes.crd.deployment.XpDeploymentNamingHelper;
import com.enonic.ec.kubernetes.crd.deployment.XpDeploymentResource;
import com.enonic.ec.kubernetes.crd.deployment.diff.DiffSpec;
import com.enonic.ec.kubernetes.crd.deployment.diff.DiffSpecNode;
import com.enonic.ec.kubernetes.crd.deployment.diff.ImmutableDiffSpec;
import com.enonic.ec.kubernetes.crd.deployment.spec.SpecNode;
import com.enonic.ec.kubernetes.operator.commands.builders.config.ConfigBuilder;
import com.enonic.ec.kubernetes.operator.commands.builders.config.ImmutableConfigBuilderCluster;
import com.enonic.ec.kubernetes.operator.commands.builders.config.ImmutableConfigBuilderNonClustered;
import com.enonic.ec.kubernetes.operator.commands.builders.spec.volumes.ImmutableVolumeBuilderNfs;
import com.enonic.ec.kubernetes.operator.commands.builders.spec.volumes.ImmutableVolumeBuilderStd;
import com.enonic.ec.kubernetes.operator.commands.builders.spec.volumes.VolumeBuilder;
import com.enonic.ec.kubernetes.operator.crd.certmanager.issuer.client.IssuerClient;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@Value.Immutable
public abstract class CreateXpDeployment
    extends Configuration
    implements CommandBuilder
{
    protected abstract KubernetesClient defaultClient();

    protected abstract IssuerClient issuerClient();

    protected abstract Optional<XpDeploymentResource> oldResource();

    protected abstract XpDeploymentResource newResource();

    private XpDeploymentResource resource()
    {
        return newResource();
    }

    private OwnerReference ownerReference()
    {
        return new OwnerReference( resource().getApiVersion(), true, true, resource().getKind(), resource().getMetadata().getName(),
                                   resource().getMetadata().getUid() );
    }

    @Value.Derived
    protected XpDeploymentNamingHelper namingHelper()
    {
        return ImmutableXpDeploymentNamingHelper.builder().spec( resource().getSpec() ).build();
    }

    @Override
    public void addCommands( ImmutableCombinedKubernetesCommand.Builder commandBuilder )
    {
        String deploymentName = resource().getSpec().deploymentName();
        String namespaceName = namingHelper().defaultNamespaceName();
        String serviceName = namingHelper().defaultResourceName();
        Map<String, String> defaultLabels = resource().getSpec().defaultLabels();

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
            defaultSharedStorageSize = Optional.of( resource().getSpec().sharedDisk() );
            volumeBuilder = ImmutableVolumeBuilderStd.builder().
                deploymentName( deploymentName ).
                sharedStoragePVCName( defaultSharedStorageName.get() ).
                build();
        }

        DiffSpec diffSpec = ImmutableDiffSpec.builder().
            oldValue( oldResource().map( XpDeploymentResource::getSpec ) ).
            newValue( newResource().getSpec() ).
            build();

        boolean isClustered = resource().getSpec().isClustered();

        Function<SpecNode, Integer> defaultMinimumAvailable;
        ConfigBuilder configBuilder;

        if ( resource().getSpec().isClustered() )
        {
            defaultMinimumAvailable = ( n ) -> ( n.replicas() / 2 ) + 1;

            SpecNode masterNode = resource().getSpec().nodes().stream().filter( SpecNode::isMasterNode ).findAny().get();
            int minimumMasterNodes = defaultMinimumAvailable.apply( masterNode );

            SpecNode dataNode = resource().getSpec().nodes().stream().filter( SpecNode::isDataNode ).findAny().get();
            int minimumDataNodes = defaultMinimumAvailable.apply( dataNode );

            configBuilder = ImmutableConfigBuilderCluster.builder().
                namespace( namespaceName ).
                serviceName( serviceName ).
                clusterName( resource().getSpec().deploymentName() ).
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
            configBuilder = ImmutableConfigBuilderNonClustered.builder().build();
        }

        if ( diffSpec.isNew() )
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

        Predicate<DiffSpecNode> createOrUpdate = n -> diffSpec.versionChanged() || diffSpec.enabledChanged() || n.shouldAddOrModify();

        // Create / Update Nodes
        diffSpec.nodesChanged().stream().
            filter( createOrUpdate ).
            forEach( diffSpecNode -> ImmutableCreateXpDeploymentNode.builder().
                defaultClient( defaultClient() ).
                ownerReference( ownerReference() ).
                namespace( namespaceName ).
                serviceName( serviceName ).
                nodeName( namingHelper().defaultResourceName( diffSpecNode.newValue().get() ) ).
                diffSpec( diffSpec ).
                diffSpecNode( diffSpecNode ).
                defaultLabels( defaultLabels ).
                configBuilder( configBuilder ).
                deploymentName( deploymentName ).
                volumeBuilder( volumeBuilder ).
                minimumAvailable( defaultMinimumAvailable.apply( diffSpecNode.newValue().get() ) ).
                build().
                addCommands( commandBuilder ) );

        // Remove old nodes
        diffSpec.nodesChanged().stream().
            filter( Diff::shouldRemove ).
            forEach( diff -> ImmutableDeleteXpDeploymentNode.builder().
                defaultClient( defaultClient() ).
                namespace( namespaceName ).
                nodeName( namingHelper().defaultResourceName( diff.oldValue().get() ) ).
                build().
                addCommands( commandBuilder ) );
    }

    private List<String> getAllMasterNodeDNS( String serviceName )
    {
        List<String> res = new LinkedList<>();
        for ( SpecNode node : resource().getSpec().nodes() )
        {
            if ( node.isMasterNode() )
            {
                String tmp = namingHelper().defaultResourceName( node );
                for ( int i = 0; i < node.replicas(); i++ )
                {
                    res.add( tmp + "-" + i + "." + serviceName );
                }
            }
        }
        return res;
    }

}
