package com.enonic.ec.kubernetes.operator.admission.reviews;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.crd.deployment.XpDeploymentResource;
import com.enonic.ec.kubernetes.crd.deployment.diff.ImmutableDiffResource;
import com.enonic.ec.kubernetes.operator.admission.model.AdmissionReviewDeployment;
import com.enonic.ec.kubernetes.operator.admission.model.AdmissionReviewExtended;

@Value.Immutable
public abstract class ReviewerDeployment
    extends Reviewer<XpDeploymentResource>
{
    @Value.Derived
    protected Class<? extends AdmissionReviewExtended<XpDeploymentResource>> getReviewClass()
    {
        return AdmissionReviewDeployment.class;
    }

    protected void createDiff( AdmissionReviewExtended<XpDeploymentResource> review )
        throws IllegalStateException
    {
        ImmutableDiffResource.builder().
            oldValue( review.getRequest().getOldObject() != null ? review.getRealRequest().getRealOldObject() : null ).
            newValue( review.getRequest().getObject() != null ? review.getRealRequest().getRealObject() : null ).
            build();
    }
}
