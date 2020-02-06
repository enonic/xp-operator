package com.enonic.ec.kubernetes.operator.operators.dns.cloudflare;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.enterprise.event.Observes;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.google.common.base.Preconditions;

import io.quarkus.runtime.StartupEvent;

import com.enonic.ec.kubernetes.operator.common.Configuration;

public class Auth
{
    private static final AtomicBoolean tokenSet = new AtomicBoolean( false );

    private static String apiToken;

    @ConfigProperty(name = "dns.cloudflare.apiToken", defaultValue = "not_set")
    String token;

    @SuppressWarnings("unused") // It is used
    public static String getApiToken()
    {
        if ( !tokenSet.get() )
        {
            try
            {
                Thread.sleep( 1000 );
            }
            catch ( InterruptedException e )
            {
                // Not a big deal
            }
        }
        return "Bearer " + apiToken;
    }

    private static void setApiToken( final String apiToken )
    {
        Auth.apiToken = apiToken;
    }

    void onStart( @Observes StartupEvent ev )
    {
        Configuration.cfgIfBool( "dns.enabled", () -> {
            Preconditions.checkState( !token.equals( "not_set" ),
                                      "You have to set the DNS token with properties 'dns.cloudflare.apiToken'" );
            Auth.setApiToken( token );
            tokenSet.set( true );
        } );
    }
}
