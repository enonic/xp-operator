package com.enonic.ec.kubernetes.api;

import java.util.Optional;
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

import com.enonic.ec.kubernetes.crd.commands.ImmutableCommandCreateXpVHost;
import com.enonic.ec.kubernetes.crd.commands.ImmutableCommandDeleteXpVHost;
import com.enonic.ec.kubernetes.crd.deployment.XpDeploymentResource;
import com.enonic.ec.kubernetes.crd.deployment.client.XpDeploymentCache;
import com.enonic.ec.kubernetes.crd.vhost.XpVHostResource;
import com.enonic.ec.kubernetes.crd.vhost.client.XpVHostCache;
import com.enonic.ec.kubernetes.crd.vhost.client.XpVHostClientProducer;
import com.enonic.ec.kubernetes.crd.vhost.diff.ImmutableDiffSpec;

@ApplicationScoped
@Path("/api")
public class ServiceVHost
{
    @Inject
    XpDeploymentCache xpDeploymentCache;

    @Inject
    XpVHostClientProducer xpVHostClientProducer;

    @Inject
    XpVHostCache xpVHostCache;

    @GET
    @Path("/{depId}/vhosts")
    @Produces("application/json")
    public Response getVHosts( @PathParam("depId") String depId )
    {
        Optional<XpDeploymentResource> parent = xpDeploymentCache.get( depId );
        if ( parent.isEmpty() )
        {
            return Response.status( 404 ).build();
        }

        return Response.ok( xpVHostCache.stream().
            filter( s -> s.getMetadata().
                getOwnerReferences().
                stream().
                filter( o -> o.getUid().equals( depId ) ).
                map( o -> o.getUid() ).
                collect( Collectors.toList() ).
                contains( depId ) ).
            collect( Collectors.toList() ) ).
            build();
    }

    @GET
    @Path("/{depId}/vhosts/{vId}")
    @Produces("application/json")
    public Response getVHost( @PathParam("depId") String depId, @PathParam("vId") String vId )
    {
        Optional<XpDeploymentResource> parent = xpDeploymentCache.get( depId );
        if ( parent.isEmpty() )
        {
            return Response.status( 404 ).build();
        }

        Optional<XpVHostResource> vHost = xpVHostCache.stream().
            filter( s -> s.getMetadata().
                getOwnerReferences().
                stream().
                filter( o -> o.getUid().equals( depId ) ).
                map( o -> o.getUid() ).
                collect( Collectors.toList() ).
                contains( depId ) ).
            filter( v -> v.getMetadata().getUid().equals( vId ) ).
            findFirst();

        if ( vHost.isEmpty() )
        {
            return Response.status( 404 ).build();
        }

        return Response.ok( resourceToJson( vHost.get() ) ).build();
    }

    @POST
    @Path("/{depId}/vhosts")
    @Consumes("application/json")
    @Produces("application/json")
    public Response createXpVHost( @PathParam("depId") String depId, XpVHostJson deployment, @Context UriInfo uriInfo )
    {
        Optional<XpDeploymentResource> parent = xpDeploymentCache.get( depId );
        if ( parent.isEmpty() )
        {
            return Response.status( 404 ).build();
        }

        // Check if there is and old one present
        Optional<XpVHostResource> old = xpVHostCache.stream().
            filter( s -> s.getMetadata().
                getOwnerReferences().
                stream().
                filter( o -> o.getUid().equals( depId ) ).
                map( o -> o.getUid() ).
                collect( Collectors.toList() ).
                contains( depId ) ).
            filter( v -> v.getSpec().host().equals( deployment.spec().host() ) ).
            findFirst();

        // Just to validate the changes
        old.ifPresent( o -> ImmutableDiffSpec.builder().
            oldValue( o.getSpec() ).
            newValue( deployment.spec() ).
            build() );

        // Send the new resource to kubernetes
        XpVHostResource resource;
        resource = ImmutableCommandCreateXpVHost.builder().
            client( xpVHostClientProducer.produce() ).
            apiVersion( deployment.apiVersion() ).
            spec( deployment.spec() ).
            owner( parent.get() ).
            build().
            execute();

        // Create response
        UriBuilder builder = uriInfo.getAbsolutePathBuilder();
        builder.path( resource.getMetadata().getUid() );
        return Response.created( builder.build() ).entity( resourceToJson( resource ) ).build();
    }

    @DELETE
    @Path("/{depId}/vhosts/{vId}")
    public Response deleteXpDeployment( @PathParam("depId") String depId, @PathParam("vId") String vId )
    {
        Optional<XpDeploymentResource> parent = xpDeploymentCache.get( depId );
        if ( parent.isEmpty() )
        {
            return Response.status( 404 ).build();
        }

        Optional<XpVHostResource> vHost = xpVHostCache.stream().
            filter( s -> s.getMetadata().
                getOwnerReferences().
                stream().
                filter( o -> o.getUid().equals( depId ) ).
                map( o -> o.getUid() ).
                collect( Collectors.toList() ).
                contains( depId ) ).
            filter( v -> v.getMetadata().getUid().equals( vId ) ).
            findFirst();

        if ( vHost.isEmpty() )
        {
            return Response.status( 404 ).build();
        }

        // Delete it
        ImmutableCommandDeleteXpVHost.builder().
            client( xpVHostClientProducer.produce() ).
            resource( vHost.get() ).
            build().
            execute();

        // Create response
        return Response.ok().build();
    }

    private static XpVHostJson resourceToJson( XpVHostResource r )
    {
        return ImmutableXpVHostJson.builder().
            apiVersion( r.getApiVersion() ).
            uid( r.getMetadata().getUid() ).
            spec( r.getSpec() ).
            build();
    }

}
