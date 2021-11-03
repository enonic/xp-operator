package com.enonic.kubernetes.operator.xp7config;

import com.enonic.kubernetes.client.v1.xp7config.Xp7Config;
import com.enonic.kubernetes.client.v1.xp7config.Xp7ConfigStatus;
import com.enonic.kubernetes.kubernetes.Clients;
import com.enonic.kubernetes.kubernetes.Informers;
import com.enonic.kubernetes.kubernetes.Searchers;
import com.enonic.kubernetes.kubernetes.commands.K8sLogHelper;
import com.enonic.kubernetes.operator.helpers.InformerEventHandler;
import io.fabric8.kubernetes.api.model.Event;
import io.fabric8.kubernetes.api.model.Pod;
import io.quarkus.runtime.StartupEvent;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.enonic.kubernetes.common.Configuration.cfgStr;
import static com.enonic.kubernetes.kubernetes.Comparators.creationTime;
import static com.enonic.kubernetes.kubernetes.Predicates.createdAfter;
import static com.enonic.kubernetes.kubernetes.Predicates.inSameNamespaceAs;
import static com.enonic.kubernetes.kubernetes.Predicates.isEnonicManaged;
import static com.enonic.kubernetes.kubernetes.Predicates.matchLabel;

/**
 * This operator class triggers maintains the Xp7Config status field
 */
@ApplicationScoped
public class OperatorXp7ConfigStatus
    extends InformerEventHandler<Event>
{
    @Inject
    Clients clients;

    @Inject
    Searchers searchers;

    @Inject
    Informers informers;

    void onStart( @Observes StartupEvent ev )
    {
        listen( informers.eventInformer() );
    }

    @Override
    public void onNewAdd( final Event newEvent )
    {
        if ("ConfigReload".equals( newEvent.getReason() )) {
            handle( newEvent );
        }
    }

    @Override
    public void onUpdate( final Event oldEvent, final Event newEvent )
    {
        // Do nothing
    }

    @Override
    public void onDelete( final Event newEvent, final boolean b )
    {
        // Do nothing
    }

    private void handle( final Event newEvent )
    {
        // Find out when the configmap was last updated
        Optional<Instant> configMapLastModifiedAt = searchers.event().stream().
            filter( inSameNamespaceAs( newEvent ) ).
            filter( e -> "ConfigModified".equals( e.getReason() ) ).
            filter( e -> e.getInvolvedObject().getName().equals( newEvent.getRelated().getName() ) ).
            sorted( creationTime().reversed() ).
            map( e -> Instant.parse( e.getMetadata().getCreationTimestamp() ) ).
            findFirst();

        // We cant make any assumptions if last modified is nonexistent
        if (configMapLastModifiedAt.isEmpty()) {
            return;
        }

        // Find pods that have been updated since then
        List<String> updatedPods = searchers.event().stream().
            filter( inSameNamespaceAs( newEvent ) ).
            filter( e -> "ConfigReload".equals( e.getReason() ) ).
            filter( createdAfter( configMapLastModifiedAt.get() ) ).
            map( e -> e.getInvolvedObject().getName() ).
            collect( Collectors.toList() );

        // Pods that have not been updated
        List<Pod> notUpdatedPods = searchers.pod().stream().
            filter( inSameNamespaceAs( newEvent ) ).
            filter( isEnonicManaged() ).
            filter( p -> !updatedPods.contains( p.getMetadata().getName() ) ).
            collect( Collectors.toList() );

        // Collect relevant XP7 configs and handle them
        searchers.xp7Config().stream().
            filter( inSameNamespaceAs( newEvent ) ).
            filter( c -> c.getStatus().getState() != Xp7ConfigStatus.State.READY ).
            forEach( c -> handle( c, notUpdatedPods ) );
    }

    private boolean handle( final Xp7Config xp7Config, final List<Pod> notUpdatedPods )
    {
        // All pods are updated, mark as ready
        if (notUpdatedPods.isEmpty()) {
            return markReady( xp7Config );
        }

        // This should apply to all pods, and some are not ready
        if (xp7Config.getSpec().getNodeGroup().equals( cfgStr( "operator.charts.values.allNodesKey" ) )) {
            return markPending( xp7Config, notUpdatedPods );
        }

        // This should apply to a particular node group
        List<Pod> relevantNonUpdated = notUpdatedPods.stream().
            filter( matchLabel( cfgStr( "operator.charts.values.labelKeys.nodeGroup" ), xp7Config.getSpec().getNodeGroup() ) ).
            collect( Collectors.toList() );

        if (relevantNonUpdated.isEmpty()) {
            return markReady( xp7Config );
        } else {
            return markPending( xp7Config, relevantNonUpdated );
        }
    }

    private boolean markPending( final Xp7Config xp7Config, final List<Pod> notUpdatedPods )
    {
        String podNames = notUpdatedPods.stream().
            map( p -> p.getMetadata().getName() ).
            collect( Collectors.joining( ", " ) );

        K8sLogHelper.logEdit( clients.xp7Configs().
            inNamespace( xp7Config.getMetadata().getNamespace() ).
            withName( xp7Config.getMetadata().getName() ), c -> c.withStatus( new Xp7ConfigStatus().
            withState( Xp7ConfigStatus.State.PENDING ).
            withMessage( "Not loaded: " + podNames ) ) );
        return false;
    }

    private boolean markReady( final Xp7Config xp7Config )
    {
        K8sLogHelper.logEdit( clients.xp7Configs().
            inNamespace( xp7Config.getMetadata().getNamespace() ).
            withName( xp7Config.getMetadata().getName() ), c -> c.withStatus( new Xp7ConfigStatus().
            withState( Xp7ConfigStatus.State.READY ).
            withMessage( "OK" ) ) );
        return true;
    }
}
