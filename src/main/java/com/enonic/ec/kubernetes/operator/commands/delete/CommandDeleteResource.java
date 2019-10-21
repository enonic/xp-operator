package com.enonic.ec.kubernetes.operator.commands.delete;

import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.enonic.ec.kubernetes.common.commands.ImmutableKubeCommandSummary;
import com.enonic.ec.kubernetes.common.commands.KubeCommand;
import com.enonic.ec.kubernetes.common.commands.KubeCommandSummary;

public abstract class CommandDeleteResource
    extends KubeCommand<Boolean>
{
    private final static Logger log = LoggerFactory.getLogger( CommandDeleteResource.class );

    protected abstract String name();

    protected abstract String namespace();

    @Value.Derived
    protected abstract String resourceKind();

    protected Boolean delete()
    {
        throw new RuntimeException( "Subclass should override this" );
    }

    @Override
    public final Boolean execute()
    {
        Boolean res = delete();
        if ( !res )
        {
            log.warn( "Deletion did not return true: " + summary() );
        }
        return res;
    }

    @Override
    public KubeCommandSummary summary()
    {
        return ImmutableKubeCommandSummary.builder().
            namespace( namespace() ).
            name( name() ).
            kind( resourceKind() ).
            action( KubeCommandSummary.Action.DELETE ).
            build();
    }
}
