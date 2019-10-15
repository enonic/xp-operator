package com.enonic.ec.kubernetes.operator.builders;

import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.client.KubernetesClient;

import com.enonic.ec.kubernetes.common.commands.CombinedCommand;
import com.enonic.ec.kubernetes.common.commands.Command;
import com.enonic.ec.kubernetes.deployment.XpDeployment.XpDeploymentResource;
import com.enonic.ec.kubernetes.operator.commands.apply.CommandApplyConfigMap;
import com.enonic.ec.kubernetes.operator.commands.apply.CommandApplyNamespace;
import com.enonic.ec.kubernetes.operator.crd.certmanager.issuer.IssuerClientProducer;

import static com.enonic.ec.kubernetes.common.assertions.Assertions.assertNotNull;

public class XpDeploymentBuilder
    implements Command<XpDeploymentResource>
{
    private final KubernetesClient defaultClient;

    private final IssuerClientProducer.IssuerClient issuerClient;

    private final XpDeploymentResource resource;

    private final OwnerReference ownerReference;

    private XpDeploymentBuilder( final Builder builder )
    {
        defaultClient = assertNotNull( "defaultClient", builder.defaultClient );
        issuerClient = assertNotNull( "issuerClient", builder.issuerClient );
        resource = assertNotNull( "resource", builder.resource );
        ownerReference = createOwnerReference();
    }

    public static Builder newBuilder()
    {
        return new Builder();
    }

    private OwnerReference createOwnerReference()
    {
        return new OwnerReference( resource.getApiVersion(), true, true, resource.getKind(), resource.getMetadata().getName(),
                                   resource.getMetadata().getUid() );
    }

    @Override
    public XpDeploymentResource execute()
        throws Exception
    {
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

        createCluster( namespaceName, commandBuilder );

        commandBuilder.build().execute();

        return resource;
    }

    public void createCluster( String namespaceName, CombinedCommand.Builder commandBuilder )
    {
        // TODO: Below here you could have multiple resources depending on the deployment

        commandBuilder.add( CommandApplyConfigMap.newBuilder().
            client( defaultClient ).
            ownerReference( ownerReference ).
            name( resource.getSpec().getFullAppName() ).
            namespace( namespaceName ).
            labels( resource.getSpec().getDefaultLabels() ).
            data( resource.getSpec().getConfig() ).
            build() );

//        commandBuilder.add( CommandApplyPodDisruptionBudget.newBuilder().
//            client( defaultClient ).
//            ownerReference( ownerReference ).
//            name( resource.getSpec().getFullAppName() ).
//            namespace( namespaceName ).
//            labels( resource.getSpec().getDefaultLabels() ).
//            spec( null ). // TODO: Fix
//            build() );
//
//        commandBuilder.add( CommandApplyStatefulSet.newBuilder().
//            client( defaultClient ).
//            ownerReference( ownerReference ).
//            name( resource.getSpec().getFullAppName() ).
//            namespace( namespaceName ).
//            labels( resource.getSpec().getDefaultLabels() ).
//            spec( null ). // TODO: Fix
//            build() );
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

    public static final class Builder
    {
        private KubernetesClient defaultClient;

        private IssuerClientProducer.IssuerClient issuerClient;

        private XpDeploymentResource resource;

        private Builder()
        {
        }

        public Builder defaultClient( final KubernetesClient val )
        {
            defaultClient = val;
            return this;
        }

        public Builder issuerClient( final IssuerClientProducer.IssuerClient val )
        {
            issuerClient = val;
            return this;
        }

        public Builder resource( final XpDeploymentResource val )
        {
            resource = val;
            return this;
        }

        public XpDeploymentBuilder build()
        {
            return new XpDeploymentBuilder( this );
        }
    }
}
