package com.enonic.cloud.operator.operators.v1alpha2.xp7vhost.commands;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.immutables.value.Value;

import com.enonic.cloud.operator.common.commands.CombinedCommandBuilder;
import com.enonic.cloud.operator.common.commands.ImmutableCombinedCommand;
import com.enonic.cloud.operator.operators.common.ResourceInfoNamespaced;
import com.enonic.cloud.operator.operators.common.cache.Caches;
import com.enonic.cloud.operator.operators.common.clients.Clients;
import com.enonic.cloud.operator.operators.v1alpha2.xp7vhost.helpers.Mapping;

import static com.enonic.cloud.operator.common.Configuration.cfgStr;
import static com.enonic.cloud.operator.common.Configuration.cfgStrFmt;

@Value.Immutable
public abstract class CommandXpVHostsApplyNodeMappings
    implements CombinedCommandBuilder
{
    protected abstract Clients clients();

    protected abstract Caches caches();

    protected abstract Map<String, List<Mapping>> nodeMappings();

    protected abstract ResourceInfoNamespaced info();

    @Override
    public void addCommands( final ImmutableCombinedCommand.Builder commandBuilder )
    {
        // Iterate over each node mapping
        for ( Map.Entry<String, List<Mapping>> e : nodeMappings().entrySet() )
        {
            String nodeName = e.getKey();
            List<Mapping> mappings = e.getValue();
            mappings.sort( Comparator.comparing( Mapping::host ) );

            // Create / Update config
            ImmutableCommandXpUpdateVHostConfigFile.builder().
                clients( clients() ).
                caches( caches() ).
                info( info() ).
                name( cfgStrFmt( "operator.deployment.xp.config.vhosts.nameTemplate", nodeName ) ).
                file( cfgStr( "operator.deployment.xp.config.vhosts.file" ) ).
                nodeGroup( nodeName ).
                mappings( mappings ).
                build().
                addCommands( commandBuilder );
        }
    }
}
