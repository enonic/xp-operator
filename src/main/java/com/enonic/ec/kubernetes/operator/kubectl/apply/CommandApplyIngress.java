package com.enonic.ec.kubernetes.operator.kubectl.apply;

import java.util.Optional;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.kubernetes.api.model.extensions.IngressSpec;
import io.fabric8.kubernetes.client.KubernetesClient;

import com.enonic.ec.kubernetes.operator.operators.clients.Clients;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@Value.Immutable
public abstract class CommandApplyIngress
    extends CommandApplyResource<Ingress>
{
    protected abstract IngressSpec spec();

    @Override
    protected Optional<Ingress> fetchResource()
    {
        return Optional.ofNullable( clients().getDefaultClient().extensions().ingresses().inNamespace( namespace().get() ).withName( name() ).get() );
    }

    @Override
    protected Ingress build( final ObjectMeta metadata )
    {
        Ingress ingress = new Ingress();
        ingress.setMetadata( metadata );
        ingress.setSpec( spec() );
        return ingress;
    }

    @Override
    protected Ingress apply( final Ingress resource )
    {
        return clients().getDefaultClient().extensions().ingresses().inNamespace( namespace().get() ).createOrReplace( resource );
    }

    @Override
    public String toString()
    {
        return super.toString();
    }
}
