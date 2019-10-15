package com.enonic.ec.kubernetes.common.commands;

import java.util.LinkedList;
import java.util.List;

public class CombinedCommand
    implements Command<Void>
{
    private final List<Command> commands;

    private CombinedCommand( final Builder builder )
    {
        commands = builder.commands;
    }

    public static Builder newBuilder()
    {
        return new Builder();
    }

    @Override
    public Void execute()
        throws Exception
    {
        for ( Command c : commands )
        {
            c.execute();
        }
        return null;
    }


    public static final class Builder
    {
        private final List<Command> commands;

        private Builder()
        {
            commands = new LinkedList<>();
        }

        public Builder add( final Command val )
        {
            commands.add( val );
            return this;
        }

        public Builder add( final List<Command> val )
        {
            val.forEach( commands::add );
            return this;
        }

        public CombinedCommand build()
        {
            return new CombinedCommand( this );
        }
    }
}
