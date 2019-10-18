package com.enonic.ec.kubernetes.operator.commands.apply;

import java.util.List;
import java.util.Map;

import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wildfly.common.annotation.Nullable;

import com.google.common.base.Preconditions;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.OwnerReference;

import com.enonic.ec.kubernetes.common.commands.Command;

public abstract class CommandApplyResource<T extends HasMetadata>
    implements Command<T>
{
    private final static Logger log = LoggerFactory.getLogger( CommandApplyResource.class );

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

    @Override
    public T execute()
    {
        T oldResource = fetchResource();

        if ( oldResource == null )
        {
            T newResource = build(
                new ObjectMeta( annotations(), null, null, null, null, null, null, null, labels(), null, name(), namespace(),
                                List.of( ownerReference() ), null, null, null ) );
            log.info( "Creating in Namespace '" + newResource.getMetadata().getNamespace() + "' " + newResource.getKind() + " '" +
                          newResource.getMetadata().getName() + "'" );
            return apply( newResource );
        }

        ObjectMeta newMetadata = ImmutableCommandMergeMetadata.builder().
            kind( oldResource.getKind() ).
            metadata( oldResource.getMetadata() ).
            ownerReference( ownerReference() ).
            labels( labels() ).
            annotations( annotations() ).
            build().
            execute();
        newMetadata.setResourceVersion( null );

        T newResource = build( newMetadata );
        log.info( "Updating in Namespace '" + newResource.getMetadata().getNamespace() + "' " + newResource.getKind() + " '" +
                      newResource.getMetadata().getName() + "'" );
        return apply( newResource );
    }

    @Value.Derived
    protected abstract T fetchResource();

    protected abstract T build( ObjectMeta metadata );

    protected abstract T apply( T resource );
}
