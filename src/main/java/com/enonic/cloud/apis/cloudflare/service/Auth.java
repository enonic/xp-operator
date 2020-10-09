package com.enonic.cloud.apis.cloudflare.service;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.enterprise.event.Observes;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.runtime.StartupEvent;

import com.enonic.cloud.common.Configuration;


public class Auth
{
    private static final Logger log = LoggerFactory.getLogger( Auth.class );

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
            if ( token.equals( "not_set" ) )
            {
                log.warn(
                    "Cloudflare token not set. Api calls will fail. Set with property 'dns.cloudflare.apiToken' or environmental variable 'DNS_CLOUDFLARE_APITOKEN'" );
            }
            else
            {
                Auth.setApiToken( token );
                tokenSet.set( true );
            }
        } );
    }
}
