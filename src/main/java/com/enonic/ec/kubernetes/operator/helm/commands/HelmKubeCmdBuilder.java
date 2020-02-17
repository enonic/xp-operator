package com.enonic.ec.kubernetes.operator.helm.commands;

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
import com.enonic.ec.kubernetes.operator.kubectl.ImmutableKubeCmd;
import com.enonic.ec.kubernetes.operator.kubectl.base.ImmutableKubeCommandOptions;
import com.enonic.ec.kubernetes.operator.operators.common.clients.Clients;

@Value.Immutable
public abstract class HelmKubeCmdBuilder
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
        return helm().templateObjects( chart(), values.get() );
    }

    @Value.Derived
    protected List<HasMetadata> newResources()
    {
        Optional<Object> values = valueBuilder().buildNewValues();
        if ( values.isEmpty() )
        {
            return Collections.emptyList();
        }
        return helm().templateObjects( chart(), values.get() );
    }

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
                ImmutableKubeCmd.builder().
                    clients( clients() ).
                    namespace( namespace() ).
                    resource( r.getValue() ).
                    options( ImmutableKubeCommandOptions.builder().
                        neverOverwrite( neverOverwrite( r.getValue().getMetadata().getAnnotations() ) ).
                        build() ).
                    build().
                    delete( commandBuilder );
            }
        }
        for ( Map.Entry<String, HasMetadata> r : newResources.entrySet() )
        {
            ImmutableKubeCmd.builder().
                clients( clients() ).
                namespace( namespace() ).
                resource( r.getValue() ).
                options( ImmutableKubeCommandOptions.builder().
                    neverOverwrite( neverOverwrite( r.getValue().getMetadata().getAnnotations() ) ).
                    build() ).
                build().
                apply( commandBuilder );
        }
    }

    private boolean neverOverwrite( final Map<String, String> annotations )
    {
        if ( annotations == null )
        {
            return false;
        }
        return annotations.getOrDefault( "neverOverwrite", "false" ).equals( "true" );
    }
}
