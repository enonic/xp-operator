package com.enonic.ec.kubernetes.kubectl;

import java.util.Optional;

import org.immutables.value.Value;

@Value.Immutable
public abstract class KubernetesCommandSummary
{
    public abstract Optional<String> namespace();

    public abstract String name();

    public abstract String kind();

    public abstract Action action();

    public abstract Optional<String> info();

    public enum Action
    {
        CREATE, UPDATE, SCALE, DELETE
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder( action().toString() );

        if ( namespace().isPresent() )
        {
            sb.append( " in " ).append( "NS '" ).append( namespace().get() ).append( "'" );
        }

        sb.append( " " ).
            append( kind() ).
            append( " '" ).
            append( name() ).
            append( "'" );

        if ( info().isPresent() )
        {
            sb.append( ": " ).append( info().get() );
        }
        return sb.toString();
    }
}
