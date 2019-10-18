package com.enonic.ec.kubernetes.operator.commands.plan;

import java.util.Properties;

import org.immutables.value.Value;

import com.google.common.base.Preconditions;

import com.enonic.ec.kubernetes.deployment.xpdeployment.XpDeploymentResourceSpecNode;

@Value.Immutable
public abstract class XpNodeDeploymentPlan
{
    protected abstract XpNodeDeploymentDiff.NodeTuple nodeTuple();

    @Value.Default
    protected boolean newDeployment()
    {
        return false;
    }

    @Value.Default
    protected boolean enabledDisabled()
    {
        return false;
    }

    @Value.Default
    protected boolean updateXp()
    {
        return false;
    }

    @Value.Derived
    public XpDeploymentResourceSpecNode node()
    {
        return nodeTuple().getNewNode();
    }

    @Value.Derived
    public boolean changeScale()
    {
        if ( nodeTuple().getOldNode() == null )
        {
            return false;
        }
        return enabledDisabled() || !nodeTuple().getOldNode().replicas().equals( nodeTuple().getOldNode().replicas() );
    }

    @Value.Derived
    public boolean changeDisruptionBudget()
    {
        if ( nodeTuple().getOldNode() == null )
        {
            return true;
        }
        return changeScale();
    }

    @Value.Derived
    public boolean changeConfigMap()
    {
        return newDeployment() || !nodeTuple().getOldNode().config().equals( nodeTuple().getNewNode().config() );
    }

    @Value.Derived
    public boolean changeStatefulSet()
    {
        if ( newDeployment() || updateXp() )
        {
            return true;
        }

        if ( !nodeTuple().getOldNode().resources().cpu().equals( nodeTuple().getOldNode().resources().cpu() ) )
        {
            return true;
        }

        if ( !nodeTuple().getOldNode().resources().memory().equals( nodeTuple().getOldNode().resources().memory() ) )
        {
            return true;
        }

        Properties oldSystemProps = nodeTuple().getOldNode().configAsProperties( "system.properties" );
        Properties newSystemProps = nodeTuple().getNewNode().configAsProperties( "system.properties" );

        if ( oldSystemProps != null && !oldSystemProps.equals( newSystemProps ) )
        {
            return true;
        }
        if ( newSystemProps != null && !newSystemProps.equals( oldSystemProps ) )
        {
            return true;
        }

        return false;
    }

    @Value.Check
    protected void check()
    {
        if ( newDeployment() )
        {
            Preconditions.checkState( !changeScale(), "Scale should never be true on new deployments" );
        }
    }
}
