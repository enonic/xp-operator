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

import com.enonic.ec.kubernetes.deployment.XpDeploymentCache;
import com.enonic.ec.kubernetes.deployment.XpDeploymentClientProducer;
import com.enonic.ec.kubernetes.deployment.XpDeploymentResource;
import com.enonic.ec.kubernetes.deployment.commands.ImmutableCommandCreateXpDeployment;
import com.enonic.ec.kubernetes.deployment.commands.ImmutableCommandDeleteXpDeployment;
import com.enonic.ec.kubernetes.deployment.diff.ImmutableDiffSpec;

@ApplicationScoped
@Path("/api")
public class DeploymentService
{
    @Inject
    XpDeploymentClientProducer xpDeploymentClientProducer;

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
        // Check if there is and old one present
        Optional<XpDeploymentResource> old = xpDeploymentCache.stream().
            filter( d -> d.getSpec().deploymentName().equals( deployment.spec().deploymentName() ) ).
            findAny();

        // Just to validate the changes
        old.ifPresent( o -> ImmutableDiffSpec.builder().
            oldValue( o.getSpec() ).
            newValue( deployment.spec() ).
            build() );

        // Send the new resource to kubernetes
        XpDeploymentResource resource = ImmutableCommandCreateXpDeployment.builder().
            client( xpDeploymentClientProducer.produce() ).
            apiVersion( deployment.apiVersion() ).
            spec( deployment.spec() ).
            build().
            execute();

        // Create response
        UriBuilder builder = uriInfo.getAbsolutePathBuilder();
        builder.path( resource.getMetadata().getUid() );
        return Response.created( builder.build() ).entity( resourceToJson( resource ) ).build();
    }

    @GET
    @Path("/{uid}")
    @Produces("application/json")
    public Response getXpDeployment( @PathParam("uid") String uid )
    {
        Optional<XpDeploymentResource> resource = xpDeploymentCache.get( uid );
        if ( resource.isEmpty() )
        {
            return Response.status( 404 ).build();
        }
        return Response.ok( resourceToJson( resource.get() ) ).build();
    }

    @PUT
    @Path("/{uid}")
    public Response editXpDeployment( @PathParam("uid") String uid, XpDeploymentJson deployment )
    {
        // Get old one
        Optional<XpDeploymentResource> resource = xpDeploymentCache.get( uid );
        if ( resource.isEmpty() )
        {
            return Response.status( 404 ).build();
        }

        // Validate the changes
        if ( !resource.get().getApiVersion().equals( deployment.apiVersion() ) )
        {
            return Response.status( 400, "cannot update deployment to a different apiVersion" ).build();
        }
        ImmutableDiffSpec.builder().
            oldValue( resource.get().getSpec() ).
            newValue( deployment.spec() ).
            build();

        // Update
        resource.get().setSpec( deployment.spec() );
        xpDeploymentClientProducer.produce().client().
            createOrReplace( resource.get() );

        return Response.ok( resourceToJson( resource.get() ) ).build();
    }

    @DELETE
    @Path("/{uid}")
    public Response deleteXpDeployment( @PathParam("uid") String uid )
    {
        // Get old one
        Optional<XpDeploymentResource> resource = xpDeploymentCache.get( uid );
        if ( resource.isEmpty() )
        {
            return Response.status( 404 ).build();
        }

        // Delete it
        ImmutableCommandDeleteXpDeployment.builder().
            client( xpDeploymentClientProducer.produce() ).
            resource( resource.get() ).
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
