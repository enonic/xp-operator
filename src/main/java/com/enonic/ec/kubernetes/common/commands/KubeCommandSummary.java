package com.enonic.ec.kubernetes.common.commands;

import org.immutables.value.Value;
import org.wildfly.common.annotation.Nullable;

@Value.Immutable
public abstract class KubeCommandSummary
{
    @Nullable
    public abstract String namespace();

    public abstract String name();

    public abstract String kind();

    public abstract Action action();

    @Nullable
    public abstract String info();

    public enum Action
    {
        CREATE, UPDATE, SCALE, DELETE
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.
            append( action() ).
            append( " " ).
            append( kind() ).
            append( " '" ).
            append( name() ).
            append( "'" );
        if ( namespace() != null )
        {
            sb.append( " in " ).append( "NS '" ).append( namespace() ).append( "'" );
        }
        if ( info() != null )
        {
            sb.append( ": " ).append( info() );
        }
        return sb.toString();
    }
}
