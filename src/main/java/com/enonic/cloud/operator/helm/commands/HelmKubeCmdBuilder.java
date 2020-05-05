package com.enonic.cloud.operator.helm.commands;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.HasMetadata;

import com.enonic.cloud.operator.common.commands.CombinedCommandBuilder;
import com.enonic.cloud.operator.common.commands.ImmutableCombinedCommand;
import com.enonic.cloud.operator.helm.Chart;
import com.enonic.cloud.operator.helm.Helm;
import com.enonic.cloud.operator.kubectl.ImmutableKubeCmd;
import com.enonic.cloud.operator.kubectl.base.ImmutableKubeCommandOptions;
import com.enonic.cloud.operator.kubectl.base.KubeCommandOptions;
import com.enonic.cloud.operator.operators.common.clients.Clients;

import static com.enonic.cloud.operator.common.Configuration.cfgStr;

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
                options( createOptions( r.getValue().getMetadata().getAnnotations() ) ).
                build().
                apply( commandBuilder );
        }
    }

    private KubeCommandOptions createOptions( final Map<String, String> annotations )
    {
        ImmutableKubeCommandOptions.Builder builder = ImmutableKubeCommandOptions.builder();
        if ( annotations == null )
        {
            return builder.build();
        }

        String neverOverwrite = cfgStr( "operator.helm.charts.Values.annotationKeys.neverOverwrite" );
        if ( annotations.containsKey( neverOverwrite ) )
        {
            builder.neverOverwrite( annotations.get( neverOverwrite ).equals( "true" ) );
        }

        String alwaysOverwrite = cfgStr( "operator.helm.charts.Values.annotationKeys.alwaysOverwrite" );
        if ( annotations.containsKey( alwaysOverwrite ) )
        {
            builder.alwaysOverwrite( annotations.get( alwaysOverwrite ).equals( "true" ) );
        }

        return builder.build();
    }
}
