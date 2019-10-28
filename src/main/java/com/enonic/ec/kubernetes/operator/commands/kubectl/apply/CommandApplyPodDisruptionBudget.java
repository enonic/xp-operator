package com.enonic.ec.kubernetes.operator.commands.kubectl.apply;

import java.util.Optional;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.policy.PodDisruptionBudget;
import io.fabric8.kubernetes.api.model.policy.PodDisruptionBudgetSpec;
import io.fabric8.kubernetes.client.KubernetesClient;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@Value.Immutable
public abstract class CommandApplyPodDisruptionBudget
    extends CommandApplyResource<PodDisruptionBudget>
{
    protected abstract KubernetesClient client();

    protected abstract PodDisruptionBudgetSpec spec();

    @Override
    protected Optional<PodDisruptionBudget> fetchResource()
    {
        return Optional.ofNullable( client().policy().podDisruptionBudget().inNamespace( namespace().get() ).withName( name() ).get() );
    }

    @Override
    protected PodDisruptionBudget build( final ObjectMeta metadata )
    {
        PodDisruptionBudget podDisruptionBudget = new PodDisruptionBudget();
        podDisruptionBudget.setMetadata( metadata );
        podDisruptionBudget.setSpec( spec() );
        return podDisruptionBudget;
    }

    @Override
    protected PodDisruptionBudget apply( final PodDisruptionBudget resource )
    {
        return client().policy().podDisruptionBudget().inNamespace( namespace().get() ).createOrReplace( resource );
    }
}
