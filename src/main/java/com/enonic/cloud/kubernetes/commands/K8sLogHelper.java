package com.enonic.cloud.kubernetes.commands;

import java.util.function.UnaryOperator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.builder.Visitor;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.dsl.Editable;
import io.fabric8.kubernetes.client.dsl.Resource;

public class K8sLogHelper
{
    private static final Logger log = LoggerFactory.getLogger( K8sLogHelper.class );

    public static String log( final K8sCommandAction action, final HasMetadata resource )
    {
        return String.format( "K8s: %s %s '%s' in NS '%s'", action, resource.getKind(), resource.getMetadata().getName(),
                              resource.getMetadata().getNamespace() );
    }

    public static <T extends HasMetadata> void logEdit( final Editable<T> editable, UnaryOperator<T> op ) {
        T res = editable.edit( op );
        log.info( log( K8sCommandAction.UPDATE, res ) );
    }

    public static <T extends HasMetadata> void logDelete( Resource<T> r )
    {
        T res = r.get();
        log.info( log( K8sCommandAction.DELETE, res ) );
        r.delete();
    }
}
