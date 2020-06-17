package com.enonic.cloud.operator.functions;

import java.util.Optional;
import java.util.function.Predicate;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.HasMetadata;

import com.enonic.cloud.common.annotations.Params;
import com.enonic.cloud.kubernetes.InformerSearcher;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7Deployment;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7DeploymentStatus;

@Value.Immutable
@Params
public abstract class XpRunning
    implements Predicate<HasMetadata>
{
    protected abstract InformerSearcher<Xp7Deployment> informerSearcher();

    @Override
    public boolean test( final HasMetadata hasMetadata )
    {
        Optional<Xp7Deployment> deployment = informerSearcher().get( hasMetadata.getMetadata().getNamespace() ).findFirst();
        if ( deployment.isEmpty() )
        {
            return false;
        }

        if ( deployment.get().getXp7DeploymentStatus() == null )
        {
            return false;
        }

        return deployment.get().getXp7DeploymentStatus().getState().equals( Xp7DeploymentStatus.State.RUNNING );
    }
}
