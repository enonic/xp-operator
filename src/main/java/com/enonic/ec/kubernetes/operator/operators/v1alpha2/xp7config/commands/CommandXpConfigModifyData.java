package com.enonic.ec.kubernetes.operator.operators.v1alpha2.xp7config.commands;

import java.util.Optional;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.ObjectMeta;

import com.enonic.ec.kubernetes.operator.common.Configuration;
import com.enonic.ec.kubernetes.operator.common.commands.CombinedCommandBuilder;
import com.enonic.ec.kubernetes.operator.common.commands.ImmutableCombinedCommand;
import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha2.config.ImmutableV1alpha2Xp7ConfigSpec;
import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha2.config.V1alpha2Xp7Config;
import com.enonic.ec.kubernetes.operator.kubectl.ImmutableKubeCmd;
import com.enonic.ec.kubernetes.operator.kubectl.KubeCmd;
import com.enonic.ec.kubernetes.operator.operators.common.ResourceInfoNamespaced;
import com.enonic.ec.kubernetes.operator.operators.common.cache.Caches;
import com.enonic.ec.kubernetes.operator.operators.common.clients.Clients;

public abstract class CommandXpConfigModifyData
    extends Configuration
    implements CombinedCommandBuilder
{
    public abstract Clients clients();

    public abstract Caches caches();

    public abstract ResourceInfoNamespaced info();

    public abstract String name();

    public abstract String file();

    public abstract String nodeGroup();

    @Value.Derived
    protected Optional<V1alpha2Xp7Config> xpConfigResource()
    {
        return caches().getConfigCache().get( info().deploymentInfo().namespaceName(), name() );
    }

    @Override
    public void addCommands( final ImmutableCombinedCommand.Builder commandBuilder )
    {
        StringBuilder sb = new StringBuilder();
        setData( sb );
        String data = sb.toString().trim();

        if ( xpConfigResource().isPresent() && xpConfigResource().get().getSpec().data().equals( data ) )
        {
            // There is no change in this config
            return;
        }

        // Create new object
        ObjectMeta meta = new ObjectMeta();
        meta.setName( name() );
        meta.setNamespace( info().deploymentInfo().namespaceName() );
        meta.setLabels( info().deploymentInfo().resource().getMetadata().getLabels() );

        V1alpha2Xp7Config config = new V1alpha2Xp7Config();
        config.setMetadata( meta );
        config.setSpec( ImmutableV1alpha2Xp7ConfigSpec.builder().
            file( file() ).
            data( data ).
            nodeGroup( nodeGroup() ).
            build() );

        // Create command to edit it
        KubeCmd cmd = ImmutableKubeCmd.builder().
            clients( clients() ).
            namespace( info().deploymentInfo().namespaceName() ).
            resource( config ).
            build();

        // The config is empty, delete the config
        if ( data.equals( "" ) )
        {
            cmd.delete( commandBuilder );
        }
        else
        {
            cmd.apply( commandBuilder );
        }
    }

    protected abstract void setData( final StringBuilder sb );
}
