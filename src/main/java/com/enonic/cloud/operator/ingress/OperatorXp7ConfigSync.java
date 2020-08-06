package com.enonic.cloud.operator.ingress;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.networking.v1beta1.HTTPIngressPath;
import io.fabric8.kubernetes.api.model.networking.v1beta1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1beta1.IngressRule;

import com.enonic.cloud.kubernetes.Clients;
import com.enonic.cloud.kubernetes.Searchers;
import com.enonic.cloud.kubernetes.commands.K8sLogHelper;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7config.Xp7Config;
import com.enonic.cloud.operator.helpers.HandlerConfig;

import static com.enonic.cloud.common.Configuration.cfgStr;

/**
 * This operator class collects all annotated ingresses and creates the VHost configuration for XP
 */
@Singleton
public class OperatorXp7ConfigSync
    extends HandlerConfig<Xp7Config>
{
    private final Logger log = LoggerFactory.getLogger( OperatorXp7ConfigSync.class );

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
            forEach( xp7Config -> handle( namespace, xp7Config ) );
    }

    private void handle( final String namespace, final Xp7Config xp7Config )
    {
        // Create a list ['all', '<nodeGroup>']
        List<String> nodeGroups =
            Arrays.asList( cfgStr( "operator.helm.charts.Values.allNodesKey" ), xp7Config.getXp7ConfigSpec().getNodeGroup() );

        // Collect all relevant ingresses
        List<Ingress> ingresses = searchers.ingress().query().
            inNamespace( xp7Config.getMetadata().getNamespace() ).
            hasNotBeenDeleted().
            filter( ingress -> hasMappingsWithNodeGroups( ingress, nodeGroups ) ).
            list();

        // Create new data
        String data = createVHostData( namespace, ingresses, nodeGroups );

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

    private boolean hasMappingsWithNodeGroups( final Ingress ingress, final List<String> nodeGroups )
    {
        Set<Mapping> targetMappings = getAnnotationMappings( ingress );

        // No mappings
        if ( targetMappings.size() == 0 )
        {
            return false;
        }

        // No rules
        if ( ingress.getSpec().getRules() == null )
        {
            return false;
        }

        for ( IngressRule rule : ingress.getSpec().getRules() )
        {
            // No paths
            if ( rule.getHttp() == null || rule.getHttp().getPaths() == null )
            {
                return false;
            }

            for ( HTTPIngressPath path : rule.getHttp().getPaths() )
            {
                // No backend
                if ( path.getBackend() == null || path.getBackend().getServiceName() == null || path.getBackend().getServicePort() == null )
                {
                    return false;
                }

                // Get service and port
                String service = path.getBackend().getServiceName();
                String port = path.getBackend().getServicePort().getStrVal() != null
                    ? path.getBackend().getServicePort().getStrVal()
                    : path.getBackend().getServicePort().getIntVal().toString();

                // Only if service is the same as a nodegroup and port is 8080
                if ( nodeGroups.contains( service ) && port.equals( "8080" ) )
                {
                    return true;
                }
            }
        }

        // Return false by default
        return false;
    }

    private String createVHostData( final String namespace, final List<Ingress> ingresses, final List<String> nodeGroups )
    {
        // Create config string builder
        StringBuilder sb;
        Optional<ConfigMap> cm = searchers.configMap().query().
            inNamespace( namespace ).withName( "extra-config" ).
            get();

        // Try to use vhost defaults
        if ( cm.isPresent() && cm.get().getData().containsKey( "vhost-defaults.cfg" ) )
        {
            sb = new StringBuilder( cm.get().getData().get( "vhost-defaults.cfg" ) );
        }
        else
        {
            log.warn( "Could not find default vhost configuration" );
            sb = new StringBuilder( "enabled = true" );
        }

        // Iterate over all ingresses
        for ( Ingress ingress : ingresses )
        {
            addVHostMappings( sb, ingress, nodeGroups );
        }

        return sb.toString();
    }

    private void addVHostMappings( final StringBuilder sb, final Ingress ingress, final List<String> nodeGroups )
    {
        Set<Mapping> mappings = getAnnotationMappings( ingress );

        // Iterate over rules
        for ( IngressRule rule : ingress.getSpec().getRules() )
        {
            // Iterate over mappings
            for ( Mapping mapping : mappings )
            {
                // Set rule host if missing
                if ( mapping.host() == null )
                {
                    mapping = MappingImpl.copyOf( mapping ).withHost( rule.getHost() );
                }

                // If host matches
                if ( mapping.host().equals( rule.getHost() ) )
                {
                    // Iterate over paths
                    for ( HTTPIngressPath path : rule.getHttp().getPaths() )
                    {
                        // Path matches mapping
                        if ( nodeGroups.contains( path.getBackend().getServiceName() ) && mapping.source().equals( path.getPath() ) )
                        {
                            addVHostMappings( sb, mapping );
                        }
                    }
                }
            }
        }
    }

    private void addVHostMappings( final StringBuilder sb, final Mapping m )
    {
        sb.append( "\n\n" );
        sb.append( String.format( "mapping.%s.host=%s\n", m.name(), m.host() ) );
        sb.append( String.format( "mapping.%s.source=%s\n", m.name(), m.source() ) );
        sb.append( String.format( "mapping.%s.target=%s", m.name(), m.target() ) );
        if ( m.idProviders() != null )
        {
            String postFix = "default";
            for ( String idProvider : m.idProviders().split( "," ) )
            {
                sb.append( "\n" );
                sb.append( String.format( "mapping.%s.idProvider.%s=%s", m.name(), idProvider, postFix ) );
                postFix = "enabled";
            }
        }
    }

    private Set<Mapping> getAnnotationMappings( final Ingress ingress )
    {
        Set<Mapping> result = new HashSet<>();
        String prefix = cfgStr( "operator.annotations.xp7.vhosts" );
        Map<String, String> annotations = ingress.getMetadata().getAnnotations();

        // Collect all mapping prefixes
        Set<String> mPrefix = new HashSet<>();
        for ( Map.Entry<String, String> e : annotations.entrySet() )
        {
            if ( e.getKey().startsWith( prefix ) && e.getKey().endsWith( ".source" ) )
            {
                mPrefix.add( e.getKey().replace( ".source", "" ) );
            }
        }

        // Create mappings
        for ( String m : mPrefix )
        {
            Function<String, String> g = ( k ) -> annotations.get( m + "." + k );
            try
            {
                String name = m.replace( prefix, ingress.getMetadata().getName() + "-" );
                result.add( MappingImpl.of( name, g.apply( "host" ), g.apply( "source" ), g.apply( "target" ), g.apply( "idproviders" ) ) );
            }
            catch ( Exception e )
            {
                log.warn( String.format( "Invalid vhost mappings on ingress '%s'", ingress.getMetadata().getName() ) );
            }
        }

        return result;
    }
}
