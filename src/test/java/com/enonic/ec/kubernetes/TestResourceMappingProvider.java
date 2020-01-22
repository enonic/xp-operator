package com.enonic.ec.kubernetes;

import java.util.HashMap;
import java.util.Map;

import io.fabric8.kubernetes.api.KubernetesResourceMappingProvider;
import io.fabric8.kubernetes.api.model.KubernetesResource;

import com.enonic.ec.kubernetes.operator.common.Configuration;
import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha1.app.V1alpha1Xp7App;
import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha2.config.V1alpha2Xp7Config;
import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha2.deployment.V1alpha2Xp7Deployment;
import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha2.vhost.V1alpha2Xp7VHost;

public class TestResourceMappingProvider
    extends Configuration
    implements KubernetesResourceMappingProvider
{
    @Override
    public Map<String, Class<? extends KubernetesResource>> getMappings()
    {
        String group = cfgStr( "operator.crd.group" );

        Map<String, Class<? extends KubernetesResource>> res = new HashMap<>();

        res.put( String.format( "%s/%s#%s", group, cfgStr( "operator.crd.v1alpha1.apiVersion" ), cfgStr( "operator.crd.apps.kind" ) ), V1alpha1Xp7App.class );
        res.put( String.format( "%s/%s#%s", group, cfgStr( "operator.crd.v1alpha2.apiVersion" ), cfgStr( "operator.crd.configs.kind" ) ), V1alpha2Xp7Config.class );
        res.put( String.format( "%s/%s#%s", group, cfgStr( "operator.crd.v1alpha2.apiVersion" ), cfgStr( "operator.crd.deployments.kind" ) ), V1alpha2Xp7Deployment.class );
        res.put( String.format( "%s/%s#%s", group, cfgStr( "operator.crd.v1alpha2.apiVersion" ), cfgStr( "operator.crd.vhosts.kind" ) ), V1alpha2Xp7VHost.class );

        return res;
    }
}
