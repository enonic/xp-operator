package com.enonic.ec.kubernetes.operator.commands.apply;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.policy.PodDisruptionBudget;
import io.fabric8.kubernetes.api.model.policy.PodDisruptionBudgetSpec;
import io.fabric8.kubernetes.client.KubernetesClient;

import com.enonic.ec.kubernetes.operator.commands.common.CommandApplyResource;

import static com.enonic.ec.kubernetes.common.assertions.Assertions.assertNotNull;

public class CommandApplyPodDisruptionBudget
    extends CommandApplyResource<PodDisruptionBudget>
{
    private final KubernetesClient client;

    private final PodDisruptionBudgetSpec spec;

    private CommandApplyPodDisruptionBudget( final Builder builder )
    {
        super( builder );
        client = assertNotNull( "client", builder.client );
        spec = assertNotNull( "spec", builder.spec );
    }

    public static Builder newBuilder()
    {
        return new Builder();
    }

    @Override
    protected PodDisruptionBudget fetchResource()
    {
        return client.policy().podDisruptionBudget().inNamespace( namespace ).withName( name ).get();
    }

    @Override
    protected PodDisruptionBudget apply( final ObjectMeta metadata )
    {
        PodDisruptionBudget podDisruptionBudget = new PodDisruptionBudget();
        podDisruptionBudget.setMetadata( metadata );
        podDisruptionBudget.setSpec( spec );
        return client.policy().podDisruptionBudget().inNamespace( namespace ).createOrReplace( podDisruptionBudget );
    }

    public static final class Builder
        extends CommandApplyResource.Builder<Builder>
    {
        private KubernetesClient client;

        private PodDisruptionBudgetSpec spec;

        public Builder client( final KubernetesClient val )
        {
            client = val;
            return this;
        }

        public Builder spec( final PodDisruptionBudgetSpec val )
        {
            spec = val;
            return this;
        }

        public CommandApplyPodDisruptionBudget build()
        {
            return new CommandApplyPodDisruptionBudget( this );
        }
    }
}
