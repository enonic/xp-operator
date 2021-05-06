package com.enonic.kubernetes.operator.api.mutation;

import com.enonic.kubernetes.client.v1alpha1.Xp7App;
import com.enonic.kubernetes.client.v1alpha1.xp7app.Xp7AppStatus;
import com.enonic.kubernetes.client.v1alpha1.xp7app.Xp7AppStatusFields;
import com.enonic.kubernetes.client.v1alpha2.Domain;
import com.enonic.kubernetes.client.v1alpha2.Xp7Config;
import com.enonic.kubernetes.client.v1alpha2.Xp7Deployment;
import com.enonic.kubernetes.client.v1alpha2.domain.DomainStatus;
import com.enonic.kubernetes.client.v1alpha2.domain.DomainStatusFields;
import com.enonic.kubernetes.client.v1alpha2.xp7config.Xp7ConfigStatus;
import com.enonic.kubernetes.client.v1alpha2.xp7deployment.Xp7DeploymentSpecNodesPreinstalledApps;
import com.enonic.kubernetes.client.v1alpha2.xp7deployment.Xp7DeploymentStatus;
import com.enonic.kubernetes.client.v1alpha2.xp7deployment.Xp7DeploymentStatusFields;
import com.enonic.kubernetes.operator.api.AdmissionOperation;
import com.enonic.kubernetes.operator.api.BaseAdmissionApi;
import com.enonic.kubernetes.operator.domain.LbServiceIpProducer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.io.BaseEncoding;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.admission.v1.AdmissionResponseBuilder;
import io.fabric8.kubernetes.api.model.admission.v1.AdmissionReview;
import io.fabric8.kubernetes.api.model.networking.v1beta1.Ingress;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static com.enonic.kubernetes.common.Configuration.cfgBool;
import static com.enonic.kubernetes.common.Configuration.cfgStr;
import static com.enonic.kubernetes.common.Configuration.cfgStrChild;
import static com.enonic.kubernetes.common.Utils.createOwnerReference;

