package com.enonic.ec.kubernetes.operator.kubectl.apply;

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

@SuppressWarnings("unchecked")
@Value.Immutable
public abstract class CommandMergeMetadata
    implements Command<ObjectMeta>
{
    private final static Logger log = LoggerFactory.getLogger( CommandMergeMetadata.class );

    protected abstract String kind();

    protected abstract ObjectMeta metadata();

    protected abstract Optional<OwnerReference> ownerReference();

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
        if ( ownerReference().isEmpty() )
        {
            return;
        }

        if ( metadata().getOwnerReferences() == null || metadata().getOwnerReferences().size() == 0 )
        {
            metadata().setOwnerReferences( List.of( ownerReference().get() ) );
            return;
        }

        for ( OwnerReference existingOwnerReference : metadata().getOwnerReferences() )
        {
            if ( existingOwnerReference.getUid().equals( ownerReference().get().getUid() ) )
            {
                return;
            }
        }
        metadata().getOwnerReferences().add( ownerReference().get() );
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static Map<String, String> mergeMap( String kind, String name, String mapType, final Map<String, String> oldMap,
                                                 final Optional<Map<String, String>> newMap )
    {
        if ( oldMap == null || oldMap.size() == 0 )
        {
            return newMap.orElse( Collections.EMPTY_MAP );
        }

        if ( newMap.isEmpty() )
        {
            return oldMap;
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
