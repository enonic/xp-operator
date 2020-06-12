package com.enonic.cloud.operator.v1alpha2xp7config;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Named;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.quarkus.runtime.StartupEvent;

import com.enonic.cloud.common.staller.RunnableStaller;
import com.enonic.cloud.kubernetes.Clients;
import com.enonic.cloud.kubernetes.InformerSearcher;
import com.enonic.cloud.kubernetes.commands.K8sLogHelper;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7config.Xp7Config;
import com.enonic.cloud.operator.InformerEventHandler;

import static com.enonic.cloud.common.Configuration.cfgStr;

@ApplicationScoped
public class OperatorConfig
    extends InformerEventHandler<Xp7Config>
{
    @Inject
    Clients clients;

    @Inject
    SharedIndexInformer<Xp7Config> xp7ConfigSharedIndexInformer;

    @Inject
    InformerSearcher<Xp7Config> xp7ConfigInformerSearcher;

    @Inject
    InformerSearcher<ConfigMap> configMapInformerSearcher;

    @Inject
    @Named("1200ms")
    RunnableStaller runnableStaller;

    void onStartup( @Observes StartupEvent _ev )
    {
        listenToInformer( xp7ConfigSharedIndexInformer );
        ;
    }

    @Override
    public void onAdd( final Xp7Config newResource )
    {
        handle( newResource.getMetadata().getNamespace() );
    }

    @Override
    public void onUpdate( final Xp7Config oldResource, final Xp7Config newResource )
    {
        if ( Objects.equals( oldResource.getXp7ConfigSpec(), newResource.getXp7ConfigSpec() ) )
        {
            return;
        }
        handle( newResource.getMetadata().getNamespace() );
    }

    @Override
    public void onDelete( final Xp7Config oldResource, final boolean b )
    {
        handle( oldResource.getMetadata().getNamespace() );
    }

    private void handle( final String namespace )
    {
        // Stall for a bit while changes are happening
        configMapInformerSearcher.
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
            final Map<String, String> data = xp7ConfigInformerSearcher.
                get( cm.getMetadata().getNamespace() ).
                filter( c -> filterRelevantXp7Config( c, cm ) ).
                collect( Collectors.toMap( c -> c.getXp7ConfigSpec().getFile(), c -> c.getXp7ConfigSpec().getData() ) );

            // Get newest version of ConfigMap
            configMapInformerSearcher.get( cm ).
                ifPresent( configMap -> {
                    if ( !Objects.equals( configMap.getData(), data ) )
                    {
                        // If there is a difference, update
                        K8sLogHelper.logDoneable( clients.k8s().
                            configMaps().
                            inNamespace( configMap.getMetadata().getNamespace() ).
                            withName( configMap.getMetadata().getName() ).
                            edit().
                            withData( data ) );
                    }
                } );
        };
    }

    private boolean filterRelevantXp7Config( final Xp7Config c, final ConfigMap cm )
    {
        // This config is for all node groups
        if ( Objects.equals( c.getXp7ConfigSpec().getNodeGroup(), cfgStr( "operator.helm.charts.Values.allNodesKey" ) ) )
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
        return Objects.equals( nodeGroup, c.getXp7ConfigSpec().getNodeGroup() );
    }
}
