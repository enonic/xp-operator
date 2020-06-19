package com.enonic.cloud.operator.v1alpha2xp7config;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
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
        // Sync all ConfigMaps with nodeGroup label
        searchers.configMap().query().
            hasNotBeenDeleted().
            hasLabel( cfgStr( "operator.helm.charts.Values.labelKeys.nodeGroup" ) ).
            forEach( this::handle );
    }

    private void handle( final ConfigMap configMap )
    {
        // Create a list ['all', '<nodeGroup>']
        List<String> nodeGroups = Arrays.asList( cfgStr( "operator.helm.charts.Values.allNodesKey" ), configMap.getMetadata().
            getLabels().
            get( cfgStr( "operator.helm.charts.Values.labelKeys.nodeGroup" ) ) );

        // Collect all data from Xp7Config that has nodeGroup 'all' or '<nodeGroup>'
        Map<String, String> data = searchers.xp7Config().query().
            inNamespace( configMap.getMetadata().getNamespace() ).
            filter( xp7Config -> nodeGroups.contains( xp7Config.getXp7ConfigSpec().getNodeGroup() ) ).
            stream().
            collect( Collectors.toMap( c -> c.getXp7ConfigSpec().getFile(), c -> c.getXp7ConfigSpec().getData() ) );

        // If data is not the same, update
        if ( !Objects.equals( configMap.getData(), data ) )
        {
            K8sLogHelper.logDoneable( clients.k8s().
                configMaps().
                inNamespace( configMap.getMetadata().getNamespace() ).
                withName( configMap.getMetadata().getName() ).
                edit().
                withData( data ) );
        }
    }
}
