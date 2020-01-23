package com.enonic.ec.kubernetes.operator.operators.v1alpha2.xp7vhost.commands;

import java.util.List;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.operator.operators.v1alpha2.xp7config.commands.CommandXpConfigModifyData;
import com.enonic.ec.kubernetes.operator.operators.v1alpha2.xp7vhost.helpers.Mapping;

@Value.Immutable
public abstract class CommandXpVHostConfigNodesApply
    extends CommandXpConfigModifyData
{
    protected abstract List<Mapping> mappings();

    @Override
    protected void setData( final StringBuilder sb )
    {
        sb.append( "enabled = true" );
        for ( Mapping m : mappings() )
        {
            sb.append( "\n\n" );
            sb.append( String.format( "mapping.%s.host=%s\n", m.name(), m.host() ) );
            sb.append( String.format( "mapping.%s.source=%s\n", m.name(), m.source() ) );
            sb.append( String.format( "mapping.%s.target=%s", m.name(), m.target() ) );
            m.idProvider().ifPresent( p -> {
                sb.append( "\n" );
                sb.append( String.format( "mapping.%s.idProvider.%s=default", m.name(), p ) );
            } );
        }
    }
}
