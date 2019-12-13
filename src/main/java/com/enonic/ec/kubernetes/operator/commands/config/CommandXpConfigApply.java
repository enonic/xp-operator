package com.enonic.ec.kubernetes.operator.commands.config;

import java.util.Collections;
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
import com.enonic.ec.kubernetes.operator.crd.config.XpConfigResource;

@Value.Immutable
public abstract class CommandXpConfigApply
    extends Configuration
    implements CombinedCommandBuilder
{
    protected abstract KubernetesClient defaultClient();

    protected abstract ConfigMap configMap();

    protected abstract List<XpConfigResource> xpConfigResources();

    @Override
    public void addCommands( final ImmutableCombinedCommand.Builder commandBuilder )
    {
        Map<String, String> oldData = configMap().getData() != null ? configMap().getData() : Collections.emptyMap();
        Map<String, String> newData = new HashMap<>();
        for ( XpConfigResource resource : xpConfigResources() )
        {
            newData.put( resource.getSpec().file(), resource.getSpec().data() );
        }
        if ( !newData.equals( oldData ) )
        {
            configMap().setData( newData );
            commandBuilder.addCommand( ImmutableCommandApplyConfigMap.builder().
                client( defaultClient() ).
                canSkipOwnerReference( true ). // Config map is owned by the XpDeployment
                namespace( configMap().getMetadata().getNamespace() ).
                name( configMap().getMetadata().getName() ).
                data( newData ).
                build() );
        }
    }
}
