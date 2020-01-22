package com.enonic.ec.kubernetes.operator.kubectl.delete;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.KubernetesClient;

@Value.Immutable
public abstract class CommandDeleteConfigMap
    extends CommandDeleteResource
{
    @Override
    protected String resourceKind()
    {
        return ConfigMap.class.getSimpleName();
    }

    @Override
    protected Boolean delete()
    {
        return clients().getDefaultClient().configMaps().inNamespace( namespace() ).withName( name() ).delete();
    }

    @Override
    public String toString()
    {
        return super.toString();
    }
}
