package com.enonic.kubernetes.operator.api;

import java.util.List;

import org.immutables.value.Value;
import org.wildfly.common.annotation.Nullable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.api.model.admission.AdmissionReview;

import com.enonic.kubernetes.common.annotations.Params;
import com.enonic.kubernetes.operator.api.mutation.Patch;

@JsonDeserialize(builder = TestFileImpl.Builder.class)
@Value.Immutable
@Params
public abstract class TestFile
{
    @Nullable
    @Value.Default
    public Boolean disabled()
    {
        return false;
    }

    public abstract AdmissionReview admissionRequest();

    @Nullable
    @Value.Default
    public List<Patch> assertPatch()
    {
        return null;
    }

    @Nullable
    @Value.Default
    public String assertException()
    {
        return null;
    }

    @Nullable
    @Value.Default
    KubernetesResource assertResult()
    {
        return null;
    }
}