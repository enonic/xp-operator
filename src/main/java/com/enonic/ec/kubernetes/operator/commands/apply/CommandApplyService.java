package com.enonic.ec.kubernetes.operator.commands.apply;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceSpec;
import io.fabric8.kubernetes.client.KubernetesClient;

@Value.Immutable
public abstract class CommandApplyService
    extends CommandApplyResource<Service>
{
    protected abstract KubernetesClient client();

    protected abstract ServiceSpec spec();

    @Override
    protected Service fetchResource()
    {
        return client().services().inNamespace( namespace() ).withName( name() ).get();
    }

    @Override
    protected Service apply( final ObjectMeta metadata )
    {
        Service service = new Service();
        service.setMetadata( metadata );
        service.setSpec( spec() );
        return client().services().inNamespace( namespace() ).createOrReplace( service );
    }

}
