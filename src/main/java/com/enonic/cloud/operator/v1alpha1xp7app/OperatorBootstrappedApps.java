//package com.enonic.cloud.operator.v1alpha1xp7app;
//
//import java.util.Comparator;
//import java.util.List;
//import java.util.Objects;
//import java.util.stream.Collectors;
//
//import javax.enterprise.context.ApplicationScoped;
//import javax.enterprise.event.Observes;
//import javax.inject.Inject;
//import javax.inject.Named;
//
//import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
//import io.quarkus.runtime.StartupEvent;
//
//import com.enonic.cloud.common.staller.RunnableStaller;
//import com.enonic.cloud.kubernetes.caches.V1alpha1Xp7AppCache;
//import com.enonic.cloud.kubernetes.caches.V1alpha2Xp7ConfigCache;
//import com.enonic.cloud.kubernetes.commands.K8sLogHelper;
//import com.enonic.cloud.kubernetes.crd.client.CrdClient;
//import com.enonic.cloud.kubernetes.crd.xp7.v1alpha1.app.V1alpha1Xp7App;
//import com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.config.ImmutableV1alpha2Xp7ConfigSpec;
//
//import static com.enonic.cloud.common.Configuration.cfgStr;
//import static com.enonic.cloud.common.Configuration.cfgStrFmt;
//
//@ApplicationScoped
//public class OperatorBootstrappedApps
//    implements ResourceEventHandler<V1alpha1Xp7App>
//{
//    @SuppressWarnings("CdiInjectionPointsInspection")
//    @Inject
//    CrdClient crdClient;
//
//    @Inject
//    V1alpha1Xp7AppCache v1alpha1Xp7AppCache;
//
//    @Inject
//    V1alpha2Xp7ConfigCache v1alpha2Xp7ConfigCache;
//
//    @Inject
//    @Named("400ms")
//    RunnableStaller runnableStaller;
//
//    void onStartup( @Observes StartupEvent _ev )
//    {
//        v1alpha1Xp7AppCache.addEventListener( this );
//    }
//
//    @Override
//    public void onAdd( final V1alpha1Xp7App newResource )
//    {
//        if ( !newResource.getSpec().options().bootstrap() )
//        {
//            return;
//        }
//        handleBootstrap( newResource.getMetadata().getNamespace() );
//    }
//
//    @Override
//    public void onUpdate( final V1alpha1Xp7App oldResource, final V1alpha1Xp7App newResource )
//    {
//        if ( !oldResource.getSpec().options().bootstrap() && !newResource.getSpec().options().bootstrap() )
//        {
//            return;
//        }
//
//        if ( Objects.equals( oldResource.getSpec(), newResource.getSpec() ) )
//        {
//            return;
//        }
//
//        handleBootstrap( newResource.getMetadata().getNamespace() );
//    }
//
//    @Override
//    public void onDelete( final V1alpha1Xp7App oldResource, final boolean b )
//    {
//        if ( !oldResource.getSpec().options().bootstrap() )
//        {
//            return;
//        }
//
//        handleBootstrap( oldResource.getMetadata().getNamespace() );
//    }
//
//    private void handleBootstrap( final String namespace )
//    {
//        // Setup names
//        final String nodeGroup = cfgStr( "operator.helm.charts.Values.allNodesKey" );
//        final String configName = cfgStrFmt( "operator.deployment.xp.config.deploy.nameTemplate", nodeGroup );
//
//        // Stall for a bit while changes are happening
//        runnableStaller.put( namespace + configName, () -> {
//            // Get all bootstrap apps in namespace
//            List<String> urls = v1alpha1Xp7AppCache.get( namespace ).
//                filter( a -> a.getSpec().options().bootstrap() ).
//                sorted( Comparator.comparing( a -> a.getMetadata().getName() ) ).
//                map( a -> a.getSpec().url() ).
//                collect( Collectors.toList() );
//
//            // Build config file
//            StringBuilder sb = new StringBuilder();
//            for ( int i = 0; i < urls.size(); i++ )
//            {
//                sb.append( "deploy." ).append( i + 1 ).append( "=" ).append( urls.get( i ) ).append( "\n" );
//            }
//            String data = sb.toString();
//
//            // Get present xp7config
//            v1alpha2Xp7ConfigCache.
//                get( namespace, configName ).ifPresent( c -> {
//                // If it is not equal to the old one, update
//                if ( !Objects.equals( c.getSpec().data(), data ) )
//                {
//                    K8sLogHelper.logDoneable( crdClient.
//                        xp7Configs().
//                        inNamespace( c.getMetadata().getNamespace() ).
//                        withName( c.getMetadata().getName() ).
//                        edit().
//                        withSpec( ImmutableV1alpha2Xp7ConfigSpec.builder().
//                            from( c.getSpec() ).
//                            data( data ).
//                            build() ) );
//                }
//            } );
//        } );
//    }
//}
