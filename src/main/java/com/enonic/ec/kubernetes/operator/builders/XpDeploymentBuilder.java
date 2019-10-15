package com.enonic.ec.kubernetes.operator.builders;

import java.util.Map;

import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.client.KubernetesClient;

import com.enonic.ec.kubernetes.common.commands.CombinedCommand;
import com.enonic.ec.kubernetes.common.commands.Command;
import com.enonic.ec.kubernetes.deployment.XpDeployment.XpDeploymentResource;
import com.enonic.ec.kubernetes.operator.commands.apply.CommandApplyConfigMap;
import com.enonic.ec.kubernetes.operator.commands.apply.CommandApplyNamespace;
import com.enonic.ec.kubernetes.operator.commands.apply.CommandApplyPodDisruptionBudget;
import com.enonic.ec.kubernetes.operator.commands.apply.CommandApplyStatefulSet;
import com.enonic.ec.kubernetes.operator.crd.certmanager.issuer.IssuerClientProducer;

import static com.enonic.ec.kubernetes.common.assertions.Assertions.assertNotNull;

public class XpDeploymentBuilder
    implements com.enonic.ec.kubernetes.common.Builder<Command<Void>>
{
    private KubernetesClient defaultClient;

    private IssuerClientProducer.IssuerClient issuerClient;

    private XpDeploymentResource resource;

    private OwnerReference ownerReference;

    private XpDeploymentBuilder()
    {
    }

    public static XpDeploymentBuilder newBuilder()
    {
        return new XpDeploymentBuilder();
    }

    public XpDeploymentBuilder defaultClient( KubernetesClient var )
    {
        defaultClient = var;
        return this;
    }

    public XpDeploymentBuilder issuerClient( IssuerClientProducer.IssuerClient var )
    {
        issuerClient = var;
        return this;
    }

    public XpDeploymentBuilder resource( XpDeploymentResource var )
    {
        resource = var;
        return this;
    }

    @Override
    public Command<Void> build()
    {
        defaultClient = assertNotNull( "defaultClient", defaultClient );
        issuerClient = assertNotNull( "issuerClient", issuerClient );
        resource = assertNotNull( "resource", resource );
        ownerReference = createOwnerReference();

        String namespaceName = resource.getSpec().getFullProjectName();

        CombinedCommand.Builder commandBuilder = CombinedCommand.newBuilder();

        commandBuilder.add( CommandApplyNamespace.newBuilder().
            client( defaultClient ).
            ownerReference( ownerReference ).
            name( namespaceName ).
            build() );

//        commandBuilder.add( CommandApplyNetworkPolicy.newBuilder().
//            client( defaultClient ).
//            ownerReference( ownerReference ).
//            name( resource.getSpec().getFullAppName() ).
//            namespace( namespaceName ).
//            labels( resource.getSpec().getDefaultLabels() ).
//            spec( null ). // TODO: Fix
//            build() );

        createXpCluster( namespaceName, commandBuilder );

        return commandBuilder.build();
    }

    private OwnerReference createOwnerReference()
    {
        return new OwnerReference( resource.getApiVersion(), true, true, resource.getKind(), resource.getMetadata().getName(),
                                   resource.getMetadata().getUid() );
    }

    private void createXpCluster( String namespace, CombinedCommand.Builder commandBuilder )
    {
        // TODO: Below here you could have multiple resources depending on the deployment
        // To begin with we will only handle a single node

        String defaultResourceName = resource.getSpec().getFullAppName();
        Map<String, String> defaultLabels = resource.getSpec().getDefaultLabels();

        commandBuilder.add( CommandApplyConfigMap.newBuilder().
            client( defaultClient ).
            ownerReference( ownerReference ).
            name( defaultResourceName ).
            namespace( namespace ).
            labels( defaultLabels ).
            data( resource.getSpec().getConfig() ).
            build() );

        commandBuilder.add( CommandApplyPodDisruptionBudget.newBuilder().
            client( defaultClient ).
            ownerReference( ownerReference ).
            name( defaultResourceName ).
            namespace( namespace ).
            labels( resource.getSpec().getDefaultLabels() ).
            spec( PodDisruptionBudgetSpecBuilder.newBuilder().
                minAvailable( 0 ). // This has to be 0 for a single node
                matchLabels( defaultLabels ).
                build() ).
            build() );

        commandBuilder.add( CommandApplyStatefulSet.newBuilder().
            client( defaultClient ).
            ownerReference( ownerReference ).
            name( defaultResourceName ).
            namespace( namespace ).
            labels( resource.getSpec().getDefaultLabels() ).
            spec( StatefulSetSpecBuilder.newBuilder().
                podLabels( defaultLabels ).
                replicas( 1 ).
                serviceName( defaultResourceName ).
                build() ).
            build() );
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
