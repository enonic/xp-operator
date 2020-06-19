package com.enonic.cloud.operator.v1alpha2xp7vhost;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;

import com.enonic.cloud.kubernetes.Clients;
import com.enonic.cloud.kubernetes.Searchers;
import com.enonic.cloud.kubernetes.commands.K8sLogHelper;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7config.Xp7Config;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7vhost.Xp7VHost;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7vhost.Xp7VHostSpecMapping;
import com.enonic.cloud.operator.helpers.HandlerConfig;

import static com.enonic.cloud.common.Configuration.cfgStr;

@Singleton
public class OperatorXp7ConfigSync
    extends HandlerConfig<Xp7Config>
{
    @Inject
    Clients clients;

    @Inject
    Searchers searchers;

    @Override
    protected Stream<Xp7Config> resourceStream()
    {
        return searchers.xp7Config().query().stream();
    }

    @Override
    public void handle( final String namespace )
    {
        // Trigger update on each vHost config in the namespace
        final String file = cfgStr( "operator.deployment.xp.config.vhosts.file" );
        searchers.xp7Config().query().
            inNamespace( namespace ).
            hasNotBeenDeleted().
            filter( xp7config -> Objects.equals( xp7config.getXp7ConfigSpec().getFile(), file ) ).
            forEach( this::handle );
    }

    private void handle( final Xp7Config xp7Config )
    {
        // Create a list ['all', '<nodeGroup>']
        List<String> nodeGroups =
            Arrays.asList( cfgStr( "operator.helm.charts.Values.allNodesKey" ), xp7Config.getXp7ConfigSpec().getNodeGroup() );

        // Collect all relevant vHosts
        List<Xp7VHost> vHosts = searchers.xp7VHost().query().
            inNamespace( xp7Config.getMetadata().getNamespace() ).
            hasNotBeenDeleted().
            filter( vHost -> hasMappingsWithNodeGroups( vHost, nodeGroups ) ).
            list();

        // Create new data
        String data = createVHostData( vHosts, nodeGroups );

        // Update if needed
        if ( !Objects.equals( xp7Config.getXp7ConfigSpec().getData(), data ) )
        {
            K8sLogHelper.logDoneable( clients.
                xp7Configs().crdClient().
                inNamespace( xp7Config.getMetadata().getNamespace() ).
                withName( xp7Config.getMetadata().getName() ).
                edit().
                withSpecData( data ) );
        }
    }

    private boolean hasMappingsWithNodeGroups( final Xp7VHost vHost, final List<String> nodeGroups )
    {
        for ( Xp7VHostSpecMapping m : vHost.getXp7VHostSpec().getXp7VHostSpecMappings() )
        {
            if ( mappingMatchesNodeGroups( m, nodeGroups ) )
            {
                return true;
            }
        }
        return false;
    }

    private String createVHostData( final List<Xp7VHost> vHosts, final List<String> nodeGroups )
    {
        StringBuilder sb = new StringBuilder( "enabled = true" );

        for ( Xp7VHost vHost : vHosts )
        {
            for ( Xp7VHostSpecMapping m : vHost.getXp7VHostSpec().getXp7VHostSpecMappings() )
            {
                if ( mappingMatchesNodeGroups( m, nodeGroups ) )
                {
                    addVHostMapping( sb, vHost.getXp7VHostSpec().getHost(), m );
                }
            }
        }

        return sb.toString();
    }

    private boolean mappingMatchesNodeGroups( final Xp7VHostSpecMapping mapping, final List<String> nodeGroups )
    {
        return nodeGroups.contains( mapping.getNodeGroup() );
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
