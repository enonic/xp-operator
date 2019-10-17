package com.enonic.ec.kubernetes.operator.commands.apply;

import org.immutables.value.Value;

import com.google.common.base.Preconditions;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.KubernetesClient;

@Value.Immutable
public abstract class CommandApplyNamespace
    extends CommandApplyResource<Namespace>
{
    protected abstract KubernetesClient client();

    @Override
    void checkValidity( HasMetadata resource )
    {
        Preconditions.checkState( resource.getMetadata().getName().equals( name() ), "Resource names do not match" );
    }

    @Override
    protected Namespace fetchResource()
    {
        return client().namespaces().withName( name() ).get();
    }

    @Override
    protected Namespace createResource( final ObjectMeta metadata )
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
