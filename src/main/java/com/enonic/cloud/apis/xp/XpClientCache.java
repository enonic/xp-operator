package com.enonic.cloud.apis.xp;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.io.BaseEncoding;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;

import com.enonic.cloud.apis.xp.service.AdminApi;
import com.enonic.cloud.apis.xp.service.AppInfo;
import com.enonic.cloud.apis.xp.service.AppInstallRequest;
import com.enonic.cloud.apis.xp.service.AppInstallResponse;
import com.enonic.cloud.apis.xp.service.AppKeyList;
import com.enonic.cloud.kubernetes.Clients;

@Singleton
public class XpClientCache
    extends CacheLoader<String, XpClientCreator>
{
    private static final Logger log = LoggerFactory.getLogger( XpClientCache.class );

    private static final BaseEncoding baseEncoding = BaseEncoding.base64();

    private final KubernetesClient client;

    private final LoadingCache<String, XpClientCreator> clientCreatorCache;

    @Inject
    public XpClientCache( final Clients clients )
    {
        this.client = clients.k8s();
        clientCreatorCache = CacheBuilder.newBuilder().
            maximumSize( 1000 ).
            expireAfterWrite( 10, TimeUnit.MINUTES ).
            build( this );
    }

    public void invalidateCache( String namespace )
    {
        clientCreatorCache.invalidate( namespace );
    }

    @SuppressWarnings("WeakerAccess")
    public List<AppInfo> list( final String namespace )
    {
        return getAdminApi( namespace ).appList().applications();
    }

    @SuppressWarnings("WeakerAccess")
    public AppInfo install( final String namespace, final AppInstallRequest req )
    {
        AppInstallResponse res = getAdminApi( namespace ).appInstall( req );
        if ( res.failure() == null )
        {
            log( namespace, "INSTALL apps", Collections.
                singletonList( res.applicationInstalledJson().application().key() ) );
        }
        else
        {
            throw new RuntimeException( res.failure() );
        }
        return res.applicationInstalledJson().application();
    }

    @SuppressWarnings("WeakerAccess")
    public void uninstall( final String namespace, final AppKeyList req )
    {
        log( namespace, "UNINSTALL apps", req.key() );
        getAdminApi( namespace ).appUninstall( req );
    }

    public void start( final String namespace, final AppKeyList req )
    {
        log( namespace, "START apps", req.key() );
        getAdminApi( namespace ).appStart( req );
    }

    public void stop( final String namespace, final AppKeyList req )
    {
        log( namespace, "STOP apps", req.key() );
        getAdminApi( namespace ).appStop( req );
    }

    private void log( String namespace, String action, List<String> keys )
    {
        log.info( String.format( "XP: %s %s in NS '%s'", action, keys, namespace ) );
    }

    private AdminApi getAdminApi( String namespace )
    {
        try
        {
            return clientCreatorCache.get( namespace ).adminApi();
        }
        catch ( ExecutionException e )
        {
            throw new RuntimeException( e );
        }
    }

    @Override
    public XpClientCreator load( final String namespace )
    {
        Secret secret = client.secrets().
            inNamespace( namespace ).
            withName( "su" ).
            get();
        Preconditions.checkState( secret != null, "Cannot find 'su' secret" );
        Preconditions.checkState( secret.getData().containsKey( "pass" ), "Cannot find key 'pass' in 'su' secret" );

        return ImmutableXpClientCreator.builder().
            namespace( namespace ).
            password( new String( baseEncoding.decode( secret.getData().get( "pass" ) ) ) ).
            build();
    }
}
