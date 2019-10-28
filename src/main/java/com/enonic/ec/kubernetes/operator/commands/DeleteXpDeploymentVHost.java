package com.enonic.ec.kubernetes.operator.commands;

import java.util.function.Function;

import org.immutables.value.Value;

import io.fabric8.kubernetes.client.KubernetesClient;

import com.enonic.ec.kubernetes.common.Configuration;
import com.enonic.ec.kubernetes.common.ImmutableTuple;
import com.enonic.ec.kubernetes.common.Tuple;
import com.enonic.ec.kubernetes.common.commands.ImmutableCombinedKubernetesCommand;
import com.enonic.ec.kubernetes.deployment.vhost.VHost;
import com.enonic.ec.kubernetes.deployment.vhost.VHostPath;
import com.enonic.ec.kubernetes.operator.commands.kubectl.delete.ImmutableCommandDeleteIngress;
import com.enonic.ec.kubernetes.operator.commands.kubectl.delete.ImmutableCommandDeleteIssuer;
import com.enonic.ec.kubernetes.operator.commands.kubectl.delete.ImmutableCommandDeleteService;
import com.enonic.ec.kubernetes.operator.crd.certmanager.issuer.IssuerClient;

@Value.Immutable
public abstract class DeleteXpDeploymentVHost
    extends Configuration
    implements CommandBuilder
{
    protected abstract KubernetesClient defaultClient();

    protected abstract IssuerClient issuerClient();

    protected abstract String namespace();

    protected abstract VHost vHost();

    protected abstract Function<VHost, String> vHostResourceName();

    protected abstract Function<Tuple<VHost, VHostPath>, String> pathResourceName();

    @Override
    public void addCommands( ImmutableCombinedKubernetesCommand.Builder commandBuilder )
    {
        String vHostResourceName = vHostResourceName().apply( vHost() );

        commandBuilder.addCommand( ImmutableCommandDeleteIngress.builder().
            client( defaultClient() ).
            namespace( namespace() ).
            name( vHostResourceName().apply( vHost() ) ).
            build() );

        for ( VHostPath path : vHost().vHostPaths() )
        {
            commandBuilder.addCommand( ImmutableCommandDeleteService.builder().
                namespace( namespace() ).
                name( pathResourceName().apply( ImmutableTuple.of( vHost(), path ) ) ).
                build() );
        }

        if ( vHost().certificate().isPresent() )
        {
            commandBuilder.addCommand( ImmutableCommandDeleteIssuer.builder().
                client( issuerClient() ).
                namespace( namespace() ).
                name( vHostResourceName ).
                build() );
        }
    }
}
