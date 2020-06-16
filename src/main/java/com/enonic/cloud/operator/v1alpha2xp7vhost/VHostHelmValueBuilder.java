package com.enonic.cloud.operator.v1alpha2xp7vhost;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.immutables.value.Value;

import com.enonic.cloud.common.annotations.Params;
import com.enonic.cloud.helm.values.BaseValues;
import com.enonic.cloud.helm.values.MapValues;
import com.enonic.cloud.helm.values.ValueBuilder;
import com.enonic.cloud.helm.values.Values;
import com.enonic.cloud.kubernetes.client.v1alpha2.xp7vhost.Xp7VHostCrd;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7vhost.Xp7VHost;

import static com.enonic.cloud.common.Configuration.cfgStr;

@Value.Immutable
@Params
public abstract class VHostHelmValueBuilder
    implements ValueBuilder<Xp7VHost>
{
    protected abstract BaseValues baseValues();

    @Override
    public Values apply( final Xp7VHost in )
    {
        Xp7VHost resource = Xp7VHostCrd.withDefaultValues( in );
        MapValues values = new MapValues( baseValues() );

        Map<String, Object> vhost = new HashMap<>();
        vhost.put( "uid", resource.getMetadata().getUid() );
        vhost.put( "name", resource.getMetadata().getName() );
        vhost.put( "namespace", resource.getMetadata().getNamespace() );
        vhost.put( "issuer", getIssuer( resource ) );
        vhost.put( "spec", resource.getXp7VHostSpec() );

        values.put( "vhost", vhost );
        values.put( "defaultLabels", defaultLabels( resource ) );
        return values;
    }

    private Object defaultLabels( final Xp7VHost resource )
    {
        if ( resource.getMetadata().getLabels() == null )
        {
            return Collections.emptyMap();
        }
        return resource.getMetadata().getLabels();
    }

    private String getIssuer( Xp7VHost resource )
    {
        if ( resource.getXp7VHostSpec().getXp7VHostSpecCertificate() == null ||
            resource.getXp7VHostSpec().getXp7VHostSpecCertificate().getAuthority() == null )
        {
            return null;
        }
        switch ( resource.getXp7VHostSpec().getXp7VHostSpecCertificate().getAuthority() )
        {
            case SELF_SIGNED:
                return cfgStr( "operator.certissuer.selfsigned" );
            case LETS_ENCRYPT_STAGING:
                return cfgStr( "operator.certissuer.letsencrypt.staging" );
            case LETS_ENCRYPT:
                return cfgStr( "operator.certissuer.letsencrypt.prod" );
            case CLUSTER_ISSUER:
                return resource.getXp7VHostSpec().getXp7VHostSpecCertificate().getIdentifier();
        }
        return null;
    }
}
