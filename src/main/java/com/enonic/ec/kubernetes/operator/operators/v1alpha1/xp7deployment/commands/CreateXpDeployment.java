package com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.commands;

import java.util.Map;
import java.util.function.Predicate;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarSource;
import io.fabric8.kubernetes.api.model.SecretKeySelector;
import io.fabric8.kubernetes.client.KubernetesClient;

import com.enonic.ec.kubernetes.operator.common.Configuration;
import com.enonic.ec.kubernetes.operator.common.commands.CombinedCommandBuilder;
import com.enonic.ec.kubernetes.operator.common.commands.ImmutableCombinedCommand;
import com.enonic.ec.kubernetes.operator.info.Diff;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7app.crd.client.Xp7AppClient;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7config.crd.client.Xp7ConfigClient;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.commands.config.ClusterConfigurator;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.commands.config.ImmutableClusterConfig;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.commands.config.ImmutableNonClusteredConfig;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.commands.volumes.ImmutableVolumeBuilderNfs;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.commands.volumes.ImmutableVolumeBuilderStd;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.commands.volumes.VolumeBuilder;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.crd.spec.Xp7DeploymentSpec;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.info.DiffXp7DeploymentSpec;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.info.DiffXp7DeploymentSpecNode;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.info.InfoXp7Deployment;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7vhost.crd.client.Xp7VHostClient;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@Value.Immutable
public abstract class CreateXpDeployment
    extends Configuration
    implements CombinedCommandBuilder
{
    protected abstract KubernetesClient defaultClient();

    protected abstract Xp7ConfigClient configClient();

    protected abstract Xp7VHostClient vHostClient();

    protected abstract Xp7AppClient appClient();

    protected abstract InfoXp7Deployment info();

    protected abstract Map<String, String> preInstallApps();

    @Value.Derived
    protected Xp7DeploymentSpec spec()
    {
        return info().resource().getSpec();
    }

    @Value.Derived
    protected DiffXp7DeploymentSpec diffSpec()
    {
        return info().diff().diffSpec();
    }

    @Override
    public void addCommands( ImmutableCombinedCommand.Builder commandBuilder )
    {
        // Setup volume configuration
        boolean useNfs = cfgBool( "operator.deployment.xp.volume.shared.nfs.enabled" );
        VolumeBuilder volumeBuilder;
        if ( useNfs )
        {
            volumeBuilder = ImmutableVolumeBuilderNfs.builder().
                info( info() ).
                nfsServer( cfgStr( "operator.deployment.xp.volume.shared.nfs.server" ) ).
                nfsPath( cfgStr( "operator.deployment.xp.volume.shared.nfs.path" ) ).
                build();
        }
        else
        {
            volumeBuilder = ImmutableVolumeBuilderStd.builder().
                info( info() ).
                build();
        }

        // Setup cluster configuration
        boolean isClustered = spec().isClustered();
        ClusterConfigurator clusterConfigurator;
        if ( isClustered )
        {
            clusterConfigurator = ImmutableClusterConfig.builder().
                client( configClient() ).
                info( info() ).
                build();
        }
        else
        {
            clusterConfigurator = ImmutableNonClusteredConfig.builder().
                client( configClient() ).
                info( info() ).
                build();
        }

        // Setup default su pass env var
        EnvVar suPass = setupSuPassword();

        // If this is a new deployment, deploy the base
        if ( diffSpec().isNew() )
        {
            ImmutableCreateXpDeploymentBase.builder().
                defaultClient( defaultClient() ).
                configClient( configClient() ).
                vHostClient( vHostClient() ).
                appClient( appClient() ).
                info( info() ).
                isClustered( isClustered ).
                suPassHash( suPass ).
                createSharedStorage( !useNfs ).
                sharedStorageSize( spec().nodesSharedDisk() ).
                preInstallApps( preInstallApps() ).
                build().
                addCommands( commandBuilder );
        }

        Predicate<DiffXp7DeploymentSpecNode> createOrUpdate =
            n -> diffSpec().versionChanged() || diffSpec().enabledChanged() || n.shouldAddOrModify();

        // Create / Update Nodes
        diffSpec().nodesChanged().stream().
            filter( createOrUpdate ).
            forEach( diffSpecNode -> ImmutableCreateXpDeploymentNode.builder().
                defaultClient( defaultClient() ).
                info( info() ).
                nodeId( getNodeId( diffSpecNode ) ).
                diffSpec( diffSpec() ).
                diffSpecNode( diffSpecNode ).
                suPassHash( suPass ).
                clusterConfigurator( clusterConfigurator ).
                volumeBuilder( volumeBuilder ).
                nodeSharedConfigChanged( diffSpec().nodeSharedConfigChanged() ).
                build().
                addCommands( commandBuilder ) );

        // Remove old nodes
        diffSpec().nodesChanged().stream().
            filter( Diff::shouldRemove ).
            forEach( diff -> ImmutableDeleteXpDeploymentNode.builder().
                defaultClient( defaultClient() ).
                info( info() ).
                nodeId( getNodeId( diff ) ).
                build().
                addCommands( commandBuilder ) );
    }

    private EnvVar setupSuPassword()
    {
        EnvVarSource suPassSource = new EnvVarSource();
        suPassSource.setSecretKeyRef( new SecretKeySelector( "suPassHash", info().suPassSecretName(), false ) );
        EnvVar suPassHash = new EnvVar();
        suPassHash.setName( "SU_PASS_HASH" );
        suPassHash.setValueFrom( suPassSource );
        return suPassHash;
    }

    private String getNodeId( final DiffXp7DeploymentSpecNode diffSpecNode )
    {
        return spec().nodes().entrySet().stream().
            filter( e -> e.getValue() ==
                diffSpecNode.newValue().get() ). // Note that this is not .equals for a reason. If it is not == then nodes with the exact same spec get mixed up
            map( e -> e.getKey() ).
            findFirst().
            get();
    }

}
