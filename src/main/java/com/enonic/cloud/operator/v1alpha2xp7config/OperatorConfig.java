package com.enonic.cloud.operator.v1alpha2xp7config;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Named;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.quarkus.runtime.StartupEvent;

import com.enonic.cloud.common.staller.RunnableStaller;
import com.enonic.cloud.kubernetes.caches.ConfigMapCache;
import com.enonic.cloud.kubernetes.caches.V1alpha2Xp7ConfigCache;
import com.enonic.cloud.kubernetes.commands.K8sLogHelper;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.config.V1alpha2Xp7Config;

import static com.enonic.cloud.common.Configuration.cfgStr;

@ApplicationScoped
public class OperatorConfig
    implements ResourceEventHandler<V1alpha2Xp7Config>
{
    @Inject
    KubernetesClient client;

    @Inject
    ConfigMapCache configMapCache;

    @Inject
    V1alpha2Xp7ConfigCache v1alpha2Xp7ConfigCache;

    @Inject
    @Named("1200ms")
    RunnableStaller runnableStaller;

    void onStartup( @Observes StartupEvent _ev )
    {
        v1alpha2Xp7ConfigCache.addEventListener( this );
    }

    @Override
    public void onAdd( final V1alpha2Xp7Config newResource )
    {
        handle( newResource.getMetadata().getNamespace() );
    }

    @Override
    public void onUpdate( final V1alpha2Xp7Config oldResource, final V1alpha2Xp7Config newResource )
    {
        if ( Objects.equals( oldResource.getSpec(), newResource.getSpec() ) )
        {
            return;
        }
        handle( newResource.getMetadata().getNamespace() );
    }

    @Override
    public void onDelete( final V1alpha2Xp7Config oldResource, final boolean b )
    {
        handle( oldResource.getMetadata().getNamespace() );
    }

    private void handle( final String namespace )
    {
        // Stall for a bit while changes are happening
        configMapCache.
            get( namespace ).
            filter( configMap -> configMap.getMetadata().getLabels() != null ).
            filter( configMap -> configMap.getMetadata().getLabels().get( cfgStr( "operator.helm.charts.Values.labelKeys.nodeGroup" ) ) !=
                null ).
            filter( configMap -> configMap.getMetadata().getDeletionTimestamp() == null ).
            forEach( cm -> runnableStaller.put( cm.getMetadata().getUid(), updateCm( cm ) ) );
    }

    private Runnable updateCm( final ConfigMap cm )
    {
        return () -> {
            // Generate new data
            final Map<String, String> data = v1alpha2Xp7ConfigCache.
                get( cm.getMetadata().getNamespace() ).
                filter( c -> filterRelevantXp7Config( c, cm ) ).
                collect( Collectors.toMap( c -> c.getSpec().file(), c -> c.getSpec().data() ) );

            // Get newest version of ConfigMap
            configMapCache.get( cm ).
                ifPresent( configMap -> {
                    if ( !Objects.equals( configMap.getData(), data ) )
                    {
                        // If there is a difference, update
                        K8sLogHelper.logDoneable( client.
                            configMaps().
                            inNamespace( configMap.getMetadata().getNamespace() ).
                            withName( configMap.getMetadata().getName() ).
                            edit().
                            withData( data ) );
                    }
                } );
        };
    }

    private boolean filterRelevantXp7Config( final V1alpha2Xp7Config c, final ConfigMap cm )
    {
        // This config is for all node groups
        if ( Objects.equals( c.getSpec().nodeGroup(), cfgStr( "operator.helm.charts.Values.allNodesKey" ) ) )
        {
            return true;
        }

        String nodeGroup = cm.getMetadata().
            getLabels().
            get( cfgStr( "operator.helm.charts.Values.labelKeys.nodeGroup" ) );

        // This config map does not have a node group label
        if ( nodeGroup == null )
        {
            return false;
        }

        // Check if node groups match
        return Objects.equals( nodeGroup, c.getSpec().nodeGroup() );
    }
}
