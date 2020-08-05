package com.enonic.cloud.operator.api.mutation;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
import io.fabric8.kubernetes.api.model.extensions.Ingress;

import com.enonic.cloud.kubernetes.model.v1alpha1.xp7app.Xp7App;
import com.enonic.cloud.kubernetes.model.v1alpha1.xp7app.Xp7AppStatus;
import com.enonic.cloud.kubernetes.model.v1alpha1.xp7app.Xp7AppStatusFields;
import com.enonic.cloud.kubernetes.model.v1alpha2.domain.Domain;
import com.enonic.cloud.kubernetes.model.v1alpha2.domain.DomainStatus;
import com.enonic.cloud.kubernetes.model.v1alpha2.domain.DomainStatusFields;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7config.Xp7Config;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7Deployment;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7DeploymentStatus;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7DeploymentStatusFields;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7vhost.Xp7VHost;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7vhost.Xp7VHostSpec;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7vhost.Xp7VHostSpecMapping;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7vhost.Xp7VHostSpecMappingOptions;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7vhost.Xp7VHostSpecMappingOptionsIngressAnnotations;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7vhost.Xp7VHostSpecOptions;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7vhost.Xp7VHostStatus;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7vhost.Xp7VHostStatusFields;
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
        addFunction( Xp7VHost.class, this::xp7VHost );
        addFunction( Domain.class, this::domain );
        addFunction( io.fabric8.kubernetes.api.model.extensions.Ingress.class, this::ingress );
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
            builder.withPatch( BaseEncoding.base64().encode( mapper.writeValueAsString( mutationRequest.getPatches() ).getBytes() ) );
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
            List<String> finalizers = mt.getAdmissionReview().getRequest().getObject().getMetadata().getFinalizers();
            String uninstallFinalizer = cfgStr( "operator.finalizer.app.uninstall" );
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

    private void xp7config( MutationRequest mutationRequest )
    {
        Xp7Config newR = (Xp7Config) mutationRequest.getAdmissionReview().getRequest().getObject();

        if ( newR.getXp7ConfigSpec() != null )
        {
            patchDefault( mutationRequest, cfgStr( "operator.helm.charts.Values.allNodesKey" ), newR.getXp7ConfigSpec().getNodeGroup(),
                          "/spec/nodeGroup" );
        }

        ensureOwnerReference( mutationRequest );
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
        Map<String, String> newAnnotations = new HashMap<>( oldAnnotations );

        if ( !newAnnotations.containsKey( "kubernetes.io/ingress.class" ) )
        {
            newAnnotations.put( "kubernetes.io/ingress.class", "nginx" );
        }
    }

    private void xp7VHost( MutationRequest mt )
    {
        Xp7VHost oldR = (Xp7VHost) mt.getAdmissionReview().getRequest().getOldObject();
        Xp7VHost newR = (Xp7VHost) mt.getAdmissionReview().getRequest().getObject();

        Xp7VHostStatus defStatus = new Xp7VHostStatus().
            withMessage( "Created" ).
            withState( Xp7VHostStatus.State.PENDING ).
            withXp7VHostStatusFields( new Xp7VHostStatusFields().
                withPublicIps( new LinkedList<>() ).
                withDnsRecordCreated( false ) );

        // This is an update => set default status to old status
        if ( oldR != null )
        {
            defStatus = oldR.getXp7VHostStatus();
        }

        // Ensure status
        if ( !patchDefault( mt, defStatus, newR.getXp7VHostStatus(), "/status" ) )
        {
            patchDefault( mt, defStatus.getMessage(), newR.getXp7VHostStatus().getMessage(), "/status/message" );
            patchDefault( mt, defStatus.getState(), newR.getXp7VHostStatus().getState(), "/status/state" );
            patchDefault( mt, defStatus.getXp7VHostStatusFields(), newR.getXp7VHostStatus().getXp7VHostStatusFields(), "/status/fields" );
        }

        // Ensure spec
        if ( newR.getXp7VHostSpec() != null )
        {
            Xp7VHostSpec spec = newR.getXp7VHostSpec();
            Xp7VHostSpecOptions defaultVHostSpecOptions = new Xp7VHostSpecOptions().
                withDnsRecord( true ).
                withCdn( true );

            if ( !patchDefault( mt, defaultVHostSpecOptions, spec.getXp7VHostSpecOptions(), "/spec/options" ) )
            {
                patchDefault( mt, defaultVHostSpecOptions.getCdn(), spec.getXp7VHostSpecOptions().getCdn(), "/spec/options/cdn" );
                patchDefault( mt, defaultVHostSpecOptions.getDnsRecord(), spec.getXp7VHostSpecOptions().getDnsRecord(),
                              "/spec/options/dnsRecord" );
            }

            for ( int i = 0; i < newR.getXp7VHostSpec().getXp7VHostSpecMappings().size(); i++ )
            {
                Xp7VHostSpecMapping m = newR.getXp7VHostSpec().getXp7VHostSpecMappings().get( i );

                final int finalI = i;
                Function<String, String> pathFunc = ( p ) -> String.format( "/spec/mappings/%d%s", finalI, p );

                patchDefault( mt, cfgStr( "operator.helm.charts.Values.allNodesKey" ), m.getNodeGroup(), pathFunc, "/nodeGroup" );

                if ( m.getXp7VHostSpecMappingIdProvider() != null )
                {
                    patchDefault( mt, "system", m.getNodeGroup(), pathFunc, "/idProviders/default" );
                }

                Xp7VHostSpecMappingOptionsIngressAnnotations defaultAnnotations = new Xp7VHostSpecMappingOptionsIngressAnnotations();
                addDefaultIngressAnnotations( defaultAnnotations );

                Xp7VHostSpecMappingOptions defSpecOpt = new Xp7VHostSpecMappingOptions().
                    withIngress( true ).
                    withXp7VHostSpecMappingOptionsIngressAnnotations( defaultAnnotations );

                // Set default options
                if ( !patchDefault( mt, defSpecOpt, m.getXp7VHostSpecMappingOptions(), pathFunc, "/options" ) )
                {
                    patchDefault( mt, defSpecOpt.getIngress(), m.getXp7VHostSpecMappingOptions().getIngress(), pathFunc,
                                  "/options/ingress" );

                    // Set default annotations
                    if ( !patchDefault( mt, defSpecOpt.getXp7VHostSpecMappingOptionsIngressAnnotations(),
                                        m.getXp7VHostSpecMappingOptions().getXp7VHostSpecMappingOptionsIngressAnnotations(), pathFunc,
                                        "/options/ingressAnnotations" ) )
                    {
                        if ( addDefaultIngressAnnotations( m.getXp7VHostSpecMappingOptions().
                            getXp7VHostSpecMappingOptionsIngressAnnotations() ) )
                        {
                            patchDefault( mt, m.getXp7VHostSpecMappingOptions().
                                getXp7VHostSpecMappingOptionsIngressAnnotations(), null, pathFunc, "/options/ingressAnnotations" );
                        }
                    }
                }
            }
        }

        ensureOwnerReference( mt );
    }

    private boolean addDefaultIngressAnnotations( final Xp7VHostSpecMappingOptionsIngressAnnotations annotations )
    {
        boolean change = false;
        if ( annotations.getAdditionalProperties().get( "ingress.kubernetes.io/rewrite-target" ) == null )
        {
            annotations.setAdditionalProperty( "ingress.kubernetes.io/rewrite-target", "/" );
            change = true;
        }

        if ( annotations.getAdditionalProperties().get( "kubernetes.io/ingress.class" ) == null )
        {
            annotations.setAdditionalProperty( "kubernetes.io/ingress.class", "nginx" );
            change = true;
        }

        boolean linkerd = cfgBool( "operator.helm.charts.Values.extensions.linkerd.enabled" );
        if ( linkerd )
        {
            String cfgSnippet = annotations.getAdditionalProperties().get( "nginx.ingress.kubernetes.io/configuration-snippet" );
            StringBuilder sb = new StringBuilder( cfgSnippet != null ? cfgSnippet : "" ).
                append( "\n" ).
                append( "proxy_set_header l5d-dst-override $service_name.$namespace.svc.cluster.local:$service_port;" ).
                append( "grpc_set_header l5d-dst-override $service_name.$namespace.svc.cluster.local:$service_port;" );
            annotations.setAdditionalProperty( "nginx.ingress.kubernetes.io/configuration-snippet", sb.toString() );
            change = true;
        }

        return change;
    }

    private void ensureOwnerReference( MutationRequest mutationRequest )
    {
        if ( !mutationRequest.getAdmissionReview().getRequest().getOperation().equals( "CREATE" ) )
        {
            return;
        }

        HasMetadata obj = mutationRequest.getAdmissionReview().getRequest().getObject();

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
