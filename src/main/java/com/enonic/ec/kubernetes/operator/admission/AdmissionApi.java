package com.enonic.ec.kubernetes.operator.admission;

import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.fabric8.kubernetes.api.model.admission.AdmissionReview;

import com.enonic.ec.kubernetes.operator.admission.reviews.ImmutableReviewerDeployment;
import com.enonic.ec.kubernetes.operator.admission.reviews.ImmutableReviewerVHost;
import com.enonic.ec.kubernetes.operator.admission.reviews.Reviewer;

@ApplicationScoped
@Path("/admission")
public class AdmissionApi
{
    private final static Logger log = LoggerFactory.getLogger( AdmissionApi.class );

    @Inject
    ObjectMapper mapper;

    @POST
    @Path("/validate")
    @Consumes("application/json")
    @Produces("application/json")
    public AdmissionReview validate( String body )
        throws IOException
    {
        AdmissionReview review = mapper.readValue( body, AdmissionReview.class );

        String group = review.getRequest().getKind().getGroup();

        if ( !group.equals( "enonic.cloud" ) )
        {
            log.warn( "Admission endpoint called with invalid group: " + group );
            return Reviewer.createReview( review.getRequest().getUid(), null );
        }

        String kind = review.getRequest().getKind().getKind();

        if ( kind.equals( "Deployment" ) )
        {
            return ImmutableReviewerDeployment.builder().
                objectMapper( mapper ).
                uid( review.getRequest().getUid() ).
                body( body ).
                build().
                response();
        }

        if ( kind.equals( "VHost" ) )
        {
            return ImmutableReviewerVHost.builder().
                objectMapper( mapper ).
                uid( review.getRequest().getUid() ).
                body( body ).
                build().
                response();
        }

        log.warn( "Admission endpoint called with invalid kind: " + kind );
        return Reviewer.createReview( review.getRequest().getUid(), null );
    }

}
