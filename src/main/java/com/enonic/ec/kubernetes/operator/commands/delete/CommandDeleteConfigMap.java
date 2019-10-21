package com.enonic.ec.kubernetes.operator.commands.delete;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.KubernetesClient;

@Value.Immutable
public abstract class CommandDeleteConfigMap
    extends CommandDeleteResource
{
    protected abstract KubernetesClient client();

    @Override
    protected String resourceKind()
    {
        return ConfigMap.class.getSimpleName();
    }

    @Override
    protected Boolean delete()
    {
        return client().configMaps().inNamespace( namespace() ).withName( name() ).delete();
    }
}
