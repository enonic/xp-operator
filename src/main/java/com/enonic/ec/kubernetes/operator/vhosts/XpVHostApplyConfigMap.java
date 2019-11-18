package com.enonic.ec.kubernetes.operator.vhosts;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.KubernetesClient;

import com.enonic.ec.kubernetes.common.commands.CombinedCommandBuilder;
import com.enonic.ec.kubernetes.common.commands.ImmutableCombinedKubernetesCommand;
import com.enonic.ec.kubernetes.crd.vhost.diff.DiffSpec;
import com.enonic.ec.kubernetes.crd.vhost.spec.ImmutableSpec;
import com.enonic.ec.kubernetes.crd.vhost.spec.Spec;
import com.enonic.ec.kubernetes.crd.vhost.spec.SpecMapping;
import com.enonic.ec.kubernetes.operator.kubectl.apply.ImmutableCommandApplyConfigMap;

@Value.Immutable
public abstract class XpVHostApplyConfigMap
    extends XpVHostApply
    implements CombinedCommandBuilder
{
    protected abstract KubernetesClient client();

    protected abstract List<DiffSpec> diffs();

    protected abstract List<ConfigMap> configMaps();

    @Override
    public void addCommands( final ImmutableCombinedKubernetesCommand.Builder commandBuilder )
    {
        for ( ConfigMap configMap : configMaps() )
        {
            String alias = configMap.getMetadata().
                getLabels().
                get( aliasLabelKey() );

            if ( alias == null )
            {
                continue;
            }

            Map<String, String> newConfig = new HashMap<>( configMap.getData() );
            applyDiffs( diffs(), alias, newConfig );
            if ( !newConfig.equals( configMap.getData() ) )
            {
                commandBuilder.addCommand( ImmutableCommandApplyConfigMap.builder().
                    client( client() ).
                    ownerReference( configMap.getMetadata().getOwnerReferences().get( 0 ) ).
                    namespace( configMap.getMetadata().getNamespace() ).
                    name( configMap.getMetadata().getName() ).
                    labels( configMap.getMetadata().getLabels() ).
                    annotations(
                        configMap.getMetadata().getAnnotations() != null ? configMap.getMetadata().getAnnotations() : new HashMap<>() ).
                    data( newConfig ).
                    build() );
            }
        }
    }

    private static void applyDiffs( final List<DiffSpec> diffs, final String alias, final Map<String, String> newConfig )
    {
        String configFile = cfgStr( "operator.deployment.xp.vHost.configFile" );
        boolean addedVHosts = false;
        StringBuilder sb = new StringBuilder( "enabled = true" ).append( "\n" );
        for ( DiffSpec diff : diffs )
        {
            if ( !diff.shouldAddOrModify() )
            {
                continue;
            }

            Optional<Spec> relevant = relevantSpec( diff.newValue().get(), alias );

            if ( relevant.isEmpty() )
            {
                continue;
            }

            addedVHosts = true;
            addToConfig( relevant.get(), sb );
        }

        if ( !addedVHosts )
        {
            if ( !newConfig.containsKey( configFile ) )
            {
                return;
            }
            sb = new StringBuilder( "enabled = false" );
        }

        newConfig.put( configFile, sb.toString().trim() );
    }

    private static void addToConfig( final Spec spec, final StringBuilder sb )
    {
        spec.mappings().forEach( m -> {
            sb.append( "mapping." ).append( m.name() ).append( ".host" ).append( "=" ).append( spec.host() ).append( "\n" );
            sb.append( "mapping." ).append( m.name() ).append( ".source" ).append( "=" ).append( m.source() ).append( "\n" );
            sb.append( "mapping." ).append( m.name() ).append( ".target" ).append( "=" ).append( m.target() ).append( "\n" );
            if ( m.idProvider() != null )
            {
                sb.append( "mapping." ).append( m.name() ).append( ".idProvider." ).append( m.idProvider() ).append( "=" ).append(
                    "default\n" );
            }
            sb.append( "\n" );
        } );
    }

    public static Optional<Spec> relevantSpec( Spec spec, String alias )
    {
        List<SpecMapping> relevantMappings =
            spec.mappings().stream().filter( m -> m.nodeAlias().equals( alias ) ).collect( Collectors.toList() );
        if ( relevantMappings.size() < 1 )
        {
            return Optional.empty();
        }
        return Optional.of( ImmutableSpec.builder().from( spec ).mappings( relevantMappings ).build() );
    }
}
