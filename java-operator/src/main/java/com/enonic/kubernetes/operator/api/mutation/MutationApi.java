package com.enonic.kubernetes.operator.api.mutation;

import com.enonic.kubernetes.crd.v1.Xp8Config;
import com.enonic.kubernetes.crd.v1.Xp8Deployment;
import com.enonic.kubernetes.crd.v1.Xp8ConfigStatus;
import com.enonic.kubernetes.crd.v1.Xp8DeploymentStatus;
import com.enonic.kubernetes.operator.api.AdmissionOperation;
import com.enonic.kubernetes.operator.api.BaseAdmissionApi;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.io.BaseEncoding;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.fabric8.kubernetes.api.model.admission.v1.AdmissionResponseBuilder;
import io.fabric8.kubernetes.api.model.admission.v1.AdmissionReview;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;

import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;

import java.util.*;
import java.util.stream.Collectors;

import static com.enonic.kubernetes.common.Configuration.cfgBool;
import static com.enonic.kubernetes.common.Configuration.cfgStr;
import static com.enonic.kubernetes.common.Configuration.cfgStrChild;
import static com.enonic.kubernetes.common.Utils.createOwnerReference;
import static com.enonic.kubernetes.kubernetes.Predicates.matchAnnotationPrefix;

@Controller("/apis/operator.enonic.cloud/v1")
public class MutationApi
    extends BaseAdmissionApi<MutationRequest>
{
    public MutationApi()
    {
        super();
        addFunction( Xp8Config.class, this::xp8config );
        addFunction( Xp8Deployment.class, this::xp8deployment );
        addFunction( Ingress.class, this::ingress );
    }

    @Post("/mutations")
    @Consumes("application/json")
    @Produces("application/json")
    public AdmissionReview mutate( @Body AdmissionReview admissionReview )
        throws JsonProcessingException
    {
        return handle( admissionReview );
    }

    @Override
    protected MutationRequest createApiObject( final AdmissionReview admissionReview )
    {
        return new MutationRequest( admissionReview );
    }

    @Override
    protected void postRequestHook( final MutationRequest mutationRequest, final AdmissionResponseBuilder builder )
        throws JsonProcessingException
    {
        if (mutationRequest.hasPatches()) {
            builder.
                withPatch( BaseEncoding.base64().encode( mapper.writeValueAsString( mutationRequest.getPatches() ).getBytes() ) ).
                withPatchType( "JSONPatch" );
        }
    }

    private void xp8config( MutationRequest mt )
    {
        // Collect old and new object
        Xp8Config oldR = (Xp8Config) mt.getAdmissionReview().getRequest().getOldObject();
        Xp8Config newR = (Xp8Config) mt.getAdmissionReview().getRequest().getObject();

        // Create default status
        Xp8ConfigStatus defStatus = new Xp8ConfigStatus();
        defStatus.setMessage( "Not loaded" );
        defStatus.setState( Xp8ConfigStatus.State.PENDING );

        // Get OP
        AdmissionOperation op = getOperation( mt.getAdmissionReview() );

        // Ensure status
        switch (op) {
            case CREATE: // Always set the default status on new objects
                patch( mt, true, "/status", newR.getStatus(), defStatus );
                break;
            case UPDATE:
                if (newR.getSpec() != null && !Serialization.asJson( newR.getSpec() ).equals( Serialization.asJson( oldR.getSpec() ) )) {
                    // On any change change, set default status
                    patch( mt, true, "/status", newR.getStatus(), defStatus );
                } else {
                    // Else make sure the old status is not removed
                    patch( mt, false, "/status", newR.getStatus(), oldR.getStatus() );
                }
                break;
            case DELETE:
                // Do nothing
                break;
        }

        // Ensure defaults
        if (newR.getSpec() != null) {
            patch( mt, false, "/spec/dataBase64", newR.getSpec().getDataBase64(), false );
            patch( mt, false, "/spec/nodeGroup", newR.getSpec().getNodeGroup(), cfgStr( "operator.charts.values.allNodesKey" ) );
        }

        if (op == AdmissionOperation.CREATE) {
            // Ensure owner reference
            ensureOwnerReference( mt );
        }
    }

    private void xp8deployment( final MutationRequest mt )
    {
        // Collect old and new object
        final Xp8Deployment oldR = (Xp8Deployment) mt.getAdmissionReview().getRequest().getOldObject();
        final Xp8Deployment newR = (Xp8Deployment) mt.getAdmissionReview().getRequest().getObject();

        // Create default status
        final com.enonic.kubernetes.crd.v1.xp8deploymentstatus.Fields defStatusFields =
            new com.enonic.kubernetes.crd.v1.xp8deploymentstatus.Fields();
        defStatusFields.setPods( new LinkedList<>() );
        final Xp8DeploymentStatus defStatus = new Xp8DeploymentStatus();
        defStatus.setMessage( "Waiting for pods" );
        defStatus.setState( Xp8DeploymentStatus.State.PENDING );
        defStatus.setFields( defStatusFields );

        if(newR.getSpec() != null && newR.getSpec().getEnabled() != null && !newR.getSpec().getEnabled()) {
            defStatus.setState( Xp8DeploymentStatus.State.STOPPED );
            defStatus.setMessage( "XP deployment stopped" );
        }

        // Get OP
        final AdmissionOperation op = getOperation( mt.getAdmissionReview() );

        // Ensure status
        switch (op) {
            case CREATE: // Always set the default status on new objects
                patch( mt, true, "/status", newR.getStatus(), defStatus );
                break;
            case UPDATE:
                if (newR.getSpec() != null && !Serialization.asJson( newR.getSpec() ).equals( Serialization.asJson( oldR.getSpec() ) )) {
                    // On any change, set default status
                    patch( mt, true, "/status", newR.getStatus(), defStatus );
                } else {
                    // Else make sure the old status is not removed
                    patch( mt, false, "/status", newR.getStatus(), oldR.getStatus() );
                }
                break;
            case DELETE:
                // Do nothing
                break;
        }
    }

    private void ingress( final MutationRequest mt )
    {
        // Collect old and new object
        Ingress newR = (Ingress) mt.getAdmissionReview().getRequest().getObject();

        if (newR == null) {
            return;
        }

        if (matchAnnotationPrefix( cfgStr( "operator.charts.values.annotationKeys.vhostMapping" ) ).negate().test( newR )) {
            return;
        }

        Map<String, String> labels = newR.getMetadata().getLabels() != null ?
            newR.getMetadata().getLabels() : new HashMap<>();

        if (!labels.containsKey( cfgStr( "operator.charts.values.labelKeys.ingressVhostLoaded" ) )) {
            labels.put( cfgStr( "operator.charts.values.labelKeys.ingressVhostLoaded" ), "false" );
            patch( mt, true, "/metadata/labels", newR.getMetadata().getLabels(), labels );
        }

        Map<String, String> oa = newR.getMetadata().getAnnotations() != null ? newR.getMetadata().getAnnotations() : new HashMap<>();
        Map<String, String> na = new HashMap<>( oa );

        boolean isNginx = "nginx".equals( newR.getSpec().getIngressClassName() ) || "nginx".equals( na.get( "kubernetes.io/ingress.class" ) );
        if (isNginx && cfgBool( "operator.charts.values.settings.linkerd" )) {
            // If linkerd is enabled
            String cfgSnippet = na.get( "nginx.ingress.kubernetes.io/configuration-snippet" );
            StringBuilder sb = new StringBuilder( cfgSnippet != null ? cfgSnippet : "" ).
                append( "\n" ).
                append( "proxy_set_header l5d-dst-override $service_name.$namespace.svc.cluster.local:$service_port;" ).
                append( "grpc_set_header l5d-dst-override $service_name.$namespace.svc.cluster.local:$service_port;" );
            na.put( "nginx.ingress.kubernetes.io/configuration-snippet", sb.toString() );
        }

        patch( mt, true, "/metadata/annotations", newR.getMetadata().getAnnotations(), na );
    }

    private void ensureOwnerReference( MutationRequest mt )
    {
        HasMetadata obj = (HasMetadata) mt.getAdmissionReview().getRequest().getObject();

        if (obj.getMetadata().getOwnerReferences() != null && !obj.getMetadata().getOwnerReferences().isEmpty()) {
            return;
        }

        Optional<Xp8Deployment> xp8Deployments = getXp8Deployment( obj );
        if (xp8Deployments.isEmpty()) {
            return;
        }

        patch( mt, false, "/metadata/ownerReferences", null, Collections.singletonList( createOwnerReference( xp8Deployments.get() ) ) );
    }

    private <T> boolean patch( MutationRequest mt, boolean force, String path, T currentValue, T value )
    {
        if (currentValue == null) {
            mt.addPatch( "add", path, value );
            return true;
        } else if (force && !Objects.equals( currentValue, value )) {
            mt.addPatch( "replace", path, value );
            return true;
        }
        return false;
    }
}
