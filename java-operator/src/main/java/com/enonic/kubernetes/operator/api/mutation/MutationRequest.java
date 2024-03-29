package com.enonic.kubernetes.operator.api.mutation;

import io.fabric8.kubernetes.api.model.admission.v1.AdmissionReview;

import java.util.LinkedList;
import java.util.List;


public class MutationRequest
{
    private final AdmissionReview admissionReview;

    private final List<Patch> patches;

    public MutationRequest( final AdmissionReview admissionReview )
    {
        this.admissionReview = admissionReview;
        this.patches = new LinkedList<>();
    }

    public AdmissionReview getAdmissionReview()
    {
        return admissionReview;
    }

    public List<Patch> getPatches()
    {
        return patches;
    }

    public void addPatch( String op, String path, Object value )
    {
        addPatch( PatchImpl.of( op, path, value ) );
    }

    private void addPatch( Patch patch )
    {
        patches.add( patch );
    }

    public boolean hasPatches()
    {
        return !patches.isEmpty();
    }
}
