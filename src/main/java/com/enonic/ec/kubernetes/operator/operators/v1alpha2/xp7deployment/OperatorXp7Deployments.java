package com.enonic.ec.kubernetes.operator.operators.v1alpha2.xp7deployment;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.Watcher;
import io.quarkus.runtime.StartupEvent;

import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha2.deployment.V1alpha2Xp7Deployment;
import com.enonic.ec.kubernetes.operator.helm.BaseValues;
import com.enonic.ec.kubernetes.operator.helm.ChartRepository;
import com.enonic.ec.kubernetes.operator.helm.Helm;
import com.enonic.ec.kubernetes.operator.helm.commands.ImmutableHelmKubeCmdBuilder;
import com.enonic.ec.kubernetes.operator.kubectl.ImmutableKubeCmd;
import com.enonic.ec.kubernetes.operator.operators.common.OperatorNamespaced;
import com.enonic.ec.kubernetes.operator.operators.common.cache.Caches;
import com.enonic.ec.kubernetes.operator.operators.common.clients.Clients;
import com.enonic.ec.kubernetes.operator.operators.v1alpha2.xp7deployment.info.ImmutableInfoXp7Deployment;
import com.enonic.ec.kubernetes.operator.operators.v1alpha2.xp7deployment.info.InfoXp7Deployment;

@Singleton
public class OperatorXp7Deployments
    extends OperatorNamespaced
{
    @Inject
    Clients clients;

    @Inject
    Caches caches;

    @Inject
    Helm helm;

    @ConfigProperty(name = "operator.helm.imageTemplate")
    String imageTemplate;

    @Inject
    @Named("local")
    ChartRepository chartRepository;

    void onStartup( @Observes StartupEvent _ev )
    {
        caches.getDeploymentCache().addWatcher( this::watch );
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private void watch( final Watcher.Action action, final String id, final Optional<V1alpha2Xp7Deployment> oldResource,
                        final Optional<V1alpha2Xp7Deployment> newResource )
    {
        InfoXp7Deployment info = ImmutableInfoXp7Deployment.builder().
            oldResource( oldResource ).
            newResource( newResource ).
            build();

        runCommands( commandBuilder -> {
            if ( action == Watcher.Action.ADDED )
            {
                // Create namespace
                ImmutableKubeCmd.builder().
                    clients( clients ).
                    resource( createNamespace( info ) ).
                    build().
                    apply( commandBuilder );

                // Create su pass
                ImmutableKubeCmd.builder().
                    clients( clients ).
                    namespace( info.namespaceName() ).
                    resource( createSecret() ).
                    build().
                    apply( commandBuilder );
            }

            if ( action == Watcher.Action.ADDED || action == Watcher.Action.MODIFIED )
            {
                // Apply chart
                ImmutableHelmKubeCmdBuilder.builder().
                    clients( clients ).
                    helm( helm ).
                    chart( chartRepository.get( "v1alpha2/xp7deployment" ) ).
                    namespace( info.namespaceName() ).
                    valueBuilder( ImmutableXp7DeploymentValues.builder().
                        baseValues( new BaseValues() ).
                        imageTemplate( imageTemplate ).
                        info( info ).
                        build() ).
                    build().
                    addCommands( commandBuilder );
            }

            // Everything will be deleted on action == Watcher.Action.DELETED automatically
        } );
    }

    private Namespace createNamespace( InfoXp7Deployment info )
    {
        ObjectMeta metaData = new ObjectMeta();
        metaData.setName( info.namespaceName() );
        metaData.setOwnerReferences( Collections.singletonList( info.ownerReference() ) );
        Namespace namespace = new Namespace();
        namespace.setMetadata( metaData );
        return namespace;
    }

    @SuppressWarnings("UnstableApiUsage")
    private Secret createSecret()
    {
        String password = generateSuPassword();
        String passHash = Hashing.sha512().hashString( password, Charsets.UTF_8 ).toString();

        ObjectMeta metaData = new ObjectMeta();
        metaData.setName( "su" );
        Secret secret = new Secret();
        secret.setMetadata( metaData );
        secret.setData( Map.of( "pass", BaseEncoding.base64().encode( password.getBytes() ), "passHash", passHash ) );

        return secret;
    }

    private String generateSuPassword()
    {
        Random random = new Random();
        final byte[] buffer = new byte[20];
        random.nextBytes( buffer );
        return BaseEncoding.base64Url().omitPadding().encode( buffer );
    }
}
