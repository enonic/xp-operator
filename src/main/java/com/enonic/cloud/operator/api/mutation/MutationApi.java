package com.enonic.cloud.operator.api.mutation;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.io.BaseEncoding;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.admission.AdmissionResponseBuilder;
import io.fabric8.kubernetes.api.model.admission.AdmissionReview;
import io.fabric8.kubernetes.api.model.networking.v1beta1.Ingress;

import com.enonic.cloud.kubernetes.model.v1alpha1.xp7app.Xp7App;
import com.enonic.cloud.kubernetes.model.v1alpha1.xp7app.Xp7AppStatus;
import com.enonic.cloud.kubernetes.model.v1alpha1.xp7app.Xp7AppStatusFields;
import com.enonic.cloud.kubernetes.model.v1alpha2.domain.Domain;
import com.enonic.cloud.kubernetes.model.v1alpha2.domain.DomainStatus;
import com.enonic.cloud.kubernetes.model.v1alpha2.domain.DomainStatusFields;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7config.Xp7Config;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7config.Xp7ConfigStatus;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7Deployment;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7DeploymentStatus;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7DeploymentStatusFields;
import com.enonic.cloud.operator.api.BaseAdmissionApi;

import static com.enonic.cloud.common.Configuration.cfgBool;
import static com.enonic.cloud.common.Configuration.cfgStr;
import static com.enonic.cloud.common.Utils.createOwnerReference;

