package com.enonic.ec.kubernetes.kubectl.delete;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.policy.PodDisruptionBudget;
import io.fabric8.kubernetes.client.KubernetesClient;

@Value.Immutable
public abstract class CommandDeletePodDisruptionBudget
    extends CommandDeleteResource
{
    protected abstract KubernetesClient client();

    @Override
    protected String resourceKind()
    {
        return PodDisruptionBudget.class.getSimpleName();
    }

    @Override
    protected Boolean delete()
    {
        return client().policy().podDisruptionBudget().inNamespace( namespace() ).withName( name() ).delete();
    }

    @Override
    public String toString()
    {
        return super.toString();
    }
}
