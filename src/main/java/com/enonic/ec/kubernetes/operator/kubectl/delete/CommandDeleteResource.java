package com.enonic.ec.kubernetes.operator.kubectl.delete;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.operator.kubectl.ImmutableKubernetesCommandSummary;
import com.enonic.ec.kubernetes.operator.kubectl.KubernetesCommand;
import com.enonic.ec.kubernetes.operator.kubectl.KubernetesCommandSummary;
import com.enonic.ec.kubernetes.operator.operators.clients.Clients;

public abstract class CommandDeleteResource
    extends KubernetesCommand<Boolean>
{
    protected abstract Clients clients();

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
