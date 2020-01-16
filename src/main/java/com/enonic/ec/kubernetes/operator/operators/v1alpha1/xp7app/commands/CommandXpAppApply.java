package com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7app.commands;

import java.util.List;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7app.crd.Xp7AppResource;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7config.commands.CommandXpConfigModifyData;

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
            sb.append( "deploy." ).append( i + 1 ).append( "=" ).append( xpAppResources().get( i ).getSpec().url() ).append( "\n" );
        }
    }
}
