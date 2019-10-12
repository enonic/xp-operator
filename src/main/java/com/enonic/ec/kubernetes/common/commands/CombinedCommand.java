package com.enonic.ec.kubernetes.common.commands;

import java.util.LinkedList;
import java.util.List;

public class CombinedCommand
    implements Command<Exception>
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
    public Exception execute()
    {
        for ( Command c : commands )
        {
            try
            {
                c.execute();
            }
            catch ( Exception e )
            {
                return e;
            }
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

        public CombinedCommand build()
        {
            return new CombinedCommand( this );
        }
    }
}
