package com.enonic.ec.kubernetes.deployment.vhost;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.common.Configuration;
import com.enonic.ec.kubernetes.common.commands.Command;
import com.enonic.ec.kubernetes.deployment.xpdeployment.XpDeploymentResourceSpecNode;
import com.enonic.ec.kubernetes.deployment.xpdeployment.XpDeploymentResourceSpecVHostCertificate;

@Value.Immutable
public abstract class VHostBuilder
    extends Configuration
    implements Command<List<VHost>>
{

    protected abstract List<XpDeploymentResourceSpecNode> nodes();

    protected abstract Map<String, XpDeploymentResourceSpecVHostCertificate> certificates();

    @Override
    public List<VHost> execute()
    {
        Set<VHostConfig> vHostConfigs = extractVHostsAndPaths();
        List<VHost> VHosts = new LinkedList<>();
        for ( VHostConfig cfg : vHostConfigs )
        {
            ImmutableVHost.Builder builder = ImmutableVHost.builder().
                host( cfg.host ).
                certificate( Optional.ofNullable( certificates().get( cfg.host ) ) );

            for ( Map.Entry<String, Set<XpDeploymentResourceSpecNode>> e : cfg.paths.entrySet() )
            {
                builder.addVHostPaths( ImmutableVHostPath.builder().
                    nodes( e.getValue() ).
                    path( e.getKey() ).
                    build() );
            }
            VHosts.add( builder.build() );
        }
        return VHosts;
    }

    private static class VHostConfig
    {
        final String host;

        final Map<String, Set<XpDeploymentResourceSpecNode>> paths;

        VHostConfig( final String host )
        {
            this.host = host;
            this.paths = new HashMap<>();
        }
    }

    private Set<VHostConfig> extractVHostsAndPaths()
    {
        Map<String, VHostConfig> tmp = new HashMap<>();

        for ( XpDeploymentResourceSpecNode node : nodes() )
        {
            Optional<Properties> p = node.configAsProperties( cfgStr( "operator.deployment.xp.vHost.configFile" ) );
            if ( p.isPresent() )
            {
                if ( !p.get().containsKey( "enabled" ) || p.get().getProperty( "enabled" ).equals( "true" ) )
                {
                    Set<String> keys = p.get().stringPropertyNames();
                    for ( String key : keys )
                    {
                        if ( key.startsWith( "mapping." ) && key.endsWith( ".host" ) )
                        {
                            String host = p.get().getProperty( key );
                            Optional<String> source = Optional.ofNullable( p.get().getProperty( key.replace( ".host", ".source" ) ) );
                            if ( source.isPresent() )
                            {
                                VHostConfig cfg = tmp.getOrDefault( host, new VHostConfig( host ) );
                                Set<XpDeploymentResourceSpecNode> nodes = cfg.paths.getOrDefault( source.get(), new HashSet<>() );
                                nodes.add( node );
                                cfg.paths.put( source.get(), nodes );
                                tmp.put( host, cfg );
                            }
                        }
                    }
                }
            }
        }
        return new HashSet<>( tmp.values() );
    }
}
