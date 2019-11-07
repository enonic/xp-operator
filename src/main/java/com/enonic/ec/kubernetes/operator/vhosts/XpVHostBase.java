package com.enonic.ec.kubernetes.operator.vhosts;

import java.util.List;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.ConfigMap;

import com.enonic.ec.kubernetes.common.Configuration;
import com.enonic.ec.kubernetes.crd.vhost.XpVHostResource;
import com.enonic.ec.kubernetes.crd.vhost.spec.SpecMapping;

public abstract class XpVHostBase
    extends Configuration
{
    protected String configMapNodeAlias( final ConfigMap configMap )
    {
        return configMap.getMetadata().
            getLabels().
            get( cfgStrFmt( "operator.deployment.xp.pod.label.aliasPrefix", "main" ) ); // TODO: FIX
    }

    protected List<SpecMapping> matchMappings( final ConfigMap configMap, XpVHostResource vHost )
    {
        return vHost.getSpec().
            mappings().
            stream().filter( m -> m.nodeAlias().equals( configMapNodeAlias( configMap ) ) ).collect( Collectors.toList() );
    }
}
