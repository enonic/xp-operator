package com.enonic.kubernetes.operator.ingress;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.networking.v1beta1.HTTPIngressPath;
import io.fabric8.kubernetes.api.model.networking.v1beta1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1beta1.IngressRule;

import com.enonic.kubernetes.kubernetes.Clients;
import com.enonic.kubernetes.kubernetes.Searchers;
import com.enonic.kubernetes.client.v1alpha2.Xp7Config;
import com.enonic.kubernetes.client.v1alpha2.xp7config.Xp7ConfigStatus;
import com.enonic.kubernetes.kubernetes.commands.K8sLogHelper;
import com.enonic.kubernetes.operator.helpers.InformerEventHandler;

import static com.enonic.kubernetes.common.Configuration.cfgStr;
import static com.enonic.kubernetes.kubernetes.Predicates.inSameNamespace;
import static com.enonic.kubernetes.kubernetes.Predicates.isDeleted;
import static com.enonic.kubernetes.kubernetes.Predicates.matchLabel;
import static com.enonic.kubernetes.kubernetes.Predicates.onCondition;

/**
 * This operator class triggers vhost sync on Ingress changes
 */
@Singleton
public class OperatorIngressLabel
    extends InformerEventHandler<Xp7Config>
    implements Runnable
{
    @Inject
    Clients clients;

    @Inject
    Searchers searchers;

    @Inject
    OperatorXp7ConfigSync operatorXp7ConfigSync;

    @Override
    protected void onNewAdd( final Xp7Config newR )
    {
        // Do nothing
    }

    @Override
    public void onUpdate( final Xp7Config oldR, final Xp7Config newR )
    {
        // Only handle if this is a vhost config and it is loaded
        onCondition( newR, this::handle, this::isVHostConfig, ( c ) -> c.getStatus().getState() == Xp7ConfigStatus.State.READY );
    }

    @Override
    public void onDelete( final Xp7Config oldR, final boolean deletedFinalStateUnknown )
    {
        // Do nothing
    }

    @Override
    public void run()
    {
        searchers.ingress().stream().
            filter( isDeleted().negate() ).
            filter( matchLabel( cfgStr( "operator.charts.values.labelKeys.ingressVhostLoaded" ), "false" ) ).
            forEach( this::setStatus );
    }

    private synchronized void handle( final Xp7Config xp7Config )
    {
        searchers.ingress().stream().
            filter( inSameNamespace( xp7Config ) ).
            filter( isDeleted().negate() ).
            filter( matchLabel( cfgStr( "operator.charts.values.labelKeys.ingressVhostLoaded" ), "false" ) ).
            forEach( this::setStatus );
    }

    private synchronized void setStatus( final Ingress ingress )
    {
        // Collect all nodegroup vhost states
        Map<String, Xp7ConfigStatus.State> states = searchers.xp7Config().stream().
            filter( inSameNamespace( ingress ) ).
            filter( isDeleted().negate() ).
            filter( this::isVHostConfig ).
            collect( Collectors.toMap( c -> c.getSpec().getNodeGroup(), c -> c.getStatus().getState() ) );

        // Set all nodeGroups state
        if ( states.values().stream().anyMatch( s -> !s.equals( Xp7ConfigStatus.State.READY ) ) )
        {
            states.put( cfgStr( "operator.charts.values.allNodesKey" ), Xp7ConfigStatus.State.PENDING );
        }
        else
        {
            states.put( cfgStr( "operator.charts.values.allNodesKey" ), Xp7ConfigStatus.State.READY );
        }

        // Figure out state
        boolean loaded = true;
        for ( IngressRule r : ingress.getSpec().getRules() )
        {
            for ( HTTPIngressPath p : r.getHttp().getPaths() )
            {
                Xp7ConfigStatus.State pathState = states.get( p.getBackend().getServiceName() );
                if ( pathState != null && !pathState.equals( Xp7ConfigStatus.State.READY ) )
                {
                    loaded = false;
                    break;
                }
            }
        }

        // Update if true
        if ( loaded )
        {
            K8sLogHelper.logEdit( clients.k8s().network().ingress().
                inNamespace( ingress.getMetadata().getNamespace() ).
                withName( ingress.getMetadata().getName() ), i -> {
                Map<String, String> labels = i.getMetadata().getLabels();
                if ( labels == null )
                {
                    labels = new HashMap<>();
                }
                labels.put( cfgStr( "operator.charts.values.labelKeys.ingressVhostLoaded" ), "true" );
                i.getMetadata().setLabels( labels );
                return i;
            } );
        }
    }

    private boolean isVHostConfig( final Xp7Config c )
    {
        return c.getSpec().getFile().equals( cfgStr( "operator.charts.values.files.vhosts" ) );
    }
}
