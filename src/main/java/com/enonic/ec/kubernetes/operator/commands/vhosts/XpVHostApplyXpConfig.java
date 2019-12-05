package com.enonic.ec.kubernetes.operator.commands.vhosts;

import java.util.List;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.operator.commands.config.XpConfigModifyData;
import com.enonic.ec.kubernetes.operator.commands.vhosts.helpers.Mapping;

@Value.Immutable
public abstract class XpVHostApplyXpConfig
    extends XpConfigModifyData
{

    protected abstract List<Mapping> mappings();

    @Override
    protected void setData( final StringBuilder sb )
    {
        for ( Mapping m : mappings() )
        {
            sb.append( String.format( "mapping.%s.host=%s\n", m.name(), m.host() ) );
            sb.append( String.format( "mapping.%s.source=%s\n", m.name(), m.source() ) );
            sb.append( String.format( "mapping.%s.target=%s\n", m.name(), m.target() ) );
            if ( m.idProvider() != null )
            {
                sb.append( String.format( "mapping.%s.idProvider.%s=default", m.name(), m.idProvider() ) );
            }
        }
    }
}
