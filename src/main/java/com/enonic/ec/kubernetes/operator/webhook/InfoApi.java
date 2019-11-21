package com.enonic.ec.kubernetes.operator.webhook;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@ApplicationScoped
@Path("/apis/operator.enonic.cloud/v1alpha1")
public class InfoApi
{
    @GET
    @Path("/")
    @Produces("application/json")
    public Map<String, String> info()
    {
        return Map.of("operator", "online");
    }
}
