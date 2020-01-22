package com.enonic.ec.kubernetes.operator.kubectl.scale;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.operator.kubectl.ImmutableKubernetesCommandSummary;
import com.enonic.ec.kubernetes.operator.kubectl.KubernetesCommand;
import com.enonic.ec.kubernetes.operator.kubectl.KubernetesCommandSummary;
import com.enonic.ec.kubernetes.operator.operators.clients.Clients;

public abstract class CommandScaleResource<T>
    extends KubernetesCommand<T>
{
    protected abstract Clients clients();

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
