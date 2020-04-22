package com.enonic.cloud.operator.operators.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.Watcher;

public class BackupRestore
{
    private static final Logger log = LoggerFactory.getLogger( BackupRestore.class );

    public static boolean isBeingRestored( String actionId, Watcher.Action action, HasMetadata resource )
    {
        if ( resource.getMetadata().getLabels() == null )
        {
            return false;
        }

        boolean isBeingRestored =
            action == Watcher.Action.ADDED && resource.getMetadata().getLabels().containsKey( "velero.io/restore-name" );

        if ( isBeingRestored )
        {
            log.info(
                String.format( "%s: BACKUP RESTORE in NS '%s' %s '%s'", actionId, resource.getMetadata().getNamespace(), resource.getKind(),
                               resource.getMetadata().getName() ) );
        }

        return isBeingRestored;
    }
}
