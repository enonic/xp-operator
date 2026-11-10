package com.enonic.kubernetes.operator.ingress;

import java.util.Map;

import org.immutables.value.Value;
import io.micronaut.core.annotation.Nullable;

import com.enonic.kubernetes.common.annotations.Params;

@Value.Immutable
@Params
public interface Mapping
{
    String MANAGEMENT_ENDPOINT = "management";

    int WEB_PORT = 8080;

    int MANAGEMENT_PORT = 4848;

    String name();

    @Nullable
    String host();

    String source();

    String target();

    @Nullable
    String idProviders();

    // The XP endpoint the mapping serves; unset, the web endpoint.
    @Nullable
    String endpoint();

    // Principals the mapping lets in, e.g. role:system.admin.
    @Nullable
    String allow();

    // Login flows by id provider key, e.g. enonic-cloud -> autologin,k8s.
    Map<String, String> flows();

    /**
     * The backend port of the ingress paths the mapping applies to, or -1 for an endpoint that has
     * none.
     */
    default int port()
    {
        if ( endpoint() == null )
        {
            return WEB_PORT;
        }
        return MANAGEMENT_ENDPOINT.equals( endpoint() ) ? MANAGEMENT_PORT : -1;
    }

    /**
     * Whether the mapping applies to an ingress path. A web mapping names the path it maps as its
     * source; a management mapping covers every path routed to the management port.
     */
    default boolean appliesTo( final String path, final Integer port )
    {
        if ( port == null || port != port() )
        {
            return false;
        }
        return endpoint() != null || source().equals( path );
    }
}
