package com.enonic.ec.kubernetes.operator.admission.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.fabric8.kubernetes.api.model.admission.AdmissionReview;

public abstract class AdmissionReviewExtended<V>
    extends AdmissionReview
{

    @JsonProperty("request")
    public void setRealRequest( AdmissionReviewExtendedRequest<? extends V> request )
    {
        super.setRequest( request );
    }

    public AdmissionReviewExtendedRequest<? extends V> getRealRequest()
    {
        return (AdmissionReviewExtendedRequest<? extends V>) super.getRequest();
    }

}
