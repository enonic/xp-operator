package com.enonic.ec.kubernetes.operator.kubectl.newapply.mapping;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.HasMetadata;

import com.enonic.ec.kubernetes.operator.common.commands.CombinedCommandBuilder;
import com.enonic.ec.kubernetes.operator.common.commands.ImmutableCombinedCommand;
import com.enonic.ec.kubernetes.operator.helm.Chart;
import com.enonic.ec.kubernetes.operator.helm.Helm;
import com.enonic.ec.kubernetes.operator.kubectl.newapply.base.KubeCommand;
import com.enonic.ec.kubernetes.operator.kubectl.newapply.base.KubeCommandResource;
import com.enonic.ec.kubernetes.operator.operators.clients.Clients;

@Value.Immutable
public abstract class KubeCommandBuilder
    implements CombinedCommandBuilder
{
    protected abstract Clients clients();

    protected abstract Helm helm();

    protected abstract Chart chart();

    protected abstract String namespace();

    protected abstract ValueBuilder valueBuilder();

    @Value.Derived
    protected List<HasMetadata> oldResources()
    {
        Optional<Object> values = valueBuilder().buildOldValues();
        if ( values.isEmpty() )
        {
            return Collections.emptyList();
        }
        return helm().templateToObjects( chart(), values.get() );
    }

    @Value.Derived
    protected List<HasMetadata> newResources()
    {
        Optional<Object> values = valueBuilder().buildNewValues();
        if ( values.isEmpty() )
        {
            return Collections.emptyList();
        }
        return helm().templateToObjects( chart(), values.get() );
    }

    @SuppressWarnings("unchecked")
    @Override
    public void addCommands( final ImmutableCombinedCommand.Builder commandBuilder )
    {
        Function<HasMetadata, String> merge = ( r ) -> r.getKind() + "#" + r.getMetadata().getName();
        Map<String, HasMetadata> oldResources = oldResources().stream().collect( Collectors.toMap( merge, r -> r ) );
        Map<String, HasMetadata> newResources = newResources().stream().collect( Collectors.toMap( merge, r -> r ) );

        for ( Map.Entry<String, HasMetadata> r : oldResources.entrySet() )
        {
            if ( !newResources.containsKey( r.getKey() ) )
            {
                KubeCommandResource cmd = CommandMapper.getCommandClass( clients(), Optional.of( namespace() ), r.getValue() );
                Optional<KubeCommand> delete = cmd.delete();
                if ( delete.isPresent() )
                {
                    commandBuilder.addCommand( delete.get() );
                }
            }
        }
        for ( Map.Entry<String, HasMetadata> r : newResources.entrySet() )
        {
            commandBuilder.addCommand( CommandMapper.
                getCommandClass( clients(), Optional.of( namespace() ), r.getValue() ).apply() );
        }
    }
}
