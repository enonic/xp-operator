package com.enonic.cloud.operator.crd;

import java.util.HashMap;
import java.util.Map;

import io.fabric8.kubernetes.api.KubernetesResourceMappingProvider;
import io.fabric8.kubernetes.api.model.KubernetesResource;

import com.enonic.cloud.operator.common.Configuration;
import com.enonic.cloud.operator.crd.xp7.v1alpha1.app.V1alpha1Xp7App;
import com.enonic.cloud.operator.crd.xp7.v1alpha1.config.V1alpha1Xp7Config;
import com.enonic.cloud.operator.crd.xp7.v1alpha1.deployment.V1alpha1Xp7Deployment;
import com.enonic.cloud.operator.crd.xp7.v1alpha1.vhost.V1alpha1Xp7VHost;
import com.enonic.cloud.operator.crd.xp7.v1alpha2.config.V1alpha2Xp7Config;
import com.enonic.cloud.operator.crd.xp7.v1alpha2.deployment.V1alpha2Xp7Deployment;
import com.enonic.cloud.operator.crd.xp7.v1alpha2.vhost.V1alpha2Xp7VHost;

public class CrdMappingProvider
    extends Configuration
    implements KubernetesResourceMappingProvider
{
    private final String group;

    private final Map<String, Class<? extends KubernetesResource>> mappings;

    public CrdMappingProvider()
    {
        group = cfgStr( "operator.crd.group" );
        mappings = new HashMap<>();
        populateMappings();
    }

    private void populateMappings()
    {
        String apps = cfgStr( "operator.crd.apps.kind" );
        String configs = cfgStr( "operator.crd.configs.kind" );
        String deployments = cfgStr( "operator.crd.deployments.kind" );
        String vHosts = cfgStr( "operator.crd.vhosts.kind" );

        String v1alpha1 = cfgStr( "operator.crd.v1alpha1.apiVersion" );
        put( v1alpha1, apps, V1alpha1Xp7App.class );
        put( v1alpha1, configs, V1alpha1Xp7Config.class );
        put( v1alpha1, deployments, V1alpha1Xp7Deployment.class );
        put( v1alpha1, vHosts, V1alpha1Xp7VHost.class );

        String v1alpha2 = cfgStr( "operator.crd.v1alpha2.apiVersion" );
        put( v1alpha2, configs, V1alpha2Xp7Config.class );
        put( v1alpha2, deployments, V1alpha2Xp7Deployment.class );
        put( v1alpha2, vHosts, V1alpha2Xp7VHost.class );
    }

    private void put( String apiVersion, String kind, Class<? extends KubernetesResource> klass )
    {
        mappings.put( String.format( "%s/%s#%s", group, apiVersion, kind ), klass );
    }

    @Override
    public Map<String, Class<? extends KubernetesResource>> getMappings()
    {
        return mappings;
    }
}
