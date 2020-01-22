package com.enonic.ec.kubernetes.operator.kubectl.apply;

import java.util.Optional;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.KubernetesClient;

import com.enonic.ec.kubernetes.operator.operators.clients.Clients;

@Value.Immutable
public abstract class CommandApplyNamespace
    extends CommandApplyResource<Namespace>
{
    @Override
    protected Optional<Namespace> fetchResource()
    {
        return Optional.ofNullable( clients().getDefaultClient().namespaces().withName( name() ).get() );
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
        return clients().getDefaultClient().namespaces().createOrReplace( resource );
    }

    @Override
    public String toString()
    {
        return super.toString();
    }
}
