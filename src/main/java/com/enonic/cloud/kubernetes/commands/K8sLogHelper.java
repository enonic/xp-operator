package com.enonic.cloud.kubernetes.commands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.Doneable;
import io.fabric8.kubernetes.api.model.HasMetadata;

public class K8sLogHelper
{
    private static final Logger log = LoggerFactory.getLogger( K8sLogHelper.class );

    public static String log( final K8sCommandAction action, final HasMetadata resource )
    {
        return String.format( "K8s: %s %s '%s' in NS '%s'", action, resource.getKind(), resource.getMetadata().getName(),
                              resource.getMetadata().getNamespace() );
    }

    public static <T extends HasMetadata> void logDoneable( final Doneable<T> doneable )
    {
        T res = doneable.done();
        log.info( log( K8sCommandAction.UPDATE, res ) );
    }
}
