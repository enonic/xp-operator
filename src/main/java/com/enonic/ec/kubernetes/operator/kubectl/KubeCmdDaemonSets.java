package com.enonic.ec.kubernetes.operator.kubectl;

import java.util.Optional;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.apps.DaemonSet;

import com.enonic.ec.kubernetes.operator.kubectl.base.KubeCommandBuilder;


@Value.Immutable
public abstract class KubeCmdDaemonSets
    extends KubeCommandBuilder<DaemonSet>
{
    @Override
    protected Optional<DaemonSet> fetch( final DaemonSet resource )
    {
        return Optional.ofNullable( clients().getDefaultClient().apps().daemonSets().
            inNamespace( resource.getMetadata().getNamespace() ).
            withName( resource.getMetadata().getName() ).
            get() );
    }

    @Override
    protected void create( final DaemonSet resource )
    {
        clients().getDefaultClient().apps().daemonSets().
            inNamespace( resource.getMetadata().getNamespace() ).
            create( resource );
    }

    @Override
    protected void patch( final DaemonSet resource )
    {
        clients().getDefaultClient().apps().daemonSets().
            inNamespace( resource.getMetadata().getNamespace() ).
            withName( resource.getMetadata().getName() ).
            patch( resource );
    }

    @Override
    protected void delete( final DaemonSet resource )
    {
        clients().getDefaultClient().apps().daemonSets().
            inNamespace( resource.getMetadata().getNamespace() ).
            withName( resource.getMetadata().getName() ).
            delete();
    }

    @Override
    protected boolean equalsSpec( final DaemonSet o, final DaemonSet n )
    {
        // TODO: Compare Specs
        return super.equalsSpec( o, n );
    }
}
