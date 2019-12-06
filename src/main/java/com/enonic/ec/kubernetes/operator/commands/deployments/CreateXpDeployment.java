package com.enonic.ec.kubernetes.operator.commands.deployments;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarSource;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.SecretKeySelector;
import io.fabric8.kubernetes.client.KubernetesClient;

import com.enonic.ec.kubernetes.common.Configuration;
import com.enonic.ec.kubernetes.common.Diff;
import com.enonic.ec.kubernetes.common.commands.CombinedCommandBuilder;
import com.enonic.ec.kubernetes.common.commands.ImmutableCombinedCommand;
import com.enonic.ec.kubernetes.kubectl.apply.ImmutableCommandApplyXp7App;
import com.enonic.ec.kubernetes.kubectl.apply.ImmutableCommandApplyXp7VHost;
import com.enonic.ec.kubernetes.operator.commands.deployments.config.ClusterConfigurator;
import com.enonic.ec.kubernetes.operator.commands.deployments.config.ImmutableClusterConfig;
import com.enonic.ec.kubernetes.operator.commands.deployments.config.ImmutableNonClusteredConfig;
import com.enonic.ec.kubernetes.operator.commands.deployments.volumes.ImmutableVolumeBuilderNfs;
import com.enonic.ec.kubernetes.operator.commands.deployments.volumes.ImmutableVolumeBuilderStd;
import com.enonic.ec.kubernetes.operator.commands.deployments.volumes.VolumeBuilder;
import com.enonic.ec.kubernetes.operator.crd.app.client.XpAppClient;
import com.enonic.ec.kubernetes.operator.crd.app.spec.ImmutableSpec;
import com.enonic.ec.kubernetes.operator.crd.config.client.XpConfigClient;
import com.enonic.ec.kubernetes.operator.crd.deployment.XpDeploymentNamingHelper;
import com.enonic.ec.kubernetes.operator.crd.deployment.XpDeploymentResource;
import com.enonic.ec.kubernetes.operator.crd.deployment.diff.DiffSpec;
import com.enonic.ec.kubernetes.operator.crd.deployment.diff.DiffSpecNode;
import com.enonic.ec.kubernetes.operator.crd.deployment.spec.Spec;
import com.enonic.ec.kubernetes.operator.crd.deployment.spec.SpecNode;
import com.enonic.ec.kubernetes.operator.crd.vhost.client.XpVHostClient;
import com.enonic.ec.kubernetes.operator.crd.vhost.spec.ImmutableSpecMapping;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@Value.Immutable
public abstract class CreateXpDeployment
    extends Configuration
    implements CombinedCommandBuilder
{
    protected abstract KubernetesClient defaultClient();

    protected abstract XpConfigClient configClient();

    protected abstract XpVHostClient vHostClient();

    protected abstract XpAppClient appClient();

    protected abstract String deploymentName();

    protected abstract Map<String, String> defaultLabels();

    protected abstract XpDeploymentResource resource();

    protected abstract DiffSpec diffSpec();

    protected abstract OwnerReference ownerReference();

    protected abstract XpDeploymentNamingHelper namingHelper();

    protected abstract Map<String, String> preInstallApps();

    @Value.Derived
    protected Spec spec()
    {
        return diffSpec().newValue().get();
    }

    @Override
    public void addCommands( ImmutableCombinedCommand.Builder commandBuilder )
    {
        String deploymentName = deploymentName();
        String namespaceName = namingHelper().defaultNamespaceName();
        String serviceName = namespaceName;
        Map<String, String> defaultLabels = defaultLabels();

        boolean useNfs = cfgBool( "operator.deployment.xp.volume.shared.nfs.enabled" );

        VolumeBuilder volumeBuilder;
        Optional<String> defaultSharedStorageName;
        Optional<Quantity> defaultSharedStorageSize;

        if ( useNfs )
        {
            defaultSharedStorageName = Optional.empty();
            defaultSharedStorageSize = Optional.empty();
            volumeBuilder = ImmutableVolumeBuilderNfs.builder().
                deploymentName( deploymentName ).
                nfsServer( cfgStr( "operator.deployment.xp.volume.shared.nfs.server" ) ).
                nfsPath( cfgStr( "operator.deployment.xp.volume.shared.nfs.path" ) ).
                build();
        }
        else
        {
            defaultSharedStorageName = Optional.of( namingHelper().defaultResourceNameWithPreFix( "shared" ) );
            defaultSharedStorageSize = Optional.of( spec().nodesSharedDisk() );
            volumeBuilder = ImmutableVolumeBuilderStd.builder().
                deploymentName( deploymentName ).
                sharedStoragePVCName( defaultSharedStorageName.get() ).
                build();
        }

        boolean isClustered = spec().isClustered();

        Function<SpecNode, Integer> defaultMinimumAvailable;
        ClusterConfigurator clusterConfigurator;

        if ( isClustered )
        {
            defaultMinimumAvailable = ( n ) -> ( n.replicas() / 2 ) + 1;

            SpecNode masterNode = spec().nodes().values().stream().filter( SpecNode::isMasterNode ).findAny().get();
            int minimumMasterNodes = defaultMinimumAvailable.apply( masterNode );

            SpecNode dataNode = spec().nodes().values().stream().filter( SpecNode::isDataNode ).findAny().get();
            int minimumDataNodes = defaultMinimumAvailable.apply( dataNode );

            clusterConfigurator = ImmutableClusterConfig.builder().
                client( configClient() ).
                namespace( namespaceName ).
                serviceName( serviceName ).
                clusterName( deploymentName() ).
                minimumMasterNodes( Math.max( 1, minimumMasterNodes ) ).
                minimumDataNodes( Math.max( 1, minimumDataNodes ) ).
                // TODO: XP is not happy with this, do some research
                // discoveryHosts( Arrays.asList( String.join( ".", serviceName, namespaceName, "svc.cluster.local" ) ) ).
                    discoveryHosts( getAllMasterNodeDNS( serviceName ) ).
                    build();
        }
        else
        {
            defaultMinimumAvailable = ( n ) -> 0; // This has to be 0 to be able to update XP
            clusterConfigurator = ImmutableNonClusteredConfig.builder().
                client( configClient() ).
                namespace( namespaceName ).
                build();
        }

        // Setup default su pass env var
        EnvVarSource suPassSource = new EnvVarSource();
        suPassSource.setSecretKeyRef( new SecretKeySelector( "suPassHash", namingHelper().defaultResourceNameWithPreFix( "su" ), false ) );
        EnvVar suPassHash = new EnvVar();
        suPassHash.setName( "SU_PASS_HASH" );
        suPassHash.setValueFrom( suPassSource );

        if ( diffSpec().isNew() )
        {
            ImmutableCreateXpDeploymentBase.builder().
                defaultClient( defaultClient() ).
                configClient( configClient() ).
                ownerReference( ownerReference() ).
                namespace( namespaceName ).
                isClustered( isClustered ).
                discoveryServiceName( serviceName ).
                suPassHash( suPassHash ).
                sharedStorageName( defaultSharedStorageName ).
                sharedStorageSize( defaultSharedStorageSize ).
                defaultLabels( defaultLabels ).
                build().
                addCommands( commandBuilder );
            preInstallApps().forEach( ( name, uri ) -> commandBuilder.addCommand( ImmutableCommandApplyXp7App.builder().
                client( appClient() ).
                namespace( namespaceName ).
                canSkipOwnerReference( true ).
                name( name ).
                spec( ImmutableSpec.builder().uri( uri ).build() ).
                build() ) );
            String healthCheckHost = cfgStr( "operator.deployment.xp.probe.healthcheck.host" );
            String healthCheckPath = cfgStr( "operator.deployment.xp.probe.healthcheck.target" );
            commandBuilder.addCommand( ImmutableCommandApplyXp7VHost.builder().
                client( vHostClient() ).
                namespace( namespaceName ).
                canSkipOwnerReference( true ).
                name( healthCheckHost ).
                spec( com.enonic.ec.kubernetes.operator.crd.vhost.spec.ImmutableSpec.builder().
                    createIngress( false ).
                    host( healthCheckHost ).
                    addMappings( ImmutableSpecMapping.builder().
                        source( "/" ).
                        target( healthCheckPath ).
                        build() ).
                    build() ).
                build() );
        }

        Predicate<DiffSpecNode> createOrUpdate = n -> diffSpec().versionChanged() || diffSpec().enabledChanged() || n.shouldAddOrModify();

        // Create / Update Nodes
        diffSpec().nodesChanged().stream().
            filter( createOrUpdate ).
            forEach( diffSpecNode -> ImmutableCreateXpDeploymentNode.builder().
                defaultClient( defaultClient() ).
                ownerReference( ownerReference() ).
                namespace( namespaceName ).
                serviceName( serviceName ).
                nodeId( getNodeId( diffSpecNode ) ).
                nodeShortName( diffSpecNode.name() ).
                nodeFullName( namingHelper().defaultResourceName( diffSpecNode.name() ) ).
                diffSpec( diffSpec() ).
                diffSpecNode( diffSpecNode ).
                suPassHash( suPassHash ).
                defaultLabels( defaultLabels ).
                clusterConfigurator( clusterConfigurator ).
                deploymentName( deploymentName ).
                volumeBuilder( volumeBuilder ).
                minimumAvailable( defaultMinimumAvailable.apply( diffSpecNode.newValue().get() ) ).
                nodeSharedConfigChanged( diffSpec().nodeSharedConfigChanged() ).
                build().
                addCommands( commandBuilder ) );

        // Remove old nodes
        diffSpec().nodesChanged().stream().
            filter( Diff::shouldRemove ).
            forEach( diff -> ImmutableDeleteXpDeploymentNode.builder().
                defaultClient( defaultClient() ).
                namespace( namespaceName ).
                nodeId( getNodeId( diff ) ).
                build().
                addCommands( commandBuilder ) );
    }

    private String getNodeId( final DiffSpecNode diffSpecNode )
    {
        return resource().getSpec().nodes().entrySet().stream().
            filter( e -> e.getValue().equals( diffSpecNode.newValue().get() ) ).
            map( e -> e.getKey() ).
            findFirst().
            get();
    }

    private List<String> getAllMasterNodeDNS( String serviceName )
    {
        List<String> res = new LinkedList<>();
        for ( Map.Entry<String, SpecNode> node : spec().nodes().entrySet() )
        {
            if ( node.getValue().isMasterNode() )
            {
                for ( int i = 0; i < node.getValue().replicas(); i++ )
                {
                    res.add( node.getKey() + "-" + i + "." + serviceName );
                }
            }
        }
        return res;
    }
}