@ApplicationScoped
@Path("/apis/operator.enonic.cloud/v1alpha1")
public class MutationApi
    extends BaseAdmissionApi<MutationRequest>
{
    public MutationApi()
    {
        super();
        addFunction( Xp7App.class, this::xp7app );
        addFunction( Xp7Config.class, this::xp7config );
        addFunction( Xp7Deployment.class, this::xp7deployment );
        addFunction( Domain.class, this::domain );
        addFunction( Ingress.class, this::ingress );
    }

    @POST
    @Path("/mutations")
    @Consumes("application/json")
    @Produces("application/json")
    public AdmissionReview validate( AdmissionReview admissionReview )
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
        if ( mutationRequest.hasPatches() )
        {
            builder.
                withPatch( BaseEncoding.base64().encode( mapper.writeValueAsString( mutationRequest.getPatches() ).getBytes() ) ).
                withPatchType( "JSONPatch" );
        }
    }

    private void xp7app( MutationRequest mt )
    {
        Xp7App oldR = (Xp7App) mt.getAdmissionReview().getRequest().getOldObject();
        Xp7App newR = (Xp7App) mt.getAdmissionReview().getRequest().getObject();

        Xp7AppStatus defStatus = new Xp7AppStatus().
            withMessage( "Created" ).
            withState( Xp7AppStatus.State.PENDING ).
            withXp7AppStatusFields( new Xp7AppStatusFields() );

        // This is an update => set default status to old status
        if ( oldR != null )
        {
            defStatus = oldR.getXp7AppStatus();
        }

        // Ensure enabled
        patchDefault( mt, true, newR.getXp7AppSpec().getEnabled(), "/spec/enabled" );

        // Ensure status
        if ( !patchDefault( mt, defStatus, newR.getXp7AppStatus(), "/status" ) )
        {
            patchDefault( mt, defStatus.getMessage(), newR.getXp7AppStatus().getMessage(), "/status/message" );
            patchDefault( mt, defStatus.getState(), newR.getXp7AppStatus().getState(), "/status/state" );
            patchDefault( mt, defStatus.getXp7AppStatusFields(), newR.getXp7AppStatus().getXp7AppStatusFields(), "/status/fields" );
        }

        // Ensure finalizers
        if ( mt.getAdmissionReview().getRequest().getOperation().equals( "CREATE" ) )
        {
            List<String> finalizers = ( (HasMetadata) mt.getAdmissionReview().getRequest().getObject() ).getMetadata().getFinalizers();
            String uninstallFinalizer = cfgStr( "operator.charts.values.finalizers.app.uninstall" );
            if ( finalizers == null )
            {
                mt.addPatch( "add", "/metadata/finalizers", Collections.singletonList( uninstallFinalizer ) );
            }
            else if ( !finalizers.contains( uninstallFinalizer ) )
            {
                finalizers.add( uninstallFinalizer );
                mt.addPatch( "replace", "/metadata/finalizers", finalizers );
            }
        }

        ensureOwnerReference( mt );
    }

    private void xp7config( MutationRequest mt )
    {
        Xp7Config newR = (Xp7Config) mt.getAdmissionReview().getRequest().getObject();

        Xp7ConfigStatus defStatus = new Xp7ConfigStatus().
            withMessage( "Not loaded" ).
            withState( Xp7ConfigStatus.State.PENDING );

        if ( !patchDefault( mt, defStatus, newR.getXp7ConfigStatus(), "/status" ) )
        {
            // There is an old status here
            Xp7Config oldR = (Xp7Config) mt.getAdmissionReview().getRequest().getOldObject();
            boolean newBase64 = newR.getXp7ConfigSpec().getDataBase64() != null ? newR.getXp7ConfigSpec().getDataBase64() : true;
            if ( !Objects.equals( oldR.getXp7ConfigSpec().getData(), newR.getXp7ConfigSpec().getData() ) ||
                !Objects.equals( oldR.getXp7ConfigSpec().getDataBase64(), newBase64 ) )
            {
                // There is a change in the data, set default status
                mt.addPatch( "replace", "/status", defStatus );
            }
        }

        if ( newR.getXp7ConfigSpec() != null )
        {
            patchDefault( mt, false, newR.getXp7ConfigSpec().getDataBase64(), "/spec/dataBase64" );
            patchDefault( mt, cfgStr( "operator.charts.values.allNodesKey" ), newR.getXp7ConfigSpec().getNodeGroup(), "/spec/nodeGroup" );
        }

        ensureOwnerReference( mt );
    }

    private void xp7deployment( MutationRequest mt )
    {
        Xp7Deployment oldR = (Xp7Deployment) mt.getAdmissionReview().getRequest().getOldObject();
        Xp7Deployment newR = (Xp7Deployment) mt.getAdmissionReview().getRequest().getObject();

        Xp7DeploymentStatus defStatus = new Xp7DeploymentStatus().
            withMessage( "Created" ).
            withState( Xp7DeploymentStatus.State.PENDING ).
            withXp7DeploymentStatusFields( new Xp7DeploymentStatusFields().
                withXp7DeploymentStatusFieldsPods( new LinkedList<>() ) );

        // This is an update => set default status to old status
        if ( oldR != null )
        {
            defStatus = oldR.getXp7DeploymentStatus();
        }

        // Ensure status
        if ( !patchDefault( mt, defStatus, newR.getXp7DeploymentStatus(), "/status" ) )
        {
            patchDefault( mt, defStatus.getMessage(), newR.getXp7DeploymentStatus().getMessage(), "/status/message" );
            patchDefault( mt, defStatus.getState(), newR.getXp7DeploymentStatus().getState(), "/status/state" );
            patchDefault( mt, defStatus.getXp7DeploymentStatusFields(), newR.getXp7DeploymentStatus().getXp7DeploymentStatusFields(),
                          "/status/fields" );
        }
    }

    private void domain( final MutationRequest mt )
    {
        Domain newR = (Domain) mt.getAdmissionReview().getRequest().getObject();
        if ( newR == null )
        {
            return;
        }

        DomainStatus defStatus = new DomainStatus().
            withState( DomainStatus.State.PENDING ).
            withMessage( "Created" ).
            withDomainStatusFields( new DomainStatusFields( new LinkedList<>(), false ) );

        if ( !patchDefault( mt, defStatus, newR.getDomainStatus(), "/status" ) )
        {
            patchDefault( mt, defStatus.getMessage(), newR.getDomainStatus().getMessage(), "/status/message" );
            patchDefault( mt, defStatus.getState(), newR.getDomainStatus().getState(), "/status/state" );
            patchDefault( mt, defStatus.getDomainStatusFields(), newR.getDomainStatus().getDomainStatusFields(), "/status/fields" );
        }

        patchDefault( mt, 3600, newR.getDomainSpec().getDnsTTL(), "/spec/dnsTTL" );
    }

    private void ingress( final MutationRequest mt )
    {
        Ingress newR = (Ingress) mt.getAdmissionReview().getRequest().getObject();
        if ( newR == null )
        {
            return;
        }

        Map<String, String> oldAnnotations =
            newR.getMetadata().getAnnotations() != null ? newR.getMetadata().getAnnotations() : new HashMap<>();
        Map<String, String> na = new HashMap<>( oldAnnotations );

        setDefault( na, "kubernetes.io/ingress.class", "nginx" );

        if ( "nginx".equals( na.get( "kubernetes.io/ingress.class" ) ) )
        {
            boolean linkerd = cfgBool( "operator.charts.values.settings.linkerd" );
            if ( linkerd )
            {
                String cfgSnippet = na.get( "nginx.ingress.kubernetes.io/configuration-snippet" );
                StringBuilder sb = new StringBuilder( cfgSnippet != null ? cfgSnippet : "" ).
                    append( "\n" ).
                    append( "proxy_set_header l5d-dst-override $service_name.$namespace.svc.cluster.local:$service_port;" ).
                    append( "grpc_set_header l5d-dst-override $service_name.$namespace.svc.cluster.local:$service_port;" );
                na.put( "nginx.ingress.kubernetes.io/configuration-snippet", sb.toString() );
            }
        }

        if ( !Objects.equals( oldAnnotations, na ) )
        {
            mt.addPatch( newR.getMetadata().getAnnotations() == null ? "add" : "replace", "/metadata/annotations", na );
        }
    }

    private void setDefault( Map<String, String> m, String key, String def )
    {
        if ( !m.containsKey( key ) )
        {
            m.put( key, def );
        }
    }

    private void ensureOwnerReference( MutationRequest mutationRequest )
    {
        if ( !mutationRequest.getAdmissionReview().getRequest().getOperation().equals( "CREATE" ) )
        {
            return;
        }

        HasMetadata obj = (HasMetadata) mutationRequest.getAdmissionReview().getRequest().getObject();

        if ( obj == null )
        {
            return;
        }

        if ( !obj.getMetadata().getOwnerReferences().isEmpty() )
        {
            return;
        }

        Optional<Xp7Deployment> xp7Deployments = getXp7Deployment( obj );
        if ( xp7Deployments.isEmpty() )
        {
            return;
        }

        mutationRequest.addPatch( "add", "/metadata/ownerReferences",
                                  Collections.singletonList( createOwnerReference( xp7Deployments.get() ) ) );
    }

    private <T> boolean patchDefault( MutationRequest mt, T defaultValue, T currentValue, String path )
    {
        if ( currentValue == null )
        {
            mt.addPatch( "add", path, defaultValue );
            return true;
        }
        return false;
    }

    private <T> boolean patchDefault( MutationRequest mt, T defaultValue, T currentValue, Function<String, String> pathFunc, String path )
    {
        return patchDefault( mt, defaultValue, currentValue, pathFunc.apply( path ) );
    }
}
