package com.enonic.cloud.operator.v1alpha2xp7vhost;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Named;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;

import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.quarkus.runtime.StartupEvent;

import com.enonic.cloud.common.staller.RunnableStaller;
import com.enonic.cloud.kubernetes.caches.V1alpha2Xp7ConfigCache;
import com.enonic.cloud.kubernetes.caches.V1alpha2Xp7VHostCache;
import com.enonic.cloud.kubernetes.commands.K8sLogHelper;
import com.enonic.cloud.kubernetes.crd.client.CrdClient;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.config.ImmutableV1alpha2Xp7ConfigSpec;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.config.V1alpha2Xp7Config;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.vhost.V1alpha2Xp7VHost;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.vhost.V1alpha2Xp7VHostSpecMapping;

import static com.enonic.cloud.common.Configuration.cfgStr;


public class OperatorVHostConfig
    implements ResourceEventHandler<V1alpha2Xp7VHost>
{
    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    CrdClient crdClient;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    V1alpha2Xp7VHostCache v1alpha2Xp7VHostCache;

    @Inject
    V1alpha2Xp7ConfigCache v1alpha2Xp7ConfigCache;

    @Inject
    @Named("400ms")
    RunnableStaller runnableStaller;

    void onStartup( @Observes StartupEvent _ev )
    {
        v1alpha2Xp7VHostCache.addEventListener( this );
    }

    @Override
    public void onAdd( final V1alpha2Xp7VHost newResource )
    {
        handle( newResource.getMetadata().getNamespace() );
    }

    @Override
    public void onUpdate( final V1alpha2Xp7VHost oldResource, final V1alpha2Xp7VHost newResource )
    {
        if ( Objects.equals( oldResource.getSpec(), newResource.getSpec() ) )
        {
            return;
        }
        handle( newResource.getMetadata().getNamespace() );
    }

    @Override
    public void onDelete( final V1alpha2Xp7VHost oldResource, final boolean b )
    {
        handle( oldResource.getMetadata().getNamespace() );
    }

    private void handle( final String namespace )
    {
        // Trigger update on each vHost config in the namespace
        final String file = cfgStr( "operator.deployment.xp.config.vhosts.file" );
        v1alpha2Xp7ConfigCache.
            get( namespace ).
            filter( xp7config -> Objects.equals( xp7config.getSpec().file(), file ) ).
            forEach( vHostConfig -> runnableStaller.put( vHostConfig.getMetadata().getUid(), updateVHostConfig( vHostConfig ) ) );
    }

    private Runnable updateVHostConfig( final V1alpha2Xp7Config vHostConfig )
    {
        return () -> {
            final String nodeGroup = vHostConfig.getSpec().nodeGroup();

            List<V1alpha2Xp7VHost> vHosts = v1alpha2Xp7VHostCache.
                get( vHostConfig.getMetadata().getNamespace() ).
                filter( vHost -> filterRelevantVhost( nodeGroup, vHost ) ).
                collect( Collectors.toList() );

            final String data = buildVHostConfigData( nodeGroup, vHosts );

            v1alpha2Xp7ConfigCache.get( vHostConfig ).
                ifPresent( xp7Config -> {
                    if ( !Objects.equals( xp7Config.getSpec().data(), data ) )
                    {
                        K8sLogHelper.logDoneable( crdClient.
                            xp7Configs().
                            inNamespace( xp7Config.getMetadata().getNamespace() ).
                            withName( xp7Config.getMetadata().getName() ).
                            edit().
                            withSpec( ImmutableV1alpha2Xp7ConfigSpec.builder().
                                from( xp7Config.getSpec() ).
                                data( data ).
                                build() ) );
                    }
                } );
        };
    }

    private boolean filterRelevantVhost( final String nodegroup, final V1alpha2Xp7VHost vHost )
    {
        final String allNodes = cfgStr( "operator.helm.charts.Values.allNodesKey" );
        for ( V1alpha2Xp7VHostSpecMapping m : vHost.getSpec().mappings() )
        {
            if ( m.nodeGroup().equals( nodegroup ) || m.nodeGroup().equals( allNodes ) )
            {
                return true;
            }
        }
        return false;
    }

    private String buildVHostConfigData( final String nodeGroup, final List<V1alpha2Xp7VHost> vHosts )
    {
        final String allNodes = cfgStr( "operator.helm.charts.Values.allNodesKey" );

        StringBuilder sb = new StringBuilder( "enabled = true" );

        for ( V1alpha2Xp7VHost vHost : vHosts )
        {
            for ( V1alpha2Xp7VHostSpecMapping mapping : vHost.getSpec().mappings() )
            {
                if ( mapping.nodeGroup().equals( allNodes ) || mapping.nodeGroup().equals( nodeGroup ) )
                {
                    addVHostMapping( sb, vHost.getSpec().host(), mapping );
                }
            }
        }

        return sb.toString();
    }

    private void addVHostMapping( final StringBuilder sb, final String host, final V1alpha2Xp7VHostSpecMapping mapping )
    {
        String name = createName( host, mapping );

        sb.append( "\n\n" );
        sb.append( String.format( "mapping.%s.host=%s\n", name, host ) );
        sb.append( String.format( "mapping.%s.source=%s\n", name, mapping.source() ) );
        sb.append( String.format( "mapping.%s.target=%s", name, mapping.target() ) );
        if ( mapping.idProviders() != null )
        {
            sb.append( "\n" );
            sb.append( String.format( "mapping.%s.idProvider.%s=default", name, mapping.idProviders().defaultIdProvider() ) );
            mapping.idProviders().enabled().forEach( p -> {
                sb.append( "\n" );
                sb.append( String.format( "mapping.%s.idProvider.%s=enabled", name, mapping.idProviders().defaultIdProvider() ) );
            } );
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    private String createName( final String host, final V1alpha2Xp7VHostSpecMapping mapping )
    {
        return Hashing.sha512().hashString( host + mapping.source(), Charsets.UTF_8 ).toString().substring( 0, 10 );
    }
}
