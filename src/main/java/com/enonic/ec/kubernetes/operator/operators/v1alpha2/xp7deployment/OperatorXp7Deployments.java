package com.enonic.ec.kubernetes.operator.operators.v1alpha2.xp7deployment;

import java.util.Map;
import java.util.Optional;
import java.util.Random;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;

import io.fabric8.kubernetes.client.Watcher;
import io.quarkus.runtime.StartupEvent;

import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha2.deployment.V1alpha2Xp7Deployment;
import com.enonic.ec.kubernetes.operator.helm.ChartRepository;
import com.enonic.ec.kubernetes.operator.helm.Helm;
import com.enonic.ec.kubernetes.operator.helm.commands.ImmutableHelmInstall;
import com.enonic.ec.kubernetes.operator.helm.commands.ImmutableHelmUninstall;
import com.enonic.ec.kubernetes.operator.helm.commands.ImmutableHelmUpgrade;
import com.enonic.ec.kubernetes.operator.kubectl.apply.ImmutableCommandApplyNamespace;
import com.enonic.ec.kubernetes.operator.kubectl.apply.ImmutableCommandApplySecret;
import com.enonic.ec.kubernetes.operator.operators.OperatorNamespaced;
import com.enonic.ec.kubernetes.operator.operators.cache.Caches;
import com.enonic.ec.kubernetes.operator.operators.clients.Clients;
import com.enonic.ec.kubernetes.operator.operators.v1alpha2.xp7deployment.info.ImmutableInfoXp7Deployment;
import com.enonic.ec.kubernetes.operator.operators.v1alpha2.xp7deployment.info.InfoXp7Deployment;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@ApplicationScoped
public class OperatorXp7Deployments
    extends OperatorNamespaced
{
    @Inject
    Clients clients;

    @Inject
    Caches caches;

    @Inject
    Helm helm;

    @ConfigProperty(name = "operator.helm.charts.path")
    String helmChartsPath;

    @ConfigProperty(name = "operator.helm.imageTemplate")
    String imageTemplate;

    @Inject
    @Named("local")
    ChartRepository chartRepository;

    void onStartup( @Observes StartupEvent _ev )
    {
        // TODO: Enable
        //xp7DeploymentCache.addWatcher( this::watch );
    }

    private void watch( final Watcher.Action action, final String id, final Optional<V1alpha2Xp7Deployment> oldResource,
                        final Optional<V1alpha2Xp7Deployment> newResource )
    {
        InfoXp7Deployment info = ImmutableInfoXp7Deployment.builder().
            oldResource( oldResource ).
            newResource( newResource ).
            build();

        String name = info.deploymentName();
        String namespace = info.namespaceName();

        runCommands( commandBuilder -> {
            if ( action == Watcher.Action.ADDED )
            {
                // Create namespace
                commandBuilder.addCommand( ImmutableCommandApplyNamespace.builder().
                    clients( clients ).
                    name( namespace ).
                    ownerReference( info.ownerReference() ).
                    build() );

                // Create su pass
                String password = generateSuPassword();
                String passHash = Hashing.sha512().hashString( password, Charsets.UTF_8 ).toString();
                commandBuilder.addCommand( ImmutableCommandApplySecret.builder().
                    clients( clients ).
                    ownerReference( info.ownerReference() ).
                    namespace( namespace ).
                    name( "su" ).
                    data( Map.of( "pass", BaseEncoding.base64().encode( password.getBytes() ), "passHash", passHash ) ).
                    build() );

                // Create deployment
                commandBuilder.addCommand( ImmutableHelmInstall.builder().
                    helm( helm ).
                    chart( chartRepository.get( "v1alpha1/test" ) ).
                    namespace( namespace ).
                    name( name ).
                    values( createValues( info ) ).
                    build() );
            }

            if ( action == Watcher.Action.ADDED || action == Watcher.Action.MODIFIED )
            {
                // Upgrade deployment
                commandBuilder.addCommand( ImmutableHelmUpgrade.builder().
                    helm( helm ).
                    chart( chartRepository.get( "v1alpha1/test" ) ).
                    namespace( namespace ).
                    name( name ).
                    values( createValues( info ) ).
                    build() );
            }

            if ( action == Watcher.Action.DELETED )
            {
                // Namespace + Secret will be deleted automatically

                // Delete deployment
                commandBuilder.addCommand( ImmutableHelmUninstall.builder().
                    helm( helm ).
                    namespace( namespace ).
                    name( name ).
                    build() );
            }
        } );
    }

    protected Object createValues( final InfoXp7Deployment info )
    {
        return ImmutableXp7DeploymentValues.builder().
            info( info ).
            imageTemplate( imageTemplate ).
            build().
            values();
    }

    private String generateSuPassword()
    {
        Random random = new Random();
        final byte[] buffer = new byte[20];
        random.nextBytes( buffer );
        return BaseEncoding.base64Url().omitPadding().encode( buffer );
    }
}
