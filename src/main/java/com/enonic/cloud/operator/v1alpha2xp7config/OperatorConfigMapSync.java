package com.enonic.cloud.operator.v1alpha2xp7config;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.ConfigMap;

import com.enonic.cloud.kubernetes.Clients;
import com.enonic.cloud.kubernetes.Searchers;
import com.enonic.cloud.kubernetes.commands.K8sLogHelper;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7config.Xp7Config;
import com.enonic.cloud.operator.helpers.HandlerConfig;

import static com.enonic.cloud.common.Configuration.cfgStr;
import static com.enonic.cloud.operator.helpers.VeleroBackups.backupRestoreInProgress;

/**
 * This operator class collects all Xp7Configs and merges them into the nodegroup ConfigMaps
 */
@Singleton
public class OperatorConfigMapSync
    extends HandlerConfig<Xp7Config>
{
    @Inject
    Clients clients;

    @Inject
    Searchers searchers;

    @Override
    protected Stream<Xp7Config> resourceStream()
    {
        return searchers.xp7Config().query().stream();
    }

    @Override
    protected void handle( final String namespace )
    {
        // List all ConfigMaps in namespace with nodeGroup label
        List<ConfigMap> configMaps = searchers.configMap().query().
            inNamespace( namespace ).
            hasNotBeenDeleted().
            hasLabel( cfgStr( "operator.charts.values.labelKeys.nodeGroup" ) ).
            filter( c -> !backupRestoreInProgress( c ) ).
            list();

        // Handle those ConfigMaps
        configMaps.forEach( this::handle );
    }

    private void handle( final ConfigMap configMap )
    {
        // Create a list ['all', '<nodeGroup>']
        List<String> nodeGroups = Arrays.asList( cfgStr( "operator.charts.values.allNodesKey" ), configMap.getMetadata().
            getLabels().
            get( cfgStr( "operator.charts.values.labelKeys.nodeGroup" ) ) );

        // Collect all data from Xp7Config that has nodeGroup 'all' or '<nodeGroup>'
        List<Xp7Config> configs = searchers.xp7Config().query().
            inNamespace( configMap.getMetadata().getNamespace() ).
            filter( xp7Config -> nodeGroups.contains( xp7Config.getXp7ConfigSpec().getNodeGroup() ) ).
            list();

        // Create variables for data
        Map<String, String> data = new HashMap<>();
        Map<String, String> binaryData = new HashMap<>();

        // Populate data
        for ( Xp7Config c : configs )
        {
            if ( Objects.equals( c.getXp7ConfigSpec().getDataBase64(), true ) )
            {
                binaryData.put( c.getXp7ConfigSpec().getFile(), c.getXp7ConfigSpec().getData() );
            }
            else
            {
                data.put( c.getXp7ConfigSpec().getFile(), c.getXp7ConfigSpec().getData() );
            }
        }

        // Get old data
        Map<String, String> oldData = configMap.getData() != null ? configMap.getData() : Collections.emptyMap();
        Map<String, String> oldBinaryData = configMap.getBinaryData() != null ? configMap.getBinaryData() : Collections.emptyMap();

        // If data is not the same, do the update
        if ( !Objects.equals( oldData, data ) || !Objects.equals( oldBinaryData, binaryData ) )
        {
            K8sLogHelper.logDoneable( clients.k8s().
                configMaps().
                inNamespace( configMap.getMetadata().getNamespace() ).
                withName( configMap.getMetadata().getName() ).
                edit().
                withData( data ).
                withBinaryData( binaryData ) );
        }
    }
}
