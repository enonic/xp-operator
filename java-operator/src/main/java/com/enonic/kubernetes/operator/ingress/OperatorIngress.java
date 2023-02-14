package com.enonic.kubernetes.operator.ingress;

import com.enonic.kubernetes.kubernetes.Clients;
import com.enonic.kubernetes.kubernetes.Informers;
import com.enonic.kubernetes.kubernetes.Searchers;
import com.enonic.kubernetes.kubernetes.commands.K8sLogHelper;
import com.enonic.kubernetes.operator.helpers.InformerEventHandler;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.quarkus.runtime.StartupEvent;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.enonic.kubernetes.common.Configuration.cfgStr;
import static com.enonic.kubernetes.kubernetes.Predicates.contains;
import static com.enonic.kubernetes.kubernetes.Predicates.isDeleted;

/**
 * This operator class triggers vhost sync on Ingress changes
 */
@ApplicationScoped
public class OperatorIngress
    extends InformerEventHandler<Ingress>
{
    @Inject
    Clients clients;

    @Inject
    OperatorXp7ConfigSync operatorXp7ConfigSync;

    @Inject
    Searchers searchers;

    @Inject
    Informers informers;

    void onStart( @Observes StartupEvent ev )
    {
        listen( informers.ingressInformer() );
    }

    @Override
    public void onNewAdd( final Ingress newResource )
    {
        handle( newResource );
    }

    @Override
    public void onUpdate( final Ingress oldResource, final Ingress newResource )
    {
        if (!ingressMappingsEqual( oldResource, newResource )) {
            handle( newResource );
        }
    }

    @Override
    public void onDelete( final Ingress oldResource, final boolean b )
    {
        handle( oldResource );
    }

    private void handle( final Ingress r )
    {
        // Bail if the namespace is being deleted
        if (searchers.namespace().match( contains( r ), isDeleted() )) {
            return;
        }

        // Handle relevant namespace
        operatorXp7ConfigSync.handle( r.getMetadata().getNamespace() );

        // Label ingress
        if (!getXpVHostAnnotations( r ).isEmpty()) {
            K8sLogHelper.logEdit( clients.k8s().network().v1().ingresses().
                inNamespace( r.getMetadata().getNamespace() ).
                withName( r.getMetadata().getName() ), i -> {
                Map<String, String> labels = i.getMetadata().getLabels();
                if (labels == null) {
                    labels = new HashMap<>();
                }
                labels.put( cfgStr( "operator.charts.values.labelKeys.ingressVhostLoaded" ), "false" );
                i.getMetadata().setLabels( labels );
                return i;
            } );
        }
    }

    private boolean ingressMappingsEqual( final Ingress oldResource, final Ingress newResource )
    {
        if (!Objects.equals( oldResource.getSpec().getRules(), newResource.getSpec().getRules() )) {
            return false;
        }

        Map<String, String> oldAnnotations = getXpVHostAnnotations( oldResource );
        Map<String, String> newAnnotations = getXpVHostAnnotations( newResource );

        return Objects.equals( oldAnnotations, newAnnotations );
    }

    private Map<String, String> getXpVHostAnnotations( final Ingress r )
    {
        Map<String, String> res = new HashMap<>();
        for (Map.Entry<String, String> e : r.getMetadata().getAnnotations().entrySet()) {
            if (e.getKey().startsWith( cfgStr( "operator.charts.values.annotationKeys.vhostMapping" ) )) {
                res.put( e.getKey(), e.getValue() );
            }
        }
        return res;
    }
}
