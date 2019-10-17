package com.enonic.ec.kubernetes.operator.commands.apply;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wildfly.common.annotation.Nullable;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.OwnerReference;

import com.enonic.ec.kubernetes.common.commands.Command;

@Value.Immutable
public abstract class CommandMergeMetadata
    implements Command<ObjectMeta>
{
    private final static Logger log = LoggerFactory.getLogger( CommandMergeMetadata.class );

    protected abstract String kind();

    protected abstract ObjectMeta metadata();

    protected abstract OwnerReference ownerReference();

    @Nullable
    protected abstract Map<String, String> labels();

    @Nullable
    protected abstract Map<String, String> annotations();

    @Override
    public ObjectMeta execute()
    {
        // TODO: Create new objects
        mergeOwnerReferences();
        metadata().setLabels( mergeMap( kind(), metadata().getName(), "label", metadata().getLabels(), labels() ) );
        metadata().setAnnotations( mergeMap( kind(), metadata().getName(), "annotation", metadata().getAnnotations(), annotations() ) );
        return metadata();
    }

    private void mergeOwnerReferences()
    {
        if ( metadata().getOwnerReferences() == null || metadata().getOwnerReferences().size() == 0 )
        {
            metadata().setOwnerReferences( List.of( ownerReference() ) );
            return;
        }

        for ( OwnerReference existingOwnerReference : metadata().getOwnerReferences() )
        {
            if ( existingOwnerReference.getUid().equals( ownerReference().getUid() ) )
            {
                return;
            }
        }
        metadata().getOwnerReferences().add( ownerReference() );
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
}
