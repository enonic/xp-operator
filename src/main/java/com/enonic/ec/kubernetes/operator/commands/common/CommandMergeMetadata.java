package com.enonic.ec.kubernetes.operator.commands.common;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.OwnerReference;

import com.enonic.ec.kubernetes.common.commands.Command;

import static com.enonic.ec.kubernetes.common.assertions.Assertions.assertNotNull;

public class CommandMergeMetadata
    implements Command<ObjectMeta>
{
    private final static Logger log = LoggerFactory.getLogger( CommandMergeMetadata.class );

    private final String kind;

    private final ObjectMeta objectMeta;

    private final OwnerReference ownerReference;

    private final Map<String, String> labels;

    private final Map<String, String> annotations;

    private CommandMergeMetadata( final Builder builder )
    {
        kind = assertNotNull( "kind", builder.kind );
        objectMeta = assertNotNull( "objectMeta", builder.objectMeta );
        ownerReference = assertNotNull( "ownerReference", builder.ownerReference );
        labels = builder.labels;
        annotations = builder.annotations;
    }

    public static Builder newBuilder()
    {
        return new Builder();
    }

    @Override
    public ObjectMeta execute()
    {
        mergeOwnerReferences( objectMeta, ownerReference );
        objectMeta.setLabels( mergeMap( kind, objectMeta.getName(), "label", objectMeta.getLabels(), labels ) );
        objectMeta.setAnnotations( mergeMap( kind, objectMeta.getName(), "annotation", objectMeta.getAnnotations(), annotations ) );
        return objectMeta;
    }

    private static void mergeOwnerReferences( final ObjectMeta objectMeta, final OwnerReference ownerReference )
    {
        if ( ownerReference == null )
        {
            return;
        }

        if ( objectMeta.getOwnerReferences() == null || objectMeta.getOwnerReferences().size() == 0 )
        {
            objectMeta.setOwnerReferences( List.of( ownerReference ) );
            return;
        }

        for ( OwnerReference existingOwnerReference : objectMeta.getOwnerReferences() )
        {
            if ( existingOwnerReference.getUid().equals( ownerReference.getUid() ) )
            {
                return;
            }
        }

        objectMeta.getOwnerReferences().add( ownerReference );
    }

    private static Map<String, String> mergeMap( String kind, String name, String mapType, final Map<String, String> oldMap,
                                                 final Map<String, String> newMap )
    {
        if ( oldMap == null )
        {
            return newMap;
        }

        if ( newMap == null || newMap.size() == 0 )
        {
            return oldMap;
        }

        for ( Map.Entry<String, String> e : newMap.entrySet() )
        {
            String tmp = oldMap.get( e.getKey() );
            if ( tmp != null && !e.getValue().equals( tmp ) )
            {
                log.warn( "Changing " + mapType + " on " + kind + " '" + name + "' from '" + tmp + "' to '" + e.getValue() + "'" );
            }
            oldMap.put( e.getKey(), e.getValue() );
        }

        return oldMap;
    }

    public static final class Builder
    {
        private ObjectMeta objectMeta;

        private OwnerReference ownerReference;

        private Map<String, String> labels;

        private Map<String, String> annotations;

        private String kind;

        private Builder()
        {
        }

        public Builder objectMeta( final ObjectMeta val )
        {
            objectMeta = val;
            return this;
        }

        public Builder ownerReference( final OwnerReference val )
        {
            ownerReference = val;
            return this;
        }

        public Builder labels( final Map<String, String> val )
        {
            labels = val;
            return this;
        }

        public Builder annotations( final Map<String, String> val )
        {
            annotations = val;
            return this;
        }

        public CommandMergeMetadata build()
        {
            return new CommandMergeMetadata( this );
        }

        public Builder kind( final String val )
        {
            kind = val;
            return this;
        }
    }
}
