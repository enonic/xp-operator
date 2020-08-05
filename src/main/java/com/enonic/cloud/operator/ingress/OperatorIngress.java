package com.enonic.cloud.operator.ingress;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Singleton;


import io.fabric8.kubernetes.api.model.networking.v1beta1.Ingress;

import com.enonic.cloud.operator.helpers.InformerEventHandler;

import static com.enonic.cloud.common.Configuration.cfgStr;

@Singleton
public class OperatorIngress
    extends InformerEventHandler<Ingress>
{
    @Inject
    OperatorXp7ConfigSync operatorXp7ConfigSync;

    @Override
    protected void init()
    {

    }

    @Override
    public void onNewAdd( final Ingress newResource )
    {
        operatorXp7ConfigSync.handle( newResource.getMetadata().getNamespace() );
    }

    @Override
    public void onUpdate( final Ingress oldResource, final Ingress newResource )
    {
        if ( ingressMappingsEqual( oldResource, newResource ) )
        {
            operatorXp7ConfigSync.handle( newResource.getMetadata().getNamespace() );
        }
    }

    @Override
    public void onDelete( final Ingress oldResource, final boolean b )
    {
        operatorXp7ConfigSync.handle( oldResource.getMetadata().getNamespace() );
    }

    private boolean ingressMappingsEqual( final Ingress oldResource, final Ingress newResource )
    {
        if ( !Objects.equals( oldResource.getSpec().getRules(), newResource.getSpec().getRules() ) )
        {
            return false;
        }

        Map<String, String> oldAnnotations = getXpVHostAnnotations( oldResource );
        Map<String, String> newAnnotations = getXpVHostAnnotations( oldResource );

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
