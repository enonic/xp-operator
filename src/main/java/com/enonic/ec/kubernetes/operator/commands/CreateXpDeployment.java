package com.enonic.ec.kubernetes.operator.commands;

import java.util.Map;
import java.util.Optional;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.client.KubernetesClient;

import com.enonic.ec.kubernetes.common.Configuration;
import com.enonic.ec.kubernetes.common.commands.ImmutableCombinedKubernetesCommand;
import com.enonic.ec.kubernetes.deployment.diff.Diff;
import com.enonic.ec.kubernetes.deployment.diff.DiffSpec;
import com.enonic.ec.kubernetes.deployment.diff.ImmutableDiffSpec;
import com.enonic.ec.kubernetes.deployment.xpdeployment.XpDeploymentResource;
import com.enonic.ec.kubernetes.operator.commands.builders.config.ConfigBuilder;
import com.enonic.ec.kubernetes.operator.commands.builders.config.ImmutableConfigBuilderCluster;
import com.enonic.ec.kubernetes.operator.commands.builders.config.ImmutableConfigBuilderNonClustered;
import com.enonic.ec.kubernetes.operator.crd.certmanager.issuer.IssuerClient;

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

    @Override
    public void addCommands( ImmutableCombinedKubernetesCommand.Builder commandBuilder )
        throws Exception
    {
        String namespaceName = resource().getSpec().defaultNamespaceName();
        String defaultBlobStorageName = resource().getSpec().defaultResourceNameWithPreFix( "blob" );
        String defaultSnapshotsStorageName = resource().getSpec().defaultResourceNameWithPreFix( "snapshots" );

        Map<String, String> defaultLabels = resource().getSpec().defaultLabels();
        String esDiscoveryServiceName = resource().getSpec().defaultResourceNameWithPostFix( "es", "discovery" );

        DiffSpec diffSpec = ImmutableDiffSpec.builder().
            oldValue( oldResource().map( r -> r.getSpec() ) ).
            newValue( newResource().getSpec() ).
            build();

        ConfigBuilder configBuilder;

        // TODO: If not clustered
        // Skip NFS
        // Skip Pod disruption budget
        // Skip discovery service

        if ( !resource().getSpec().isClusteredDeployment() )
        {
            configBuilder = ImmutableConfigBuilderNonClustered.
                builder().
                build();
        }
        else
        {
            configBuilder = ImmutableConfigBuilderCluster.builder().
                namespace( namespaceName ).
                esDiscoveryService( esDiscoveryServiceName ).
                clusterName( resource().getSpec().deploymentName() ).
                minimumMasterNodes( resource().getSpec().minimumMasterNodes() ).
                minimumDataNodes( resource().getSpec().minimumDataNodes() ).
                build();
        }

        if ( diffSpec.isNew() )
        {
            ImmutableCreateXpDeploymentBase.builder().
                defaultClient( defaultClient() ).
                ownerReference( ownerReference() ).
                namespace( namespaceName ).
                blobStorageName( defaultBlobStorageName ).
                blobStorageSize( resource().getSpec().sharedDisks().blob() ).
                snapshotsStorageName( defaultSnapshotsStorageName ).
                snapshotsStorageSize( resource().getSpec().sharedDisks().snapshots() ).
                defaultLabels( defaultLabels ).
                esDiscoveryService( esDiscoveryServiceName ).
                build().
                addCommands( commandBuilder );
        }

        // Create / Update Nodes
        diffSpec.nodesChanged().stream().
            filter( Diff::shouldAddOrModify ).
            forEach( diff -> ImmutableCreateXpDeploymentNode.builder().
                defaultClient( defaultClient() ).
                ownerReference( ownerReference() ).
                namespace( namespaceName ).
                nodeName( resource().getSpec().defaultResourceNameWithPostFix( diff.newValue().get().alias() ) ). // TODO: Move this
                diffSpecNode( diff ).
                defaultLabels( defaultLabels ).
                configBuilder( configBuilder ).
                blobStorageName( defaultBlobStorageName ).
                snapshotsStorageName( defaultSnapshotsStorageName ).
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
                nodeName( resource().getSpec().defaultResourceNameWithPostFix( diff.oldValue().get().alias() ) ). // TODO: Move this
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
