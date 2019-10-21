package com.enonic.ec.kubernetes.operator.commands.apply;

import java.util.List;
import java.util.Map;

import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wildfly.common.annotation.Nullable;

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
    private final static Logger log = LoggerFactory.getLogger( CommandApplyResource.class );

    private KubeCommandSummary summary;

    protected abstract String name();

    @Nullable
    protected abstract String namespace();

    protected abstract OwnerReference ownerReference();

    protected abstract Map<String, String> labels();

    protected abstract Map<String, String> annotations();

    void checkValidity( HasMetadata resource )
    {
        Preconditions.checkState( resource.getMetadata().getName().equals( name() ), "Resource names do not match" );
        Preconditions.checkState( resource.getMetadata().getNamespace().equals( namespace() ), "Resource namespace do not match" );
    }

    @Value.Derived
    public T preparedResource()
    {
        T oldResource = fetchResource();
        T newResource = null;
        KubeCommandSummary.Action action = null;

        if ( oldResource == null )
        {
            newResource = build(
                new ObjectMeta( annotations(), null, null, null, null, null, null, null, labels(), null, name(), namespace(),
                                List.of( ownerReference() ), null, null, null ) );
            action = KubeCommandSummary.Action.CREATE;
        }

        if ( newResource == null )
        {
            checkValidity( oldResource );
            ObjectMeta newMetadata = ImmutableCommandMergeMetadata.builder().
                kind( oldResource.getKind() ).
                metadata( oldResource.getMetadata() ).
                ownerReference( ownerReference() ).
                labels( labels() ).
                annotations( annotations() ).
                build().
                execute();
            newMetadata.setResourceVersion( null );

            newResource = build( newMetadata );
            action = KubeCommandSummary.Action.UPDATE;
        }

        summary = ImmutableKubeCommandSummary.builder().
            namespace( newResource instanceof Namespace ? null : namespace() ).
            name( newResource.getMetadata().getName() ).
            kind( newResource.getKind() ).
            action( action ).
            build();

        return newResource;
    }

    @Override
    public final T execute()
    {
        return apply( preparedResource() );
    }

    @Value.Derived
    protected abstract T fetchResource();

    protected abstract T build( ObjectMeta metadata );

    protected abstract T apply( T resource );

    @Override
    public KubeCommandSummary summary()
    {
        return summary;
    }
}
