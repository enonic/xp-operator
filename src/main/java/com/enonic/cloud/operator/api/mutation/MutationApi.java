package com.enonic.cloud.operator.api.mutation;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
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

import com.enonic.cloud.kubernetes.model.v1alpha1.xp7app.Xp7App;
import com.enonic.cloud.kubernetes.model.v1alpha1.xp7app.Xp7AppStatus;
import com.enonic.cloud.kubernetes.model.v1alpha1.xp7app.Xp7AppStatusFields;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7config.Xp7Config;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7Deployment;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7DeploymentSpecNodeGroups;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7DeploymentStatus;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7DeploymentStatusFields;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7DeploymentStatusFieldsPods;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7vhost.Xp7VHost;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7vhost.Xp7VHostSpecMapping;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7vhost.Xp7VHostSpecMappingOptions;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7vhost.Xp7VHostSpecOptions;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7vhost.Xp7VHostStatus;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7vhost.Xp7VHostStatusFields;
import com.enonic.cloud.operator.api.BaseAdmissionApi;
import com.enonic.cloud.operator.functions.CreateOwnerReference;

import static com.enonic.cloud.common.Configuration.cfgStr;

@ApplicationScoped
@Path("/apis/operator.enonic.cloud/v1alpha1")
public class MutationApi
    extends BaseAdmissionApi<MutationRequest>
{
    private static CreateOwnerReference createOwnerReference = new CreateOwnerReference();

    public MutationApi()
    {
        super();
        addFunction( Xp7App.class, this::xp7app );
        addFunction( Xp7Config.class, this::xp7config );
        addFunction( Xp7Deployment.class, this::xp7deployment );
        addFunction( Xp7VHost.class, this::xp7VHost );
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

    public void xp7app( MutationRequest mutationRequest )
    {
        Xp7App oldApp = (Xp7App) mutationRequest.getAdmissionReview().getRequest().getOldObject();
        Xp7App newApp = (Xp7App) mutationRequest.getAdmissionReview().getRequest().getObject();

        Xp7AppStatus defaultStatus = new Xp7AppStatus().
            withMessage( "Created" ).
            withState( Xp7AppStatus.State.PENDING ).
            withXp7AppStatusFields( new Xp7AppStatusFields() );

        // This is an update => set default status to old status
        if ( oldApp != null )
        {
            defaultStatus = oldApp.getXp7AppStatus();
        }

        // Ensure status
        if ( newApp.getXp7AppStatus() == null )
        {
            mutationRequest.addPatch( "add", "/status", defaultStatus );
        }
        else
        {
            if ( newApp.getXp7AppStatus().getMessage() == null )
            {
                mutationRequest.addPatch( "add", "/status/message", defaultStatus.getMessage() );
            }
            if ( newApp.getXp7AppStatus().getState() == null )
            {
                mutationRequest.addPatch( "add", "/status/state", defaultStatus.getState() );
            }
            if ( newApp.getXp7AppStatus().getXp7AppStatusFields() == null )
            {
                mutationRequest.addPatch( "add", "/status/fields", defaultStatus.getXp7AppStatusFields() );
            }
        }

        // Ensure finalizers
        if ( mutationRequest.getAdmissionReview().getRequest().getOperation().equals( "CREATE" ) )
        {
            List<String> finalizers = mutationRequest.getAdmissionReview().getRequest().getObject().getMetadata().getFinalizers();
            String uninstallFinalizer = cfgStr( "operator.finalizer.app.uninstall" );
            if ( finalizers == null )
            {
                mutationRequest.addPatch( "add", "/metadata/finalizers", Collections.singletonList( uninstallFinalizer ) );
            }
            else if ( !finalizers.contains( uninstallFinalizer ) )
            {
                finalizers.add( uninstallFinalizer );
                mutationRequest.addPatch( "replace", "/metadata/finalizers", finalizers );
            }
        }

        ensureOwnerReference( mutationRequest );
    }

    public void xp7config( MutationRequest mutationRequest )
    {
        Xp7Config newConfig = (Xp7Config) mutationRequest.getAdmissionReview().getRequest().getObject();

        // Set missing node group to allNodesKey
        if ( newConfig.getXp7ConfigSpec().getNodeGroup() == null )
        {
            mutationRequest.addPatch( "add", "/spec/nodeGroup", cfgStr( "operator.helm.charts.Values.allNodesKey" ) );
        }

        ensureOwnerReference( mutationRequest );
    }

    public void xp7deployment( MutationRequest mutationRequest )
    {
        Xp7Deployment oldDeployment = (Xp7Deployment) mutationRequest.getAdmissionReview().getRequest().getOldObject();
        Xp7Deployment newDeployment = (Xp7Deployment) mutationRequest.getAdmissionReview().getRequest().getObject();

        Xp7DeploymentStatus defaultStatus = new Xp7DeploymentStatus().
            withMessage( "Created" ).
            withState( Xp7DeploymentStatus.State.PENDING ).
            withXp7DeploymentStatusFields( new Xp7DeploymentStatusFields().
                withXp7DeploymentStatusFieldsPods( new Xp7DeploymentStatusFieldsPods() ) );

        // This is an update => set default status to old status
        if ( oldDeployment != null )
        {
            defaultStatus = oldDeployment.getXp7DeploymentStatus();
        }

        // Ensure status
        if ( newDeployment.getXp7DeploymentStatus() == null )
        {
            mutationRequest.addPatch( "add", "/status", defaultStatus );
        }
        else
        {
            if ( newDeployment.getXp7DeploymentStatus().getMessage() == null )
            {
                mutationRequest.addPatch( "add", "/status/message", defaultStatus.getMessage() );
            }
            if ( newDeployment.getXp7DeploymentStatus().getState() == null )
            {
                mutationRequest.addPatch( "add", "/status/state", defaultStatus.getState() );
            }
            if ( newDeployment.getXp7DeploymentStatus().getXp7DeploymentStatusFields() == null )
            {
                mutationRequest.addPatch( "add", "/status/fields", defaultStatus.getXp7DeploymentStatusFields() );
            }
        }

        // Ensure spec
        if ( newDeployment.getXp7DeploymentSpec() != null )
        {
            if ( newDeployment.getXp7DeploymentSpec().getXp7DeploymentSpecNodesSharedDisks() == null )
            {
                mutationRequest.addPatch( "add", "/spec/nodesSharedDisks", Collections.emptyList() );
            }

            if ( newDeployment.getXp7DeploymentSpec().getXp7DeploymentSpecNodeGroups() == null )
            {
                mutationRequest.addPatch( "add", "/spec/nodeGroups", new Xp7DeploymentSpecNodeGroups() );
            }
        }
    }

    public void xp7VHost( MutationRequest mutationRequest )
    {
        Xp7VHost oldVHost = (Xp7VHost) mutationRequest.getAdmissionReview().getRequest().getOldObject();
        Xp7VHost newVHost = (Xp7VHost) mutationRequest.getAdmissionReview().getRequest().getObject();

        Xp7VHostStatus defaultStatus = new Xp7VHostStatus().
            withMessage( "Created" ).
            withState( Xp7VHostStatus.State.PENDING ).
            withXp7VHostStatusFields( new Xp7VHostStatusFields().
                withPublicIps( new LinkedList<>() ).
                withDnsRecordCreated( false ) );

        // This is an update => set default status to old status
        if ( oldVHost != null )
        {
            defaultStatus = oldVHost.getXp7VHostStatus();
        }

        // Ensure status
        if ( newVHost.getXp7VHostStatus() == null )
        {
            mutationRequest.addPatch( "add", "/status", defaultStatus );
        }
        else
        {
            if ( newVHost.getXp7VHostStatus().getMessage() == null )
            {
                mutationRequest.addPatch( "add", "/status/message", defaultStatus.getMessage() );
            }
            if ( newVHost.getXp7VHostStatus().getState() == null )
            {
                mutationRequest.addPatch( "add", "/status/state", defaultStatus.getState() );
            }
            if ( newVHost.getXp7VHostStatus().getXp7VHostStatusFields() == null )
            {
                mutationRequest.addPatch( "add", "/status/fields", defaultStatus.getXp7VHostStatusFields() );
            }
            else
            {
                if ( newVHost.getXp7VHostStatus().getXp7VHostStatusFields().getPublicIps() == null )
                {
                    mutationRequest.addPatch( "add", "/status/fields/publicIps", defaultStatus.getXp7VHostStatusFields().getPublicIps() );
                }
                if ( newVHost.getXp7VHostStatus().getXp7VHostStatusFields().getDnsRecordCreated() == null )
                {
                    mutationRequest.addPatch( "add", "/status/fields/dnsRecordCreated",
                                              defaultStatus.getXp7VHostStatusFields().getDnsRecordCreated() );
                }
            }
        }

        // Ensure spec
        if ( newVHost.getXp7VHostSpec() != null )
        {
            Xp7VHostSpecOptions defaultVHostSpecOptions = new Xp7VHostSpecOptions().
                withDnsRecord( true ).
                withCdn( true );

            if ( newVHost.getXp7VHostSpec().getXp7VHostSpecOptions() == null )
            {
                mutationRequest.addPatch( "add", "/spec/options", defaultVHostSpecOptions );
            }

            for ( int i = 0; i < newVHost.getXp7VHostSpec().getXp7VHostSpecMappings().size(); i++ )
            {
                Xp7VHostSpecMapping m = newVHost.getXp7VHostSpec().getXp7VHostSpecMappings().get( i );

                final int finalI = i;
                Function<String, String> pathFunc = ( p ) -> String.format( "/spec/mappings/%d%s", finalI, p );

                if ( m.getNodeGroup() == null )
                {
                    mutationRequest.addPatch( "add", pathFunc.apply( "/nodeGroup" ), cfgStr( "operator.helm.charts.Values.allNodesKey" ) );
                }

                if ( m.getXp7VHostSpecMappingIdProvider() != null )
                {
                    if ( m.getXp7VHostSpecMappingIdProvider().getDefault() == null )
                    {
                        mutationRequest.addPatch( "add", pathFunc.apply( "/idProviders/default" ), "system" );
                    }
                }

                if ( m.getXp7VHostSpecMappingOptions() == null )
                {
                    mutationRequest.addPatch( "add", pathFunc.apply( "/options" ), new Xp7VHostSpecMappingOptions().
                        withIngress( true ).
                        withIngressMaxBodySize( "100m" ).
                        withIpWhitelist( new LinkedList<>() ).
                        withSslRedirect( newVHost.getXp7VHostSpec().getXp7VHostSpecCertificate() != null ).
                        withStatusCake( false ).
                        withStickySession( false ) );
                }
            }
        }

        ensureOwnerReference( mutationRequest );
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

        List<Xp7Deployment> xp7Deployments = getXp7Deployment( obj );
        if ( xp7Deployments.isEmpty() )
        {
            return;
        }

        mutationRequest.addPatch( "add", "/metadata/ownerReferences/0", createOwnerReference.apply( xp7Deployments.get( 0 ) ) );
    }
}
