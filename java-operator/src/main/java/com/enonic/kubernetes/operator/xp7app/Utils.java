package com.enonic.kubernetes.operator.xp7app;

import com.enonic.kubernetes.client.v1.xp7app.Xp7App;
import com.enonic.kubernetes.kubernetes.Clients;
import com.enonic.kubernetes.kubernetes.commands.K8sLogHelper;

public class Utils
{
    static void triggerAppReinstall( final Clients clients, final Xp7App app )
    {
        K8sLogHelper.logEdit( clients.xp7Apps()
            .inNamespace( app.getMetadata().getNamespace() )
            .withName( app.getMetadata().getName() ), a -> {
            // We do this by resetting the whole status object
            a.setStatus( null );
            return a;
        } );
    }
}
