package com.enonic.cloud.operator.operators.v1alpha2.xp7config;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.client.Watcher;
import io.quarkus.runtime.StartupEvent;

import com.enonic.cloud.operator.crd.xp7.v1alpha2.config.V1alpha2Xp7Config;
import com.enonic.cloud.operator.operators.common.OperatorNamespaced;
import com.enonic.cloud.operator.operators.common.ResourceInfoNamespaced;
import com.enonic.cloud.operator.operators.common.cache.Caches;
import com.enonic.cloud.operator.operators.common.clients.Clients;
import com.enonic.cloud.operator.operators.v1alpha2.xp7config.commands.ImmutableCommandConfigMapUpdateAll;
import com.enonic.cloud.operator.operators.v1alpha2.xp7config.info.DiffXp7Config;
import com.enonic.cloud.operator.operators.v1alpha2.xp7config.info.ImmutableInfoXp7Config;

import static com.enonic.cloud.operator.operators.common.BackupRestore.isBeingRestored;

@SuppressWarnings("WeakerAccess")
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

        i.ifPresent( info -> runCommands( actionId, ( commandBuilder ) -> {
            if ( isBeingRestored( actionId, action, info.resource() ) )
            {
                // This is a backup restore, just ignore
                return;
            }

            ImmutableCommandConfigMapUpdateAll.builder().
                clients( clients ).
                caches( caches ).
                info( info ).
                build().
                addCommands( commandBuilder );
        } ) );
    }
}
