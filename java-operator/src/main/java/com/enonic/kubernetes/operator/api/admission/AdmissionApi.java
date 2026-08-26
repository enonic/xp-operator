package com.enonic.kubernetes.operator.api.admission;

import com.enonic.kubernetes.crd.v1.Xp8Config;
import com.enonic.kubernetes.crd.v1.Xp8Deployment;
import com.enonic.kubernetes.crd.v1.xp8deploymentspec.NodeGroups;
import com.enonic.kubernetes.crd.v1.xp8deploymentspec.nodegroups.Sidecars;
import com.enonic.kubernetes.operator.api.AdmissionOperation;
import com.enonic.kubernetes.operator.api.BaseAdmissionApi;
import com.enonic.kubernetes.operator.ingress.Mapping;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Preconditions;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.api.model.admission.v1.AdmissionReview;
import io.fabric8.kubernetes.api.model.networking.v1.HTTPIngressPath;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;


import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;

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
import static com.enonic.kubernetes.kubernetes.Predicates.matchAnnotationPrefix;
import static com.enonic.kubernetes.kubernetes.Predicates.withName;
import static com.enonic.kubernetes.operator.ingress.OperatorXp8ConfigSync.getAnnotationMappings;

@Controller("/apis/operator.enonic.cloud/v1")
public class AdmissionApi
    extends BaseAdmissionApi<AdmissionReview>
{
    public AdmissionApi()
    {
        super();
        addFunction( Xp8Config.class, this::xp8config );
        addFunction( Xp8Deployment.class, this::xp8deployment );
        addFunction( Ingress.class, this::ingress );
    }

    @Post("/validations")
    @Consumes("application/json")
    @Produces("application/json")
    public AdmissionReview validate( @Body AdmissionReview admissionReview )
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
        final AdmissionOperation op = getOperation( admissionReview );

        if ( op == AdmissionOperation.DELETE )
        {
            return;
        }

        final Ingress newIngress = (Ingress) admissionReview.getRequest().getObject();

        if ( matchAnnotationPrefix( cfgStr( "operator.charts.values.annotationKeys.vhostMapping" ) ).negate().test( newIngress ) )
        {
            return;
        }

        final Set<Mapping> mappings = getAnnotationMappings( newIngress );
        Preconditions.checkArgument( !mappings.isEmpty(), "malformed 'enonic.cloud/xp8.vhost.mapping' annotations" );

        for ( Mapping m : mappings )
        {
            List<String> paths = newIngress.getSpec().getRules().stream().map( r -> r.getHttp()
                .getPaths()
                .stream().
                // TODO: Filter nodegroups
                    filter( p -> p.getBackend().getService().getPort().getNumber() == 8080 )
                .map( HTTPIngressPath::getPath )
                .collect( Collectors.toList() ) ).flatMap( Collection::stream ).collect( Collectors.toList() );
            Preconditions.checkArgument( paths.contains( m.source() ), String.format(
                "source '%s' in 'enonic.cloud/xp8.vhost.mapping' annotation not defined in ingress rules on host %s, port 8080", m.source(),
                m.host() ) );
        }
    }

    private void xp8config( AdmissionReview admissionReview )
    {
        final AdmissionOperation op = getOperation( admissionReview );

        if ( op == AdmissionOperation.DELETE )
        {
            return;
        }

        final Xp8Config newConfig = (Xp8Config) admissionReview.getRequest().getObject();

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
        final List<Xp8Config> presentConfigs = searchers.xp8Config()
            .stream()
            .filter( inSameNamespaceAs( newConfig ) )
            .filter( withName( newConfig.getMetadata().getName() ).negate() )
            .filter( fieldEquals( newConfig, c -> c.getSpec().getFile() ) )
            .filter( inNodeGroupAllOr( newConfig.getSpec().getNodeGroup() ) )
            .collect( Collectors.toList() );

        if ( !presentConfigs.isEmpty() )
        {
            Preconditions.checkState( false, "XpConfig '%s' already defines file '%s'", presentConfigs.get( 0 ).getMetadata().getName(),
                                      presentConfigs.get( 0 ).getSpec().getFile() );
        }

        // Check for present deployment
        if ( op == AdmissionOperation.CREATE )
        {
            assertXp8Deployment( admissionReview, Collections.singleton( newConfig.getSpec().getNodeGroup() ) );
        }
    }

    private void xp8deployment( AdmissionReview admissionReview )
    {
        final AdmissionOperation op = getOperation( admissionReview );

        final Xp8Deployment newDeployment = (Xp8Deployment) admissionReview.getRequest().getObject();

        if ( op != AdmissionOperation.DELETE )
        {
            // Check spec
            Preconditions.checkState( newDeployment.getSpec() != null, "'spec' cannot be null" );
            Preconditions.checkState( newDeployment.getSpec().getEnabled() != null, "'spec.enabled' cannot be null" );
            Preconditions.checkState( newDeployment.getSpec().getXpVersion() != null, "'spec.xpVersion' cannot be null" );
            Preconditions.checkState( newDeployment.getSpec().getNodesSharedDisks() != null,
                                      "'spec.nodesSharedDisks' cannot be null" );
            Preconditions.checkState( newDeployment.getSpec().getNodeGroups() != null,
                                      "'spec.nodeGroups' cannot be null" );

            Preconditions.checkState( newDeployment.getSpec().getNodeGroups() != null,
                                      "'spec.nodeGroups' cannot be null" );

            // Check status
            Preconditions.checkState( newDeployment.getStatus() != null, "'status' cannot be null" );
            Preconditions.checkState( newDeployment.getStatus().getMessage() != null, "'status.message' cannot be null" );
            Preconditions.checkState( newDeployment.getStatus().getState() != null, "'status.state' cannot be null" );
            Preconditions.checkState( newDeployment.getStatus().getFields() != null, "'status.fields' cannot be null" );
            Preconditions.checkState( newDeployment.getStatus().getFields().getPods() != null,
                                      "'status.fields.pods' cannot be null" );

            // Check node groups
            int nrOfMasterNodes = 0;
            final List<NodeGroups> xp8DeploymentSpecNodeGroups = newDeployment.getSpec().getNodeGroups();
            for ( int i = 0; i < xp8DeploymentSpecNodeGroups.size(); i++ )
            {
                final NodeGroups ng = xp8DeploymentSpecNodeGroups.get( i );

                Preconditions.checkState( ng.getName() != null, "'spec.nodeGroups[" + i + "].name' cannot be null" );
                Preconditions.checkState( !ng.getName().equals( cfgStr( "operator.charts.values.allNodesKey" ) ),
                                          "'spec.nodeGroups[" + i + "].name' cannot be " + cfgStr( "operator.charts.values.allNodesKey" ) );
                dns1123( "spec.nodeGroups[" + i + "].name", ng.getName() );
                Preconditions.checkState( ng.getData() != null, "'spec.nodeGroups[" + i + "].data' cannot be null" );
                Preconditions.checkState( ng.getMaster() != null, "'spec.nodeGroups[" + i + "].master' cannot be null" );
                Preconditions.checkState( ng.getReplicas() != null, "'spec.nodeGroups[" + i + "].replicas' cannot be null" );
                Preconditions.checkState( ng.getReplicas() >= 0, "'spec.nodeGroups[" + i + "].replicas' has to be >= 0" );
                Preconditions.checkState( ng.getEnv() != null,
                                          "'spec.nodeGroups[" + i + "].env' cannot be null" );
                Preconditions.checkState( ng.getResources() != null,
                                          "'spec.nodeGroups[" + i + "].resources' cannot be null" );
                Preconditions.checkState( ng.getResources().getCpu() != null,
                                          "'spec.nodeGroups[" + i + "].resources.cpu' cannot be null" );
                Preconditions.checkState( ng.getResources().getMemory() != null,
                                          "'spec.nodeGroups[" + i + "].resources.memory' cannot be null" );
                Preconditions.checkState( ng.getResources().getMemory().contains( "Mi" ) ||
                                              ng.getResources().getMemory().contains( "Gi" ),
                                          "'spec.nodeGroups[" + i + "].resources.memory' can only be defined with Gi or Mi" );
                Preconditions.checkState( ng.getResources().getDisks() != null,
                                          "'spec.nodeGroups[" + i + "].resources.disks' cannot be null" );

                // Check disks
                if ( ng.getData() )
                {
                    Preconditions.checkState( ng.getResources()
                                                  .getDisks()
                                                  .stream()
                                                  .anyMatch( d -> d.getName().equals( "index" ) ),
                                              "nodes with data=true must have disk 'index' defined" );
                }

                if ( ng.getMaster() )
                {
                    nrOfMasterNodes += ng.getReplicas();
                }

                //check sidecars
                Preconditions.checkState( ng.getSidecars() != null,
                                          "'spec.nodeGroups[" + i + "].sidecars' cannot be null" );

                final List<Sidecars> xp8DeploymentSpecNodeGroupSidecars =
                    ng.getSidecars();

                for ( int j = 0; j < xp8DeploymentSpecNodeGroupSidecars.size(); j++ )
                {
                    final Sidecars sidecar = xp8DeploymentSpecNodeGroupSidecars.get( j );

                    Preconditions.checkState( sidecar.getName() != null,
                                              "'spec.nodeGroups[" + i + "].sidecars[" + j + "].name' cannot be null" );
                    Preconditions.checkState( sidecar.getImage() != null,
                                              "'spec.nodeGroups[" + i + "].sidecars[" + j + "].image' cannot be null" );
                }
            }

            // Check replicas
            Preconditions.checkState( nrOfMasterNodes > 0, "some nodeGroups must have master=true" );
            Preconditions.checkState( nrOfMasterNodes % 2 == 1, "number of master nodes has to be an odd number" );
        }

        // Strict label and name validation
        cfgIfBool( "operator.deployment.xp.labels.strictValidation", () -> {
            Preconditions.checkState( newDeployment.getMetadata() != null, "'metadata' cannot be null" );
            Preconditions.checkState( newDeployment.getMetadata().getLabels() != null, "'metadata.labels' cannot be null" );

            final String cloud = newDeployment.getMetadata().getLabels().get( cfgStr( "operator.deployment.xp.labels.cloud" ) );
            final String solution = newDeployment.getMetadata().getLabels().get( cfgStr( "operator.deployment.xp.labels.solution" ) );
            final String environment = newDeployment.getMetadata().getLabels().get( cfgStr( "operator.deployment.xp.labels.environment" ) );
            final String service = newDeployment.getMetadata().getLabels().get( cfgStr( "operator.deployment.xp.labels.service" ) );

            Preconditions.checkState( cloud != null, String.format( "'metadata.labels.%s' cannot be null",
                                                                    cfgStr( "operator.deployment.xp.labels.cloud" ) ) );
            Preconditions.checkState( solution != null, String.format( "'metadata.labels.%s' cannot be null",
                                                                       cfgStr( "operator.deployment.xp.labels.solution" ) ) );
            Preconditions.checkState( environment != null, String.format( "'metadata.labels.%s' cannot be null",
                                                                          cfgStr( "operator.deployment.xp.labels.environment" ) ) );
            Preconditions.checkState( service != null, String.format( "'metadata.labels.%s' cannot be null",
                                                                      cfgStr( "operator.deployment.xp.labels.service" ) ) );
        } );

        if ( op == AdmissionOperation.CREATE )
        {
            final Optional<Xp8Deployment> xp8Deployments = getXp8Deployment( (KubernetesResource) admissionReview.getRequest().getObject() );
            Preconditions.checkState( xp8Deployments.isEmpty(), "There is already an Xp8Deployment in NS '%s'",
                                      newDeployment.getMetadata().getNamespace() );

            String xpVersion = newDeployment.getSpec().getXpVersion();
            Matcher m = Pattern.compile( "^(?:enonic/xp:)?([0-9]+\\.[0-9]+\\.[0-9]+)" ).matcher( xpVersion );
            if ( m.find() )
            {
                Preconditions.checkState( m.group( 1 ).startsWith( "8." ), "Operator only supports XP version 8.x" );
            }
        }
    }

    private void assertXp8Deployment( AdmissionReview admissionReview, Set<String> nodeGroups )
    {
        final Optional<Xp8Deployment> xp8Deployments = getXp8Deployment( (KubernetesResource) admissionReview.getRequest().getObject() );
        Preconditions.checkState( xp8Deployments.isPresent(), "No Xp8Deployment found in NS '%s'",
                                  ( (HasMetadata) admissionReview.getRequest().getObject() ).getMetadata().getNamespace() );
        if ( nodeGroups != null )
        {
            final Set<String> xpDeploymentNodeGroups = xp8Deployments.get()
                .getSpec()
                .getNodeGroups()
                .stream()
                .map( NodeGroups::getName )
                .collect( Collectors.toSet() );
            final Set<String> tmp = new HashSet<>( nodeGroups );
            tmp.removeAll( xpDeploymentNodeGroups );
            tmp.remove( cfgStr( "operator.charts.values.allNodesKey" ) );

            Preconditions.checkState( tmp.isEmpty(), String.format( "Xp8Deployment '%s' does not contain nodeGroups %s",
                                                                    xp8Deployments.get().getMetadata().getName(), nodeGroups ) );
        }
    }
}