@ApplicationScoped
@Path("/apis/operator.enonic.cloud/v1alpha1")
public class MutationApi
    extends BaseAdmissionApi<MutationRequest>
{
    @Inject
    LbServiceIpProducer lbServiceIpProducer;

    List<Xp7DeploymentSpecNodesPreinstalledApps> preInstalledApps;

    public MutationApi()
    {
        super();
        addFunction( Xp7App.class, this::xp7app );
        addFunction( Xp7Config.class, this::xp7config );
        addFunction( Xp7Deployment.class, this::xp7deployment );
        addFunction( Domain.class, this::domain );
        addFunction( Ingress.class, this::ingress );

        preInstalledApps = new LinkedList<>();
        cfgStrChild( "operator.preInstalledApps" ).
            entrySet().
            forEach( ( e ) -> preInstalledApps.add( new Xp7DeploymentSpecNodesPreinstalledApps().
                withName( e.getKey() ).
                withUrl( e.getValue() ) ) );
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
        if (mutationRequest.hasPatches()) {
            builder.
                withPatch( BaseEncoding.base64().encode( mapper.writeValueAsString( mutationRequest.getPatches() ).getBytes() ) ).
                withPatchType( "JSONPatch" );
        }
    }

    private void xp7app( MutationRequest mt )
    {
        // Collect old and new object
        Xp7App oldR = (Xp7App) mt.getAdmissionReview().getRequest().getOldObject();
        Xp7App newR = (Xp7App) mt.getAdmissionReview().getRequest().getObject();

        // Create default status
        Xp7AppStatus defStatus = new Xp7AppStatus().
            withMessage( "Created" ).
            withState( Xp7AppStatus.State.PENDING ).
            withXp7AppStatusFields( new Xp7AppStatusFields() );

        // Get OP
        AdmissionOperation op = getOperation( mt.getAdmissionReview() );

        // Ensure status
        switch (op) {
            case CREATE: // Always set the default status on new objects
                patch( mt, true, "/status", newR.getStatus(), defStatus );
                break;
            case UPDATE:
                if (newR.getSpec() != null && !newR.getSpec().getUrl().equals( oldR.getSpec().getUrl() )) {
                    // On url change, set default status
                    patch( mt, true, "/status", newR.getStatus(), defStatus );
                } else {
                    // Else make sure the old status is not removed
                    patch( mt, false, "/status", newR.getStatus(), oldR.getStatus() );
                }
                break;
            case DELETE:
                // Set pending deletion status
                oldR.getStatus().setState( Xp7AppStatus.State.PENDING );
                oldR.getStatus().setMessage( "Pending deletion" );
                patch( mt, true, "/status", newR.getStatus(), oldR.getStatus() );
                break;
        }

        // Ensure enabled
        patch( mt, false, "/spec/enabled", newR.getSpec().getEnabled(), true );

        if (op == AdmissionOperation.CREATE) {
            // Ensure finalizers
            List<String> oldFinalizers = ((HasMetadata) mt.getAdmissionReview().getRequest().getObject()).getMetadata().getFinalizers();
            Set<String> newFinalizers = oldFinalizers != null ? new HashSet<>( oldFinalizers ) : new HashSet<>();
            String uninstallFinalizer = cfgStr( "operator.charts.values.finalizers.app.uninstall" );
            if (!newFinalizers.contains( uninstallFinalizer )) {
                newFinalizers.add( uninstallFinalizer );
                patch( mt, true, "/metadata/finalizers", null, newFinalizers );
            }

            // Ensure owner reference
            ensureOwnerReference( mt );
        }
    }

    private void xp7config( MutationRequest mt )
    {
        // Collect old and new object
        Xp7Config oldR = (Xp7Config) mt.getAdmissionReview().getRequest().getOldObject();
        Xp7Config newR = (Xp7Config) mt.getAdmissionReview().getRequest().getObject();

        // Create default status
        Xp7ConfigStatus defStatus = new Xp7ConfigStatus().
            withMessage( "Not loaded" ).
            withState( Xp7ConfigStatus.State.PENDING );

        // Get OP
        AdmissionOperation op = getOperation( mt.getAdmissionReview() );

        // Ensure status
        switch (op) {
            case CREATE: // Always set the default status on new objects
                patch( mt, true, "/status", newR.getStatus(), defStatus );
                break;
            case UPDATE:
                if (newR.getSpec() != null && !newR.getSpec().equals( oldR.getSpec() )) {
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

    private void xp7deployment( MutationRequest mt )
    {
        // Collect old and new object
        Xp7Deployment oldR = (Xp7Deployment) mt.getAdmissionReview().getRequest().getOldObject();
        Xp7Deployment newR = (Xp7Deployment) mt.getAdmissionReview().getRequest().getObject();

        // Create default status
        Xp7DeploymentStatus defStatus = new Xp7DeploymentStatus().
            withMessage( "Waiting for pods" ).
            withState( Xp7DeploymentStatus.State.PENDING ).
            withXp7DeploymentStatusFields( new Xp7DeploymentStatusFields().
                withXp7DeploymentStatusFieldsPods( new LinkedList<>() ) );

        // Get OP
        AdmissionOperation op = getOperation( mt.getAdmissionReview() );

        // Ensure status
        switch (op) {
            case CREATE: // Always set the default status on new objects
                patch( mt, true, "/status", newR.getStatus(), defStatus );
                break;
            case UPDATE:
                if (newR.getSpec() != null && !newR.getSpec().equals( oldR.getSpec() )) {
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

        List<Xp7DeploymentSpecNodesPreinstalledApps> preInstall = newR.getSpec().getNodesPreinstalledApps();
        if (preInstall == null || preInstall.isEmpty()) {
            patch( mt, true, "/spec/nodesPreinstalledApps", newR.getSpec().getNodesPreinstalledApps(), preInstalledApps );
        }
    }

    private void domain( final MutationRequest mt )
    {
        // Collect old and new object
        Domain oldR = (Domain) mt.getAdmissionReview().getRequest().getOldObject();
        Domain newR = (Domain) mt.getAdmissionReview().getRequest().getObject();

        // Create default status
        DomainStatus defStatus = new DomainStatus().
            withState( DomainStatus.State.PENDING ).
            withMessage( "Waiting for DNS records" ).
            withDomainStatusFields( new DomainStatusFields( lbServiceIpProducer.get(), false ) );

        // Get OP
        AdmissionOperation op = getOperation( mt.getAdmissionReview() );

        // Ensure status
        switch (op) {
            case CREATE: // Always set the default status on new objects
                patch( mt, true, "/status", newR.getStatus(), defStatus );
                break;
            case UPDATE:
                if (newR.getSpec() != null && !newR.getSpec().equals( oldR.getSpec() )) {
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

        if (newR.getSpec() != null) {
            // Set default TTL
            patch( mt, false, "/spec/dnsTTL", newR.getSpec().getDnsTTL(), 3600 );
        }
    }

    private void ingress( final MutationRequest mt )
    {
        // Collect old and new object
        Ingress oldR = (Ingress) mt.getAdmissionReview().getRequest().getOldObject();
        Ingress newR = (Ingress) mt.getAdmissionReview().getRequest().getObject();

        if (newR == null) {
            return;
        }

        Map<String, String> oa = newR.getMetadata().getAnnotations() != null ? newR.getMetadata().getAnnotations() : new HashMap<>();
        Map<String, String> na = new HashMap<>( oa );

        setDefaultValueInMap( na, "kubernetes.io/ingress.class", "nginx" );

        if ("nginx".equals( na.get( "kubernetes.io/ingress.class" ) ) && cfgBool( "operator.charts.values.settings.linkerd" )) {
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

    private void setDefaultValueInMap( Map<String, String> m, String key, String def )
    {
        if (!m.containsKey( key )) {
            m.put( key, def );
        }
    }

    private void ensureOwnerReference( MutationRequest mt )
    {
        HasMetadata obj = (HasMetadata) mt.getAdmissionReview().getRequest().getObject();

        if (obj.getMetadata().getOwnerReferences() != null && !obj.getMetadata().getOwnerReferences().isEmpty()) {
            return;
        }

        Optional<Xp7Deployment> xp7Deployments = getXp7Deployment( obj );
        if (xp7Deployments.isEmpty()) {
            return;
        }

        patch( mt, false, "/metadata/ownerReferences", null, Collections.singletonList( createOwnerReference( xp7Deployments.get() ) ) );
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
