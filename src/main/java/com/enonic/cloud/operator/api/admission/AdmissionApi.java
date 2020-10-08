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

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.admission.AdmissionReview;
import io.fabric8.kubernetes.api.model.networking.v1beta1.Ingress;

import com.enonic.cloud.common.Validator;
import com.enonic.cloud.kubernetes.model.v1alpha1.xp7app.Xp7App;
import com.enonic.cloud.kubernetes.model.v1alpha2.domain.Domain;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7config.Xp7Config;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7Deployment;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7DeploymentSpecNodeGroup;
import com.enonic.cloud.operator.api.AdmissionOperation;
import com.enonic.cloud.operator.api.BaseAdmissionApi;

import static com.enonic.cloud.common.Configuration.cfgIfBool;
import static com.enonic.cloud.common.Configuration.cfgStr;
import static com.enonic.cloud.common.Validator.dns1123;
import static com.enonic.cloud.operator.helpers.VeleroBackups.backupRestoreInProgress;

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
        addFunction( Domain.class, this::domain );
        addFunction( Ingress.class, this::ingress );
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

    private void ingress( AdmissionReview admissionReview )
    {
        // Do nothing
    }

    private void xp7app( AdmissionReview admissionReview )
    {
        AdmissionOperation op = getOperation( admissionReview );

        if ( op == AdmissionOperation.DELETE )
        {
            return;
        }

        Xp7App newApp = (Xp7App) admissionReview.getRequest().getObject();

        // Check spec
        Preconditions.checkState( newApp.getXp7AppSpec() != null, "'spec' cannot be null" );
        Preconditions.checkState( newApp.getXp7AppSpec().getUrl() != null, "'spec.url' cannot be null" );
        Preconditions.checkState( newApp.getXp7AppSpec().getEnabled() != null, "'spec.enabled' cannot be null" );

        // Check status
        Preconditions.checkState( newApp.getXp7AppStatus() != null, "'status' cannot be null" );
        Preconditions.checkState( newApp.getXp7AppStatus().getMessage() != null, "'status.message' cannot be null" );
        Preconditions.checkState( newApp.getXp7AppStatus().getState() != null, "'status.state' cannot be null" );
        Preconditions.checkState( newApp.getXp7AppStatus().getXp7AppStatusFields() != null, "'status.fields' cannot be null" );

        if ( !backupRestoreInProgress( newApp ) && op == AdmissionOperation.CREATE )
        {
            assertXp7Deployment( admissionReview, null );
        }
    }

    private void xp7config( AdmissionReview admissionReview )
    {
        AdmissionOperation op = getOperation( admissionReview );

        if ( op == AdmissionOperation.DELETE )
        {
            return;
        }

        Xp7Config newConfig = (Xp7Config) admissionReview.getRequest().getObject();

        // Check spec
        Preconditions.checkState( newConfig.getXp7ConfigSpec() != null, "'spec' cannot be null" );
        Preconditions.checkState( newConfig.getXp7ConfigSpec().getNodeGroup() != null, "'spec.nodeGroup' cannot be null" );
        Preconditions.checkState( newConfig.getXp7ConfigSpec().getData() != null, "'spec.data' cannot be null" );
        Preconditions.checkState( newConfig.getXp7ConfigSpec().getFile() != null, "'spec.file' cannot be null" );
        Preconditions.checkState( newConfig.getXp7ConfigSpec().getDataBase64() != null, "'spec.dataBase64' cannot be null" );

        // Check status
        Preconditions.checkState( newConfig.getXp7ConfigStatus() != null, "'status' cannot be null" );
        Preconditions.checkState( newConfig.getXp7ConfigStatus().getMessage() != null, "'status.message' cannot be null" );
        Preconditions.checkState( newConfig.getXp7ConfigStatus().getState() != null, "'status.state' cannot be null" );

        // Check for file clash
        List<Xp7Config> presentConfigs = searchers.xp7Config().query().
            inNamespace( newConfig.getMetadata().getNamespace() ).
            filter( c -> !c.getMetadata().getName().equals( newConfig.getMetadata().getName() ) ).
            filter( c -> c.getXp7ConfigSpec().getFile().equals( newConfig.getXp7ConfigSpec().getFile() ) ).
            filter( c -> c.getXp7ConfigSpec().getNodeGroup().equals( newConfig.getXp7ConfigSpec().getFile() ) ||
                c.getXp7ConfigSpec().getNodeGroup().equals( cfgStr( "operator.charts.values.allNodesKey" ) ) ).
            list();
        if ( !presentConfigs.isEmpty() )
        {
            Preconditions.checkState( false, "XpConfig '%s' already defines file '%s'", presentConfigs.get( 0 ).getMetadata().getName(),
                                      presentConfigs.get( 0 ).getXp7ConfigSpec().getFile() );
        }

        // Check for present deployment
        if ( !backupRestoreInProgress( newConfig ) && op == AdmissionOperation.CREATE )
        {
            assertXp7Deployment( admissionReview, Collections.singleton( newConfig.getXp7ConfigSpec().getNodeGroup() ) );
        }
    }

    private void xp7deployment( AdmissionReview admissionReview )
    {
        AdmissionOperation op = getOperation( admissionReview );

        Xp7Deployment newDeployment = (Xp7Deployment) admissionReview.getRequest().getObject();

        if ( op != AdmissionOperation.DELETE )
        {
            // Check spec
            Preconditions.checkState( newDeployment.getXp7DeploymentSpec() != null, "'spec' cannot be null" );
            Preconditions.checkState( newDeployment.getXp7DeploymentSpec().getEnabled() != null, "'spec.enabled' cannot be null" );
            Preconditions.checkState( newDeployment.getXp7DeploymentSpec().getXpVersion() != null, "'spec.xpVersion' cannot be null" );
            Preconditions.checkState( newDeployment.getXp7DeploymentSpec().getXp7DeploymentSpecNodesSharedDisks() != null,
                                      "'spec.nodesSharedDisks' cannot be null" );
            Preconditions.checkState( newDeployment.getXp7DeploymentSpec().getXp7DeploymentSpecNodeGroups() != null,
                                      "'spec.nodeGroups' cannot be null" );

            // Check status
            Preconditions.checkState( newDeployment.getXp7DeploymentStatus() != null, "'status' cannot be null" );
            Preconditions.checkState( newDeployment.getXp7DeploymentStatus().getMessage() != null, "'status.message' cannot be null" );
            Preconditions.checkState( newDeployment.getXp7DeploymentStatus().getState() != null, "'status.state' cannot be null" );
            Preconditions.checkState( newDeployment.getXp7DeploymentStatus().getXp7DeploymentStatusFields() != null,
                                      "'status.fields' cannot be null" );
            Preconditions.checkState(
                newDeployment.getXp7DeploymentStatus().getXp7DeploymentStatusFields().getXp7DeploymentStatusFieldsPods() != null,
                "'status.fields.pods' cannot be null" );

            // Check node groups
            int nrOfMasterNodes = 0;
            int i = 0;
            for ( Xp7DeploymentSpecNodeGroup ng : newDeployment.getXp7DeploymentSpec().getXp7DeploymentSpecNodeGroups() )
            {
                Preconditions.checkState( ng.getName() != null, "'spec.nodeGroups[" + i + "].name' cannot be null" );
                Preconditions.checkState( !ng.getName().equals( cfgStr( "operator.charts.values.allNodesKey" ) ),
                                          "'spec.nodeGroups[" + i + "].name' cannot be " + cfgStr( "operator.charts.values.allNodesKey" ) );
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

                // Check disks
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

            // Check replicas
            Preconditions.checkState( nrOfMasterNodes > 0, "some nodeGroups must have master=true" );
            Preconditions.checkState( nrOfMasterNodes % 2 == 1, "number of master nodes has to be an odd number" );
        }

        // Strict label and name validation
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

        if ( op == AdmissionOperation.CREATE )
        {
            Optional<Xp7Deployment> xp7Deployments = getXp7Deployment( admissionReview.getRequest().getObject() );
            Preconditions.checkState( xp7Deployments.isEmpty(), "There is already an Xp7Deployment in NS '%s'",
                                      newDeployment.getMetadata().getNamespace() );
        }
    }


    private void domain( final AdmissionReview admissionReview )
    {
        AdmissionOperation op = getOperation( admissionReview );

        if ( op == AdmissionOperation.DELETE )
        {
            return;
        }

        Domain newDomain = (Domain) admissionReview.getRequest().getObject();

        // Check spec
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

        // Check status
        Preconditions.checkState( newDomain.getDomainStatus() != null, "'status' cannot be null" );
        Preconditions.checkState( newDomain.getDomainStatus().getMessage() != null, "'status.message' cannot be null" );
        Preconditions.checkState( newDomain.getDomainStatus().getState() != null, "'status.state' cannot be null" );
        Preconditions.checkState( newDomain.getDomainStatus().getDomainStatusFields() != null, "'status.fields' cannot be null" );
        Preconditions.checkState( newDomain.getDomainStatus().getDomainStatusFields().getDnsRecordCreated() != null,
                                  "'status.fields.dnsRecordCreated' cannot be null" );
        Preconditions.checkState( newDomain.getDomainStatus().getDomainStatusFields().getPublicIps() != null,
                                  "'status.fields.publicIps' cannot be null" );

        if ( op == AdmissionOperation.UPDATE )
        {
            Domain oldDomain = (Domain) admissionReview.getRequest().getOldObject();
            Preconditions.checkState( oldDomain.getDomainSpec().getHost().equals( newDomain.getDomainSpec().getHost() ),
                                      "'spec.host' cannot be changed" );
        }
    }

    private void assertXp7Deployment( AdmissionReview admissionReview, Set<String> nodeGroups )
    {
        Optional<Xp7Deployment> xp7Deployments = getXp7Deployment( admissionReview.getRequest().getObject() );
        Preconditions.checkState( xp7Deployments.isPresent(), "No Xp7Deployment found in NS '%s'",
                                  ( (HasMetadata) admissionReview.getRequest().getObject() ).getMetadata().getNamespace() );
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
            tmp.remove( cfgStr( "operator.charts.values.allNodesKey" ) );

            Preconditions.checkState( tmp.isEmpty(), String.format( "Xp7Deployment '%s' does not contain nodeGroups %s",
                                                                    xp7Deployments.get().getMetadata().getName(), nodeGroups ) );
        }
    }
}
