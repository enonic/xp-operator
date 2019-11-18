package com.enonic.ec.kubernetes.operator.admission.reviews;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.crd.vhost.XpVHostResource;
import com.enonic.ec.kubernetes.crd.vhost.diff.ImmutableDiffResource;
import com.enonic.ec.kubernetes.operator.admission.model.AdmissionReviewExtended;
import com.enonic.ec.kubernetes.operator.admission.model.AdmissionReviewVHost;

@Value.Immutable
public abstract class ReviewerVHost
    extends Reviewer<XpVHostResource>
{
    @Value.Derived
    protected Class<? extends AdmissionReviewExtended<XpVHostResource>> getReviewClass()
    {
        return AdmissionReviewVHost.class;
    }

    protected void createDiff( AdmissionReviewExtended<XpVHostResource> review )
        throws IllegalStateException
    {
        ImmutableDiffResource.builder().
            oldValue( review.getRequest().getOldObject() != null ? review.getRealRequest().getRealOldObject() : null ).
            newValue( review.getRequest().getObject() != null ? review.getRealRequest().getRealObject() : null ).
            build();
    }
}
