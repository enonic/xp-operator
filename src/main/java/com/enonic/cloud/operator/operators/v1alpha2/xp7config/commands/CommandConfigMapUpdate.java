package com.enonic.cloud.operator.operators.v1alpha2.xp7config.commands;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.ConfigMap;

import com.enonic.cloud.operator.common.commands.CombinedCommandBuilder;
import com.enonic.cloud.operator.common.commands.ImmutableCombinedCommand;
import com.enonic.cloud.operator.crd.xp7.v1alpha2.config.V1alpha2Xp7Config;
import com.enonic.cloud.operator.kubectl.ImmutableKubeCmd;
import com.enonic.cloud.operator.operators.common.clients.Clients;

@Value.Immutable
public abstract class CommandConfigMapUpdate
    implements CombinedCommandBuilder
{
    protected abstract Clients clients();

    protected abstract ConfigMap configMap();

    protected abstract List<V1alpha2Xp7Config> xpConfigResources();

    @Override
    public void addCommands( final ImmutableCombinedCommand.Builder commandBuilder )
    {
        // Build the config map
        Map<String, String> newData = new HashMap<>();
        for ( V1alpha2Xp7Config resource : xpConfigResources() )
        {
            newData.put( resource.getSpec().file(), resource.getSpec().data() );
        }

        // Apply the config map
        configMap().setData( newData );
        ImmutableKubeCmd.builder().
            clients( clients() ).
            namespace( configMap().getMetadata().getNamespace() ).
            resource( configMap() ).
            build().
            apply( commandBuilder );
    }
}
