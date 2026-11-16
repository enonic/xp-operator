package com.enonic.kubernetes.operator.ingress;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A vhost mapping of an ingress.
 *
 * @param endpoint the XP endpoint the mapping serves; unset, the web endpoint
 * @param allow    principals the mapping lets in, e.g. role:system.admin
 * @param flows    login flows by id provider key, e.g. enonic-cloud -> autologin,k8s
 * @param authorizationServers issuer URL of the authorization server by id provider key, for OAuth
 *                 discovery on a mapping that issues no tokens itself
 * @param context  vhost context settings by key, e.g. api.rest.enabled -> false, in the order given
 */
@NullMarked
public record Mapping(
    String name,
    @Nullable String host,
    String source,
    String target,
    @Nullable String idProviders,
    @Nullable String endpoint,
    @Nullable String allow,
    Map<String, String> flows,
    Map<String, String> authorizationServers,
    Map<String, String> context )
{
    private static final Pattern CONTEXT_KEY = Pattern.compile( "[^\\s=#!\\\\][^\\s=\\\\]*" );

    public static final String MANAGEMENT_ENDPOINT = "management";

    public static final int WEB_PORT = 8080;

    public static final int MANAGEMENT_PORT = 4848;

    public Mapping
    {
        Objects.requireNonNull( name, "name" );
        Objects.requireNonNull( source, "source" );
        Objects.requireNonNull( target, "target" );
        flows = Map.copyOf( flows );
        authorizationServers = Map.copyOf( authorizationServers );
        context = Collections.unmodifiableMap( new LinkedHashMap<>( context ) );
    }

    public static Map<String, String> parseContext( final @Nullable String value )
    {
        final Map<String, String> context = new LinkedHashMap<>();
        if ( value == null )
        {
            return context;
        }
        for ( String line : value.split( "\\R" ) )
        {
            if ( line.isBlank() )
            {
                continue;
            }
            final int eq = line.indexOf( '=' );
            if ( eq < 0 )
            {
                throw new IllegalArgumentException( String.format( "context line '%s' has no '='", line.strip() ) );
            }
            final String key = line.substring( 0, eq ).strip();
            if ( !CONTEXT_KEY.matcher( key ).matches() )
            {
                throw new IllegalArgumentException( String.format( "context key '%s' is not a valid configuration key", key ) );
            }
            final String setting = line.substring( eq + 1 ).strip();
            if ( setting.contains( "\\" ) )
            {
                throw new IllegalArgumentException( String.format( "context value of '%s' contains a backslash", key ) );
            }
            if ( context.put( key, setting ) != null )
            {
                throw new IllegalArgumentException( String.format( "context key '%s' is given more than once", key ) );
            }
        }
        return context;
    }

    public Mapping withHost( final String host )
    {
        return new Mapping( name, host, source, target, idProviders, endpoint, allow, flows, authorizationServers, context );
    }

    /**
     * The backend port of the ingress paths the mapping applies to, or -1 for an endpoint that has
     * none.
     */
    public int port()
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
    public boolean appliesTo( final String path, final @Nullable Integer port )
    {
        if ( port == null || port != port() )
        {
            return false;
        }
        return endpoint() != null || source().equals( path );
    }
}
