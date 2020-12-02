package com.enonic.cloud.operator.v1alpha2xp7config;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.Event;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Pod;

import com.enonic.cloud.kubernetes.Clients;
import com.enonic.cloud.kubernetes.Searchers;
import com.enonic.cloud.kubernetes.commands.K8sLogHelper;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7config.Xp7Config;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7config.Xp7ConfigStatus;
import com.enonic.cloud.operator.helpers.InformerEventHandler;

import static com.enonic.cloud.common.Configuration.cfgStr;

/**
 * This operator class triggers maintains the Xp7Config status field
 */
@Singleton
public class OperatorXp7ConfigStatus
    extends InformerEventHandler<Event>
{
    @Inject
    Clients clients;

    @Inject
    Searchers searchers;

    @Override
    public void onNewAdd( final Event newEvent )
    {
        handle( newEvent );
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

    private synchronized void handle( final Event newEvent )
    {
        if ( !"ConfigReload".equals( newEvent.getReason() ) )
        {
            return;
        }

        // Find out when the configmap was last updated
        Optional<Instant> configMapLastModifiedAt = searchers.event().query().
            inNamespace( newEvent.getMetadata().getNamespace() ).
            stream().
            filter( e -> "ConfigModified".equals( e.getReason() ) ).
            filter( e -> e.getInvolvedObject().getName().equals( newEvent.getRelated().getName() ) ).
            sorted( Comparator.comparing( ( HasMetadata a ) -> a.getMetadata().getCreationTimestamp() ).reversed() ).
            map( e -> Instant.parse( e.getMetadata().getCreationTimestamp() ) ).
            findFirst();

        // We cant make any assumptions if last modified is nonexistent
        if ( configMapLastModifiedAt.isEmpty() )
        {
            return;
        }

        // Find pods that have been updated since then
        List<String> updatedPods = searchers.event().
            query().
            inNamespace( newEvent.getMetadata().getNamespace() ).
            filter( e -> "ConfigReload".equals( e.getReason() ) ).
            youngerThen( configMapLastModifiedAt.get() ).
            stream().
            map( e -> e.getInvolvedObject().getName() ).
            collect( Collectors.toList() );

        // Pods that have not been updated
        List<Pod> notUpdatedPods = searchers.pod().query().
            inNamespace( newEvent.getMetadata().getNamespace() ).
            isEnonicManaged().
            filter( p -> !updatedPods.contains( p.getMetadata().getName() ) ).
            list();

        // Collect relevant XP7 configs and handle them
        searchers.xp7Config().
            query().
            inNamespace( newEvent.getMetadata().getNamespace() ).
            filter( c -> c.getXp7ConfigStatus().getState() != Xp7ConfigStatus.State.READY ).
            forEach( c -> handle( c, notUpdatedPods ) );
    }

    private boolean handle( final Xp7Config xp7Config, final List<Pod> notUpdatedPods )
    {
        // All pods are updated, mark as ready
        if ( notUpdatedPods.isEmpty() )
        {
            return markReady( xp7Config );
        }

        // This should apply to all pods, and some are not ready
        if ( xp7Config.getXp7ConfigSpec().getNodeGroup().equals( cfgStr( "operator.charts.values.allNodesKey" ) ) )
        {
            return markPending( xp7Config, notUpdatedPods );
        }

        // This should apply to a particular node group
        List<Pod> relevantNonUpdated = notUpdatedPods.stream().
            filter( p -> Objects.equals( p.getMetadata().
                getLabels().
                get( cfgStr( "operator.charts.values.labelKeys.nodeGroup" ) ), xp7Config.getXp7ConfigSpec().
                getNodeGroup() ) ).
            collect( Collectors.toList() );

        if ( relevantNonUpdated.isEmpty() )
        {
            return markReady( xp7Config );
        }
        else
        {
            return markPending( xp7Config, relevantNonUpdated );
        }
    }

    private boolean markPending( final Xp7Config xp7Config, final List<Pod> notUpdatedPods )
    {
        String podNames = notUpdatedPods.stream().
            map( p -> p.getMetadata().getName() ).
            collect( Collectors.joining( ", " ) );

        K8sLogHelper.logDoneable( clients.xp7Configs().crdClient().
            inNamespace( xp7Config.getMetadata().getNamespace() ).
            withName( xp7Config.getMetadata().getName() ).
            edit().
            withStatus( new Xp7ConfigStatus().
                withState( Xp7ConfigStatus.State.PENDING ).
                withMessage( "Not loaded: " + podNames ) ) );
        return false;
    }

    private boolean markReady( final Xp7Config xp7Config )
    {
        K8sLogHelper.logDoneable( clients.xp7Configs().crdClient().
            inNamespace( xp7Config.getMetadata().getNamespace() ).
            withName( xp7Config.getMetadata().getName() ).
            edit().
            withStatus( new Xp7ConfigStatus().
                withState( Xp7ConfigStatus.State.READY ).
                withMessage( "OK" ) ) );
        return true;
    }
}
