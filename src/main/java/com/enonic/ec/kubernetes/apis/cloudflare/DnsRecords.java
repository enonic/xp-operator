package com.enonic.ec.kubernetes.apis.cloudflare;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.enonic.ec.kubernetes.apis.cloudflare.model.DnsRecord;
import com.enonic.ec.kubernetes.apis.cloudflare.model.DnsRecordCreateResponse;
import com.enonic.ec.kubernetes.apis.cloudflare.model.DnsRecordDeleteResponse;
import com.enonic.ec.kubernetes.apis.cloudflare.model.DnsRecordListResponse;

@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@Path("/zones/{zone_identifier}/dns_records")
@RegisterRestClient(configKey = "dns.cloudflare")
public interface DnsRecords
{
    @GET
    @Path("/")
    @Produces("application/json")
    @ClientHeaderParam(name = "Authorization", value = "{com.enonic.ec.kubernetes.apis.cloudflare.Auth.getApiToken}")
    DnsRecordListResponse list( @PathParam("zone_identifier") String zone_identifier, @QueryParam("name") String name,
                                @QueryParam("type") String type );

    @POST
    @Path("/")
    @Consumes("application/json")
    @Produces("application/json")
    @ClientHeaderParam(name = "Authorization", value = "{com.enonic.ec.kubernetes.apis.cloudflare.Auth.getApiToken}")
    DnsRecordCreateResponse create( @PathParam("zone_identifier") String zone_identifier, DnsRecord record );

    @PUT
    @Path("/{record_identifier}")
    @Consumes("application/json")
    @Produces("application/json")
    @ClientHeaderParam(name = "Authorization", value = "{com.enonic.ec.kubernetes.apis.cloudflare.Auth.getApiToken}")
    DnsRecordCreateResponse update( @PathParam("zone_identifier") String zone_identifier,
                                    @PathParam("record_identifier") String record_identifier, DnsRecord record );

    @DELETE
    @Path("/{record_identifier}")
    @Produces("application/json")
    @ClientHeaderParam(name = "Authorization", value = "{com.enonic.ec.kubernetes.apis.cloudflare.Auth.getApiToken}")
    DnsRecordDeleteResponse delete( @PathParam("zone_identifier") String zone_identifier,
                                    @PathParam("record_identifier") String record_identifier );

}
