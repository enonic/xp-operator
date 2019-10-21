package com.enonic.ec.kubernetes.operator.commands.scale;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.common.commands.ImmutableKubeCommandSummary;
import com.enonic.ec.kubernetes.common.commands.KubeCommand;
import com.enonic.ec.kubernetes.common.commands.KubeCommandSummary;

public abstract class CommandScaleResource<T>
    extends KubeCommand<T>
{
    protected abstract String name();

    protected abstract String namespace();

    protected abstract int scale();

    @Value.Derived
    protected abstract String resourceKind();

    protected T applyScale()
    {
        throw new RuntimeException( "Subclass should override this" );
    }

    @Override
    public final T execute()
    {
        return applyScale();
    }

    @Override
    public KubeCommandSummary summary()
    {
        return ImmutableKubeCommandSummary.builder().
            namespace( namespace() ).
            name( name() ).
            kind( resourceKind() ).
            action( KubeCommandSummary.Action.SCALE ).
            info( Integer.toString( scale() ) ).
            build();
    }
}
