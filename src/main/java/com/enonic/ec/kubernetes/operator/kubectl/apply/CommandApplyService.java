package com.enonic.ec.kubernetes.operator.kubectl.apply;

import java.util.Optional;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceSpec;
import io.fabric8.kubernetes.client.KubernetesClient;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@Value.Immutable
public abstract class CommandApplyService
    extends CommandApplyResource<Service>
{
    protected abstract KubernetesClient client();

    protected abstract ServiceSpec spec();

    @Override
    protected Optional<Service> fetchResource()
    {
        return Optional.ofNullable( client().services().inNamespace( namespace().get() ).withName( name() ).get() );
    }

    @Override
    protected Service build( final ObjectMeta metadata )
    {
        Service service = new Service();
        service.setMetadata( metadata );
        service.setSpec( spec() );
        return service;
    }

    @Override
    protected Service apply( final Service resource )
    {
        return client().services().inNamespace( namespace().get() ).createOrReplace( resource );
    }
}
