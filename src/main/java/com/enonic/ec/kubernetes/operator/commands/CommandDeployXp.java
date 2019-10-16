package com.enonic.ec.kubernetes.operator.commands;

import java.util.Map;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.client.KubernetesClient;

import com.enonic.ec.kubernetes.common.commands.Command;
import com.enonic.ec.kubernetes.common.commands.ImmutableCombinedCommand;
import com.enonic.ec.kubernetes.deployment.xpdeployment.XpDeploymentResource;
import com.enonic.ec.kubernetes.operator.builders.ImmutablePodDisruptionBudgetSpecBuilder;
import com.enonic.ec.kubernetes.operator.commands.apply.ImmutableCommandApplyConfigMap;
import com.enonic.ec.kubernetes.operator.commands.apply.ImmutableCommandApplyNamespace;
import com.enonic.ec.kubernetes.operator.commands.apply.ImmutableCommandApplyPodDisruptionBudget;
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

        createXpCluster( namespaceName, commandBuilder );

        return commandBuilder.build().execute();
    }

    private void createXpCluster( String namespace, ImmutableCombinedCommand.Builder commandBuilder )
    {
        // TODO: Below here you could have multiple resources depending on the deployment
        // To begin with we will only handle a single node

        String defaultResourceName = resource().getSpec().fullAppName();
        Map<String, String> defaultLabels = resource().getSpec().defaultLabels();

        commandBuilder.addCommand( ImmutableCommandApplyPodDisruptionBudget.builder().
            client( defaultClient() ).
            ownerReference( ownerReference() ).
            name( "" ).
            namespace( defaultResourceName ).
            labels( defaultLabels ).
            spec( ImmutablePodDisruptionBudgetSpecBuilder.builder().
                minAvailable( 0 ).
                matchLabels( defaultLabels ).
                build().
                execute() ).
            build() );

        commandBuilder.addCommand( ImmutableCommandApplyConfigMap.builder().
            client( defaultClient() ).
            ownerReference( ownerReference() ).
            name( defaultResourceName ).
            namespace( namespace ).
            labels( defaultLabels ).
            data( resource().getSpec().config() ).
            build() );

//        commandBuilder.addCommand( CommandApplyStatefulSet.newBuilder().
//            client( defaultClient ).
//            ownerReference( ownerReference ).
//            name( defaultResourceName ).
//            namespace( namespace ).
//            labels( resource.getSpec().defaultLabels() ).
//            spec( StatefulSetSpecBuilder.newBuilder().
//                podLabels( defaultLabels ).
//                replicas( 1 ).
//                serviceName( defaultResourceName ).
//                build() ).
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
}
