package com.enonic.ec.kubernetes.operator.commands.apply;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    protected abstract Optional<Map<String, String>> labels();

    protected abstract Optional<Map<String, String>> annotations();

    @Override
    public ObjectMeta execute()
    {
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
                                                 final Optional<Map<String, String>> newMap )
    {
        if ( oldMap == null )
        {
            return newMap.orElse( Collections.EMPTY_MAP );
        }

        if ( newMap.isPresent() || newMap.get().size() == 0 )
        {
            return Collections.EMPTY_MAP;
        }

        for ( Map.Entry<String, String> e : newMap.get().entrySet() )
        {
            Optional<String> tmp = Optional.ofNullable( oldMap.get( e.getKey() ) );
            if ( tmp.isPresent() && !e.getValue().equals( tmp.get() ) )
            {
                log.warn( "Changing " + mapType + " on " + kind + " '" + name + "' from '" + tmp.get() + "' to '" + e.getValue() + "'" );
            }
            oldMap.put( e.getKey(), e.getValue() );
        }

        return oldMap;
    }
}
