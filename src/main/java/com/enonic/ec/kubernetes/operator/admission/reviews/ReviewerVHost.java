package com.enonic.ec.kubernetes.operator.admission.reviews;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.crd.vhost.XpVHostResource;
import com.enonic.ec.kubernetes.crd.vhost.diff.ImmutableDiffSpec;
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
        ImmutableDiffSpec.builder().
            oldValue( review.getRequest().getOldObject() != null ? review.getRealRequest().getRealOldObject().getSpec() : null ).
            newValue( review.getRequest().getObject() != null ? review.getRealRequest().getRealObject().getSpec() : null ).
            build();
    }
}
