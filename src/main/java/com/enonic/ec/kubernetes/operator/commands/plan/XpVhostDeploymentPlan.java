package com.enonic.ec.kubernetes.operator.commands.plan;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.deployment.vhost.Vhost;

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
    public boolean changeIssuer()
    {
        if ( vHostTuple().getNewVhost().certificate() == null )
        {
            return false;
        }
        return !vHostTuple().getNewVhost().certificate().equals( vHostTuple().getOldVhost().certificate() );
    }


    @Value.Derived
    public boolean changeIngress()
    {
        if ( vHostTuple().getOldVhost() == null )
        {
            return true;
        }
        return vHostTuple().getOldVhost().equals( vHostTuple().getNewVhost() );
    }

}
