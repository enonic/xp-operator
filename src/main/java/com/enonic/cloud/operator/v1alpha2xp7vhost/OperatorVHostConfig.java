package com.enonic.cloud.operator.v1alpha2xp7vhost;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;

import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.quarkus.runtime.StartupEvent;

import com.enonic.cloud.common.staller.RunnableStaller;
import com.enonic.cloud.kubernetes.Clients;
import com.enonic.cloud.kubernetes.InformerSearcher;
import com.enonic.cloud.kubernetes.commands.K8sLogHelper;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7config.Xp7Config;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7vhost.Xp7VHost;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7vhost.Xp7VHostSpecMapping;
import com.enonic.cloud.operator.helpers.InformerEventHandler;

import static com.enonic.cloud.common.Configuration.cfgStr;


public class OperatorVHostConfig
    extends InformerEventHandler<Xp7VHost>
{
    @Inject
    Clients clients;

    @Inject
    SharedIndexInformer<Xp7VHost> xp7VHostSharedIndexInformer;

    @Inject
    InformerSearcher<Xp7VHost> xp7VHostInformerSearcher;

    @Inject
    InformerSearcher<Xp7Config> xp7ConfigInformerSearcher;

    @Inject
    @Named("400ms")
    RunnableStaller runnableStaller;

    @ConfigProperty(name = "operator.tasks.statusDelaySeconds")
    Long statusDelay;

    void onStartup( @Observes StartupEvent _ev )
    {
        listenToInformer( xp7VHostSharedIndexInformer );
    }

    @Override
    public void onAdd( final Xp7VHost newResource )
    {
        handle( newResource.getMetadata().getNamespace() );
    }

    @Override
    public void onUpdate( final Xp7VHost oldResource, final Xp7VHost newResource )
    {
        if ( Objects.equals( oldResource.getXp7VHostSpec(), newResource.getXp7VHostSpec() ) )
        {
            return;
        }
        handle( newResource.getMetadata().getNamespace() );
    }

    @Override
    public void onDelete( final Xp7VHost oldResource, final boolean b )
    {
        handle( oldResource.getMetadata().getNamespace() );
    }

    private void handle( final String namespace )
    {
        // Trigger update on each vHost config in the namespace
        final String file = cfgStr( "operator.deployment.xp.config.vhosts.file" );
        xp7ConfigInformerSearcher.
            get( namespace ).
            filter( xp7config -> Objects.equals( xp7config.getXp7ConfigSpec().getFile(), file ) ).
            forEach( vHostConfig -> runnableStaller.put( vHostConfig.getMetadata().getUid(), updateVHostConfig( vHostConfig ) ) );
    }

    private Runnable updateVHostConfig( final Xp7Config vHostConfig )
    {
        return () -> {
            final String nodeGroup = vHostConfig.getXp7ConfigSpec().getNodeGroup();

            List<Xp7VHost> vHosts = xp7VHostInformerSearcher.
                get( vHostConfig.getMetadata().getNamespace() ).
                filter( vHost -> filterRelevantVhost( nodeGroup, vHost ) ).
                collect( Collectors.toList() );

            final String data = buildVHostConfigData( nodeGroup, vHosts );

            xp7ConfigInformerSearcher.get( vHostConfig ).
                ifPresent( xp7Config -> {
                    if ( !Objects.equals( xp7Config.getXp7ConfigSpec().getData(), data ) )
                    {
                        K8sLogHelper.logDoneable( clients.
                            xp7Configs().crdClient().
                            inNamespace( xp7Config.getMetadata().getNamespace() ).
                            withName( xp7Config.getMetadata().getName() ).
                            edit().
                            withSpec( xp7Config.getXp7ConfigSpec().withData( data ) ) );
                    }
                } );
        };
    }

    private boolean filterRelevantVhost( final String nodegroup, final Xp7VHost vHost )
    {
        final String allNodes = cfgStr( "operator.helm.charts.Values.allNodesKey" );
        for ( Xp7VHostSpecMapping m : vHost.getXp7VHostSpec().getXp7VHostSpecMappings() )
        {
            if ( m.getNodeGroup().equals( nodegroup ) || m.getNodeGroup().equals( allNodes ) )
            {
                return true;
            }
        }
        return false;
    }

    private String buildVHostConfigData( final String nodeGroup, final List<Xp7VHost> vHosts )
    {
        final String allNodes = cfgStr( "operator.helm.charts.Values.allNodesKey" );

        StringBuilder sb = new StringBuilder( "enabled = true" );

        for ( Xp7VHost vHost : vHosts )
        {
            for ( Xp7VHostSpecMapping mapping : vHost.getXp7VHostSpec().getXp7VHostSpecMappings() )
            {
                if ( mapping.getNodeGroup().equals( allNodes ) || mapping.getNodeGroup().equals( nodeGroup ) )
                {
                    addVHostMapping( sb, vHost.getXp7VHostSpec().getHost(), mapping );
                }
            }
        }

        return sb.toString();
    }

    private void addVHostMapping( final StringBuilder sb, final String host, final Xp7VHostSpecMapping mapping )
    {
        String name = createName( host, mapping );

        sb.append( "\n\n" );
        sb.append( String.format( "mapping.%s.host=%s\n", name, host ) );
        sb.append( String.format( "mapping.%s.source=%s\n", name, mapping.getSource() ) );
        sb.append( String.format( "mapping.%s.target=%s", name, mapping.getTarget() ) );
        if ( mapping.getXp7VHostSpecMappingIdProvider() != null )
        {
            sb.append( "\n" );
            sb.append( String.format( "mapping.%s.idProvider.%s=default", name, mapping.getXp7VHostSpecMappingIdProvider().getDefault() ) );
            mapping.getXp7VHostSpecMappingIdProvider().getEnabled().forEach( p -> {
                sb.append( "\n" );
                sb.append( String.format( "mapping.%s.idProvider.%s=enabled", name, p ) );
            } );
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    private String createName( final String host, final Xp7VHostSpecMapping mapping )
    {
        return Hashing.sha256().hashString( host + mapping.getSource(), Charsets.UTF_8 ).toString().substring( 0, 10 );
    }
}
