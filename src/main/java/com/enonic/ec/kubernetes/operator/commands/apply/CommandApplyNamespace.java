package com.enonic.ec.kubernetes.operator.commands.apply;

import java.util.Optional;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.KubernetesClient;

@Value.Immutable
public abstract class CommandApplyNamespace
    extends CommandApplyResource<Namespace>
{
    protected abstract KubernetesClient client();

    @Override
    protected Optional<Namespace> fetchResource()
    {
        return Optional.ofNullable( client().namespaces().withName( name() ).get() );
    }

    @Override
    protected Namespace build( final ObjectMeta metadata )
    {
        Namespace namespace = new Namespace();
        namespace.setMetadata( metadata );
        return namespace;
    }

    @Override
    protected Namespace apply( final Namespace resource )
    {
        return client().namespaces().createOrReplace( resource );
    }
}
