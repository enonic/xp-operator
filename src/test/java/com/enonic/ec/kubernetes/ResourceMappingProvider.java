package com.enonic.ec.kubernetes;

import java.util.HashMap;
import java.util.Map;

import io.fabric8.kubernetes.api.KubernetesResourceMappingProvider;
import io.fabric8.kubernetes.api.model.KubernetesResource;

import com.enonic.ec.kubernetes.common.Configuration;
import com.enonic.ec.kubernetes.operator.crd.app.XpAppResource;
import com.enonic.ec.kubernetes.operator.crd.config.XpConfigResource;
import com.enonic.ec.kubernetes.operator.crd.deployment.XpDeploymentResource;
import com.enonic.ec.kubernetes.operator.crd.vhost.XpVHostResource;

public class ResourceMappingProvider
    extends Configuration
    implements KubernetesResourceMappingProvider
{
    @Override
    public Map<String, Class<? extends KubernetesResource>> getMappings()
    {
        String group = cfgStr( "operator.crd.xp.group" );
        String apiVersion = cfgStr( "operator.crd.xp.apiVersion" );

        Map<String, Class<? extends KubernetesResource>> res = new HashMap<>();

        res.put( String.format( "%s/%s#%s", group, apiVersion, cfgStr( "operator.crd.xp.deployments.kind" ) ), XpDeploymentResource.class );
        res.put( String.format( "%s/%s#%s", group, apiVersion, cfgStr( "operator.crd.xp.vhosts.kind" ) ), XpVHostResource.class );
        res.put( String.format( "%s/%s#%s", group, apiVersion, cfgStr( "operator.crd.xp.configs.kind" ) ), XpConfigResource.class );
        res.put( String.format( "%s/%s#%s", group, apiVersion, cfgStr( "operator.crd.xp.apps.kind" ) ), XpAppResource.class );

        return res;
    }
}
