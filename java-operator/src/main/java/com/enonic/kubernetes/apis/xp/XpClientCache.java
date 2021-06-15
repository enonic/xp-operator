package com.enonic.kubernetes.apis.xp;

import com.enonic.kubernetes.apis.xp.service.AppEvent;
import com.enonic.kubernetes.apis.xp.service.AppInfo;
import com.enonic.kubernetes.apis.xp.service.AppInstallResponse;
import com.enonic.kubernetes.apis.xp.service.ImmutableAppInstallRequest;
import com.enonic.kubernetes.apis.xp.service.ImmutableAppKey;
import com.enonic.kubernetes.kubernetes.Clients;
import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.io.BaseEncoding;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.enonic.kubernetes.common.Configuration.cfgLong;
import static com.enonic.kubernetes.common.SingletonAssert.singletonAssert;

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
        singletonAssert( this, "constructor" );
        this.client = clients.k8s();
        clientCreatorCache = CacheBuilder.newBuilder().
            maximumSize( 1000 ).
            expireAfterWrite( 10, TimeUnit.MINUTES ).
            build( this );
    }

    @Override
    public XpClient load( final XpClientCacheKey key )
        throws XpClientException
    {
        log.debug( String.format( "XpClientCache load: %s", key ) );
        Secret secret = client.secrets().
            inNamespace( key.namespace() ).
            withName( "su" ).
            get();
        Preconditions.checkState( secret != null, "Cannot find 'su' secret" );
        Preconditions.checkState( secret.getData().containsKey( "pass" ), "Cannot find key 'pass' in 'su' secret" );

        XpClient client = new XpClient(
            XpClientParamsImpl.builder()
                .namespace( key.namespace() )
                .nodeGroup( key.nodeGroup() )
                .username( "su" )
                .password( new String( baseEncoding.decode( secret.getData().get( "pass" ) ) ) )
                .timeout( cfgLong( "operator.deployment.xp.management.timeout" ) )
                .build(),
            () -> this.invalidate( key )
        );

        log.debug( String.format( "XpClientCache load waiting for SSE connection: %s", key ) );
        client.waitForConnection( cfgLong( "operator.deployment.xp.management.timeout" ) );
        log.debug( String.format( "XpClientCache load successful: %s", key ) );

        return client;
    }

    private synchronized void invalidate( final XpClientCacheKey key )
    {
        log.debug( String.format( "XpClientCache invalidate: %s", key ) );
        clientCreatorCache.invalidate( key );
    }

    private String defaultNodeGroup()
    {
        return "all";
    }

    private void log( String namespace, String action, String key )
    {
        log.info( String.format( "XP: %s %s in NS '%s'", action, key, namespace ) );
    }

    private synchronized XpClient getClient( XpClientCacheKey key )
        throws XpClientException
    {
        try {
            return clientCreatorCache.get( key );
        } catch (ExecutionException e) {
            if (e.getCause() != null && e.getCause() instanceof IOException) {
                throw (XpClientException) e.getCause();
            } else {
                throw new RuntimeException( e );
            }
        }
    }

    public void appAddListener( final String namespace, final Consumer<AppEvent> eventConsumer )
        throws XpClientException
    {
        getClient( XpClientCacheKeyImpl.of( namespace, defaultNodeGroup() ) ).addEventListener( eventConsumer );
    }

    @SuppressWarnings("WeakerAccess")
    public AppInfo appInstall( final String namespace, final String url, final String sha512 )
        throws XpClientException
    {
        AppInstallResponse res = getClient( XpClientCacheKeyImpl.of( namespace, defaultNodeGroup() ) ).appInstall(
            ImmutableAppInstallRequest.builder().url( url ).sha512( sha512 ).build() );
        if (res.failure() == null) {
            log( namespace, "INSTALL app", res.applicationInstalledJson().application().key() );
        } else {
            throw new XpClientException( res.failure() );
        }
        return res.applicationInstalledJson().application();
    }

    @SuppressWarnings("WeakerAccess")
    public void appUninstall( final String namespace, final String key )
        throws XpClientException
    {
        log( namespace, "UNINSTALL app", key );
        getClient( XpClientCacheKeyImpl.of( namespace, defaultNodeGroup() ) ).appUninstall(
            ImmutableAppKey.builder().key( key ).build() );
    }

    public void appStart( final String namespace, final String key )
        throws XpClientException
    {
        log( namespace, "START app", key );
        getClient( XpClientCacheKeyImpl.of( namespace, defaultNodeGroup() ) ).appStart(
            ImmutableAppKey.builder().key( key ).build() );
    }

    public void appStop( final String namespace, final String key )
        throws XpClientException
    {
        log( namespace, "STOP app", key );
        getClient( XpClientCacheKeyImpl.of( namespace, defaultNodeGroup() ) ).appStop(
            ImmutableAppKey.builder().key( key ).build() );
    }

    public List<AppInfo> appList( final String namespace )
        throws XpClientException
    {
        return getClient( XpClientCacheKeyImpl.of( namespace, defaultNodeGroup() ) ).appList();
    }

    public Optional<AppInfo> appInfo( final String namespace, final String key )
        throws XpClientException
    {
        return appList( namespace ).stream().filter( appInfo -> appInfo.key().equals( key ) ).findFirst();
    }

    public void closeClients( final String namespace )
    {
        List<Map.Entry<XpClientCacheKey, XpClient>> clients = clientCreatorCache.asMap().entrySet().stream().
            filter( e -> e.getKey().namespace().equals( namespace ) ).
            collect( Collectors.toList() );

        clients.forEach( e -> e.getValue().closeClient( null ) );
    }
}
