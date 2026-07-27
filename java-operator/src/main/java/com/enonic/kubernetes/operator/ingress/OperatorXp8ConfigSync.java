package com.enonic.kubernetes.operator.ingress;

import com.enonic.kubernetes.crd.v1.Xp8Config;
import com.enonic.kubernetes.kubernetes.Clients;
import com.enonic.kubernetes.kubernetes.Searchers;
import com.enonic.kubernetes.kubernetes.commands.K8sLogHelper;
import com.enonic.kubernetes.operator.Operator;
import com.google.common.base.Function;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.networking.v1.HTTPIngressPath;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.IngressRule;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.runtime.server.event.ServerStartupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.enonic.kubernetes.common.Configuration.cfgLong;
import static com.enonic.kubernetes.common.Configuration.cfgStr;
import static com.enonic.kubernetes.kubernetes.Predicates.inNamespace;
import static com.enonic.kubernetes.kubernetes.Predicates.inSameNamespaceAs;
import static com.enonic.kubernetes.kubernetes.Predicates.isDeleted;
import static com.enonic.kubernetes.kubernetes.Predicates.withName;
import static java.util.stream.Collectors.groupingBy;

/**
 * This operator class collects all annotated ingresses and creates the VHost configuration for XP
 */
@Singleton
public class OperatorXp8ConfigSync
    implements Runnable, ApplicationEventListener<ServerStartupEvent>
{
    private static final Logger log = LoggerFactory.getLogger( OperatorXp8ConfigSync.class );

    @Inject
    Operator operator;

    @Inject
    Clients clients;

    @Inject
    Searchers searchers;

    @Override
    public void onApplicationEvent( ServerStartupEvent event )
    {
        operator.schedule( cfgLong( "operator.tasks.sync.interval" ), this );
    }

    @Override
    public void run()
    {
        searchers.xp8Config().stream().collect( groupingBy( r -> r.getMetadata().getNamespace() ) ).
            keySet().
            forEach( this::handle );
    }

    public static Set<Mapping> getAnnotationMappings( final Ingress ingress )
    {
        Set<Mapping> result = new HashSet<>();
        String prefix = cfgStr( "operator.charts.values.annotationKeys.vhostMapping" );
        Map<String, String> annotations = ingress.getMetadata().getAnnotations();

        // Collect all mapping prefixes
        Set<String> mPrefix = new HashSet<>();
        for (Map.Entry<String, String> e : annotations.entrySet()) {
            if (e.getKey().startsWith( prefix ) && e.getKey().endsWith( ".source" )) {
                mPrefix.add( e.getKey().replace( ".source", "" ) );
            }
        }

        // Create mappings
        for (String m : mPrefix) {
            Function<String, String> g = ( k ) -> annotations.get( m + "." + k );
            try {
                String name = m.replace( prefix, ingress.getMetadata().getName() + "-" );
                result.add( MappingImpl.of( name, g.apply( "host" ), g.apply( "source" ), g.apply( "target" ), g.apply( "idproviders" ) ) );
            } catch (Exception e) {
                log.warn( String.format( "Invalid vhost mappings on ingress '%s'", ingress.getMetadata().getName() ) );
            }
        }

        return result;
    }

    public void handle( final String namespace )
    {
        // Trigger update on each vHost config in the namespace
        final String file = cfgStr( "operator.charts.values.files.vhosts" );
        searchers.xp8Config().stream().
            filter( inNamespace( namespace ) ).
            filter( isDeleted().negate() ).
            filter( xp8config -> Objects.equals( xp8config.getSpec().getFile(), file ) ).
            forEach( xp8Config -> handle( namespace, xp8Config ) );
    }

    private void handle( final String namespace, final Xp8Config xp8Config )
    {
        // Create a list ['all', '<nodeGroup>']
        List<String> nodeGroups = Arrays.asList( cfgStr( "operator.charts.values.allNodesKey" ), xp8Config.getSpec().getNodeGroup() );

        // Collect all relevant ingresses
        List<Ingress> ingresses = searchers.ingress().stream().
            filter( inSameNamespaceAs( xp8Config ) ).
            filter( isDeleted().negate() ).
            filter( ingress -> hasMappingsWithNodeGroups( ingress, nodeGroups ) ).
            collect( Collectors.toList() );

        // Create new data
        String data = createVHostData( namespace, ingresses, nodeGroups );

        // Update if needed
        if (!Objects.equals( xp8Config.getSpec().getData(), data )) {
            log.debug("Syncing vhost ConfigMap: {} in {}", xp8Config.getMetadata().getName(), xp8Config.getMetadata().getNamespace());

            K8sLogHelper.logEdit( clients.xp8Configs().
                inNamespace( xp8Config.getMetadata().getNamespace() ).
                withName( xp8Config.getMetadata().getName() ), c -> {
                c.getSpec().setData( data );
                return c;
            } );
        }
    }

    private boolean hasMappingsWithNodeGroups( final Ingress ingress, final List<String> nodeGroups )
    {
        Set<Mapping> targetMappings = getAnnotationMappings( ingress );

        // No mappings
        if (targetMappings.isEmpty()) {
            return false;
        }

        // No rules
        if (ingress.getSpec().getRules() == null) {
            return false;
        }

        for (IngressRule rule : ingress.getSpec().getRules()) {
            // No paths
            if (rule.getHttp() == null || rule.getHttp().getPaths() == null) {
                return false;
            }

            for (HTTPIngressPath path : rule.getHttp().getPaths()) {
                // No backend
                if (path.getBackend() == null || path.getBackend().getService().getName() == null || path.getBackend().getService().getPort() == null) {
                    return false;
                }

                // Get service and port
                String service = path.getBackend().getService().getName();
                Integer port = path.getBackend().getService().getPort().getNumber();

                // Only if service is the same as a nodegroup and port is 8080
                if (nodeGroups.contains( service ) && port == 8080) {
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
        Optional<ConfigMap> cm = searchers.configMap().stream().
            filter( inNamespace( namespace ) ).
            filter( withName( "extra-config" ) ).
            findFirst();

        // Try to use vhost defaults
        if (cm.isPresent() && cm.get().getData().containsKey( "vhost-defaults.cfg" )) {
            sb = new StringBuilder( cm.get().getData().get( "vhost-defaults.cfg" ) );
        } else {
            log.warn( String.format( "Could not find default vhost configuration in NS '%s'", namespace ) );
            sb = new StringBuilder( "enabled = true" );
        }

        // Iterate over all ingresses
        for (Ingress ingress : ingresses) {
            addVHostMappings( sb, ingress, nodeGroups );
        }

        return sb.toString();
    }

    private void addVHostMappings( final StringBuilder sb, final Ingress ingress, final List<String> nodeGroups )
    {
        Set<Mapping> mappings = getAnnotationMappings( ingress );

        // Iterate over rules
        for (IngressRule rule : ingress.getSpec().getRules()) {
            // Iterate over mappings
            for (Mapping mapping : mappings) {
                // Set rule host if missing
                if (mapping.host() == null) {
                    mapping = MappingImpl.copyOf( mapping ).withHost( rule.getHost() );
                }

                // If host matches
                if (mapping.host().equals( rule.getHost() )) {
                    // Iterate over paths
                    for (HTTPIngressPath path : rule.getHttp().getPaths()) {
                        // Path matches mapping
                        if (nodeGroups.contains( path.getBackend().getService().getName() ) && mapping.source().equals( path.getPath() )) {
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
        if (m.idProviders() != null) {
            String postFix = "default";
            for (String idProvider : m.idProviders().split( "," )) {
                sb.append( "\n" );
                sb.append( String.format( "mapping.%s.idProvider.%s=%s", m.name(), idProvider, postFix ) );
                postFix = "enabled";
            }
        }
    }
}
