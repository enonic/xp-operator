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
import com.google.common.base.Preconditions;

import io.fabric8.kubernetes.api.model.Status;
import io.fabric8.kubernetes.api.model.admission.AdmissionResponse;
import io.fabric8.kubernetes.api.model.admission.AdmissionReview;

import com.enonic.ec.kubernetes.operator.crd.app.XpAppResource;
import com.enonic.ec.kubernetes.operator.crd.config.XpConfigResource;
import com.enonic.ec.kubernetes.operator.crd.deployment.XpDeploymentResource;
import com.enonic.ec.kubernetes.operator.crd.deployment.client.XpDeploymentCache;
import com.enonic.ec.kubernetes.operator.crd.vhost.XpVHostResource;
import com.enonic.ec.kubernetes.operator.crd.vhost.spec.SpecMapping;

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
    XpDeploymentCache xpDeploymentCache;

    @POST
    @Path("/validations")
    @Consumes("application/json")
    @Produces("application/json")
    public AdmissionReview validate( String body )
        throws IOException
    {
        @SuppressWarnings("unchecked") Map<String, Object> admission = mapper.readValue( body, Map.class );
        log.debug( "Admission review: " + body );
        String uid = (String) ( (Map) admission.get( "request" ) ).get( "uid" );
        try
        {
            AdmissionReview review = mapper.readValue( body, AdmissionReview.class );
            if ( review.getRequest().getOperation().equals( "CREATE" ) || review.getRequest().getOperation().equals( "UPDATE" ) )
            {
                Consumer<AdmissionReview> reviewer = getReviewConsumers().
                    getOrDefault( review.getRequest().getKind().getKind(), this::defaultReviewConsumers );
                reviewer.accept( review );
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
        res.put( xp7DeploymentKind, this::xpDeploymentReview );
        res.put( xp7vHostKind, this::xpVHostReview );
        res.put( xp7ConfigKind, this::xpConfigReview );
        res.put( xp7AppKind, this::xpAppReview );
        return res;
    }

    private void xpDeploymentReview( final AdmissionReview review )
    {
        com.enonic.ec.kubernetes.operator.crd.deployment.diff.ImmutableDiffResource.builder().
            oldValue( Optional.ofNullable(
                review.getRequest().getOldObject() != null ? (XpDeploymentResource) review.getRequest().getOldObject() : null ) ).
            newValue( Optional.ofNullable(
                review.getRequest().getObject() != null ? (XpDeploymentResource) review.getRequest().getObject() : null ) ).
            build();
    }

    private void xpVHostReview( final AdmissionReview review )
    {
        com.enonic.ec.kubernetes.operator.crd.vhost.diff.ImmutableDiffResource diff =
            com.enonic.ec.kubernetes.operator.crd.vhost.diff.ImmutableDiffResource.builder().
                oldValue( Optional.ofNullable(
                    review.getRequest().getOldObject() != null ? (XpVHostResource) review.getRequest().getOldObject() : null ) ).
                newValue( Optional.ofNullable(
                    review.getRequest().getObject() != null ? (XpVHostResource) review.getRequest().getObject() : null ) ).
                build();
        if ( diff.newValue().isPresent() )
        {
            for ( SpecMapping mapping : diff.newValue().get().getSpec().mappings() )
            {
                if ( !mapping.node().equals( allNodesPicker ) )
                {
                    checkIfNodeExists( diff.newValue().get().getMetadata().getNamespace(), mapping.node(), "spec.mappings.node" );
                }
            }
        }
    }

    private void xpConfigReview( final AdmissionReview review )
    {
        com.enonic.ec.kubernetes.operator.crd.config.diff.ImmutableDiffResource diff =
            com.enonic.ec.kubernetes.operator.crd.config.diff.ImmutableDiffResource.builder().
                oldValue( Optional.ofNullable(
                    review.getRequest().getOldObject() != null ? (XpConfigResource) review.getRequest().getOldObject() : null ) ).
                newValue( Optional.ofNullable(
                    review.getRequest().getObject() != null ? (XpConfigResource) review.getRequest().getObject() : null ) ).
                build();
        if ( diff.newValue().isPresent() && !diff.newValue().get().getSpec().node().equals( allNodesPicker ) )
        {
            checkIfNodeExists( diff.newValue().get().getMetadata().getNamespace(), diff.newValue().get().getSpec().node(), "spec.node" );
        }
    }

    private void xpAppReview( final AdmissionReview review )
    {
        com.enonic.ec.kubernetes.operator.crd.app.diff.ImmutableDiffResource diff =
            com.enonic.ec.kubernetes.operator.crd.app.diff.ImmutableDiffResource.builder().
                oldValue( Optional.ofNullable(
                    review.getRequest().getOldObject() != null ? (XpAppResource) review.getRequest().getOldObject() : null ) ).
                newValue( Optional.ofNullable(
                    review.getRequest().getObject() != null ? (XpAppResource) review.getRequest().getObject() : null ) ).
                build();
    }

    private void checkIfNodeExists( final String namespace, final String node, final String field )
    {
        Optional<XpDeploymentResource> deployment =
            xpDeploymentCache.stream().filter( r -> r.getMetadata().getName().equals( namespace ) ).findFirst();
        Preconditions.checkState( deployment.isPresent(),
                                  xp7vHostKind + " can only be created in namespaces that are created by " + xp7DeploymentKind );
        Preconditions.checkState( deployment.get().getSpec().nodes().containsKey( node ),
                                  "Field '" + field + "' with value '" + node + "' has to match a node in a " + xp7DeploymentKind );
    }

    private void defaultReviewConsumers( AdmissionReview r )
    {
        throw new RuntimeException( "Could not find reviewer for kind: " + r.getRequest().getKind().getKind() );
    }
}
