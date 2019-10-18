package com.enonic.ec.kubernetes.operator.commands;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wildfly.common.annotation.Nullable;

import com.enonic.ec.kubernetes.common.commands.Command;
import com.enonic.ec.kubernetes.deployment.xpdeployment.XpDeploymentResourceSpecNode;
import com.enonic.ec.kubernetes.deployment.xpdeployment.XpDeploymentResourceSpecVhostCertificate;

@Value.Immutable
public abstract class VhostBuilder
    implements Command<Vhosts>
{

    private final static Logger log = LoggerFactory.getLogger( VhostBuilder.class );

    protected abstract List<XpDeploymentResourceSpecNode> nodes();

    @Nullable
    protected abstract Map<String, XpDeploymentResourceSpecVhostCertificate> certificates();

    @Override
    public Vhosts execute()
    {
        Set<VhostConfig> vhostConfigs = extractVhostsAndPaths();
        Vhosts vhost = new Vhosts();
        for ( VhostConfig cfg : vhostConfigs )
        {
            ImmutableVhost.Builder builder = ImmutableVhost.builder().
                host( cfg.host ).
                certificate( certificates() != null ? certificates().get( cfg.host ) : null );

            for ( Map.Entry<String, Set<XpDeploymentResourceSpecNode>> e : cfg.paths.entrySet() )
            {
                builder.addVhostPaths( ImmutableVhostPath.builder().
                    nodes( e.getValue() ).
                    path( e.getKey() ).
                    build() );
            }
            vhost.add( builder.build() );
        }
        return vhost;
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
            Properties p = node.configAsProperties( "com.enonic.xp.web.vhost.cfg" );
            if ( p != null )
            {
                if ( !p.containsKey( "enabled" ) || p.getProperty( "enabled" ).equals( "true" ) )
                {
                    Set<String> keys = p.stringPropertyNames();
                    for ( String key : keys )
                    {
                        if ( key.startsWith( "mapping." ) && key.endsWith( ".host" ) )
                        {
                            String host = p.getProperty( key );
                            String source = p.getProperty( key.replace( ".host", ".source" ) );
                            if ( source != null )
                            {
                                VhostConfig cfg = tmp.getOrDefault( host, new VhostConfig( host ) );
                                Set<XpDeploymentResourceSpecNode> nodes = cfg.paths.getOrDefault( source, new HashSet<>() );
                                nodes.add( node );
                                cfg.paths.put( source, nodes );
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
