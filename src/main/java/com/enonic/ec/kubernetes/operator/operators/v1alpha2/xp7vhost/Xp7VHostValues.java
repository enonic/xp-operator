package com.enonic.ec.kubernetes.operator.operators.v1alpha2.xp7vhost;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha2.vhost.V1alpha2Xp7VHost;
import com.enonic.ec.kubernetes.operator.helm.BaseValues;
import com.enonic.ec.kubernetes.operator.kubectl.newapply.mapping.ValueBuilder;
import com.enonic.ec.kubernetes.operator.operators.ResourceInfoNamespaced;
import com.enonic.ec.kubernetes.operator.operators.v1alpha2.xp7vhost.info.DiffXp7VHost;

import static com.enonic.ec.kubernetes.operator.common.Configuration.cfgStr;

@Value.Immutable
public abstract class Xp7VHostValues
    implements ValueBuilder
{
    protected abstract BaseValues baseValues();

    protected abstract ResourceInfoNamespaced<V1alpha2Xp7VHost, DiffXp7VHost> info();

    protected Object createValues( V1alpha2Xp7VHost resource )
    {
        Map<String, Object> values = new HashMap<>( baseValues() );

        Map<String, Object> vhost = new HashMap<>();
        vhost.put( "name", resource.getMetadata().getName() );
        vhost.put( "issuer", getIssuer( resource ) );
        vhost.put( "spec", resource.getSpec() );

        values.put( "vhost", vhost );
        return values;
    }

    private String getIssuer( V1alpha2Xp7VHost resource )
    {
        if ( resource.getSpec().certificate() == null || resource.getSpec().certificate().authority() == null )
        {
            return null;
        }
        switch ( resource.getSpec().certificate().authority() )
        {
            case SELF_SIGNED:
                return cfgStr( "operator.certissuer.selfsigned" );
            case LETS_ENCRYPT_STAGING:
                return cfgStr( "operator.certissuer.letsencrypt.staging" );
            case LETS_ENCRYPT_PROD:
                return cfgStr( "operator.certissuer.letsencrypt.prod" );
        }
        return null;
    }

    @Override
    public Optional<Object> buildOldValues()
    {
        if ( info().oldResource().isEmpty() )
        {
            return Optional.empty();
        }
        return Optional.of( createValues( info().oldResource().get() ) );
    }

    @Override
    public Optional<Object> buildNewValues()
    {
        if ( info().newResource().isEmpty() )
        {
            return Optional.empty();
        }
        return Optional.of( createValues( info().newResource().get() ) );
    }
}
