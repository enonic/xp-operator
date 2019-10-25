package com.enonic.ec.kubernetes.operator.commands.delete;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.common.commands.ImmutableKubernetesCommandSummary;
import com.enonic.ec.kubernetes.common.commands.KubernetesCommand;
import com.enonic.ec.kubernetes.common.commands.KubernetesCommandSummary;

public abstract class CommandDeleteResource
    extends KubernetesCommand<Boolean>
{
    protected abstract String name();

    protected abstract String namespace();

    @Value.Derived
    protected abstract String resourceKind();

    @SuppressWarnings("WeakerAccess")
    protected Boolean delete()
    {
        throw new RuntimeException( "Subclass should override this" );
    }

    @Override
    public final Boolean execute()
    {
        return delete();
    }

    @Override
    public KubernetesCommandSummary summary()
    {
        return ImmutableKubernetesCommandSummary.builder().
            namespace( namespace() ).
            name( name() ).
            kind( resourceKind() ).
            action( KubernetesCommandSummary.Action.DELETE ).
            build();
    }
}
