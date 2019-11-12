package com.enonic.ec.kubernetes.operator.kubectl.scale;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.common.commands.ImmutableKubernetesCommandSummary;
import com.enonic.ec.kubernetes.common.commands.KubernetesCommand;
import com.enonic.ec.kubernetes.common.commands.KubernetesCommandSummary;

public abstract class CommandScaleResource<T>
    extends KubernetesCommand<T>
{
    protected abstract String name();

    protected abstract String namespace();

    protected abstract int scale();

    @Value.Derived
    protected abstract String resourceKind();

    @SuppressWarnings("WeakerAccess")
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
    public KubernetesCommandSummary summary()
    {
        return ImmutableKubernetesCommandSummary.builder().
            namespace( namespace() ).
            name( name() ).
            kind( resourceKind() ).
            action( KubernetesCommandSummary.Action.SCALE ).
            info( Integer.toString( scale() ) ).
            build();
    }
}
