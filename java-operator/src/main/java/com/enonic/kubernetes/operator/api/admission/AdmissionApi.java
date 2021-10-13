package com.enonic.kubernetes.operator.api.admission;

import com.enonic.kubernetes.client.v1alpha1.Xp7App;
import com.enonic.kubernetes.client.v1alpha2.Domain;
import com.enonic.kubernetes.client.v1alpha2.Xp7Config;
import com.enonic.kubernetes.client.v1alpha2.Xp7Deployment;
import com.enonic.kubernetes.client.v1alpha2.xp7deployment.Xp7DeploymentSpecNodeGroup;
import com.enonic.kubernetes.common.Validator;
import com.enonic.kubernetes.operator.api.AdmissionOperation;
import com.enonic.kubernetes.operator.api.BaseAdmissionApi;
import com.enonic.kubernetes.operator.ingress.Mapping;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Preconditions;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.admission.v1.AdmissionReview;
import io.fabric8.kubernetes.api.model.networking.v1.HTTPIngressPath;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import org.apache.maven.artifact.versioning.ComparableVersion;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.enonic.kubernetes.common.Configuration.cfgIfBool;
import static com.enonic.kubernetes.common.Configuration.cfgStr;
import static com.enonic.kubernetes.common.Validator.dns1123;
import static com.enonic.kubernetes.kubernetes.Predicates.fieldEquals;
import static com.enonic.kubernetes.kubernetes.Predicates.inNodeGroupAllOr;
import static com.enonic.kubernetes.kubernetes.Predicates.inSameNamespaceAs;
import static com.enonic.kubernetes.kubernetes.Predicates.isBeingBackupRestored;
import static com.enonic.kubernetes.kubernetes.Predicates.matchAnnotationPrefix;
import static com.enonic.kubernetes.kubernetes.Predicates.withName;
import static com.enonic.kubernetes.operator.ingress.OperatorXp7ConfigSync.getAnnotationMappings;

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
        AdmissionOperation op = getOperation( admissionReview );

        if (op == AdmissionOperation.DELETE) {
            return;
        }

        Ingress newIngress = (Ingress) admissionReview.getRequest().getObject();

        if (matchAnnotationPrefix( cfgStr( "operator.charts.values.annotationKeys.vhostMapping" ) ).negate().test( newIngress )) {
            return;
        }

        Set<Mapping> mappings = getAnnotationMappings( newIngress );
        Preconditions.checkArgument( !mappings.isEmpty(), "malformed 'enonic.cloud/xp7.vhost.mapping' annotations" );

        for (Mapping m : mappings) {
            List<String> paths = newIngress.getSpec().getRules().stream().
                map( r -> r.getHttp().getPaths().stream().
                    // TODO: Filter nodegroups
                        filter( p -> p.getBackend().getService().getPort().getNumber() == 8080 ).
                        map( HTTPIngressPath::getPath ).collect( Collectors.toList() ) ).
                flatMap( Collection::stream ).
                collect( Collectors.toList() );
            Preconditions.checkArgument( paths.contains( m.source() ), String.format(
                "source '%s' in 'enonic.cloud/xp7.vhost.mapping' annotation not defined in ingress rules on host %s, port 8080", m.source(),
                m.host() ) );
        }
    }

    private void xp7app( AdmissionReview admissionReview )
    {
        AdmissionOperation op = getOperation( admissionReview );

        if (op == AdmissionOperation.DELETE) {
            return;
        }

        Xp7App newApp = (Xp7App) admissionReview.getRequest().getObject();

        // Check spec
        Preconditions.checkState( newApp.getSpec() != null, "'spec' cannot be null" );
        Preconditions.checkState( newApp.getSpec().getUrl() != null, "'spec.url' cannot be null" );
        Preconditions.checkState( newApp.getSpec().getEnabled() != null, "'spec.enabled' cannot be null" );

        // Check status
        Preconditions.checkState( newApp.getStatus() != null, "'status' cannot be null" );
        Preconditions.checkState( newApp.getStatus().getMessage() != null, "'status.message' cannot be null" );
        Preconditions.checkState( newApp.getStatus().getState() != null, "'status.state' cannot be null" );
        Preconditions.checkState( newApp.getStatus().getXp7AppStatusFields() != null, "'status.fields' cannot be null" );

        if (isBeingBackupRestored().negate().
            and( h -> op == AdmissionOperation.CREATE ).
            test( newApp )) {
            assertXp7Deployment( admissionReview, null );
        }
    }

    private void xp7config( AdmissionReview admissionReview )
    {
        AdmissionOperation op = getOperation( admissionReview );

        if (op == AdmissionOperation.DELETE) {
            return;
        }

        Xp7Config newConfig = (Xp7Config) admissionReview.getRequest().getObject();

        // Check spec
        Preconditions.checkState( newConfig.getSpec() != null, "'spec' cannot be null" );
        Preconditions.checkState( newConfig.getSpec().getNodeGroup() != null, "'spec.nodeGroup' cannot be null" );
        Preconditions.checkState( newConfig.getSpec().getData() != null, "'spec.data' cannot be null" );
        Preconditions.checkState( newConfig.getSpec().getFile() != null, "'spec.file' cannot be null" );
        Preconditions.checkState( newConfig.getSpec().getDataBase64() != null, "'spec.dataBase64' cannot be null" );

        // Check status
        Preconditions.checkState( newConfig.getStatus() != null, "'status' cannot be null" );
        Preconditions.checkState( newConfig.getStatus().getMessage() != null, "'status.message' cannot be null" );
        Preconditions.checkState( newConfig.getStatus().getState() != null, "'status.state' cannot be null" );

        // Check for file clash
        List<Xp7Config> presentConfigs = searchers.xp7Config().stream().
            filter( inSameNamespaceAs( newConfig ) ).
            filter( withName( newConfig.getMetadata().getName() ).negate() ).
            filter( fieldEquals( newConfig, c -> c.getSpec().getFile() ) ).
            filter( inNodeGroupAllOr( newConfig.getSpec().getNodeGroup() ) ).
            collect( Collectors.toList() );

        if (!presentConfigs.isEmpty()) {
            Preconditions.checkState( false, "XpConfig '%s' already defines file '%s'", presentConfigs.get( 0 ).getMetadata().getName(),
                presentConfigs.get( 0 ).getSpec().getFile() );
        }

        // Check for present deployment
        if (isBeingBackupRestored().negate().
            and( h -> op == AdmissionOperation.CREATE ).
            test( newConfig )) {
            assertXp7Deployment( admissionReview, Collections.singleton( newConfig.getSpec().getNodeGroup() ) );
        }
    }

    private void xp7deployment( AdmissionReview admissionReview )
    {
        AdmissionOperation op = getOperation( admissionReview );

        Xp7Deployment newDeployment = (Xp7Deployment) admissionReview.getRequest().getObject();

        if (op != AdmissionOperation.DELETE) {
            // Check spec
            Preconditions.checkState( newDeployment.getSpec() != null, "'spec' cannot be null" );
            Preconditions.checkState( newDeployment.getSpec().getEnabled() != null, "'spec.enabled' cannot be null" );
            Preconditions.checkState( newDeployment.getSpec().getXpVersion() != null, "'spec.xpVersion' cannot be null" );
            Preconditions.checkState( newDeployment.getSpec().getXp7DeploymentSpecNodesSharedDisks() != null,
                "'spec.nodesSharedDisks' cannot be null" );
            Preconditions.checkState( newDeployment.getSpec().getXp7DeploymentSpecNodeGroups() != null,
                "'spec.nodeGroups' cannot be null" );

            // Check status
            Preconditions.checkState( newDeployment.getStatus() != null, "'status' cannot be null" );
            Preconditions.checkState( newDeployment.getStatus().getMessage() != null, "'status.message' cannot be null" );
            Preconditions.checkState( newDeployment.getStatus().getState() != null, "'status.state' cannot be null" );
            Preconditions.checkState( newDeployment.getStatus().getXp7DeploymentStatusFields() != null,
                "'status.fields' cannot be null" );
            Preconditions.checkState(
                newDeployment.getStatus().getXp7DeploymentStatusFields().getXp7DeploymentStatusFieldsPods() != null,
                "'status.fields.pods' cannot be null" );

            // Check node groups
            int nrOfMasterNodes = 0;
            int i = 0;
            for (Xp7DeploymentSpecNodeGroup ng : newDeployment.getSpec().getXp7DeploymentSpecNodeGroups()) {
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
                if (ng.getData()) {
                    Preconditions.checkState(
                        ng.getXp7DeploymentSpecNodeGroupResources().getXp7DeploymentSpecNodeGroupDisks().stream().anyMatch(
                            d -> d.getName().equals( "index" ) ), "nodes with data=true must have disk 'index' defined" );
                }

                if (ng.getMaster()) {
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

        if (op == AdmissionOperation.CREATE) {
            Optional<Xp7Deployment> xp7Deployments = getXp7Deployment( admissionReview.getRequest().getObject() );
            Preconditions.checkState( xp7Deployments.isEmpty(), "There is already an Xp7Deployment in NS '%s'",
                newDeployment.getMetadata().getNamespace() );

            // Assert version is > 7.7.X, if we cant parse version, just let it go
            ComparableVersion currentVersion = new ComparableVersion( "7.7.0" );
            try {
                if (newDeployment.getSpec().getXpVersion().startsWith( "7." )) {
                    currentVersion = new ComparableVersion( newDeployment.getSpec().getXpVersion() );
                } else if (newDeployment.getSpec().getXpVersion().startsWith( "enonic/xp:7." )) {
                    String pattern = "^enonic\\/xp:([0-9]+\\.[0-9]+\\.[0-9]+)";
                    Matcher m = Pattern.compile( pattern ).matcher( newDeployment.getSpec().getXpVersion() );
                    if (m.find()) {
                        currentVersion = new ComparableVersion( m.group( 1 ) );
                    }
                }
            } catch (Exception e) {
                // Just ignore
            }

            Preconditions.checkState( currentVersion.compareTo( new ComparableVersion( "7.6.100" ) ) > 0,
                "Operator only supports XP version 7.7 and higher" );
        }
    }


    private void domain( final AdmissionReview admissionReview )
    {
        AdmissionOperation op = getOperation( admissionReview );

        if (op == AdmissionOperation.DELETE) {
            return;
        }

        Domain newDomain = (Domain) admissionReview.getRequest().getObject();

        // Check spec
        Preconditions.checkState( newDomain.getSpec() != null, "'spec' cannot be null" );
        Preconditions.checkState( newDomain.getSpec().getHost() != null, "'spec.host' cannot be null" );
        Validator.dns1123( "spec.host", newDomain.getSpec().getHost() );
        Preconditions.checkState( newDomain.getSpec().getDnsRecord() != null, "'spec.dnsRecord' cannot be null" );

        if (newDomain.getSpec().getDnsRecord()) {
            Preconditions.checkState( newDomain.getSpec().getCdn() != null, "'spec.cdn' cannot be null if dnsRecord = true" );
        }

        if (newDomain.getSpec().getDomainSpecCertificate() != null) {
            Preconditions.checkState( newDomain.getSpec().getDomainSpecCertificate().getAuthority() != null,
                "'spec.certificate.authority' cannot be null" );
            switch (newDomain.getSpec().getDomainSpecCertificate().getAuthority()) {
                case CUSTOM:
                    Preconditions.checkState( newDomain.getSpec().getDomainSpecCertificate().getIdentifier() != null,
                        "'spec.certificate.identifier' cannot be null when authority is CUSTOM" );
                case CLUSTER_ISSUER:
                    Preconditions.checkState( newDomain.getSpec().getDomainSpecCertificate().getIdentifier() != null,
                        "'spec.certificate.identifier' cannot be null when authority is CLUSTER_ISSUER" );
            }
        }

        // Check status
        Preconditions.checkState( newDomain.getStatus() != null, "'status' cannot be null" );
        Preconditions.checkState( newDomain.getStatus().getMessage() != null, "'status.message' cannot be null" );
        Preconditions.checkState( newDomain.getStatus().getState() != null, "'status.state' cannot be null" );
        Preconditions.checkState( newDomain.getStatus().getDomainStatusFields() != null, "'status.fields' cannot be null" );
        Preconditions.checkState( newDomain.getStatus().getDomainStatusFields().getDnsRecordCreated() != null,
            "'status.fields.dnsRecordCreated' cannot be null" );
        Preconditions.checkState( newDomain.getStatus().getDomainStatusFields().getPublicIps() != null,
            "'status.fields.publicIps' cannot be null" );

        if (op == AdmissionOperation.UPDATE) {
            Domain oldDomain = (Domain) admissionReview.getRequest().getOldObject();
            Preconditions.checkState( oldDomain.getSpec().getHost().equals( newDomain.getSpec().getHost() ),
                "'spec.host' cannot be changed" );
        }
    }

    private void assertXp7Deployment( AdmissionReview admissionReview, Set<String> nodeGroups )
    {
        Optional<Xp7Deployment> xp7Deployments = getXp7Deployment( admissionReview.getRequest().getObject() );
        Preconditions.checkState( xp7Deployments.isPresent(), "No Xp7Deployment found in NS '%s'",
            ((HasMetadata) admissionReview.getRequest().getObject()).getMetadata().getNamespace() );
        if (nodeGroups != null) {
            Set<String> xpDeploymentNodeGroups = xp7Deployments.get().
                getSpec().
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
