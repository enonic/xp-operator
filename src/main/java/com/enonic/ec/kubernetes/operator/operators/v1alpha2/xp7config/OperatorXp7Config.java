package com.enonic.ec.kubernetes.operator.operators.v1alpha2.xp7config;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.client.Watcher;
import io.quarkus.runtime.StartupEvent;

import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha2.config.V1alpha2Xp7Config;
import com.enonic.ec.kubernetes.operator.operators.common.OperatorNamespaced;
import com.enonic.ec.kubernetes.operator.operators.common.ResourceInfoNamespaced;
import com.enonic.ec.kubernetes.operator.operators.common.cache.Caches;
import com.enonic.ec.kubernetes.operator.operators.common.clients.Clients;
import com.enonic.ec.kubernetes.operator.operators.v1alpha2.xp7config.commands.ImmutableCommandConfigMapUpdateAll;
import com.enonic.ec.kubernetes.operator.operators.v1alpha2.xp7config.info.DiffXp7Config;
import com.enonic.ec.kubernetes.operator.operators.v1alpha2.xp7config.info.ImmutableInfoXp7Config;

@ApplicationScoped
public class OperatorXp7Config
    extends OperatorNamespaced
{
    private static final Logger log = LoggerFactory.getLogger( OperatorXp7Config.class );

    @Inject
    Clients clients;

    @Inject
    Caches caches;

    void onStartup( @Observes StartupEvent _ev )
    {
        caches.getConfigCache().addEventListener( this::watchXpConfig );
        log.info( "Started listening for Xp7Config events" );
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private void watchXpConfig( final String actionId, final Watcher.Action action, final Optional<V1alpha2Xp7Config> oldResource,
                                final Optional<V1alpha2Xp7Config> newResource )
    {
        Optional<ResourceInfoNamespaced<V1alpha2Xp7Config, DiffXp7Config>> i = getInfo( action, () -> ImmutableInfoXp7Config.builder().
            caches( caches ).
            oldResource( oldResource ).
            newResource( newResource ).
            build() );

        i.ifPresent( info -> {

            runCommands( actionId, ( commandBuilder ) -> ImmutableCommandConfigMapUpdateAll.builder().
                clients( clients ).
                caches( caches ).
                info( info ).
                build().
                addCommands( commandBuilder ) );
        } );
    }

//    private void watchConfigMap( final Watcher.Action action, final String id, final Optional<ConfigMap> oldResource,
//                                 final Optional<ConfigMap> newResource )
//    {
//        // This is to make sure that the config map is in sync with the Xp7Configs
//        newResource.ifPresent( oldConfigMap -> {
//            String cmdId = createCmdId();
//            stallAndRunCommands( "", 60000L, cmdId, commandBuilder -> {
//                // Fetch from the cache status of the config map
//                caches.getConfigMapCache().
//                    get( oldConfigMap.getMetadata().getNamespace(), oldConfigMap.getMetadata().getName() ).
//                    ifPresent( configMap -> {
//                        log.info( String.format( "%s: In NS '%s' ensuring ConfigMap '%s' consistency", cmdId,
//                                                 configMap.getMetadata().getNamespace(), configMap.getMetadata().getName() ) );
//
//                        // Get all relevant XpConfig for this ConfigMap
//                        List<V1alpha2Xp7Config> allXpConfigs = ImmutableRelevantXp7Config.builder().
//                            caches( caches ).
//                            configMap( configMap ).
//                            build().
//                            xp7Configs();
//
//                        // Update ConfigMap
//                        ImmutableCommandConfigMapUpdate.builder().
//                            clients( clients ).
//                            configMap( configMap ).
//                            xpConfigResources( allXpConfigs ).
//                            build().
//                            addCommands( commandBuilder );
//                    } );
//            } );
//        } );
//    }
}
