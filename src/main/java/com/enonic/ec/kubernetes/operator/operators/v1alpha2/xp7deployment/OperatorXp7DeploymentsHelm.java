package com.enonic.ec.kubernetes.operator.operators.v1alpha2.xp7deployment;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.enonic.ec.kubernetes.operator.operators.OperatorNamespaced;
import com.enonic.ec.kubernetes.operator.common.DefaultClientProducer;
import com.enonic.ec.kubernetes.operator.helm.ChartRepository;
import com.enonic.ec.kubernetes.operator.helm.Helm;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@ApplicationScoped
public class OperatorXp7DeploymentsHelm
    extends OperatorNamespaced
{
    @Inject
    DefaultClientProducer defaultClientProducer;

    //@Inject
    //Xp7DeploymentCache xp7DeploymentCache;

    @Inject
    Helm helm;

    @ConfigProperty(name = "operator.helm.charts.path")
    String helmChartsPath;

    @ConfigProperty(name = "operator.helm.imageTemplate")
    String imageTemplate;

    @Inject
    @Named("local")
    ChartRepository chartRepository;

//    void onStartup( @Observes StartupEvent _ev )
//    {
//        // TODO: Enable
//        //xp7DeploymentCache.addWatcher( this::watch );
//    }
//
//    private void watch( final Watcher.Action action, final String id, final Optional<Xp7DeploymentResource> oldResource,
//                        final Optional<Xp7DeploymentResource> newResource )
//    {
//        InfoXp7Deployment info = ImmutableInfoXp7Deployment.builder().
//            oldResource( oldResource ).
//            newResource( newResource ).
//            build();
//
//        String name = info.deploymentName();
//        String namespace = info.namespaceName();
//
//        runCommands( commandBuilder -> {
//            if ( action == Watcher.Action.ADDED )
//            {
//                // Create namespace
//                commandBuilder.addCommand( ImmutableCommandApplyNamespace.builder().
//                    client( defaultClientProducer.client() ).
//                    name( namespace ).
//                    ownerReference( info.ownerReference() ).
//                    build() );
//
//                // Create su pass
//                String password = generateSuPassword();
//                String passHash = Hashing.sha512().hashString( password, Charsets.UTF_8 ).toString();
//                commandBuilder.addCommand( ImmutableCommandApplySecret.builder().
//                    client( defaultClientProducer.client() ).
//                    ownerReference( info.ownerReference() ).
//                    namespace( namespace ).
//                    name( "su" ).
//                    data( Map.of( "pass", BaseEncoding.base64().encode( password.getBytes() ), "passHash", passHash ) ).
//                    build() );
//
//                // Create deployment
//                commandBuilder.addCommand( ImmutableHelmInstall.builder().
//                    helm( helm ).
//                    chart( chartRepository.get( "v1alpha1/test" ) ).
//                    namespace( namespace ).
//                    name( name ).
//                    values( createValues( info ) ).
//                    build() );
//            }
//
//            if ( action == Watcher.Action.ADDED || action == Watcher.Action.MODIFIED )
//            {
//                // Upgrade deployment
//                commandBuilder.addCommand( ImmutableHelmUpgrade.builder().
//                    helm( helm ).
//                    chart( chartRepository.get( "v1alpha1/test" ) ).
//                    namespace( namespace ).
//                    name( name ).
//                    values( createValues( info ) ).
//                    build() );
//            }
//
//            if ( action == Watcher.Action.DELETED )
//            {
//                // Namespace + Secret will be deleted automatically
//
//                // Delete deployment
//                commandBuilder.addCommand( ImmutableHelmUninstall.builder().
//                    helm( helm ).
//                    namespace( namespace ).
//                    name( name ).
//                    build() );
//            }
//        } );
//    }
//
//    protected Object createValues( final InfoXp7Deployment info )
//    {
//        return ImmutableXp7DeploymentValues.builder().
//            info( info ).
//            imageTemplate( imageTemplate ).
//            build().
//            values();
//    }
//
//    private String generateSuPassword()
//    {
//        Random random = new Random();
//        final byte[] buffer = new byte[20];
//        random.nextBytes( buffer );
//        return BaseEncoding.base64Url().omitPadding().encode( buffer );
//    }
}
