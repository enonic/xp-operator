package com.enonic.ec.kubernetes.operator.operators.xp7deployment;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;

import io.fabric8.kubernetes.client.Watcher;
import io.quarkus.runtime.StartupEvent;

import com.enonic.ec.kubernetes.operator.OperatorNamespaced;
import com.enonic.ec.kubernetes.operator.common.client.DefaultClientProducer;
import com.enonic.ec.kubernetes.operator.crd.xp7deployment.Xp7DeploymentResource;
import com.enonic.ec.kubernetes.operator.crd.xp7deployment.client.Xp7DeploymentCache;
import com.enonic.ec.kubernetes.operator.helm.ChartRepository;
import com.enonic.ec.kubernetes.operator.helm.Helm;
import com.enonic.ec.kubernetes.operator.helm.LocalRepository;
import com.enonic.ec.kubernetes.operator.helm.commands.HelmUpgrade;
import com.enonic.ec.kubernetes.operator.helm.commands.ImmutableHelmInstall;
import com.enonic.ec.kubernetes.operator.helm.commands.ImmutableHelmUninstall;
import com.enonic.ec.kubernetes.operator.helm.commands.ImmutableHelmUpgrade;
import com.enonic.ec.kubernetes.operator.info.xp7deployment.ImmutableInfoXp7Deployment;
import com.enonic.ec.kubernetes.operator.info.xp7deployment.InfoXp7Deployment;
import com.enonic.ec.kubernetes.operator.kubectl.apply.ImmutableCommandApplyNamespace;
import com.enonic.ec.kubernetes.operator.kubectl.apply.ImmutableCommandApplySecret;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@ApplicationScoped
public class OperatorXp7DeploymentsHelm
    extends OperatorNamespaced
{
    @Inject
    DefaultClientProducer defaultClientProducer;

    @Inject
    Xp7DeploymentCache xp7DeploymentCache;

    @Inject
    Helm helm;

    @ConfigProperty(name = "operator.helm.charts.path")
    String helmChartsPath;

    @ConfigProperty(name = "operator.helm.imageTemplate")
    String imageTemplate;

    ChartRepository chartRepository;

    void onStartup( @Observes StartupEvent _ev )
    {
        chartRepository = new LocalRepository( new File( helmChartsPath ) );
        xp7DeploymentCache.addWatcher( this::watch );
    }

    private void watch( final Watcher.Action action, final String id, final Optional<Xp7DeploymentResource> oldResource,
                        final Optional<Xp7DeploymentResource> newResource )
    {
        InfoXp7Deployment info = ImmutableInfoXp7Deployment.builder().
            oldResource( oldResource ).
            newResource( newResource ).
            build();

        String name = info.deploymentName() + "new";
        String namespace = info.namespaceName() + "new";

        runCommands( commandBuilder -> {
            if ( action == Watcher.Action.ADDED )
            {
                // Create namespace
                commandBuilder.addCommand( ImmutableCommandApplyNamespace.builder().
                    client( defaultClientProducer.client() ).
                    name( namespace ).
                    ownerReference( info.ownerReference() ).
                    build() );

                // Create su pass
                String password = generateSuPassword();
                String passHash = Hashing.sha512().hashString( password, Charsets.UTF_8 ).toString();
                commandBuilder.addCommand( ImmutableCommandApplySecret.builder().
                    client( defaultClientProducer.client() ).
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

    private Map<String, Object> createValues( final InfoXp7Deployment info )
    {
        Map<String, Object> values = new HashMap<>();
        values.put( "image", String.format( imageTemplate, info.resource().getSpec().xpVersion() ) );
        values.put( "defaultLabels", info.defaultLabels() );
        values.put( "properties", Collections.emptyMap() );

        Map<String, Object> deployment = new HashMap<>();
        deployment.put( "name", info.deploymentName() );
        deployment.put( "clustered", false ); // TODO: Fix
        deployment.put( "spec", info.resource().getSpec() );
        values.put( "deployment", deployment );

        return values;
    }

    private String generateSuPassword()
    {
        Random random = new Random();
        final byte[] buffer = new byte[20];
        random.nextBytes( buffer );
        return BaseEncoding.base64Url().omitPadding().encode( buffer );
    }
}
