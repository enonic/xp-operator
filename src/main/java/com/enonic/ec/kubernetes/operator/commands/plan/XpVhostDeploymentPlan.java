package com.enonic.ec.kubernetes.operator.commands.plan;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.deployment.vhost.Vhost;
import com.enonic.ec.kubernetes.deployment.vhost.VhostPath;

@Value.Immutable
public abstract class XpVhostDeploymentPlan
{
    protected abstract XpVhostDeploymentDiff.VhostTuple vHostTuple();

    @Value.Derived
    public Vhost vhost()
    {
        return vHostTuple().getNewVhost();
    }

    @Value.Derived
    public List<VhostPath> newPaths()
    {
        return vhost().vhostPaths();
    }

    @Value.Derived
    public List<VhostPath> pathsToDelete()
    {
        if ( vHostTuple().getOldVhost().isEmpty() )
        {
            return Collections.EMPTY_LIST;
        }
        return vHostTuple().getOldVhost().get().vhostPaths().stream().
            filter( p -> !vHostTuple().getNewVhost().vhostPaths().contains( p ) ).
            collect( Collectors.toList() );
    }

    @Value.Derived
    public boolean changeIssuer()
    {
        if ( vHostTuple().getNewVhost().certificate().isEmpty() )
        {
            return false;
        }
        if ( vHostTuple().getOldVhost().isEmpty() )
        {
            return true;
        }
        return !vHostTuple().getNewVhost().certificate().equals( vHostTuple().getOldVhost().get().certificate() );
    }


    @Value.Derived
    public boolean changeIngress()
    {
        if ( vHostTuple().getOldVhost().isEmpty() )
        {
            return true;
        }
        return !vHostTuple().getOldVhost().get().equals( vHostTuple().getNewVhost() );
    }

}
