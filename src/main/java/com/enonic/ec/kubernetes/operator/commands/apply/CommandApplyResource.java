package com.enonic.ec.kubernetes.operator.commands.apply;

import java.util.List;
import java.util.Map;

import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wildfly.common.annotation.Nullable;

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

    protected void checkValidity( HasMetadata resource )
        throws Exception
    {
        if ( !resource.getMetadata().getName().equals( name() ) )
        {
            throw new Exception( "Resource names do not match" );
        }
        if ( !resource.getMetadata().getNamespace().equals( namespace() ) )
        {
            throw new Exception( "Resource namespace do not match" );
        }
    }

    @Override
    public T execute()
        throws Exception
    {
        T resource = fetchResource();
        ObjectMeta metadata = null;
        String method = null;
        if ( resource != null )
        {
            metadata = ImmutableCommandMergeMetadata.builder().
                kind( resource.getKind() ).
                metadata( resource.getMetadata() ).
                ownerReference( ownerReference() ).
                labels( labels() ).
                annotations( annotations() ).
                build().
                execute();
            metadata.setResourceVersion( null );
            method = "Updated";
        }
        else
        {
            metadata = new ObjectMeta( annotations(), null, null, null, null, null, null, null, labels(), null, name(), namespace(),
                                       List.of( ownerReference() ), null, null, null );
            method = "Created";
        }

        resource = apply( metadata );
        log.info( method + " in Namespace '" + namespace() + "' " + resource.getKind() + " '" + name() + "'" );
        return resource;
    }

    @Value.Derived
    protected abstract T fetchResource();

    @Value.Derived
    protected abstract T apply( ObjectMeta metadata );

}
