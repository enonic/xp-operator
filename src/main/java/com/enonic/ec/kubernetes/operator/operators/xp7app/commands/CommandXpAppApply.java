package com.enonic.ec.kubernetes.operator.operators.xp7app.commands;

import java.util.List;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.operator.operators.xp7config.commands.CommandXpConfigModifyData;
import com.enonic.ec.kubernetes.operator.crd.xp7app.Xp7AppResource;

@Value.Immutable
public abstract class CommandXpAppApply
    extends CommandXpConfigModifyData
{
    protected abstract List<Xp7AppResource> xpAppResources();

    @Override
    protected void setData( final StringBuilder sb )
    {
        for ( int i = 0; i < xpAppResources().size(); i++ )
        {
            sb.append( "deploy." ).append( i + 1 ).append( "=" ).append( xpAppResources().get( i ).getSpec().uri() ).append( "\n" );
        }
    }
}
