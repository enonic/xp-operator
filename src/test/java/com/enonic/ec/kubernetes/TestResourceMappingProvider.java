package com.enonic.ec.kubernetes;

import java.util.HashMap;
import java.util.Map;

import io.fabric8.kubernetes.api.KubernetesResourceMappingProvider;
import io.fabric8.kubernetes.api.model.KubernetesResource;

import com.enonic.ec.kubernetes.operator.common.Configuration;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7app.crd.Xp7AppResource;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7config.crd.Xp7ConfigResource;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.crd.Xp7DeploymentResource;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7vhost.crd.Xp7VHostResource;

public class TestResourceMappingProvider
    extends Configuration
    implements KubernetesResourceMappingProvider
{
    @Override
    public Map<String, Class<? extends KubernetesResource>> getMappings()
    {
        String group = cfgStr( "operator.crd.xp.group" );
        String apiVersion = cfgStr( "operator.crd.xp.apiVersion" );

        Map<String, Class<? extends KubernetesResource>> res = new HashMap<>();

        res.put( String.format( "%s/%s#%s", group, apiVersion, cfgStr( "operator.crd.xp.deployments.kind" ) ),
                 Xp7DeploymentResource.class );
        res.put( String.format( "%s/%s#%s", group, apiVersion, cfgStr( "operator.crd.xp.vhosts.kind" ) ), Xp7VHostResource.class );
        res.put( String.format( "%s/%s#%s", group, apiVersion, cfgStr( "operator.crd.xp.configs.kind" ) ), Xp7ConfigResource.class );
        res.put( String.format( "%s/%s#%s", group, apiVersion, cfgStr( "operator.crd.xp.apps.kind" ) ), Xp7AppResource.class );

        return res;
    }
}
