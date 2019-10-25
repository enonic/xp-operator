package com.enonic.ec.kubernetes.operator.commands.plan;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.deployment.vhost.VHost;
import com.enonic.ec.kubernetes.deployment.vhost.VHostPath;

@SuppressWarnings("unchecked")
@Value.Immutable
public abstract class XpVHostDeploymentPlan
{
    protected abstract XpVHostDeploymentDiff.VHostTuple vHostTuple();

    @Value.Derived
    public VHost vhost()
    {
        return vHostTuple().getNewVHost();
    }

    @Value.Derived
    public List<VHostPath> newPaths()
    {
        return vhost().vHostPaths();
    }

    @Value.Derived
    public List<VHostPath> pathsToDelete()
    {
        if ( vHostTuple().getOldVHost().isEmpty() )
        {
            return Collections.EMPTY_LIST;
        }
        return vHostTuple().getOldVHost().get().vHostPaths().stream().
            filter( p -> !vHostTuple().getNewVHost().vHostPaths().contains( p ) ).
            collect( Collectors.toList() );
    }

    @Value.Derived
    public boolean changeIssuer()
    {
        if ( vHostTuple().getNewVHost().certificate().isEmpty() )
        {
            return false;
        }
        if ( vHostTuple().getOldVHost().isEmpty() )
        {
            return true;
        }
        return !vHostTuple().getNewVHost().certificate().equals( vHostTuple().getOldVHost().get().certificate() );
    }


    @Value.Derived
    public boolean changeIngress()
    {
        if ( vHostTuple().getOldVHost().isEmpty() )
        {
            return true;
        }
        return !vHostTuple().getOldVHost().get().equals( vHostTuple().getNewVHost() );
    }
}
