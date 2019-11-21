package com.enonic.ec.kubernetes.operator.api.admission;

import java.io.IOException;
import java.util.HashMap;
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

import io.fabric8.kubernetes.api.model.Status;
import io.fabric8.kubernetes.api.model.admission.AdmissionResponse;
import io.fabric8.kubernetes.api.model.admission.AdmissionReview;

import com.enonic.ec.kubernetes.crd.deployment.XpDeploymentResource;
import com.enonic.ec.kubernetes.crd.vhost.XpVHostResource;

@ApplicationScoped
@Path("/apis/operator.enonic.cloud/v1alpha1/admission")
public class AdmissionApi
{
    private final static Logger log = LoggerFactory.getLogger( AdmissionApi.class );

    @ConfigProperty(name = "operator.crd.xp.deployments.kind")
    String xp7SolutionKind;

    @ConfigProperty(name = "operator.crd.xp.vhosts.kind")
    String xp7vHostKind;

    @Inject
    ObjectMapper mapper;

    @POST
    @Path("/validate")
    @Consumes("application/json")
    @Produces("application/json")
    public AdmissionReview validate( String body )
        throws IOException
    {
        log.info( "AdmissionReview started" );
        Map<String, Object> admission = mapper.readValue( body, Map.class );
        String uid = (String) ( (Map) admission.get( "request" ) ).get( "uid" );
        try
        {
            AdmissionReview review = mapper.readValue( body, AdmissionReview.class );
            Consumer<AdmissionReview> reviewer = getReviewConsumers().
                getOrDefault( review.getRequest().getKind().getKind(), this::defaultReviewConsumers );
            reviewer.accept( review );
        }
        catch ( IOException ex )
        {
            log.error( "AdmissionReview failed", ex );
            return createReview( uid, AdmissionExceptionHandler.extractJacksonMessage( ex ), ex.getClass().getSimpleName() );
        }
        log.info( "AdmissionReview success" );
        return createReview( uid, null, null );
    }

    public static AdmissionReview createReview( String uid, String errorCause, String errorMessage )
    {
        AdmissionReview review = new AdmissionReview();
        AdmissionResponse response = new AdmissionResponse();
        review.setResponse( response );

        response.setUid( uid );

        if ( errorMessage == null )
        {
            response.setAllowed( true );
            return review;
        }

        Status status = new Status();
        status.setCode( 400 );
        status.setMessage( errorMessage );
        status.setReason( errorCause );

        response.setAllowed( false );
        response.setStatus( status );

        return review;
    }

    private Map<String, Consumer<AdmissionReview>> getReviewConsumers()
    {
        Map<String, Consumer<AdmissionReview>> res = new HashMap<>();
        res.put( xp7SolutionKind, review -> com.enonic.ec.kubernetes.crd.deployment.diff.ImmutableDiffResource.builder().
            oldValue( Optional.ofNullable(
                review.getRequest().getOldObject() != null ? (XpDeploymentResource) review.getRequest().getOldObject() : null ) ).
            newValue( Optional.ofNullable(
                review.getRequest().getObject() != null ? (XpDeploymentResource) review.getRequest().getObject() : null ) ).
            build() );

        res.put( xp7vHostKind, review -> com.enonic.ec.kubernetes.crd.vhost.diff.ImmutableDiffResource.builder().
            oldValue( Optional.ofNullable(
                review.getRequest().getOldObject() != null ? (XpVHostResource) review.getRequest().getOldObject() : null ) ).
            newValue(
                Optional.ofNullable( review.getRequest().getObject() != null ? (XpVHostResource) review.getRequest().getObject() : null ) ).
            build() );

        return res;
    }

    private void defaultReviewConsumers( AdmissionReview r )
    {
        throw new RuntimeException( "Could not find reviewer for kind: " + r.getRequest().getKind().getKind() );
    }
}
