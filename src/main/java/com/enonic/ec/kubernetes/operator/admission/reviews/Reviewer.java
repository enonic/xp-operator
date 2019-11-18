package com.enonic.ec.kubernetes.operator.admission.reviews;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.fabric8.kubernetes.api.model.Status;
import io.fabric8.kubernetes.api.model.admission.AdmissionResponse;
import io.fabric8.kubernetes.api.model.admission.AdmissionReview;

import com.enonic.ec.kubernetes.operator.admission.model.AdmissionReviewExtended;

public abstract class Reviewer<T>
{
    protected abstract ObjectMapper objectMapper();

    protected abstract String uid();

    protected abstract String body();

    @Value.Derived
    public AdmissionReview response()
    {
        AdmissionReviewExtended<T> review = null;
        try
        {
            review = objectMapper().readValue( body(), getReviewClass() );
            createDiff( review );
        }
        catch ( Exception ex )
        {
            if ( ex.getCause() != null )
            {
                return createReview( uid(), ex.getCause() );
            }
            return createReview( uid(), ex );
        }

        return createReview( uid(), null );
    }

    public static AdmissionReview createReview( String uid, Throwable error )
    {
        AdmissionReview review = new AdmissionReview();
        AdmissionResponse response = new AdmissionResponse();
        review.setResponse( response );

        response.setUid( uid );

        if ( error == null )
        {
            response.setAllowed( true );
            return review;
        }

        Status status = new Status();
        status.setCode( 400 );
        status.setMessage( error.getMessage() );
        status.setReason( error.getClass().getSimpleName() );

        response.setAllowed( false );
        response.setStatus( status );

        return review;
    }

    protected abstract Class<? extends AdmissionReviewExtended<T>> getReviewClass();

    protected abstract void createDiff( AdmissionReviewExtended<T> review )
        throws IllegalStateException;
}
