package com.enonic.ec.kubernetes.operator.commands.deployments;

import java.util.Map;
import java.util.Optional;
import java.util.Random;

import org.immutables.value.Value;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.client.KubernetesClient;

import com.enonic.ec.kubernetes.common.Configuration;
import com.enonic.ec.kubernetes.common.commands.CombinedCommandBuilder;
import com.enonic.ec.kubernetes.common.commands.ImmutableCombinedCommand;
import com.enonic.ec.kubernetes.kubectl.apply.ImmutableCommandApplyNamespace;
import com.enonic.ec.kubernetes.kubectl.apply.ImmutableCommandApplyPvc;
import com.enonic.ec.kubernetes.kubectl.apply.ImmutableCommandApplySecret;
import com.enonic.ec.kubernetes.kubectl.apply.ImmutableCommandApplyService;
import com.enonic.ec.kubernetes.kubectl.apply.ImmutableCommandApplyXp7App;
import com.enonic.ec.kubernetes.kubectl.apply.ImmutableCommandApplyXp7Config;
import com.enonic.ec.kubernetes.kubectl.apply.ImmutableCommandApplyXp7VHost;
import com.enonic.ec.kubernetes.operator.commands.deployments.spec.ImmutablePvcSpecBuilder;
import com.enonic.ec.kubernetes.operator.commands.deployments.spec.ImmutableServiceSpecBuilder;
import com.enonic.ec.kubernetes.operator.info.xp7deployment.InfoXp7Deployment;
import com.enonic.ec.kubernetes.operator.crd.xp7app.client.Xp7AppClient;
import com.enonic.ec.kubernetes.operator.crd.xp7app.spec.ImmutableXp7AppSpec;
import com.enonic.ec.kubernetes.operator.crd.xp7config.client.Xp7ConfigClient;
import com.enonic.ec.kubernetes.operator.crd.xp7config.spec.ImmutableXp7ConfigSpec;
import com.enonic.ec.kubernetes.operator.crd.xp7vhost.client.Xp7VHostClient;
import com.enonic.ec.kubernetes.operator.crd.xp7vhost.spec.ImmutableXp7VHostSpec;
import com.enonic.ec.kubernetes.operator.crd.xp7vhost.spec.ImmutableXp7VHostSpecMapping;

@Value.Immutable
public abstract class CreateXpDeploymentBase
    extends Configuration
    implements CombinedCommandBuilder
{
    protected abstract KubernetesClient defaultClient();

    protected abstract Xp7ConfigClient configClient();

    protected abstract Xp7VHostClient vHostClient();

    protected abstract Xp7AppClient appClient();

    protected abstract InfoXp7Deployment info();

    protected abstract EnvVar suPassHash();

    protected abstract boolean createSharedStorage();

    protected abstract Optional<Quantity> sharedStorageSize();

    protected abstract boolean isClustered();

    protected abstract Map<String, String> preInstallApps();

    private String generateSuPassword()
    {
        Random random = new Random();
        final byte[] buffer = new byte[20];
        random.nextBytes( buffer );
        return BaseEncoding.base64Url().omitPadding().encode( buffer );
    }

    @Override
    public void addCommands( ImmutableCombinedCommand.Builder commandBuilder )
    {
        // Create namespace
        commandBuilder.addCommand( ImmutableCommandApplyNamespace.builder().
            client( defaultClient() ).
            ownerReference( info().ownerReference() ).
            name( info().namespaceName() ).
            build() );

        // Create su pass
        String pass = generateSuPassword();
        String passHash = Hashing.sha512().hashString( pass, Charsets.UTF_8 ).toString();

        String passBase64 = BaseEncoding.base64().encode( pass.getBytes() );
        String passHashBase64 = BaseEncoding.base64().encode( passHash.getBytes() );

        // Create secret with password
        commandBuilder.addCommand( ImmutableCommandApplySecret.builder().
            client( defaultClient() ).
            ownerReference( info().ownerReference() ).
            namespace( info().namespaceName() ).
            name( suPassHash().getValueFrom().getSecretKeyRef().getName() ).
            data( Map.of( "suPass", passBase64, suPassHash().getValueFrom().getSecretKeyRef().getKey(), passHashBase64 ) ).
            build() );

        // Create system config
        commandBuilder.addCommand( ImmutableCommandApplyXp7Config.builder().
            client( configClient() ).
            ownerReference( info().ownerReference() ).
            namespace( info().namespaceName() ).
            name( cfgStrFmt( "operator.config.xp.system.name", cfgStr( "operator.deployment.xp.allNodes" ) ) ).
            spec( ImmutableXp7ConfigSpec.builder().
                node( cfgStr( "operator.deployment.xp.allNodes" ) ).
                file( cfgStr( "operator.config.xp.system.file" ) ).
                data( new StringBuilder().
                    append( "xp.suPassword = {sha512}${env." ).append( suPassHash().getName() ).append( "}" ).append( "\n" ).
                    append( "xp.init.adminUserCreation = false" ).
                    toString() ).
                build() ).
            build() );

        if ( createSharedStorage() )
        {
            // Create PVC
            commandBuilder.addCommand( ImmutableCommandApplyPvc.builder().
                client( defaultClient() ).
                ownerReference( info().ownerReference() ).
                namespace( info().namespaceName() ).
                name( info().sharedStorageName() ).
                labels( info().defaultLabels() ).
                spec( ImmutablePvcSpecBuilder.builder().
                    size( sharedStorageSize().get() ).
                    addAccessMode( cfgStr( "operator.deplyoment.xp.volume.shared.accessMode" ) ).
                    build().
                    spec() ).
                build() );
        }

        // Create service pointing to all nodes
        commandBuilder.addCommand( ImmutableCommandApplyService.builder().
            client( defaultClient() ).
            ownerReference( info().ownerReference() ).
            namespace( info().namespaceName() ).
            name( info().allNodesServiceName() ).
            labels( info().defaultLabels() ).
            spec( ImmutableServiceSpecBuilder.builder().
                selector( info().defaultLabels() ).
                putPorts( cfgStr( "operator.deployment.xp.port.es.discovery.name" ),
                          cfgInt( "operator.deployment.xp.port.es.discovery.number" ) ).
                publishNotReadyAddresses( true ).
                build().
                spec() ).
            build() );

        // Pre Install Apps
        preInstallApps().forEach( ( name, uri ) -> commandBuilder.addCommand( ImmutableCommandApplyXp7App.builder().
            client( appClient() ).
            namespace( info().namespaceName() ).
            canSkipOwnerReference( true ).
            name( name ).
            spec( ImmutableXp7AppSpec.builder().
                uri( uri ).
                build() ).
            build() ) );

        // Setup vHost for node health checks
        String healthCheckHost = cfgStr( "operator.deployment.xp.probe.healthcheck.host" );
        String healthCheckPath = cfgStr( "operator.deployment.xp.probe.healthcheck.target" );
        commandBuilder.addCommand( ImmutableCommandApplyXp7VHost.builder().
            client( vHostClient() ).
            namespace( info().namespaceName() ).
            canSkipOwnerReference( true ).
            name( healthCheckHost ).
            spec( ImmutableXp7VHostSpec.builder().
                skipIngress( true ).
                host( healthCheckHost ).
                addMappings( ImmutableXp7VHostSpecMapping.builder().
                    node( cfgStr( "operator.deployment.xp.allNodes" ) ).
                    source( "/" ).
                    target( healthCheckPath ).
                    build() ).
                build() ).
            build() );

        // TODO: Create network policy
    }
}
