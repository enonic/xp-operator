package com.enonic.ec.kubernetes.operator.commands.apply;

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
    protected Ingress fetchResource()
    {
        return client().extensions().ingresses().inNamespace( namespace() ).withName( name() ).get();
    }

    @Override
    protected Ingress apply( final ObjectMeta metadata )
    {
        Ingress ingress = new Ingress();
        ingress.setMetadata( metadata );
        ingress.setSpec( spec() );
        return client().extensions().ingresses().inNamespace( namespace() ).createOrReplace( ingress );
    }
}
