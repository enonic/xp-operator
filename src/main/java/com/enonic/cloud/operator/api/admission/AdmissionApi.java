package com.enonic.cloud.operator.api.admission;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Preconditions;

import io.fabric8.kubernetes.api.model.admission.AdmissionReview;

import com.enonic.cloud.kubernetes.InformerSearcher;
import com.enonic.cloud.kubernetes.model.v1alpha1.xp7app.Xp7App;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7config.Xp7Config;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7Deployment;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7DeploymentSpecNodeGroup;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7vhost.Xp7VHost;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7vhost.Xp7VHostSpecCertificate;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7vhost.Xp7VHostSpecMapping;
import com.enonic.cloud.operator.api.BaseAdmissionApi;

import static com.enonic.cloud.common.Configuration.cfgStr;
import static com.enonic.cloud.common.Validator.dns1035;

@ApplicationScoped
@Path("/apis/operator.enonic.cloud/v1alpha1")
public class AdmissionApi
    extends BaseAdmissionApi<AdmissionReview>
{
    @Inject
    InformerSearcher<Xp7Config> xp7ConfigInformerSearcher;

    public AdmissionApi()
    {
        super();
        addFunction( Xp7App.class, this::xp7app );
        addFunction( Xp7Config.class, this::xp7config );
        addFunction( Xp7Deployment.class, this::xp7deployment );
        addFunction( Xp7VHost.class, this::xp7VHost );
    }

    @POST
    @Path("/validations")
    @Consumes("application/json")
    @Produces("application/json")
    public AdmissionReview validate( AdmissionReview admissionReview )
        throws JsonProcessingException
    {
        return handle( admissionReview );
    }

    @Override
    protected AdmissionReview createApiObject( final AdmissionReview admissionReview )
    {
        return admissionReview;
    }

    public void xp7app( AdmissionReview admissionReview )
    {
        Xp7App newApp = (Xp7App) admissionReview.getRequest().getObject();

        if ( newApp != null )
        {
            Preconditions.checkState( newApp.getXp7AppSpec() != null, "'spec' cannot be null" );
            Preconditions.checkState( newApp.getXp7AppSpec().getUrl() != null, "'spec.url' cannot be null" );

            assertXp7Deployment( admissionReview );
        }
    }

    public void xp7config( AdmissionReview admissionReview )
    {
        Xp7Config newConfig = (Xp7Config) admissionReview.getRequest().getObject();

        if ( newConfig != null )
        {
            Preconditions.checkState( newConfig.getXp7ConfigSpec() != null, "'spec' cannot be null" );
            Preconditions.checkState( newConfig.getXp7ConfigSpec().getNodeGroup() != null, "'spec.nodeGroup' cannot be null" );
            Preconditions.checkState( newConfig.getXp7ConfigSpec().getData() != null, "'spec.data' cannot be null" );
            Preconditions.checkState( newConfig.getXp7ConfigSpec().getFile() != null, "'spec.file' cannot be null" );

            // Check for file clash
            List<Xp7Config> presentConfigs = xp7ConfigInformerSearcher.get( newConfig.getMetadata().getNamespace() ).
                filter( c -> !c.getMetadata().getName().equals( newConfig.getMetadata().getName() ) ).
                filter( c -> c.getXp7ConfigSpec().getFile().equals( newConfig.getXp7ConfigSpec().getFile() ) ).
                filter( c -> c.getXp7ConfigSpec().getNodeGroup().equals( newConfig.getXp7ConfigSpec().getFile() ) ||
                    c.getXp7ConfigSpec().getNodeGroup().equals( cfgStr( "" ) ) ).
                collect( Collectors.toList() );
            if ( !presentConfigs.isEmpty() )
            {
                Preconditions.checkState( false, "XpConfig '%s' already defines file '%s'", presentConfigs.get( 0 ).getMetadata().getName(),
                                          presentConfigs.get( 0 ).getXp7ConfigSpec().getFile() );
            }

            // Check for present deployment
            assertXp7Deployment( admissionReview );
        }
    }

    public void xp7deployment( AdmissionReview admissionReview )
    {
        Xp7Deployment newDeployment = (Xp7Deployment) admissionReview.getRequest().getObject();

        if ( newDeployment != null )
        {
            Preconditions.checkState( newDeployment.getXp7DeploymentSpec() != null, "'spec' cannot be null" );
            Preconditions.checkState( newDeployment.getXp7DeploymentSpec().getEnabled() != null, "'spec.enabled' cannot be null" );
            Preconditions.checkState( newDeployment.getXp7DeploymentSpec().getXpVersion() != null, "'spec.xpVersion' cannot be null" );
            Preconditions.checkState( newDeployment.getXp7DeploymentSpec().getXp7DeploymentSpecNodesSharedDisks() != null,
                                      "'spec.nodesSharedDisks' cannot be null" );
            Preconditions.checkState( newDeployment.getXp7DeploymentSpec().getXp7DeploymentSpecNodeGroups() != null,
                                      "'spec.nodeGroups' cannot be null" );

            for ( Map.Entry<String, Xp7DeploymentSpecNodeGroup> e : newDeployment.getXp7DeploymentSpec().getXp7DeploymentSpecNodeGroups().getAdditionalProperties().entrySet() )
            {
                dns1035( "spec.nodeGroups[" + e.getKey() + "]", e.getKey() );
                Preconditions.checkState( e.getValue().getData() != null, "'spec.nodeGroups[" + e.getKey() + "].data cannot be null'" );
                Preconditions.checkState( e.getValue().getMaster() != null, "'spec.nodeGroups[" + e.getKey() + "].master cannot be null'" );
                Preconditions.checkState( e.getValue().getReplicas() != null,
                                          "'spec.nodeGroups[" + e.getKey() + "].replicas cannot be null'" );
                Preconditions.checkState( e.getValue().getXp7DeploymentSpecNodeGroupEnvironment() != null,
                                          "'spec.nodeGroups[" + e.getKey() + "].env cannot be null'" );
                Preconditions.checkState( e.getValue().getXp7DeploymentSpecNodeGroupResources() != null,
                                          "'spec.nodeGroups[" + e.getKey() + "].resources cannot be null'" );
                Preconditions.checkState( e.getValue().getXp7DeploymentSpecNodeGroupResources().getCpu() != null,
                                          "'spec.nodeGroups[" + e.getKey() + "].resources.cpu cannot be null'" );
                Preconditions.checkState( e.getValue().getXp7DeploymentSpecNodeGroupResources().getMemory() != null,
                                          "'spec.nodeGroups[" + e.getKey() + "].resources.memory cannot be null'" );
                Preconditions.checkState(
                    e.getValue().getXp7DeploymentSpecNodeGroupResources().getXp7DeploymentSpecNodeGroupDisks() != null,
                    "'spec.nodeGroups[" + e.getKey() + "].resources.disks cannot be null'" );
            }
        }

        if ( admissionReview.getRequest().getOperation().equals( "CREATE" ) )
        {

            List<Xp7Deployment> xp7Deployments = getXp7Deployment( admissionReview.getRequest().getObject() );
            Preconditions.checkState( xp7Deployments.size() == 0, "There is already an Xp7Deployment in NS '%s'",
                                      admissionReview.getRequest().getObject().getMetadata().getNamespace() );
        }
    }

    public void xp7VHost( AdmissionReview admissionReview )
    {
        Xp7VHost newVHost = (Xp7VHost) admissionReview.getRequest().getObject();
        if ( newVHost != null )
        {
            Preconditions.checkState( newVHost.getXp7VHostSpec() != null, "'spec' cannot be null" );
            Preconditions.checkState( newVHost.getXp7VHostSpec().getHost() != null, "'spec.host' cannot be null" );
            Preconditions.checkState( newVHost.getXp7VHostSpec().getXp7VHostSpecOptions() != null, "'spec.options' cannot be null" );
            Preconditions.checkState( newVHost.getXp7VHostSpec().getXp7VHostSpecOptions().getCdn() != null,
                                      "'spec.options.cdn' cannot be null" );
            Preconditions.checkState( newVHost.getXp7VHostSpec().getXp7VHostSpecOptions().getDnsRecord() != null,
                                      "'spec.options.dnsRecord' cannot be null" );

            if ( newVHost.getXp7VHostSpec().getXp7VHostSpecCertificate() != null )
            {
                Xp7VHostSpecCertificate.Authority authority = newVHost.getXp7VHostSpec().getXp7VHostSpecCertificate().getAuthority();
                Preconditions.checkState( authority != null, "'spec.certificate.authority' cannot be null" );

                if ( authority.equals( Xp7VHostSpecCertificate.Authority.SELF_SIGNED ) )
                {
                    Preconditions.checkState( newVHost.getXp7VHostSpec().getXp7VHostSpecCertificate().getIdentifier() != null,
                                              "'spec.certificate.identifier' cannot be null when authority is '%s'", authority );
                }
                else if ( authority.equals( Xp7VHostSpecCertificate.Authority.SECRET ) )
                {
                    Preconditions.checkState( newVHost.getXp7VHostSpec().getXp7VHostSpecCertificate().getIdentifier() != null,
                                              "'spec.certificate.identifier' cannot be null when authority is '%s'", authority );
                }
            }

            Preconditions.checkState( newVHost.getXp7VHostSpec().getXp7VHostSpecMappings() != null, "'spec.mappings' cannot be null" );

            int i = 0;
            for ( Xp7VHostSpecMapping m : newVHost.getXp7VHostSpec().getXp7VHostSpecMappings() )
            {
                Preconditions.checkState( m.getNodeGroup() != null, "'spec.mappings[%s].nodeGroup' cannot be null", i );
                Preconditions.checkState( m.getSource() != null, "'spec.mappings[%s].source' cannot be null", i );
                Preconditions.checkState( m.getTarget() != null, "'spec.mappings[%s].target' cannot be null", i );
                i++;
            }

            assertXp7Deployment( admissionReview );
        }
    }

    private void assertXp7Deployment( AdmissionReview admissionReview )
    {
        if ( admissionReview.getRequest().getOperation().equals( "CREATE" ) )
        {
            List<Xp7Deployment> xp7Deployments = getXp7Deployment( admissionReview.getRequest().getObject() );
            Preconditions.checkState( xp7Deployments.size() == 1, "No Xp7Deployment found in NS '%s'",
                                      admissionReview.getRequest().getObject().getMetadata().getNamespace() );
        }
    }
}
