package com.enonic.cloud.operator.operators.v1alpha1.xp7app;


import java.util.List;
import java.util.stream.Collectors;

import org.immutables.value.Value;

import com.enonic.cloud.operator.crd.xp7.v1alpha1.app.V1alpha1Xp7App;
import com.enonic.cloud.operator.operators.common.cache.Caches;
import com.enonic.cloud.operator.operators.v1alpha2.xp7config.Xp7ConfigAggregator;

import static com.enonic.cloud.operator.common.Configuration.cfgStr;

@Value.Immutable
public abstract class Xp7AppAggregator
    extends Xp7ConfigAggregator
{
    protected abstract Caches caches();

    @Override
    protected String file()
    {
        return cfgStr( "operator.deployment.xp.config.deploy.file" );
    }

    @Override
    protected String data()
    {
        List<V1alpha1Xp7App> apps = caches().getAppCache().getByNamespace( metadata().getNamespace() ).collect( Collectors.toList() );
        StringBuilder sb = new StringBuilder();
        for ( int i = 0; i < apps.size(); i++ )
        {
            sb.append( "deploy." ).append( i + 1 ).append( "=" ).append( apps.get( i ).getSpec().url() ).append( "\n" );
        }
        return sb.toString();
    }
}
