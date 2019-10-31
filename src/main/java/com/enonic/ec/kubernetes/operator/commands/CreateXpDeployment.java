package com.enonic.ec.kubernetes.operator.commands;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.client.KubernetesClient;

import com.enonic.ec.kubernetes.common.Configuration;
import com.enonic.ec.kubernetes.common.commands.ImmutableCombinedKubernetesCommand;
import com.enonic.ec.kubernetes.deployment.ImmutableXpDeploymentNamingHelper;
import com.enonic.ec.kubernetes.deployment.XpDeploymentNamingHelper;
import com.enonic.ec.kubernetes.deployment.XpDeploymentResource;
import com.enonic.ec.kubernetes.deployment.diff.Diff;
import com.enonic.ec.kubernetes.deployment.diff.DiffSpec;
import com.enonic.ec.kubernetes.deployment.diff.DiffSpecNode;
import com.enonic.ec.kubernetes.deployment.diff.ImmutableDiffSpec;
import com.enonic.ec.kubernetes.deployment.spec.SpecNode;
import com.enonic.ec.kubernetes.operator.commands.builders.config.ConfigBuilder;
import com.enonic.ec.kubernetes.operator.commands.builders.config.ImmutableConfigBuilderCluster;
import com.enonic.ec.kubernetes.operator.crd.certmanager.issuer.IssuerClient;

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

    // TODO: Remove
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

    private List<String> getAllNodeDNS( String serviceName )
    {
        List<String> res = new LinkedList<>();
        for ( SpecNode node : resource().getSpec().nodes() )
        {
            String tmp = namingHelper().defaultResourceName( node );
            for ( int i = 0; i < node.replicas(); i++ )
            {
                res.add( tmp + "-" + i + "." + serviceName );
            }
        }
        return res;
    }

    @Override
    public void addCommands( ImmutableCombinedKubernetesCommand.Builder commandBuilder )
    {
        String namespaceName = namingHelper().defaultNamespaceName();
        String serviceName = namingHelper().defaultResourceName();

        String defaultBlobStorageName = namingHelper().defaultResourceNameWithPreFix( "blob" );
        String defaultSnapshotsStorageName = namingHelper().defaultResourceNameWithPreFix( "snapshots" );

        Map<String, String> defaultLabels = resource().getSpec().defaultLabels();

        DiffSpec diffSpec = ImmutableDiffSpec.builder().
            oldValue( oldResource().map( XpDeploymentResource::getSpec ) ).
            newValue( newResource().getSpec() ).
            build();

        Function<SpecNode, Integer> defaultMinimumAvailable = ( n ) -> ( n.replicas() / 2 ) + 1;

        SpecNode masterNode = resource().getSpec().nodes().stream().filter( SpecNode::isMasterNode ).findAny().get();
        int minimumMasterNodes = defaultMinimumAvailable.apply( masterNode );

        SpecNode dataNode = resource().getSpec().nodes().stream().filter( SpecNode::isDataNode ).findAny().get();
        int minimumDataNodes = defaultMinimumAvailable.apply( dataNode );

        ConfigBuilder configBuilder = ImmutableConfigBuilderCluster.builder().
            namespace( namespaceName ).
            serviceName( serviceName ).
            clusterName( resource().getSpec().deploymentName() ).
            minimumMasterNodes( minimumMasterNodes ).
            minimumDataNodes( minimumDataNodes ).
//            discoveryHosts( Arrays.asList( String.join( ".", serviceName, namespaceName, "svc.cluster.local" ) ) ). // TODO: XP is not happy with this
    discoveryHosts( getAllNodeDNS( serviceName ) ).
                build();

        // TODO: If not clustered
        // Skip NFS
        // Skip Pod disruption budget
        // Skip discovery service

        if ( diffSpec.isNew() )
        {
            ImmutableCreateXpDeploymentBase.builder().
                defaultClient( defaultClient() ).
                ownerReference( ownerReference() ).
                namespace( namespaceName ).
                serviceName( serviceName ).
                blobStorageName( defaultBlobStorageName ).
                blobStorageSize( resource().getSpec().sharedDisks().blob() ).
                snapshotsStorageName( defaultSnapshotsStorageName ).
                snapshotsStorageSize( resource().getSpec().sharedDisks().snapshots() ).
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
                blobStorageName( defaultBlobStorageName ).
                snapshotsStorageName( defaultSnapshotsStorageName ).
                minimumAvailable( defaultMinimumAvailable.apply( diffSpecNode.newValue().get() ) ).
                build().
                addCommands( commandBuilder ) );

        // Create / Update vHosts
        diffSpec.vHostsChanged().stream().
            filter( Diff::shouldAddOrModify ).
            forEach( diff -> ImmutableCreateXpDeploymentVHost.builder().
                defaultClient( defaultClient() ).
                issuerClient( issuerClient() ).
                ownerReference( ownerReference() ).
                namespace( namespaceName ).
                diffVHost( diff ).
                defaultLabels( defaultLabels ).
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

        // Remove old vHosts
        diffSpec.vHostsChanged().stream().
            filter( Diff::shouldRemove ).
            forEach( diff -> ImmutableDeleteXpDeploymentVHost.builder().
                defaultClient( defaultClient() ).
                issuerClient( issuerClient() ).
                namespace( namespaceName ).
                vHost( diff.oldValue().get() ).
                build().
                addCommands( commandBuilder ) );
    }
}
