package com.enonic.ec.kubernetes.operator.kubectl;

import java.util.Objects;
import java.util.Optional;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.ConfigMap;

import com.enonic.ec.kubernetes.operator.kubectl.base.KubeCommandBuilder;


@Value.Immutable
public abstract class KubeCmdConfigMaps
    extends KubeCommandBuilder<ConfigMap>
{
    @Override
    protected Optional<ConfigMap> fetch( final ConfigMap resource )
    {
        return Optional.ofNullable( clients().getDefaultClient().configMaps().
            inNamespace( resource.getMetadata().getNamespace() ).
            withName( resource.getMetadata().getName() ).
            get() );
    }

    @Override
    protected void createOrReplace( final ConfigMap resource )
    {
        clients().getDefaultClient().configMaps().
            inNamespace( resource.getMetadata().getNamespace() ).
            createOrReplace( resource );
    }

    @Override
    protected void patch( final ConfigMap resource )
    {
        clients().getDefaultClient().configMaps().
            inNamespace( resource.getMetadata().getNamespace() ).
            withName( resource.getMetadata().getName() ).
            patch( resource );
    }

    @Override
    protected void delete( final ConfigMap resource )
    {
        clients().getDefaultClient().configMaps().
            inNamespace( resource.getMetadata().getNamespace() ).
            withName( resource.getMetadata().getName() ).
            delete();
    }

    @Override
    protected boolean equalsSpec( final ConfigMap o, final ConfigMap n )
    {
        return Objects.equals( o.getData(), n.getData() );
    }
}
