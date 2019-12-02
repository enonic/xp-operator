package com.enonic.ec.kubernetes.operator.vhosts;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.crd.vhost.diff.DiffSpec;
import com.enonic.ec.kubernetes.crd.vhost.spec.ImmutableSpec;
import com.enonic.ec.kubernetes.crd.vhost.spec.Spec;
import com.enonic.ec.kubernetes.crd.vhost.spec.SpecMapping;
import com.enonic.ec.kubernetes.operator.deployments.config.ConfigBuilder;

@Value.Immutable
public abstract class ConfigBuilderVHost
    extends ConfigBuilder
{
    protected abstract String node();

    protected abstract List<DiffSpec> diffs();

    public Map<String, String> create()
    {
        String vHostConfigFile = cfgStr( "operator.deployment.xp.vHost.configFile" );
        Map<String, String> newConfig = new HashMap<>( baseConfig() );
        newConfig.remove( vHostConfigFile );

        setIfNotSet( newConfig, vHostConfigFile, setFunc -> {
            for ( DiffSpec diff : diffs() )
            {
                if ( !diff.shouldAddOrModify() )
                {
                    continue;
                }

                Optional<Spec> relevant = relevantSpec( diff.newValue().get() );

                if ( relevant.isEmpty() )
                {
                    continue;
                }

                relevant.get().mappings().forEach( m -> {
                    String host = relevant.get().host();
                    String name = m.name( host );
                    setFunc.apply( String.format( "mapping.%s.host", name ), host );
                    setFunc.apply( String.format( "mapping.%s.source", name ), m.source() );
                    setFunc.apply( String.format( "mapping.%s.target", name ), m.target() );
                    if ( m.idProvider() != null )
                    {
                        setFunc.apply( String.format( "mapping.%s.idProvider.%s", name, m.idProvider() ), "default" );
                    }
                } );

                // Enable vHosts since we added some vHosts
                setFunc.apply( "enabled", true );
            }

            // Set disable vHosts if nothing has been done
            setFunc.apply( "enabled", false );
        } );

        return newConfig;
    }

    private Optional<Spec> relevantSpec( Spec spec )
    {
        List<SpecMapping> relevantMappings =
            spec.mappings().stream().filter( m -> m.node().equals( node() ) ).collect( Collectors.toList() );
        if ( relevantMappings.size() < 1 )
        {
            return Optional.empty();
        }
        return Optional.of( ImmutableSpec.builder().from( spec ).mappings( relevantMappings ).build() );
    }
}
