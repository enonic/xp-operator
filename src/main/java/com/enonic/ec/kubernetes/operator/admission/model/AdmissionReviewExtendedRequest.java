package com.enonic.ec.kubernetes.operator.admission.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.admission.AdmissionRequest;

public class AdmissionReviewExtendedRequest<T extends HasMetadata>
    extends AdmissionRequest
{

    @JsonProperty("object")
    public void setRealObject( T object )
    {
        super.setObject( object );
    }

    public T getRealObject()
    {
        return (T) super.getObject();
    }

    @JsonProperty("oldObject")
    public void setRealOldObject( T oldObject )
    {
        super.setOldObject( oldObject );
    }

    public T getRealOldObject()
    {
        return (T) super.getOldObject();
    }
}
