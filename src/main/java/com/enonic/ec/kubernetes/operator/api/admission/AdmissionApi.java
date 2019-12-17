package com.enonic.ec.kubernetes.operator.api.admission;

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

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Status;
import io.fabric8.kubernetes.api.model.admission.AdmissionResponse;
import io.fabric8.kubernetes.api.model.admission.AdmissionReview;

import com.enonic.ec.kubernetes.operator.crd.xp7app.Xp7AppResource;
import com.enonic.ec.kubernetes.operator.crd.xp7config.Xp7ConfigResource;
import com.enonic.ec.kubernetes.operator.crd.xp7deployment.Xp7DeploymentResource;
import com.enonic.ec.kubernetes.operator.crd.xp7deployment.client.Xp7DeploymentCache;
import com.enonic.ec.kubernetes.operator.crd.xp7vhost.Xp7VHostResource;
import com.enonic.ec.kubernetes.operator.info.xp7app.ImmutableInfoXp7App;
import com.enonic.ec.kubernetes.operator.info.xp7config.ImmutableInfoXp7Config;
import com.enonic.ec.kubernetes.operator.info.xp7deployment.ImmutableInfoXp7Deployment;
import com.enonic.ec.kubernetes.operator.info.xp7vhost.ImmutableInfoXp7VHost;

@ApplicationScoped
@Path("/apis/operator.enonic.cloud/v1alpha1")
public class AdmissionApi
{
    private final static Logger log = LoggerFactory.getLogger( AdmissionApi.class );

    @ConfigProperty(name = "operator.crd.xp.deployments.kind")
    String xp7DeploymentKind;

    @ConfigProperty(name = "operator.crd.xp.vhosts.kind")
    String xp7vHostKind;

    @ConfigProperty(name = "operator.crd.xp.configs.kind")
    String xp7ConfigKind;

    @ConfigProperty(name = "operator.crd.xp.apps.kind")
    String xp7AppKind;

    @ConfigProperty(name = "operator.deployment.xp.allNodes")
    String allNodesPicker;

    @Inject
    ObjectMapper mapper;

    @Inject
    Xp7DeploymentCache xp7DeploymentCache;

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
            return createReview( uid, message, ex.getClass().getSimpleName() );
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
        ImmutableInfoXp7VHost.builder().
            xpDeploymentCache( xp7DeploymentCache ).
            oldResource( Optional.ofNullable( review.getRequest().getOldObject() ).map( obj -> (Xp7VHostResource) obj ) ).
            newResource( Optional.ofNullable( review.getRequest().getObject() ).map( obj -> (Xp7VHostResource) obj ) ).
            build();
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
