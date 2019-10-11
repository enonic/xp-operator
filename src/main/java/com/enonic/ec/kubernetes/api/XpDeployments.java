package com.enonic.ec.kubernetes.api;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import com.enonic.ec.kubernetes.common.commands.CommandCreateXpDeployment;
import com.enonic.ec.kubernetes.common.commands.CommandDeleteXpDeployment;
import com.enonic.ec.kubernetes.common.crd.CrdClientsProducer;
import com.enonic.ec.kubernetes.common.crd.XpDeployment.XpDeploymentResource;
import com.enonic.ec.kubernetes.common.crd.XpDeploymentCache;

@ApplicationScoped
@Path("/xp")
public class XpDeployments
{

    @Inject
    CrdClientsProducer.XpDeploymentClient xpDeploymentClient;

    @Inject
    XpDeploymentCache xpDeploymentCache;

    @GET
    @Path("/")
    @Produces("application/json")
    public List<String> getXpDeployments()
    {
        // TODO: Create a command?
        return xpDeploymentCache.list().stream().map( r -> r.getMetadata().getUid() ).collect( Collectors.toList() );
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

        return Response.created( builder.build() ).build();
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
        return Response.ok( XpDeploymentJson.
            newBuilder().
            apiVersion( resource.getApiVersion() ).
            uid( resource.getMetadata().getUid() ).
            spec( resource.getSpec() ).
            build() ).build();
    }

//    @PUT
//    @Path("/{uid}")
//    public Response editXpDeployment( @PathParam("uid") String uid, XpDeployment deployment )
//    {
//        XpDeploymentResource resource = xpDeploymentCache.get( uid );
//        if ( resource == null )
//        {
//            return Response.status( 404 ).build();
//        }
//        resource.setSpec( deployment.spec ); // TODO: Do this better
//
//        xpDeploymentClient.getClient().createOrReplace( resource );
//
//        return Response.ok().build();
//    }

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

}
