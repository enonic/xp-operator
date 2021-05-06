package com.enonic.kubernetes.operator.v1alpha1xp7app;

import com.enonic.kubernetes.client.v1alpha1.Xp7App;
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
