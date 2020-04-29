package com.enonic.cloud.operator.operators.v1alpha2.xp7vhost;


import org.immutables.value.Value;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;

import com.enonic.cloud.operator.crd.xp7.v1alpha2.vhost.V1alpha2Xp7VHost;
import com.enonic.cloud.operator.crd.xp7.v1alpha2.vhost.V1alpha2Xp7VHostSpecMapping;
import com.enonic.cloud.operator.operators.common.cache.Caches;
import com.enonic.cloud.operator.operators.v1alpha2.xp7config.Xp7ConfigAggregator;

import static com.enonic.cloud.operator.common.Configuration.cfgStr;

@Value.Immutable
public abstract class Xp7VHostAggregator
    extends Xp7ConfigAggregator
{
    protected abstract Caches caches();

    @Override
    protected String file()
    {
        return cfgStr( "operator.deployment.xp.config.vhosts.file" );
    }

    @Override
    protected String data()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "enabled = true" );

        // Get all vHosts in namespace
        caches().getVHostCache().
            getByNamespace( metadata().getNamespace() ).
            // Iterate over mappings
                forEach( vHost -> vHost.getSpec().mappings().forEach( mapping -> {
                // Add all mappings that match the nodeGroup
                if ( matchNodeGroup( nodeGroup(), mapping.nodeGroup() ) )
                {
                    String name = createName( vHost, mapping );

                    sb.append( "\n\n" );
                    sb.append( String.format( "mapping.%s.host=%s\n", name, vHost.getSpec().host() ) );
                    sb.append( String.format( "mapping.%s.source=%s\n", name, mapping.source() ) );
                    sb.append( String.format( "mapping.%s.target=%s", name, mapping.target() ) );
                    if ( mapping.idProviders() != null )
                    {
                        sb.append( "\n" );
                        sb.append( String.format( "mapping.%s.idProvider.%s=default", name, mapping.idProviders().defaultIdProvider() ) );
                        mapping.idProviders().enabled().forEach( p -> {
                            sb.append( "\n" );
                            sb.append(
                                String.format( "mapping.%s.idProvider.%s=enabled", name, mapping.idProviders().defaultIdProvider() ) );
                        } );
                    }
                }
            } ) );
        return sb.toString();
    }

    @SuppressWarnings("UnstableApiUsage")
    private String createName( final V1alpha2Xp7VHost vHost, final V1alpha2Xp7VHostSpecMapping mapping )
    {
        return Hashing.sha512().hashString( vHost.getSpec().host() + mapping.source(), Charsets.UTF_8 ).toString().substring( 0, 10 );
    }
}
