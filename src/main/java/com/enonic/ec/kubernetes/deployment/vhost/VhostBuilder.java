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

import com.enonic.ec.kubernetes.common.commands.Command;
import com.enonic.ec.kubernetes.deployment.xpdeployment.XpDeploymentResourceSpecNode;
import com.enonic.ec.kubernetes.deployment.xpdeployment.XpDeploymentResourceSpecVhostCertificate;

@Value.Immutable
public abstract class VhostBuilder
    implements Command<List<Vhost>>
{

    protected abstract List<XpDeploymentResourceSpecNode> nodes();

    protected abstract Map<String, XpDeploymentResourceSpecVhostCertificate> certificates();

    @Override
    public List<Vhost> execute()
    {
        Set<VhostConfig> vhostConfigs = extractVhostsAndPaths();
        List<Vhost> vhosts = new LinkedList<>();
        for ( VhostConfig cfg : vhostConfigs )
        {
            ImmutableVhost.Builder builder = ImmutableVhost.builder().
                host( cfg.host ).
                certificate( Optional.ofNullable( certificates().get( cfg.host ) ) );

            for ( Map.Entry<String, Set<XpDeploymentResourceSpecNode>> e : cfg.paths.entrySet() )
            {
                builder.addVhostPaths( ImmutableVhostPath.builder().
                    nodes( e.getValue() ).
                    path( e.getKey() ).
                    build() );
            }
            vhosts.add( builder.build() );
        }
        return vhosts;
    }

    private static class VhostConfig
    {
        String host;

        Map<String, Set<XpDeploymentResourceSpecNode>> paths;

        public VhostConfig( final String host )
        {
            this.host = host;
            this.paths = new HashMap<>();
        }
    }

    private Set<VhostConfig> extractVhostsAndPaths()
    {
        Map<String, VhostConfig> tmp = new HashMap<>();

        for ( XpDeploymentResourceSpecNode node : nodes() )
        {
            Optional<Properties> p = node.configAsProperties( "com.enonic.xp.web.vhost.cfg" );
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
                                VhostConfig cfg = tmp.getOrDefault( host, new VhostConfig( host ) );
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
        Set<VhostConfig> res = new HashSet<>();
        tmp.values().stream().forEach( c -> res.add( c ) );
        return res;
    }
}
