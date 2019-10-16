package com.enonic.ec.kubernetes.operator.commands.apply;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.policy.PodDisruptionBudget;
import io.fabric8.kubernetes.api.model.policy.PodDisruptionBudgetSpec;
import io.fabric8.kubernetes.client.KubernetesClient;

@Value.Immutable
public abstract class CommandApplyPodDisruptionBudget
    extends CommandApplyResource<PodDisruptionBudget>
{
    protected abstract KubernetesClient client();

    protected abstract PodDisruptionBudgetSpec spec();

    @Override
    protected PodDisruptionBudget fetchResource()
    {
        return client().policy().podDisruptionBudget().inNamespace( namespace() ).withName( name() ).get();
    }

    @Override
    protected PodDisruptionBudget apply( final ObjectMeta metadata )
    {
        PodDisruptionBudget podDisruptionBudget = new PodDisruptionBudget();
        podDisruptionBudget.setMetadata( metadata );
        podDisruptionBudget.setSpec( spec() );
        return client().policy().podDisruptionBudget().inNamespace( namespace() ).createOrReplace( podDisruptionBudget );
    }

}
