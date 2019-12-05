package com.enonic.ec.kubernetes.kubectl.apply;

import java.util.Map;
import java.util.Optional;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@Value.Immutable
public abstract class CommandApplySecret
    extends CommandApplyResource<Secret>
{
    protected abstract KubernetesClient client();

    protected abstract Map<String, String> data();

    @Override
    protected Optional<Secret> fetchResource()
    {
        return Optional.ofNullable( client().secrets().inNamespace( namespace().get() ).withName( name() ).get() );
    }

    @Override
    protected Secret build( final ObjectMeta metadata )
    {
        Secret secret = new Secret();
        secret.setMetadata( metadata );
        secret.setData( data() );
        return secret;
    }

    @Override
    protected Secret apply( final Secret resource )
    {
        return client().secrets().inNamespace( namespace().get() ).createOrReplace( resource );
    }

    @Override
    public String toString()
    {
        return super.toString();
    }
}
