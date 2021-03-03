package com.enonic.kubernetes.apis.xp.service;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("/")
public interface ManagementApi
{
    @POST
    @Path("/app/installUrl")
    @Consumes("application/json")
    @Produces("application/json")
    AppInstallResponse appInstall( AppInstallRequest req );

    @POST
    @Path("/app/uninstall")
    @Consumes("application/json")
    void appUninstall( AppKey req );

    @POST
    @Path("/app/start")
    @Consumes("application/json")
    void appStart( AppKey req );

    @POST
    @Path("/app/stop")
    @Consumes("application/json")
    void appStop( AppKey req );
}
