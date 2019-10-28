package com.enonic.ec.kubernetes.operator.commands;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.client.KubernetesClient;

import com.enonic.ec.kubernetes.common.Configuration;
import com.enonic.ec.kubernetes.common.Tuple;
import com.enonic.ec.kubernetes.common.commands.ImmutableCombinedKubernetesCommand;
import com.enonic.ec.kubernetes.deployment.vhost.VHost;
import com.enonic.ec.kubernetes.deployment.vhost.VHostPath;
import com.enonic.ec.kubernetes.deployment.xpdeployment.XpDeploymentResource;
import com.enonic.ec.kubernetes.deployment.xpdeployment.XpDeploymentResourceSpecNode;
import com.enonic.ec.kubernetes.operator.commands.builders.config.ConfigBuilderCluster;
import com.enonic.ec.kubernetes.operator.commands.plan.ImmutableXpNodeDeploymentDiff;
import com.enonic.ec.kubernetes.operator.commands.plan.ImmutableXpVHostDeploymentDiff;
import com.enonic.ec.kubernetes.operator.commands.plan.XpNodeDeploymentDiff;
import com.enonic.ec.kubernetes.operator.commands.plan.XpNodeDeploymentPlan;
import com.enonic.ec.kubernetes.operator.commands.plan.XpVHostDeploymentDiff;
import com.enonic.ec.kubernetes.operator.commands.plan.XpVHostDeploymentPlan;
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

    private XpDeploymentResource resource()
    {
        return newResource();
    }

    private boolean newDeployment()
    {
        return oldResource().isEmpty();
    }

    private XpNodeDeploymentDiff nodeDiff()
    {
        return ImmutableXpNodeDeploymentDiff.builder().
            oldDeployment( oldResource() ).
            newDeployment( newResource() ).
            build();
    }

    private XpVHostDeploymentDiff vHostDiff()
    {
        return ImmutableXpVHostDeploymentDiff.builder().
            oldDeployment( oldResource() ).
            newDeployment( newResource() ).
            build();
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

        Map<String, String> defaultLabels = resource().getSpec().defaultLabels();
        String esDiscoveryServiceName = resource().getSpec().defaultResourceNameWithPostFix( "es", "discovery" );

        Function<VHost, String> vHostResourceName = vHost -> vHost.getVHostResourceName( resource() );
        Function<Tuple<VHost, VHostPath>, String> pathResourceName = t -> t.b().getPathResourceName( resource(), t.a() );

        ConfigBuilderCluster configBuilder = null;

        if ( newDeployment() )
        {
            ImmutableCreateXpDeploymentBase.builder().
                defaultClient( defaultClient() ).
                ownerReference( ownerReference() ).
                namespace( namespaceName ).
                blobStorageName( defaultBlobStorageName ).
                blobStorageSize( resource().getSpec().sharedDisks().blob() ).
                defaultLabels( defaultLabels ).
                esDiscoveryService( esDiscoveryServiceName ).
                build().
                addCommands( commandBuilder );
        }

        for ( XpNodeDeploymentPlan nodePlan : nodeDiff().deploymentPlans() )
        {
            ImmutableCreateXpDeploymentNode.builder().
                defaultClient( defaultClient() ).
                ownerReference( ownerReference() ).
                namespace( namespaceName ).
                nodeName( resource().getSpec().defaultResourceNameWithPostFix( nodePlan.node().alias() ) ).
                nodePlan( nodePlan ).
                defaultLabels( defaultLabels ).
                configBuilder( configBuilder ).
                blobStorageName( defaultBlobStorageName ).
                build().
                addCommands( commandBuilder );
        }

        for ( XpVHostDeploymentPlan vHostPlan : vHostDiff().deploymentPlans() )
        {
            ImmutableCreateXpDeploymentVHost.builder().
                defaultClient( defaultClient() ).
                issuerClient( issuerClient() ).
                ownerReference( ownerReference() ).
                namespace( namespaceName ).
                vHostPlan( vHostPlan ).
                vHostResourceName( vHostResourceName ).
                pathResourceName( pathResourceName ).
                resource( resource() ).
                defaultLabels( defaultLabels ).
                build().
                addCommands( commandBuilder );
        }

        for ( XpDeploymentResourceSpecNode oldNode : nodeDiff().nodesRemoved() )
        {
            ImmutableDeleteXpDeploymentNode.builder().
                defaultClient( defaultClient() ).
                namespace( namespaceName ).
                nodeName( resource().getSpec().defaultResourceNameWithPostFix( oldNode.alias() ) ).
                build().
                addCommands( commandBuilder );
        }

        for ( VHost oldVHost : vHostDiff().vHostsRemoved() )
        {
            ImmutableDeleteXpDeploymentVHost.builder().
                defaultClient( defaultClient() ).
                issuerClient( issuerClient() ).
                namespace( namespaceName ).
                vHost( oldVHost ).
                vHostResourceName( vHostResourceName ).
                pathResourceName( pathResourceName ).
                build().
                addCommands( commandBuilder );
        }
    }
}
