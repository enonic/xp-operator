package com.enonic.ec.kubernetes.operator.vhosts;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.KubernetesClient;

import com.enonic.ec.kubernetes.common.commands.ImmutableCombinedKubernetesCommand;
import com.enonic.ec.kubernetes.crd.vhost.spec.Spec;
import com.enonic.ec.kubernetes.operator.commands.CommandBuilder;
import com.enonic.ec.kubernetes.operator.commands.kubectl.apply.ImmutableCommandApplyConfigMap;

@Value.Immutable
public abstract class XpVHostConfigMap
    extends XpVHostBase
    implements CommandBuilder
{
    protected abstract KubernetesClient defaultClient();

    protected abstract ConfigMap configMap();

    protected abstract List<Spec> vHosts();

    @Override
    public void addCommands( final ImmutableCombinedKubernetesCommand.Builder commandBuilder )
    {
        Map<String, String> newData = new HashMap<>( configMap().getData() );

        StringBuilder sb = new StringBuilder( "enabled = true" ).append( "\n" );

        if ( vHosts().size() == 0 )
        {
            newData.remove( "com.enonic.xp.web.vhost.cfg" );
        }
        else
        {
            vHosts().forEach( h -> {
                h.mappings().forEach( m -> {
                    sb.append( "mapping." ).append( m.name() ).append( ".host" ).append( "=" ).append( h.host() ).append( "\n" );
                    sb.append( "mapping." ).append( m.name() ).append( ".source" ).append( "=" ).append( m.source() ).append( "\n" );
                    sb.append( "mapping." ).append( m.name() ).append( ".target" ).append( "=" ).append( m.target() ).append( "\n" );
                    m.idProvider().ifPresent(
                        i -> sb.append( "mapping." ).append( m.name() ).append( ".idProvider." ).append( i ).append( "=" ).append(
                            "default\n" ) );
                    sb.append( "\n" );
                } );
            } );

        }

        if ( !newData.equals( configMap().getData() ) )
        {
            commandBuilder.addCommand( ImmutableCommandApplyConfigMap.builder().
                namespace( configMap().getMetadata().getNamespace() ).
                name( configMap().getMetadata().getName() ).
                labels( configMap().getMetadata().getLabels() ).
                annotations( configMap().getMetadata().getAnnotations() ).
                data( newData ).
                build() );
        }
    }
}
