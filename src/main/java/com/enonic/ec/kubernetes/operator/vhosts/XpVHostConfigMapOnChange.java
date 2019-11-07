package com.enonic.ec.kubernetes.operator.vhosts;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.KubernetesClient;

import com.enonic.ec.kubernetes.common.commands.ImmutableCombinedKubernetesCommand;
import com.enonic.ec.kubernetes.crd.vhost.client.XpVHostCache;
import com.enonic.ec.kubernetes.crd.vhost.spec.Spec;
import com.enonic.ec.kubernetes.operator.commands.CommandBuilder;

@Value.Immutable
public abstract class XpVHostConfigMapOnChange
    extends XpVHostBase
    implements CommandBuilder
{
    protected abstract KubernetesClient client();

    protected abstract XpVHostCache xpVHostCache();

    protected abstract ConfigMap configMap();

    @Override
    public void addCommands( final ImmutableCombinedKubernetesCommand.Builder commandBuilder )
    {
        List<Spec> relevantVHostMappings = xpVHostCache().stream().
            // Only look into the same namespace
                filter( v -> v.getMetadata().getNamespace().equals( configMap().getMetadata().getNamespace() ) ).
            // Only look into vHosts that modify this config map
                filter( v -> v.getSpec().doesModify( configMap() ) ).
                map( v -> v.getSpec().relevantSpec( configMapNodeAlias( configMap() ) ) ).
                filter( Optional::isPresent ).map( Optional::get ).
                collect( Collectors.toList() );

        ImmutableXpVHostConfigMap.builder().
            defaultClient( client() ).
            vHosts( relevantVHostMappings ).
            configMap( configMap() ).
            build().addCommands( commandBuilder );
    }

}
