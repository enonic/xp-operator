package com.enonic.ec.kubernetes.operator.commands.vhosts;

import org.immutables.value.Value;

import io.fabric8.kubernetes.client.KubernetesClient;

import com.enonic.ec.kubernetes.common.commands.ImmutableCombinedCommand;
import com.enonic.ec.kubernetes.kubectl.delete.ImmutableCommandDeleteIngress;
import com.enonic.ec.kubernetes.operator.crd.vhost.XpVHostResource;

@Value.Immutable
public abstract class XpVHostDeleteIngress
    extends XpVHostCommand
{
    protected abstract KubernetesClient defaultClient();

    protected abstract XpVHostResource resource();

    @Override
    public void addCommands( final ImmutableCombinedCommand.Builder commandBuilder )
    {
        if ( resource().getSpec().createIngress() )
        {
            commandBuilder.addCommand( ImmutableCommandDeleteIngress.builder().
                client( defaultClient() ).
                namespace( resource().getMetadata().getNamespace() ).
                name( getIngressName( resource() ) ).
                build() );
        }
    }
}
