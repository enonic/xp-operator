package com.enonic.ec.kubernetes.operator.operators.v1alpha1.api.admission;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Status;
import io.fabric8.kubernetes.api.model.admission.AdmissionResponse;
import io.fabric8.kubernetes.api.model.admission.AdmissionReview;

import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7app.crd.Xp7AppResource;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7app.info.ImmutableInfoXp7App;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7config.crd.Xp7ConfigResource;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7config.info.ImmutableInfoXp7Config;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.crd.Xp7DeploymentResource;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.crd.client.Xp7DeploymentCache;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.info.ImmutableInfoXp7Deployment;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7vhost.crd.Xp7VHostResource;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7vhost.crd.client.Xp7VHostCache;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7vhost.info.ImmutableInfoXp7VHost;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7vhost.info.InfoXp7VHost;

@ApplicationScoped
@Path("/apis/operator.enonic.cloud/v1alpha1")
public class AdmissionApi
{
    private final static Logger log = LoggerFactory.getLogger( AdmissionApi.class );

    @ConfigProperty(name = "operator.crd.v1alpha1.deployments.kind")
    String xp7DeploymentKind;

    @ConfigProperty(name = "operator.crd.v1alpha1.vhosts.kind")
    String xp7vHostKind;

    @ConfigProperty(name = "operator.crd.v1alpha1.configs.kind")
    String xp7ConfigKind;

    @ConfigProperty(name = "operator.crd.v1alpha1.apps.kind")
    String xp7AppKind;

    @ConfigProperty(name = "operator.deployment.xp.allNodes")
    String allNodesPicker;

    @Inject
    ObjectMapper mapper;

    @Inject
    Xp7DeploymentCache xp7DeploymentCache;

    @Inject
    Xp7VHostCache xp7VHostCache;

    public static AdmissionReview createReview( String uid, String errorCause, String errorMessage )
    {
        AdmissionReview review = new AdmissionReview();
        AdmissionResponse response = new AdmissionResponse();
        review.setResponse( response );

        response.setUid( uid );

        Status status = new Status();
        response.setStatus( status );

        if ( errorMessage == null )
        {
            response.setAllowed( true );
            return review;
        }

        response.setAllowed( false );
        status.setCode( 400 );
        status.setMessage( errorMessage );
        status.setReason( errorCause );

        return review;
    }

    private Consumer<AdmissionReview> getReviewConsumer( final AdmissionReview review )
    {
        HasMetadata obj = review.getRequest().getOldObject();
        if ( review.getRequest().getObject() != null )
        {
            obj = review.getRequest().getObject();
        }

        if ( obj instanceof Xp7DeploymentResource )
        {
            return this::xpDeploymentReview;
        }
        else if ( obj instanceof Xp7VHostResource )
        {
            return this::xpVHostReview;
        }
        else if ( obj instanceof Xp7ConfigResource )
        {
            return this::xpConfigReview;
        }
        else if ( obj instanceof Xp7AppResource )
        {
            return this::xpAppReview;
        }
        else
        {
            return this::defaultReviewConsumers;
        }
    }

    @POST
    @Path("/validations")
    @Consumes("application/json")
    @Produces("application/json")
    public AdmissionReview validate( String body )
        throws IOException
    {
        log.debug( "Admission review: " + body );
        @SuppressWarnings("unchecked") Map<String, Object> admission = mapper.readValue( body, Map.class );

        // We have to extract the uid this way if there is a validation error during
        // mapping of the actual objects.
        String uid = (String) ( (Map) admission.get( "request" ) ).get( "uid" );

        AdmissionReview review = null;

        try
        {
            review = mapper.readValue( body, AdmissionReview.class );
            if ( review.getRequest().getOperation().equals( "CREATE" ) || review.getRequest().getOperation().equals( "UPDATE" ) )
            {
                getReviewConsumer( review ).accept( review );
            }
        }
        catch ( Exception ex )
        {
            String message = AdmissionExceptionHandler.extractJacksonMessage( ex );
            log.warn( "AdmissionReview failed: " + message );
            return createReview( uid, ex.getClass().getSimpleName(), message );
        }
        return createReview( uid, null, null );
    }

    private void xpDeploymentReview( final AdmissionReview review )
    {
        ImmutableInfoXp7Deployment.builder().
            oldResource( Optional.ofNullable( review.getRequest().getOldObject() ).map( obj -> (Xp7DeploymentResource) obj ) ).
            newResource( Optional.ofNullable( review.getRequest().getObject() ).map( obj -> (Xp7DeploymentResource) obj ) ).
            build();
    }

    private void xpVHostReview( final AdmissionReview review )
    {
        InfoXp7VHost vHost = ImmutableInfoXp7VHost.builder().
            xpDeploymentCache( xp7DeploymentCache ).
            oldResource( Optional.ofNullable( review.getRequest().getOldObject() ).map( obj -> (Xp7VHostResource) obj ) ).
            newResource( Optional.ofNullable( review.getRequest().getObject() ).map( obj -> (Xp7VHostResource) obj ) ).
            build();
        if(!vHost.resource().getSpec().skipIngress()) {
            long sameHost = xp7VHostCache.stream().
                filter( r -> !r.getMetadata().getUid().equals( vHost.resource().getMetadata().getUid() ) ).
                filter( r -> !r.getSpec().skipIngress()).
                filter( r -> r.getSpec().host().equals( vHost.resource().getSpec().host() ) ).
                count();
            Preconditions.checkState( sameHost < 1L, "This host is being used by another Xp7VHost" );
        }
    }

    private void xpConfigReview( final AdmissionReview review )
    {
        ImmutableInfoXp7Config.builder().
            xpDeploymentCache( xp7DeploymentCache ).
            oldResource( Optional.ofNullable( review.getRequest().getOldObject() ).map( obj -> (Xp7ConfigResource) obj ) ).
            newResource( Optional.ofNullable( review.getRequest().getObject() ).map( obj -> (Xp7ConfigResource) obj ) ).
            build();
    }

    private void xpAppReview( final AdmissionReview review )
    {
        ImmutableInfoXp7App.builder().
            xpDeploymentCache( xp7DeploymentCache ).
            oldResource( Optional.ofNullable( review.getRequest().getOldObject() ).map( obj -> (Xp7AppResource) obj ) ).
            newResource( Optional.ofNullable( review.getRequest().getObject() ).map( obj -> (Xp7AppResource) obj ) ).
            build();
    }

    private void defaultReviewConsumers( AdmissionReview r )
    {
        log.warn( "Admission review sent to endpoint that has unknown kind: " + r.getRequest().getKind().getKind() );
    }
}
