package com.enonic.ec.kubernetes.operator.commands.deployments;

import java.util.Map;
import java.util.Optional;
import java.util.Random;

import org.immutables.value.Value;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.client.KubernetesClient;

import com.enonic.ec.kubernetes.common.Configuration;
import com.enonic.ec.kubernetes.common.commands.CombinedCommandBuilder;
import com.enonic.ec.kubernetes.common.commands.ImmutableCombinedCommand;
import com.enonic.ec.kubernetes.kubectl.apply.ImmutableCommandApplyNamespace;
import com.enonic.ec.kubernetes.kubectl.apply.ImmutableCommandApplyPvc;
import com.enonic.ec.kubernetes.kubectl.apply.ImmutableCommandApplySecret;
import com.enonic.ec.kubernetes.kubectl.apply.ImmutableCommandApplyService;
import com.enonic.ec.kubernetes.kubectl.apply.ImmutableCommandApplyXp7Config;
import com.enonic.ec.kubernetes.operator.commands.deployments.spec.ImmutablePvcSpecBuilder;
import com.enonic.ec.kubernetes.operator.commands.deployments.spec.ImmutableServiceSpecBuilder;
import com.enonic.ec.kubernetes.operator.crd.config.client.XpConfigClient;
import com.enonic.ec.kubernetes.operator.crd.config.spec.ImmutableSpec;

@Value.Immutable
public abstract class CreateXpDeploymentBase
    extends Configuration
    implements CombinedCommandBuilder
{
    protected abstract KubernetesClient defaultClient();

    protected abstract XpConfigClient configClient();

    protected abstract OwnerReference ownerReference();

    protected abstract String namespace();

    protected abstract String discoveryServiceName();

    protected abstract EnvVar suPassHash();

    protected abstract Map<String, String> defaultLabels();

    protected abstract Optional<String> sharedStorageName();

    protected abstract Optional<Quantity> sharedStorageSize();

    protected abstract boolean isClustered();

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
            ownerReference( ownerReference() ).
            name( namespace() ).
            build() );

        // Create su pass
        String pass = generateSuPassword();
        String passHash = Hashing.sha512().hashString( pass, Charsets.UTF_8 ).toString();

        String passBase64 = BaseEncoding.base64().encode( pass.getBytes() );
        String passHashBase64 = BaseEncoding.base64().encode( passHash.getBytes() );

        commandBuilder.addCommand( ImmutableCommandApplySecret.builder().
            client( defaultClient() ).
            ownerReference( ownerReference() ).
            namespace( namespace() ).
            name( suPassHash().getValueFrom().getSecretKeyRef().getName() ).
            data( Map.of( "suPass", passBase64, suPassHash().getValueFrom().getSecretKeyRef().getKey(), passHashBase64 ) ).
            build() );

        commandBuilder.addCommand( ImmutableCommandApplyXp7Config.builder().
            client( configClient() ).
            ownerReference( ownerReference() ).
            namespace( namespace() ).
            name( "system" ).
            spec( ImmutableSpec.builder().
                file( "system.properties" ).
                data( new StringBuilder().
                    append( "xp.suPassword = {sha512}${env." ).append( suPassHash().getName() ).append( "}" ).append( "\n" ).
                    append( "xp.init.adminUserCreation = false" ).
                    toString() ).
                build() ).
            build() );

        if ( sharedStorageName().isPresent() )
        {
            commandBuilder.addCommand( ImmutableCommandApplyPvc.builder().
                client( defaultClient() ).
                ownerReference( ownerReference() ).
                namespace( namespace() ).
                name( sharedStorageName().get() ).
                labels( defaultLabels() ).
                spec( ImmutablePvcSpecBuilder.builder().
                    size( sharedStorageSize().get() ).
                    addAccessMode( isClustered() ? "ReadWriteMany" : "ReadWriteOnce" ). // TODO: This only works on minikube
                    build().
                    spec() ).
                build() );
        }

        // Create es discovery service
        commandBuilder.addCommand( ImmutableCommandApplyService.builder().
            client( defaultClient() ).
            ownerReference( ownerReference() ).
            namespace( namespace() ).
            name( discoveryServiceName() ).
            labels( defaultLabels() ).
            spec( ImmutableServiceSpecBuilder.builder().
                selector( defaultLabels() ).
                putPorts( cfgStr( "operator.deployment.xp.port.es.discovery.name" ),
                          cfgInt( "operator.deployment.xp.port.es.discovery.number" ) ).
                publishNotReadyAddresses( true ).
                build().
                spec() ).
            build() );

        // TODO: Create network policy
    }
}
