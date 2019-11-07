package com.enonic.ec.kubernetes.operator.vhosts;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.KubernetesClient;

import com.enonic.ec.kubernetes.common.cache.ConfigMapCache;
import com.enonic.ec.kubernetes.common.commands.ImmutableCombinedKubernetesCommand;
import com.enonic.ec.kubernetes.crd.vhost.XpVHostResource;
import com.enonic.ec.kubernetes.crd.vhost.diff.DiffSpec;
import com.enonic.ec.kubernetes.crd.vhost.diff.ImmutableDiffSpec;
import com.enonic.ec.kubernetes.crd.vhost.spec.Spec;
import com.enonic.ec.kubernetes.operator.commands.CommandBuilder;
import com.enonic.ec.kubernetes.operator.crd.certmanager.issuer.client.IssuerClient;

@Value.Immutable
public abstract class XpVHostCreate
    extends XpVHostBase
    implements CommandBuilder
{
    protected abstract KubernetesClient defaultClient();

    protected abstract IssuerClient issuerClient();

    protected abstract ConfigMapCache configMapCache();

    protected abstract Optional<XpVHostResource> oldResource();

    protected abstract Optional<XpVHostResource> newResource();

    @Value.Derived
    protected DiffSpec diffSpec()
    {
        return ImmutableDiffSpec.builder().
            oldValue( oldResource().map( XpVHostResource::getSpec ).orElse( null ) ).
            newValue( newResource().map( XpVHostResource::getSpec ).orElse( null ) ).
            build();
    }

    @Override
    public void addCommands( final ImmutableCombinedKubernetesCommand.Builder commandBuilder )
    {
        if ( diffSpec().shouldAddOrModify() )
        {
            modifyVHost( commandBuilder );
        }

        if ( diffSpec().shouldRemove() )
        {
            removeVHost( commandBuilder );
        }
    }

    private void modifyVHost( final ImmutableCombinedKubernetesCommand.Builder commandBuilder )
    {
        List<ConfigMap> configMapsInNamespace = configMapCache().stream().
            filter( c -> c.getMetadata().getNamespace().equals( newResource().get().getMetadata().getNamespace() ) ).
            collect( Collectors.toList() );


        List<Spec> relevantSpec = new LinkedList<>(  );
        for(ConfigMap c: configMapsInNamespace) {
            String configMapNodeAlias = configMapNodeAlias( c );
            boolean wasInOld = oldResource().isPresent() && oldResource().get().getSpec().doesModify( c );
            boolean isInNew = newResource().get().getSpec().doesModify( c );
            if(wasInOld || isInNew) {
                relevantSpec.add( newResource().get().getSpec().relevantSpec( configMapNodeAlias ).get() );
            }
        }


    }

    private void removeVHost( final ImmutableCombinedKubernetesCommand.Builder commandBuilder )
    {

    }
}
