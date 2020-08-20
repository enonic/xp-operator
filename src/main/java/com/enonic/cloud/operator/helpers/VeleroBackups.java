package com.enonic.cloud.operator.helpers;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import io.fabric8.kubernetes.api.model.HasMetadata;

import static com.enonic.cloud.common.Configuration.cfgStr;

public class VeleroBackups
{
    public static boolean backupRestoreInProgress( final HasMetadata hasMetadata )
    {
        Map<String, String> labels = hasMetadata.getMetadata().getLabels();

        if ( labels == null )
        {
            return false;
        }

        if ( !labels.containsKey( cfgStr( "operator.labels.velero.backupName" ) ) ||
            !labels.containsKey( cfgStr( "operator.labels.velero.backupRestore" ) ) )
        {
            return false;
        }

        // If resource younger than a minute
        return Duration.between( Instant.parse( hasMetadata.getMetadata().getCreationTimestamp() ), Instant.now() ).getSeconds() < 60;
    }
}
