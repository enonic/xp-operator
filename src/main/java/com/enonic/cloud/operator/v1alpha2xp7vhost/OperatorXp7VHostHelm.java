package com.enonic.cloud.operator.v1alpha2xp7vhost;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.enonic.cloud.helm.functions.Templator;
import com.enonic.cloud.helm.values.BaseValues;
import com.enonic.cloud.helm.values.MapValues;
import com.enonic.cloud.helm.values.ValueBuilder;
import com.enonic.cloud.helm.values.Values;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7vhost.Xp7VHost;
import com.enonic.cloud.operator.helpers.HandlerHelm;

import static com.enonic.cloud.common.Configuration.cfgStr;
import static com.enonic.cloud.common.Utils.createOwnerReference;
import static com.enonic.cloud.kubernetes.client.Utils.cloneResource;

@Singleton
public class OperatorXp7VHostHelm
    extends HandlerHelm<Xp7VHost>
{
    @Inject
    @Named("v1alpha2/xp7vhost")
    Templator templator;

    @Override
    protected ValueBuilder<Xp7VHost> getValueBuilder( final BaseValues baseValues )
    {
        return new Xp7VHostValueBuilder( baseValues );
    }

    @Override
    protected Templator getTemplator()
    {
        return templator;
    }

    @Override
    protected boolean specEquals( final Xp7VHost oldResource, final Xp7VHost newResource )
    {
        return oldResource.getXp7VHostSpec().equals( newResource.getXp7VHostSpec() );
    }

    public static class Xp7VHostValueBuilder
        implements ValueBuilder<Xp7VHost>
    {

        private final BaseValues baseValues;

        public Xp7VHostValueBuilder( final BaseValues baseValues )
        {
            this.baseValues = baseValues;
        }

        @Override
        public Values apply( final Xp7VHost in )
        {
            Xp7VHost resource = cloneResource( in );
            MapValues values = new MapValues( baseValues );

            Map<String, Object> vhost = new HashMap<>();
            vhost.put( "uid", resource.getMetadata().getUid() );
            vhost.put( "name", resource.getMetadata().getName() );
            vhost.put( "namespace", resource.getMetadata().getNamespace() );
            vhost.put( "issuer", getIssuer( resource ) );
            vhost.put( "spec", resource.getXp7VHostSpec() );

            values.put( "vhost", vhost );
            values.put( "ownerReferences", Collections.singletonList( createOwnerReference( resource ) ) );
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
                    return cfgStr( "operator.cert.issuer.selfSigned" );
                case LETS_ENCRYPT_STAGING:
                    return cfgStr( "operator.cert.issuer.letsEncrypt.staging" );
                case LETS_ENCRYPT:
                    return cfgStr( "operator.cert.issuer.letsEncrypt.prod" );
                case CLUSTER_ISSUER:
                    return resource.getXp7VHostSpec().getXp7VHostSpecCertificate().getIdentifier();
            }
            return null;
        }
    }
}
