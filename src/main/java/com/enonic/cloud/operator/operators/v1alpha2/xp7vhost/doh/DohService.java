package com.enonic.cloud.operator.operators.v1alpha2.xp7vhost.doh;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@Path("/dns-query")
@RegisterRestClient(configKey = "dns.doh")
public interface DohService
{
    @GET
    @Path("/")
    @Produces("application/json")
    DohResponse query( @QueryParam("type") String type, @QueryParam("name") String name );

}
