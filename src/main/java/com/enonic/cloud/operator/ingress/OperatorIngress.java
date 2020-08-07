package com.enonic.cloud.operator.ingress;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.networking.v1beta1.Ingress;

import com.enonic.cloud.operator.helpers.InformerEventHandler;

import static com.enonic.cloud.common.Configuration.cfgStr;

/**
 * This operator class triggers vhost sync on Ingress changes
 */
@Singleton
public class OperatorIngress
    extends InformerEventHandler<Ingress>
{
    @Inject
    OperatorXp7ConfigSync operatorXp7ConfigSync;

    @Override
    public void onNewAdd( final Ingress newResource )
    {
        handle( newResource );
    }

    @Override
    public void onUpdate( final Ingress oldResource, final Ingress newResource )
    {
        if ( !ingressMappingsEqual( oldResource, newResource ) )
        {
            handle( newResource );
        }
    }

    @Override
    public void onDelete( final Ingress oldResource, final boolean b )
    {
        handle( oldResource );
    }

    private void handle( final Ingress oldResource )
    {
        // Handle relevant namespace
        operatorXp7ConfigSync.handle( oldResource.getMetadata().getNamespace() );
    }

    private boolean ingressMappingsEqual( final Ingress oldResource, final Ingress newResource )
    {
        if ( !Objects.equals( oldResource.getSpec().getRules(), newResource.getSpec().getRules() ) )
        {
            return false;
        }

        Map<String, String> oldAnnotations = getXpVHostAnnotations( oldResource );
        Map<String, String> newAnnotations = getXpVHostAnnotations( newResource );

        return Objects.equals( oldAnnotations, newAnnotations );
    }

    private Map<String, String> getXpVHostAnnotations( final Ingress r )
    {
        Map<String, String> res = new HashMap<>();
        for ( Map.Entry<String, String> e : r.getMetadata().getAnnotations().entrySet() )
        {
            if ( e.getKey().startsWith( cfgStr( "operator.annotations.xp7.vhosts" ) ) )
            {
                res.put( e.getKey(), e.getValue() );
            }
        }
        return res;
    }
}