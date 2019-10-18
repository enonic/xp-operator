package com.enonic.ec.kubernetes.api;

import java.util.List;
import java.util.Optional;
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

import com.enonic.ec.kubernetes.deployment.CrdClientsProducer;
import com.enonic.ec.kubernetes.deployment.ImmutableCommandCreateXpDeployment;
import com.enonic.ec.kubernetes.deployment.ImmutableCommandDeleteXpDeployment;
import com.enonic.ec.kubernetes.deployment.XpDeploymentCache;
import com.enonic.ec.kubernetes.deployment.xpdeployment.XpDeploymentResource;
import com.enonic.ec.kubernetes.deployment.xpdeployment.XpDeploymentSpecChangeValidation;

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
    public List<String> getXpDeployments()
    {
        return xpDeploymentCache.stream().map( e -> e.getMetadata().getUid() ).collect( Collectors.toList() );
    }

    @POST
    @Path("/")
    @Consumes("application/json")
    @Produces("application/json")
    public Response createXpDeployment( XpDeploymentJson deployment, @Context UriInfo uriInfo )
    {
        Optional<XpDeploymentResource> old =
            xpDeploymentCache.stream().filter( d -> d.getSpec().fullName().equals( deployment.spec().fullName() ) ).findAny();
        if ( old.isPresent() )
        {
            XpDeploymentSpecChangeValidation.check( old.get().getSpec(), deployment.spec() );
        }

        XpDeploymentResource resource = ImmutableCommandCreateXpDeployment.builder().
            client( xpDeploymentClient ).
            apiVersion( deployment.apiVersion() ).
            spec( deployment.spec() ).
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

        if ( !resource.getApiVersion().equals( deployment.apiVersion() ) )
        {
            return Response.status( 400, "cannot update deployment to a different apiVersion" ).build();
        }

        XpDeploymentSpecChangeValidation.check( resource.getSpec(), deployment.spec() );

        resource.setSpec( deployment.spec() );
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

        ImmutableCommandDeleteXpDeployment.builder().
            client( xpDeploymentClient ).
            resource( resource ).
            build().
            execute();

        return Response.ok().build();
    }

    private static XpDeploymentJson resourceToJson( XpDeploymentResource r )
    {
        return ImmutableXpDeploymentJson.builder().
            apiVersion( r.getApiVersion() ).
            uid( r.getMetadata().getUid() ).
            spec( r.getSpec() ).
            build();
    }

}
