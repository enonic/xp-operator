package com.enonic.ec.kubernetes.operator.commands.apps;

import java.util.List;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.operator.commands.config.CommandXpConfigModifyData;
import com.enonic.ec.kubernetes.operator.crd.app.XpAppResource;

@Value.Immutable
public abstract class CommandXpAppApply
    extends CommandXpConfigModifyData
{
    protected abstract List<XpAppResource> xpAppResources();

    @Override
    protected void setData( final StringBuilder sb )
    {
        for ( int i = 0; i < xpAppResources().size(); i++ )
        {
            sb.append( "deploy." ).append( i + 1 ).append( "=" ).append( xpAppResources().get( i ).getSpec().uri() ).append( "\n" );
        }
    }
}
