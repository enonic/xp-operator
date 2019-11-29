package com.enonic.ec.kubernetes.dns.cloudflare;

import javax.enterprise.event.Observes;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.runtime.StartupEvent;

public class Auth
{
    private static String apiToken;

    public static void setApiToken( final String apiToken )
    {
        Auth.apiToken = apiToken;
    }

    public static String getApiToken()
    {
        return "Bearer " + apiToken;
    }

    @ConfigProperty(name = "dns.cloudflare.apiToken", defaultValue = "not_set")
    private String token;

    void onStart( @Observes StartupEvent ev )
    {
        Auth.setApiToken( token );
    }
}
