package com.enonic.kubernetes.operator.v1alpha2xp7config;

import com.enonic.kubernetes.client.v1alpha2.Xp7Config;
import com.enonic.kubernetes.common.TaskRunner;
import com.enonic.kubernetes.kubernetes.ActionLimiter;
import com.enonic.kubernetes.kubernetes.Clients;
import com.enonic.kubernetes.kubernetes.Searchers;
import com.enonic.kubernetes.kubernetes.commands.K8sLogHelper;
import com.enonic.kubernetes.operator.Operator;
import com.google.common.hash.Hashing;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.quarkus.runtime.StartupEvent;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.enonic.kubernetes.common.Configuration.cfgLong;
import static com.enonic.kubernetes.common.Configuration.cfgStr;
import static com.enonic.kubernetes.kubernetes.Predicates.contains;
import static com.enonic.kubernetes.kubernetes.Predicates.hasLabel;
import static com.enonic.kubernetes.kubernetes.Predicates.inNamespace;
import static com.enonic.kubernetes.kubernetes.Predicates.inNodeGroupAllOr;
import static com.enonic.kubernetes.kubernetes.Predicates.inSameNamespaceAs;
import static com.enonic.kubernetes.kubernetes.Predicates.isBeingBackupRestored;
import static com.enonic.kubernetes.kubernetes.Predicates.isDeleted;
import static java.util.stream.Collectors.groupingBy;

/**
 * This operator class collects all Xp7Configs and merges them into the nodegroup ConfigMaps
 */
@ApplicationScoped
public class OperatorConfigMapSync
    implements Runnable
{
    @Inject
    Operator operator;

    @Inject
    Clients clients;

    @Inject
    Searchers searchers;

    @Inject
    TaskRunner taskRunner;

    ActionLimiter limiter;

    void onStart( @Observes StartupEvent ev )
    {
        limiter = new ActionLimiter( taskRunner );
        operator.schedule( cfgLong( "operator.tasks.sync.interval" ), this );
    }

    @Override
    public void run()
    {
        searchers.xp7Config().stream().collect( groupingBy( r -> r.getMetadata().getNamespace() ) ).
            keySet().
            forEach( this::handle );
    }

    protected void handle( final String namespace )
    {
        // Handle all ConfigMaps in namespace with nodeGroup label
        limiter.limit( 1000L, namespace, String::hashCode, () -> searchers.configMap().stream().
            filter( inNamespace( namespace ) ).
            filter( isDeleted().negate() ).
            filter( hasLabel( cfgStr( "operator.charts.values.labelKeys.nodeGroup" ) ) ).
            filter( isBeingBackupRestored().negate() ).
            forEach( this::handle ) );
    }

    private synchronized void handle( final ConfigMap configMap )
    {
        // Namespace is being deleted
        if (searchers.namespace().stream().
            anyMatch( contains( configMap ).and( isDeleted() ) )) {
            return;
        }

        // Get ConfigMap nodegroup
        String nodeGroup = configMap.getMetadata().
            getLabels().
            get( cfgStr( "operator.charts.values.labelKeys.nodeGroup" ) );

        // Collect all data from Xp7Config that has nodeGroup 'all' or '<nodeGroup>'
        List<Xp7Config> configs = searchers.xp7Config().stream().
            filter( inSameNamespaceAs( configMap ) ).
            filter( inNodeGroupAllOr( nodeGroup ) ).
            collect( Collectors.toList() );

        // Create variables for data
        Map<String, String> data = new HashMap<>();
        Map<String, String> binaryData = new HashMap<>();

        // Populate data
        StringBuilder index = new StringBuilder();
        for (Xp7Config c : configs) {
            if (Objects.equals( c.getSpec().getDataBase64(), true )) {
                binaryData.put( c.getSpec().getFile(), c.getSpec().getData() );
            } else {
                data.put( c.getSpec().getFile(), c.getSpec().getData() );
            }
            index.
                append( c.getMetadata().getName() ).append( "\t" ).
                append( c.getSpec().getNodeGroup() ).append( "\t" ).
                append( c.getSpec().getFile() ).append( "\n" );
        }
        // We add this to be sure that events are always sent when small changes are made
        data.put( "operator-hash", Hashing.sha256().hashBytes( index.toString().getBytes() ).toString() );

        // Get old data
        Map<String, String> oldData = configMap.getData() != null ? configMap.getData() : Collections.emptyMap();
        Map<String, String> oldBinaryData = configMap.getBinaryData() != null ? configMap.getBinaryData() : Collections.emptyMap();

        // If data is not the same, do the update
        if (!Objects.equals( oldData, data ) || !Objects.equals( oldBinaryData, binaryData )) {
            K8sLogHelper.logEdit( clients.k8s().
                configMaps().
                inNamespace( configMap.getMetadata().getNamespace() ).
                withName( configMap.getMetadata().getName() ), c -> {
                c.setData( data );
                c.setBinaryData( binaryData );
                return c;
            } );
        }
    }
}
