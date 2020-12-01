package com.enonic.cloud.apis.xp;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

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

import com.enonic.cloud.apis.xp.service.AppEvent;
import com.enonic.cloud.apis.xp.service.AppInfo;
import com.enonic.cloud.apis.xp.service.AppInstallResponse;
import com.enonic.cloud.apis.xp.service.ImmutableAppInstallRequest;
import com.enonic.cloud.apis.xp.service.ImmutableAppKey;
import com.enonic.cloud.kubernetes.Clients;

import static com.enonic.cloud.common.Configuration.cfgStr;

@Singleton
public class XpClientCache
    extends CacheLoader<XpClientCacheKey, XpClient>
{
    private static final Logger log = LoggerFactory.getLogger( XpClientCache.class );

    private static final BaseEncoding baseEncoding = BaseEncoding.base64();

    private final KubernetesClient client;

    private final LoadingCache<XpClientCacheKey, XpClient> clientCreatorCache;

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

    private String defaultNodeGroup()
    {
        return cfgStr( "operator.charts.values.allNodesKey" );
    }

    @SuppressWarnings("WeakerAccess")
    public synchronized void addEventListener( final String namespace, final Consumer<AppEvent> eventConsumer )
    {
        getClient( XpClientCacheKeyImpl.of( namespace, defaultNodeGroup() ) ).appsSSE().addEventListener( eventConsumer );
    }

    @SuppressWarnings("WeakerAccess")
    public AppInfo install( final String namespace, final String url )
    {
        AppInstallResponse res = getClient( XpClientCacheKeyImpl.of( namespace, defaultNodeGroup() ) ).appsApi().appInstall(
            ImmutableAppInstallRequest.builder().url( url ).build() );
        if ( res.failure() == null )
        {
            log( namespace, "INSTALL app", res.applicationInstalledJson().application().key() );
        }
        else
        {
            throw new RuntimeException( res.failure() );
        }
        return res.applicationInstalledJson().application();
    }

    @SuppressWarnings("WeakerAccess")
    public void uninstall( final String namespace, final String key )
    {
        log( namespace, "UNINSTALL app", key );
        getClient( XpClientCacheKeyImpl.of( namespace, defaultNodeGroup() ) ).appsApi().appUninstall(
            ImmutableAppKey.builder().key( key ).build() );
    }

    public void start( final String namespace, final String key )
    {
        log( namespace, "START app", key );
        getClient( XpClientCacheKeyImpl.of( namespace, defaultNodeGroup() ) ).appsApi().appStart(
            ImmutableAppKey.builder().key( key ).build() );
    }

    public void stop( final String namespace, final String key )
    {
        log( namespace, "STOP app", key );
        getClient( XpClientCacheKeyImpl.of( namespace, defaultNodeGroup() ) ).appsApi().appStop(
            ImmutableAppKey.builder().key( key ).build() );
    }

    private void log( String namespace, String action, String key )
    {
        log.info( String.format( "XP: %s %s in NS '%s'", action, key, namespace ) );
    }

    private XpClient getClient( XpClientCacheKey key )
    {
        try
        {
            return clientCreatorCache.get( key );
        }
        catch ( ExecutionException e )
        {
            throw new RuntimeException( e );
        }
    }

    @Override
    public XpClient load( final XpClientCacheKey key )
    {
        Secret secret = client.secrets().
            inNamespace( key.namespace() ).
            withName( "su" ).
            get();
        Preconditions.checkState( secret != null, "Cannot find 'su' secret" );
        Preconditions.checkState( secret.getData().containsKey( "pass" ), "Cannot find key 'pass' in 'su' secret" );

        return ImmutableXpClient.builder().
            namespace( key.namespace() ).
            nodeGroup( key.nodeGroup() ).
            password( new String( baseEncoding.decode( secret.getData().get( "pass" ) ) ) ).
            build();
    }
}
