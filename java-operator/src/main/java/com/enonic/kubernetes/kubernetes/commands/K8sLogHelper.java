package com.enonic.kubernetes.kubernetes.commands;

import java.net.HttpURLConnection;
import java.util.function.UnaryOperator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.Resource;

public class K8sLogHelper
{
    private static final Logger log = LoggerFactory.getLogger( K8sLogHelper.class );

    private static final int MAX_EDIT_ATTEMPTS = 10;

    public static String log( final K8sCommandAction action, final HasMetadata resource )
    {
        return String.format( "K8s: %s %s '%s' in NS '%s'", action, resource.getKind(), resource.getMetadata().getName(),
                              resource.getMetadata().getNamespace() );
    }

    /**
     * Replaces the resource with {@code op} applied to what the server holds now. An unlocked replace
     * retries a conflict by resending the same object under the newest resourceVersion, which puts back
     * whatever another writer changed in between, such as cloud-api's data while a status is set here.
     * So the version that was read is locked, and a conflict reads again and redoes the edit.
     */
    public static <T extends HasMetadata> void logEdit( final Resource<T> editable, UnaryOperator<T> op ) {
        for (int attempt = 1; ; attempt++) {
            T current = editable.get();
            if (current.getMetadata() != null) {
                current.getMetadata().setManagedFields( null );
            }
            String resourceVersion = current.getMetadata().getResourceVersion();
            T updated = op.apply( current );
            try {
                T res = editable.lockResourceVersion( resourceVersion ).replace( updated );
                log.info( log( K8sCommandAction.UPDATE, res ) );
                return;
            } catch (KubernetesClientException e) {
                if (e.getCode() != HttpURLConnection.HTTP_CONFLICT || attempt == MAX_EDIT_ATTEMPTS) {
                    throw e;
                }
                log.debug( "Conflict on attempt {}, editing again: {}", attempt, log( K8sCommandAction.UPDATE, current ) );
            }
        }
    }

    public static <T extends HasMetadata> void logDelete( Resource<T> r )
    {
        T res = r.get();
        log.info( log( K8sCommandAction.DELETE, res ) );
        r.delete();
    }
}
