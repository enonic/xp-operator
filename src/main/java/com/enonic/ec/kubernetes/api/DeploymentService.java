package com.enonic.ec.kubernetes.api;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import com.enonic.ec.kubernetes.deployment.CommandCreateXpDeployment;
import com.enonic.ec.kubernetes.deployment.CommandDeleteXpDeployment;
import com.enonic.ec.kubernetes.deployment.CrdClientsProducer;
import com.enonic.ec.kubernetes.deployment.XpDeployment.XpDeploymentResource;
import com.enonic.ec.kubernetes.deployment.XpDeploymentCache;

@ApplicationScoped
@Path("/api")
public class DeploymentService
{

    @Inject
    CrdClientsProducer.XpDeploymentClient xpDeploymentClient;

    @Inject
    XpDeploymentCache xpDeploymentCache;

    @GET
    @Path("/")
    @Produces("application/json")
    public List<XpDeploymentJson> getXpDeployments()
    {
        return xpDeploymentCache.stream().map( DeploymentService::resourceToJson ).collect( Collectors.toList() );
    }

    @POST
    @Path("/")
    @Consumes("application/json")
    @Produces("application/json")
    public Response createXpDeployment( XpDeploymentJson deployment, @Context UriInfo uriInfo )
    {
        XpDeploymentResource resource = CommandCreateXpDeployment.newBuilder().
            client( xpDeploymentClient ).
            apiVersion( deployment.getApiVersion() ).
            spec( deployment.getSpec() ).
            build().
            execute();

        UriBuilder builder = uriInfo.getAbsolutePathBuilder();
        builder.path( resource.getMetadata().getUid() );

        return Response.created( builder.build() ).entity( resourceToJson( resource ) ).build();
    }

    @GET
    @Path("/{uid}")
    @Produces("application/json")
    public Response getXpDeployment( @PathParam("uid") String uid )
    {
        XpDeploymentResource resource = xpDeploymentCache.get( uid );
        if ( resource == null )
        {
            return Response.status( 404 ).build();
        }
        return Response.ok( resourceToJson( resource ) ).build();
    }

    @PUT
    @Path("/{uid}")
    public Response editXpDeployment( @PathParam("uid") String uid, XpDeploymentJson deployment )
    {
        XpDeploymentResource resource = xpDeploymentCache.get( uid );
        if ( resource == null )
        {
            return Response.status( 404 ).build();
        }

        if ( !resource.getApiVersion().equals( deployment.getApiVersion() ) )
        {
            return Response.status( 400, "cannot update deployment to a different apiVersion" ).build();
        }

        resource.setSpec( deployment.getSpec() );
        xpDeploymentClient.getClient().createOrReplace( resource );

        return Response.ok( resourceToJson( resource ) ).build();
    }

    @DELETE
    @Path("/{uid}")
    public Response deleteXpDeployment( @PathParam("uid") String uid )
    {
        XpDeploymentResource resource = xpDeploymentCache.get( uid );
        if ( resource == null )
        {
            return Response.status( 404 ).build();
        }

        CommandDeleteXpDeployment.newBuilder().
            client( xpDeploymentClient ).
            resource( resource ).
            build().
            execute();

        return Response.ok().build();
    }

    private static XpDeploymentJson resourceToJson( XpDeploymentResource r )
    {
        return XpDeploymentJson.newBuilder().
            apiVersion( r.getApiVersion() ).
            uid( r.getMetadata().getUid() ).
            spec( r.getSpec() ).
            build();
    }

}
