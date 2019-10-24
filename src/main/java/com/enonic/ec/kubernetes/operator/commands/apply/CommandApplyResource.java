package com.enonic.ec.kubernetes.operator.commands.apply;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.immutables.value.Value;

import com.google.common.base.Preconditions;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.OwnerReference;

import com.enonic.ec.kubernetes.common.commands.ImmutableKubeCommandSummary;
import com.enonic.ec.kubernetes.common.commands.KubeCommand;
import com.enonic.ec.kubernetes.common.commands.KubeCommandSummary;

public abstract class CommandApplyResource<T extends HasMetadata>
    extends KubeCommand<T>
{
    private KubeCommandSummary summary;

    protected abstract String name();

    protected abstract Optional<String> namespace();

    protected abstract Optional<OwnerReference> ownerReference();

    protected abstract Map<String, String> labels();

    protected abstract Map<String, String> annotations();

    @Value.Default
    protected boolean canSkipOwnerReference()
    {
        return false;
    }

    @Value.Check
    protected void check()
    {
        Preconditions.checkState( ownerReference().isPresent() || canSkipOwnerReference(), "ownerReference missing" );
    }

    void checkValidityAgainstOld( HasMetadata oldResource )
    {
        if ( !( oldResource instanceof Namespace ) )
        {
            Preconditions.checkState( oldResource.getMetadata().getNamespace().equals( namespace().get() ),
                                      "Resource namespace do not match" );
        }
        Preconditions.checkState( oldResource.getMetadata().getName().equals( name() ), "Resource names do not match" );
    }

    @Value.Derived
    public T preparedResource()
    {
        Optional<T> oldResource = fetchResource();
        Optional<T> newResource = Optional.empty();
        KubeCommandSummary.Action action = KubeCommandSummary.Action.CREATE;

        if ( oldResource.isEmpty() )
        {
            newResource = Optional.of( build(
                new ObjectMeta( annotations(), null, null, null, null, null, null, null, labels(), null, name(),
                                namespace().isPresent() ? namespace().get() : null,
                                ownerReference().isPresent() ? List.of( ownerReference().get() ) : null, null, null, null ) ) );
        }

        if ( newResource.isEmpty() )
        {
            checkValidityAgainstOld( oldResource.get() );
            ObjectMeta newMetadata = ImmutableCommandMergeMetadata.builder().
                kind( oldResource.get().getKind() ).
                metadata( oldResource.get().getMetadata() ).
                ownerReference( ownerReference() ).
                labels( labels() ).
                annotations( annotations() ).
                build().
                execute();
            newMetadata.setResourceVersion( null );

            newResource = Optional.of( build( newMetadata ) );
            action = KubeCommandSummary.Action.UPDATE;
        }

        summary = ImmutableKubeCommandSummary.builder().
            namespace( newResource.get() instanceof Namespace ? Optional.empty() : namespace() ).
            name( newResource.get().getMetadata().getName() ).
            kind( newResource.get().getKind() ).
            action( action ).
            build();

        return newResource.get();
    }

    @Override
    public final T execute()
    {
        return apply( preparedResource() );
    }

    @Value.Derived
    protected abstract Optional<T> fetchResource();

    protected abstract T build( ObjectMeta metadata );

    protected abstract T apply( T resource );

    @Override
    public KubeCommandSummary summary()
    {
        return summary;
    }
}
