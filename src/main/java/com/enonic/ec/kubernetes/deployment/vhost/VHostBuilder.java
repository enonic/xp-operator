package com.enonic.ec.kubernetes.deployment.vhost;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import javax.xml.bind.DatatypeConverter;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.common.Configuration;
import com.enonic.ec.kubernetes.deployment.xpdeployment.spec.Spec;
import com.enonic.ec.kubernetes.deployment.xpdeployment.spec.SpecNode;

@Value.Immutable
public abstract class VHostBuilder
    extends Configuration
{
    protected abstract Spec spec();

    @Value.Derived
    public List<VHost> vHosts()
    {
        Set<VHostConfig> vHostConfigs = extractVHostsAndPaths();
        List<VHost> VHosts = new LinkedList<>();
        for ( VHostConfig cfg : vHostConfigs )
        {

            ImmutableVHost.Builder builder = ImmutableVHost.builder().
                host( cfg.host ).
                vHostResourceName( vHostResourceName( spec(), cfg.host ) ).
                certificate( Optional.ofNullable( spec().vHostCertificates().get( cfg.host ) ) );

            for ( Map.Entry<String, Set<SpecNode>> e : cfg.paths.entrySet() )
            {
                builder.addVHostPaths( ImmutableVHostPath.builder().
                    nodes( e.getValue() ).
                    path( e.getKey() ).
                    pathResourceName( vHostPathResourceName( spec(), cfg.host, e.getKey() ) ).
                    vHostLabels( vHostPathLabels( cfg.host, e.getKey() ) ).
                    build() );
            }
            VHosts.add( builder.build() );
        }
        return VHosts;
    }

    private String vHostResourceName( Spec spec, String host )
    {
        return spec.defaultResourceNameWithPostFix( host.replace( ".", "-" ) );
    }

    private String vHostPathResourceName( Spec spec, String host, String path )
    {
        try
        {
            MessageDigest md = MessageDigest.getInstance( "MD5" );
            md.update( path.getBytes() );
            String hash = DatatypeConverter.printHexBinary( md.digest() );
            return spec.defaultResourceNameWithPostFix( host.replace( ".", "-" ), hash );
        }
        catch ( NoSuchAlgorithmException e )
        {
            throw new RuntimeException( e );
        }
    }

    private Map<String, String> vHostPathLabels( String host, String path )
    {
        String pathLabel = path.replace( "/", "_" );
        if ( pathLabel.equals( "_" ) )
        {
            pathLabel = "root";
        }
        else
        {
            pathLabel = pathLabel.substring( 1 );
        }
        Map<String, String> res = new HashMap<>();
        res.put( cfgStr( "operator.deployment.xp.vHost.label.vHost" ), host );
        res.put( cfgStr( "operator.deployment.xp.vHost.label.path" ), pathLabel );
        return res;
    }

    private static class VHostConfig
    {
        final String host;

        final Map<String, Set<SpecNode>> paths;

        VHostConfig( final String host )
        {
            this.host = host;
            this.paths = new HashMap<>();
        }
    }

    private Set<VHostConfig> extractVHostsAndPaths()
    {
        Map<String, VHostConfig> tmp = new HashMap<>();

        for ( SpecNode node : spec().nodes() )
        {
            Optional<Properties> p = node.config().getAsProperties( cfgStr( "operator.deployment.xp.vHost.configFile" ) );
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
                                Set<SpecNode> nodes = cfg.paths.getOrDefault( source.get(), new HashSet<>() );
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
