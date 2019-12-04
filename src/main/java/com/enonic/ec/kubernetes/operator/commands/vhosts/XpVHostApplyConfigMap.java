package com.enonic.ec.kubernetes.operator.commands.vhosts;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.KubernetesClient;

import com.enonic.ec.kubernetes.common.Configuration;
import com.enonic.ec.kubernetes.common.commands.CombinedCommandBuilder;
import com.enonic.ec.kubernetes.common.commands.ImmutableCombinedCommand;
import com.enonic.ec.kubernetes.kubectl.apply.ImmutableCommandApplyConfigMap;
import com.enonic.ec.kubernetes.operator.commands.vhosts.config.ImmutableConfigBuilderVHost;
import com.enonic.ec.kubernetes.operator.crd.vhost.diff.DiffSpec;

@Value.Immutable
public abstract class XpVHostApplyConfigMap
    extends Configuration
    implements CombinedCommandBuilder
{
    protected abstract KubernetesClient client();

    protected abstract List<DiffSpec> diffs();

    protected abstract List<ConfigMap> configMaps();

    @Override
    public void addCommands( final ImmutableCombinedCommand.Builder commandBuilder )
    {
        for ( ConfigMap configMap : configMaps() )
        {
            String alias = configMap.getMetadata().
                getLabels().
                get( cfgStr( "operator.deployment.xp.labels.pod.name" ) );

            if ( alias == null )
            {
                continue;
            }

            Map<String, String> newConfig = ImmutableConfigBuilderVHost.builder().
                baseConfig( configMap.getData() ).
                node( alias ).
                diffs( diffs() ).
                build().
                create();

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
}
