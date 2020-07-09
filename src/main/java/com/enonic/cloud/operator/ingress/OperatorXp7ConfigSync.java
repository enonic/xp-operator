package com.enonic.cloud.operator.ingress;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.extensions.HTTPIngressPath;
import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.kubernetes.api.model.extensions.IngressRule;

import com.enonic.cloud.kubernetes.Clients;
import com.enonic.cloud.kubernetes.Searchers;
import com.enonic.cloud.kubernetes.commands.K8sLogHelper;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7config.Xp7Config;
import com.enonic.cloud.operator.helpers.HandlerConfig;

import static com.enonic.cloud.common.Configuration.cfgStr;

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
        Map<String, Properties> targetMappings = getAnnotationMappings( ingress );
        if ( targetMappings.size() == 0 )
        {
            return false;
        }

        if ( ingress.getSpec().getRules() == null )
        {
            return false;
        }

        for ( IngressRule rule : ingress.getSpec().getRules() )
        {
            if ( rule.getHttp() == null || rule.getHttp().getPaths() == null )
            {
                return false;
            }
            for ( HTTPIngressPath path : rule.getHttp().getPaths() )
            {
                if ( path.getBackend() == null || path.getBackend().getServiceName() == null || path.getBackend().getServicePort() == null )
                {
                    return false;
                }

                String service = path.getBackend().getServiceName();
                String port = path.getBackend().getServicePort().getStrVal() != null
                    ? path.getBackend().getServicePort().getStrVal()
                    : path.getBackend().getServicePort().getIntVal().toString();
                if ( nodeGroups.contains( service ) && port.equals( "8080" ) )
                {
                    return true;
                }

            }
        }

        return false;
    }

    private String createVHostData( final String namespace, final List<Ingress> ingresses, final List<String> nodeGroups )
    {
        StringBuilder sb;
        Optional<ConfigMap> cm = searchers.configMap().query().
            inNamespace( namespace ).withName( "extra-config" ).
            get();

        if ( cm.isPresent() && cm.get().getData().containsKey( "vhost-defaults.cfg" ) )
        {
            sb = new StringBuilder( cm.get().getData().get( "vhost-defaults.cfg" ) );
        }
        else
        {
            log.warn( "Could not find default vhost configuration" );
            sb = new StringBuilder( "enabled = true" );
        }
        sb.append( "\n" );

        for ( Ingress ingress : ingresses )
        {
            Map<String, Properties> mappings = getAnnotationMappings( ingress );
            for ( IngressRule rule : ingress.getSpec().getRules() )
            {
                for ( HTTPIngressPath path : rule.getHttp().getPaths() )
                {
                    if ( nodeGroups.contains( path.getBackend().getServiceName() ) )
                    {
                        if ( mappings.containsKey( path.getPath() ) )
                        {
                            String source = path.getPath();
                            String mappingName =
                                String.format( "%s-%s", ingress.getMetadata().getName(), mappings.get( source ).getProperty( "name" ) );
                            String target = mappings.get( path.getPath() ).getProperty( "target" );
                            String idProviders = mappings.get( path.getPath() ).getProperty( "idproviders" );
                            addVHostMappings( sb, mappingName, rule.getHost(), source, target, idProviders );
                        }
                    }
                }
            }
        }

        return sb.toString();
    }

    private Map<String, Properties> getAnnotationMappings( final Ingress ingress )
    {
        String prefix = cfgStr( "operator.annotations.xp7.vhosts" );
        Map<String, Properties> mappings = new HashMap<>();

        for ( Map.Entry<String, String> e : ingress.getMetadata().getAnnotations().entrySet() )
        {
            if ( e.getKey().startsWith( prefix ) )
            {
                try
                {
                    final Properties p = new Properties();
                    p.load( new StringReader( e.getValue() ) );
                    p.put( "name", e.getKey().replace( prefix, "" ) );
                    mappings.put( p.getProperty( "source" ), p );
                }
                catch ( IOException ex )
                {
                    log.warn( String.format( "Invalid vhost mappings on ingress '%s'", ingress.getMetadata().getName() ) );
                }
            }
        }

        return mappings;
    }

    private void addVHostMappings( final StringBuilder sb, final String name, final String host, final String path, final String target,
                                   final String idProviders )
    {
        sb.append( "\n" );
        sb.append( String.format( "mapping.%s.host=%s\n", name, host ) );
        sb.append( String.format( "mapping.%s.source=%s\n", name, path ) );
        sb.append( String.format( "mapping.%s.target=%s", name, target ) );
        if ( idProviders != null )
        {
            sb.append( "\n" );
            String postFix = "default";
            for ( String idProvider : idProviders.split( "," ) )
            {
                sb.append( String.format( "mapping.%s.idProvider.%s=%s", name, idProvider, postFix ) );
                sb.append( "\n" );
                postFix = "enabled";
            }
        }
    }
}
