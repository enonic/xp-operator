package com.enonic.cloud.operator.api.admission;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Preconditions;

import io.fabric8.kubernetes.api.model.admission.AdmissionReview;

import com.enonic.cloud.common.Validator;
import com.enonic.cloud.kubernetes.model.v1alpha1.xp7app.Xp7App;
import com.enonic.cloud.kubernetes.model.v1alpha2.domain.Domain;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7config.Xp7Config;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7Deployment;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7DeploymentSpecNodeGroup;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7vhost.Xp7VHost;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7vhost.Xp7VHostSpecCertificate;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7vhost.Xp7VHostSpecMapping;
import com.enonic.cloud.operator.api.BaseAdmissionApi;

import static com.enonic.cloud.common.Configuration.cfgIfBool;
import static com.enonic.cloud.common.Configuration.cfgStr;
import static com.enonic.cloud.common.Validator.dns1123;

@ApplicationScoped
@Path("/apis/operator.enonic.cloud/v1alpha1")
public class AdmissionApi
    extends BaseAdmissionApi<AdmissionReview>
{
    public AdmissionApi()
    {
        super();
        addFunction( Xp7App.class, this::xp7app );
        addFunction( Xp7Config.class, this::xp7config );
        addFunction( Xp7Deployment.class, this::xp7deployment );
        addFunction( Xp7VHost.class, this::xp7VHost );
        addFunction( Domain.class, this::domain );
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

    private void xp7app( AdmissionReview admissionReview )
    {
        Xp7App newApp = (Xp7App) admissionReview.getRequest().getObject();

        if ( newApp != null )
        {
            Preconditions.checkState( newApp.getXp7AppSpec() != null, "'spec' cannot be null" );
            Preconditions.checkState( newApp.getXp7AppSpec().getUrl() != null, "'spec.url' cannot be null" );

            assertXp7Deployment( admissionReview, null );
        }
    }

    private void xp7config( AdmissionReview admissionReview )
    {
        Xp7Config newConfig = (Xp7Config) admissionReview.getRequest().getObject();

        if ( newConfig != null )
        {
            Preconditions.checkState( newConfig.getXp7ConfigSpec() != null, "'spec' cannot be null" );
            Preconditions.checkState( newConfig.getXp7ConfigSpec().getNodeGroup() != null, "'spec.nodeGroup' cannot be null" );
            Preconditions.checkState( newConfig.getXp7ConfigSpec().getData() != null, "'spec.data' cannot be null" );
            Preconditions.checkState( newConfig.getXp7ConfigSpec().getFile() != null, "'spec.file' cannot be null" );

            // Check for file clash
            List<Xp7Config> presentConfigs = searchers.xp7Config().query().
                inNamespace( newConfig.getMetadata().getNamespace() ).
                filter( c -> !c.getMetadata().getName().equals( newConfig.getMetadata().getName() ) ).
                filter( c -> c.getXp7ConfigSpec().getFile().equals( newConfig.getXp7ConfigSpec().getFile() ) ).
                filter( c -> c.getXp7ConfigSpec().getNodeGroup().equals( newConfig.getXp7ConfigSpec().getFile() ) ||
                    c.getXp7ConfigSpec().getNodeGroup().equals( cfgStr( "operator.helm.charts.Values.allNodesKey" ) ) ).
                list();

            if ( !presentConfigs.isEmpty() )
            {
                Preconditions.checkState( !presentConfigs.isEmpty(), "XpConfig '%s' already defines file '%s'",
                                          presentConfigs.get( 0 ).getMetadata().getName(),
                                          presentConfigs.get( 0 ).getXp7ConfigSpec().getFile() );
            }

            // Check for present deployment
            assertXp7Deployment( admissionReview, Collections.singleton( newConfig.getXp7ConfigSpec().getNodeGroup() ) );
        }
    }

    private void xp7deployment( AdmissionReview admissionReview )
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

            int nrOfMasterNodes = 0;
            int i = 0;
            for ( Xp7DeploymentSpecNodeGroup ng : newDeployment.getXp7DeploymentSpec().getXp7DeploymentSpecNodeGroups() )
            {
                Preconditions.checkState( ng.getName() != null, "'spec.nodeGroups[" + i + "].name' cannot be null" );
                Preconditions.checkState( !ng.getName().equals( cfgStr( "operator.helm.charts.Values.allNodesKey" ) ),
                                          "'spec.nodeGroups[" + i + "].name' cannot be " +
                                              cfgStr( "operator.helm.charts.Values.allNodesKey" ) );
                dns1123( "spec.nodeGroups[" + i + "].name", ng.getName() );
                Preconditions.checkState( ng.getData() != null, "'spec.nodeGroups[" + i + "].data' cannot be null" );
                Preconditions.checkState( ng.getMaster() != null, "'spec.nodeGroups[" + i + "].master' cannot be null" );
                Preconditions.checkState( ng.getReplicas() != null, "'spec.nodeGroups[" + i + "].replicas' cannot be null" );
                Preconditions.checkState( ng.getReplicas() >= 0, "'spec.nodeGroups[" + i + "].replicas' has to be >= 0" );
                Preconditions.checkState( ng.getXp7DeploymentSpecNodeGroupEnvironment() != null,
                                          "'spec.nodeGroups[" + i + "].env' cannot be null" );
                Preconditions.checkState( ng.getXp7DeploymentSpecNodeGroupResources() != null,
                                          "'spec.nodeGroups[" + i + "].resources' cannot be null" );
                Preconditions.checkState( ng.getXp7DeploymentSpecNodeGroupResources().getCpu() != null,
                                          "'spec.nodeGroups[" + i + "].resources.cpu' cannot be null" );
                Preconditions.checkState( ng.getXp7DeploymentSpecNodeGroupResources().getMemory() != null,
                                          "'spec.nodeGroups[" + i + "].resources.memory' cannot be null" );
                Preconditions.checkState( ng.getXp7DeploymentSpecNodeGroupResources().getMemory().contains( "Mi" ) ||
                                              ng.getXp7DeploymentSpecNodeGroupResources().getMemory().contains( "Gi" ),
                                          "'spec.nodeGroups[" + i + "].resources.memory' can only be defined with Gi or Mi" );
                Preconditions.checkState( ng.getXp7DeploymentSpecNodeGroupResources().getXp7DeploymentSpecNodeGroupDisks() != null,
                                          "'spec.nodeGroups[" + i + "].resources.disks' cannot be null" );

                if ( ng.getData() )
                {
                    Preconditions.checkState(
                        ng.getXp7DeploymentSpecNodeGroupResources().getXp7DeploymentSpecNodeGroupDisks().stream().anyMatch(
                            d -> d.getName().equals( "index" ) ), "nodes with data=true must have disk 'index' defined" );
                }

                if ( ng.getMaster() )
                {
                    nrOfMasterNodes += ng.getReplicas();
                }
                i++;
            }
            Preconditions.checkState( nrOfMasterNodes > 0, "some nodeGroups must have master=true" );
            Preconditions.checkState( nrOfMasterNodes % 2 == 1, "number of master nodes has to be an odd number" );
        }

        cfgIfBool( "operator.deployment.xp.labels.strictValidation", () -> {
            Preconditions.checkState( newDeployment.getMetadata() != null, "'metadata' cannot be null" );
            Preconditions.checkState( newDeployment.getMetadata().getLabels() != null, "'metadata.labels' cannot be null" );

            String cloud = newDeployment.getMetadata().getLabels().get( cfgStr( "operator.deployment.xp.labels.cloud" ) );
            String solution = newDeployment.getMetadata().getLabels().get( cfgStr( "operator.deployment.xp.labels.solution" ) );
            String environment = newDeployment.getMetadata().getLabels().get( cfgStr( "operator.deployment.xp.labels.environment" ) );
            String service = newDeployment.getMetadata().getLabels().get( cfgStr( "operator.deployment.xp.labels.service" ) );

            Preconditions.checkState( cloud != null, String.format( "'metadata.labels.%s' cannot be null",
                                                                    cfgStr( "operator.deployment.xp.labels.cloud" ) ) );
            Preconditions.checkState( solution != null, String.format( "'metadata.labels.%s' cannot be null",
                                                                       cfgStr( "operator.deployment.xp.labels.solution" ) ) );
            Preconditions.checkState( environment != null, String.format( "'metadata.labels.%s' cannot be null",
                                                                          cfgStr( "operator.deployment.xp.labels.environment" ) ) );
            Preconditions.checkState( service != null, String.format( "'metadata.labels.%s' cannot be null",
                                                                      cfgStr( "operator.deployment.xp.labels.service" ) ) );

            String name = String.format( "%s-%s-%s-%s", cloud, solution, environment, service );
            Preconditions.checkState( newDeployment.getMetadata().getName().equals( name ), String.format(
                "Xp7Deployment name must be equal to <Cloud>-<Solution>-<Environment>-<Service> according to labels, i.e: '%s'", name ) );
        } );

        if ( admissionReview.getRequest().getOperation().equals( "CREATE" ) )
        {

            Optional<Xp7Deployment> xp7Deployments = getXp7Deployment( admissionReview.getRequest().getObject() );
            Preconditions.checkState( xp7Deployments.isEmpty(), "There is already an Xp7Deployment in NS '%s'",
                                      admissionReview.getRequest().getObject().getMetadata().getNamespace() );
        }
    }

    private void xp7VHost( AdmissionReview admissionReview )
    {
        Xp7VHost newVHost = (Xp7VHost) admissionReview.getRequest().getObject();
        if ( newVHost != null )
        {
            Preconditions.checkState( newVHost.getXp7VHostSpec() != null, "'spec' cannot be null" );
            Preconditions.checkState( newVHost.getXp7VHostSpec().getHost() != null, "'spec.host' cannot be null" );
            Validator.dns1123( "spec.host", newVHost.getXp7VHostSpec().getHost() );
            Preconditions.checkState( newVHost.getXp7VHostSpec().getXp7VHostSpecOptions() != null, "'spec.options' cannot be null" );
            Preconditions.checkState( newVHost.getXp7VHostSpec().getXp7VHostSpecOptions().getCdn() != null,
                                      "'spec.options.cdn' cannot be null" );
            Preconditions.checkState( newVHost.getXp7VHostSpec().getXp7VHostSpecOptions().getDnsRecord() != null,
                                      "'spec.options.dnsRecord' cannot be null" );

            if ( newVHost.getXp7VHostSpec().getXp7VHostSpecCertificate() != null )
            {
                Xp7VHostSpecCertificate.Authority authority = newVHost.getXp7VHostSpec().getXp7VHostSpecCertificate().getAuthority();
                Preconditions.checkState( authority != null, "'spec.certificate.authority' cannot be null" );

                if ( authority.equals( Xp7VHostSpecCertificate.Authority.CLUSTER_ISSUER ) )
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
            Preconditions.checkState( !newVHost.getXp7VHostSpec().getXp7VHostSpecMappings().isEmpty(), "'spec.mappings' cannot be empty" );

            Set<String> nodeGroups = new HashSet<>();
            Set<String> sources = new HashSet<>();
            int i = 0;
            for ( Xp7VHostSpecMapping m : newVHost.getXp7VHostSpec().getXp7VHostSpecMappings() )
            {
                Preconditions.checkState( m.getNodeGroup() != null, "'spec.mappings[%s].nodeGroup' cannot be null", i );
                Preconditions.checkState( m.getSource() != null, "'spec.mappings[%s].source' cannot be null", i );
                Preconditions.checkState( m.getTarget() != null, "'spec.mappings[%s].target' cannot be null", i );
                Preconditions.checkState( !sources.contains( m.getSource() ), "'spec.mappings.source' has to be unique" );
                nodeGroups.add( m.getNodeGroup() );
                sources.add( m.getSource() );
                i++;
            }

            assertXp7Deployment( admissionReview, nodeGroups );
            if ( admissionReview.getRequest().getOperation().equals( "CREATE" ) )
            {
                Set<String> hosts = searchers.xp7VHost().query().list().stream().
                    filter( v -> v.getXp7VHostSpec().getXp7VHostSpecMappings().stream().anyMatch(
                        c -> c.getXp7VHostSpecMappingOptions().getIngress() ) ).
                    map( v -> v.getXp7VHostSpec().getHost() ).collect( Collectors.toSet() );
                Preconditions.checkState( !hosts.contains( newVHost.getXp7VHostSpec().getHost() ),
                                          "host is being used by another Xp7VHost" );
            }
        }
    }

    private void assertXp7Deployment( AdmissionReview admissionReview, Set<String> nodeGroups )
    {
        if ( admissionReview.getRequest().getOperation().equals( "CREATE" ) )
        {
            Optional<Xp7Deployment> xp7Deployments = getXp7Deployment( admissionReview.getRequest().getObject() );
            Preconditions.checkState( xp7Deployments.isPresent(), "No Xp7Deployment found in NS '%s'",
                                      admissionReview.getRequest().getObject().getMetadata().getNamespace() );
            if ( nodeGroups != null )
            {
                Set<String> xpDeploymentNodeGroups = xp7Deployments.get().
                    getXp7DeploymentSpec().
                    getXp7DeploymentSpecNodeGroups().
                    stream().
                    map( Xp7DeploymentSpecNodeGroup::getName ).
                    collect( Collectors.toSet() );
                Set<String> tmp = new HashSet<>( nodeGroups );
                tmp.removeAll( xpDeploymentNodeGroups );
                tmp.remove( cfgStr( "operator.helm.charts.Values.allNodesKey" ) );

                Preconditions.checkState( tmp.isEmpty(), String.format( "Xp7Deployment '%s' does not contain nodeGroups %s",
                                                                        xp7Deployments.get().getMetadata().getName(), nodeGroups ) );
            }
        }
    }


    private void domain( final AdmissionReview admissionReview )
    {
        Domain oldDomain = (Domain) admissionReview.getRequest().getOldObject();
        Domain newDomain = (Domain) admissionReview.getRequest().getObject();
        if ( newDomain == null )
        {
            return;
        }

        Preconditions.checkState( newDomain.getDomainSpec() != null, "'spec' cannot be null" );
        Preconditions.checkState( newDomain.getDomainSpec().getHost() != null, "'spec.host' cannot be null" );
        Validator.dns1123( "spec.host", newDomain.getDomainSpec().getHost() );
        Preconditions.checkState( newDomain.getDomainSpec().getDnsRecord() != null, "'spec.dnsRecord' cannot be null" );
        if ( newDomain.getDomainSpec().getDnsRecord() )
        {
            Preconditions.checkState( newDomain.getDomainSpec().getCdn() != null, "'spec.cdn' cannot be null if dnsRecord = true" );
        }
        if ( newDomain.getDomainSpec().getDomainSpecCertificate() != null )
        {
            Preconditions.checkState( newDomain.getDomainSpec().getDomainSpecCertificate().getAuthority() != null,
                                      "'spec.certificate.authority' cannot be null" );
            switch ( newDomain.getDomainSpec().getDomainSpecCertificate().getAuthority() )
            {
                case CUSTOM:
                    Preconditions.checkState( newDomain.getDomainSpec().getDomainSpecCertificate().getIdentifier() != null,
                                              "'spec.certificate.identifier' cannot be null when authority is CUSTOM" );
                case CLUSTER_ISSUER:
                    Preconditions.checkState( newDomain.getDomainSpec().getDomainSpecCertificate().getIdentifier() != null,
                                              "'spec.certificate.identifier' cannot be null when authority is CLUSTER_ISSUER" );
            }
        }

        if ( oldDomain != null && newDomain != null )
        {
            Preconditions.checkState( oldDomain.getDomainSpec().getHost().equals( newDomain.getDomainSpec().getHost() ),
                                      "'spec.host' cannot be changed" );
        }
    }
}
