package com.enonic.cloud.operator.operators.v1alpha1.xp7app.commands;

import java.util.List;

import org.immutables.value.Value;

import com.enonic.cloud.operator.crd.xp7.v1alpha1.app.V1alpha1Xp7App;
import com.enonic.cloud.operator.operators.v1alpha2.xp7config.commands.CommandXpConfigModify;

@Value.Immutable
public abstract class CommandXpUpdateDeployConfigFile
    extends CommandXpConfigModify
{
    protected abstract List<V1alpha1Xp7App> xpAppResources();

    @Override
    protected void setData( final StringBuilder sb )
    {
        // Build deploy config file contents
        for ( int i = 0; i < xpAppResources().size(); i++ )
        {
            sb.append( "deploy." ).append( i + 1 ).append( "=" ).append( xpAppResources().get( i ).getSpec().url() ).append( "\n" );
        }
    }
}
