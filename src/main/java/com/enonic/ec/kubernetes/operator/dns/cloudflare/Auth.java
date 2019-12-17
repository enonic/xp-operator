package com.enonic.ec.kubernetes.operator.dns.cloudflare;

import javax.enterprise.event.Observes;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.runtime.StartupEvent;

public class Auth
{
    private static String apiToken;

    @ConfigProperty(name = "dns.cloudflare.apiToken", defaultValue = "not_set")
    String token;

    public static String getApiToken()
    {
        return "Bearer " + apiToken;
    }

    public static void setApiToken( final String apiToken )
    {
        Auth.apiToken = apiToken;
    }

    void onStart( @Observes StartupEvent ev )
    {
        Auth.setApiToken( token );
    }
}
