package com.enonic.ec.kubernetes.operator.commands.apply;

import org.immutables.value.Value;

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
    protected void checkValidity( final HasMetadata resource )
        throws Exception
    {
        if ( !resource.getMetadata().getName().equals( name() ) )
        {
            throw new Exception( "Resource names do not match" );
        }
    }

    @Override
    protected Namespace fetchResource()
    {
        return client().namespaces().withName( name() ).get();
    }

    @Override
    protected Namespace apply( final ObjectMeta metadata )
    {
        Namespace namespace = new Namespace();
        namespace.setMetadata( metadata );
        return client().namespaces().createOrReplace( namespace );
    }
}
