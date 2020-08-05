package com.enonic.cloud.apis.xp.service;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("/admin/rest")
public interface AdminApi
{
    @GET
    @Path("/application/list")
    @Produces("application/json")
    AppListResponse appList();

    @POST
    @Path("/application/start")
    @Consumes("application/json")
    @Produces("application/json")
    Void appStart( AppKeyList req );

    @POST
    @Path("/application/stop")
    @Consumes("application/json")
    @Produces("application/json")
    Void appStop( AppKeyList req );

    @POST
    @Path("/application/uninstall")
    @Consumes("application/json")
    @Produces("application/json")
    Void appUninstall( AppKeyList req );

    @POST
    @Path("/application/installUrl")
    @Consumes("application/json")
    @Produces("application/json")
    AppInstallResponse appInstall( AppInstallRequest req );
}
