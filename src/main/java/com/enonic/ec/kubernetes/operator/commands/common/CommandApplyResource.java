package com.enonic.ec.kubernetes.operator.commands.common;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.OwnerReference;

import com.enonic.ec.kubernetes.common.commands.Command;

import static com.enonic.ec.kubernetes.common.assertions.Assertions.assertNotNull;

public abstract class CommandApplyResource<T extends HasMetadata>
    implements Command<T>
{
    private final static Logger log = LoggerFactory.getLogger( CommandApplyResource.class );

    protected final String name;

    protected final String namespace;

    protected final OwnerReference ownerReference;

    protected final Map<String, String> labels;

    protected final Map<String, String> annotations;

    protected CommandApplyResource( final Builder<?> builder )
    {
        name = assertNotNull( "name", builder.name );
        namespace = assertNotNull( "namespace", builder.namespace );
        ownerReference = assertNotNull( "ownerReference", builder.ownerReference );
        labels = builder.labels;
        annotations = builder.annotations;
    }

    public static Builder newBuilder()
    {
        return new Builder();
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
            if ( !resource.getMetadata().getName().equals( name ) )
            {
                throw new Exception( "Resource names do not match" );
            }
            if ( !resource.getMetadata().getNamespace().equals( namespace ) )
            {
                throw new Exception( "Resource namespace do not match" );
            }
            metadata = CommandMergeMetadata.newBuilder().
                kind( resource.getKind() ).
                objectMeta( resource.getMetadata() ).
                ownerReference( ownerReference ).
                labels( labels ).
                annotations( annotations ).
                build().
                execute();
            method = "Updated";
        }
        else
        {
            metadata = new ObjectMeta( annotations, null, null, null, null, null, null, null, labels, null, name, namespace,
                                       List.of( ownerReference ), null, null, null );
            method = "Created";
        }

        resource = apply( metadata );
        log.info( method + " in Namespace '" + namespace + "' " + resource.getKind() + " '" + name + "'" );
        return resource;
    }


    protected abstract T fetchResource();

    protected abstract T apply( ObjectMeta metadata );

    public static class Builder<B extends Builder>
    {
        private String name;

        private String namespace;

        private OwnerReference ownerReference;

        private Map<String, String> labels;

        private Map<String, String> annotations;

        protected Builder()
        {
        }

        public B name( final String val )
        {
            name = val;
            return (B) this;
        }

        public B namespace( final String val )
        {
            namespace = val;
            return (B) this;
        }

        public B ownerReference( final OwnerReference val )
        {
            ownerReference = val;
            return (B) this;
        }

        public B labels( final Map<String, String> val )
        {
            labels = val;
            return (B) this;
        }

        public B annotations( final Map<String, String> val )
        {
            annotations = val;
            return (B) this;
        }
    }
}
