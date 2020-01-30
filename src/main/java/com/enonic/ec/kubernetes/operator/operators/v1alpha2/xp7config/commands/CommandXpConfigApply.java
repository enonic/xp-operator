package com.enonic.ec.kubernetes.operator.operators.v1alpha2.xp7config.commands;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.ConfigMap;

import com.enonic.ec.kubernetes.operator.common.Configuration;
import com.enonic.ec.kubernetes.operator.common.commands.CombinedCommandBuilder;
import com.enonic.ec.kubernetes.operator.common.commands.ImmutableCombinedCommand;
import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha2.config.V1alpha2Xp7Config;
import com.enonic.ec.kubernetes.operator.kubectl.ImmutableKubeCmd;
import com.enonic.ec.kubernetes.operator.operators.common.clients.Clients;

@Value.Immutable
public abstract class CommandXpConfigApply
    extends Configuration
    implements CombinedCommandBuilder
{
    protected abstract Clients clients();

    protected abstract ConfigMap configMap();

    protected abstract List<V1alpha2Xp7Config> xpConfigResources();

    @Override
    public void addCommands( final ImmutableCombinedCommand.Builder commandBuilder )
    {
        Map<String, String> oldData = configMap().getData() != null ? configMap().getData() : Collections.emptyMap();
        Map<String, String> newData = new HashMap<>();
        for ( V1alpha2Xp7Config resource : xpConfigResources() )
        {
            newData.put( resource.getSpec().file(), resource.getSpec().data() );
        }
        if ( !newData.equals( oldData ) )
        {
            configMap().setData( newData );
            ImmutableKubeCmd.builder().
                clients( clients() ).
                namespace( configMap().getMetadata().getNamespace() ).
                resource( configMap() ).
                build().
                apply( commandBuilder );
        }
    }
}
