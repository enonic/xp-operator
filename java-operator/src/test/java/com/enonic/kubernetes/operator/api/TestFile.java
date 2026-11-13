package com.enonic.kubernetes.operator.api;

import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.api.model.admission.v1.AdmissionReview;

import com.enonic.kubernetes.operator.api.mutation.Patch;

@NullMarked
public record TestFile(
    boolean disabled,
    AdmissionReview admissionRequest,
    @Nullable List<Patch> assertPatch,
    @Nullable String assertException,
    @Nullable KubernetesResource assertResult )
{
}
