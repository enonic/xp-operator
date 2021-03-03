package com.enonic.cloud.apis.doh.service;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@RegisterRestClient(configKey = "dns.doh")
public interface DohService
{
    @GET
    @Path("/dns-query")
    @Produces("application/dns-json")
    DohResponse query( @QueryParam("type") String type, @QueryParam("name") String name );

}
