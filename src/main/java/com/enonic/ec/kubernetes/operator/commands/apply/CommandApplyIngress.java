package com.enonic.ec.kubernetes.operator.commands.apply;

import java.util.Optional;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.kubernetes.api.model.extensions.IngressSpec;
import io.fabric8.kubernetes.client.KubernetesClient;

@Value.Immutable
public abstract class CommandApplyIngress
    extends CommandApplyResource<Ingress>
{
    protected abstract KubernetesClient client();

    protected abstract IngressSpec spec();

    @Override
    protected Optional<Ingress> fetchResource()
    {
        return Optional.ofNullable( client().extensions().ingresses().inNamespace( namespace().get() ).withName( name() ).get() );
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
        return client().extensions().ingresses().inNamespace( namespace().get() ).createOrReplace( resource );
    }
}
