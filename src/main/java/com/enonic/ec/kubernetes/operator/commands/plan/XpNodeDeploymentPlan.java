package com.enonic.ec.kubernetes.operator.commands.plan;

import java.util.Optional;
import java.util.Properties;

import org.immutables.value.Value;

import com.google.common.base.Preconditions;

import com.enonic.ec.kubernetes.deployment.xpdeployment.XpDeploymentResourceSpecNode;
import com.enonic.ec.kubernetes.deployment.xpdeployment.XpDeploymentSpecChangeValidation;

@Value.Immutable
public abstract class XpNodeDeploymentPlan
{
    protected abstract XpNodeDeploymentDiff.NodeTuple nodeTuple();

    @Value.Default
    protected boolean enabled()
    {
        return true;
    }

    @Value.Default
    protected boolean newDeployment()
    {
        return false;
    }

    @Value.Default
    protected boolean enabledDisabledChanged()
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
    public int scale()
    {
        return enabled() ? nodeTuple().getNewNode().replicas() : 0;
    }

    @Value.Derived
    public boolean changeScale()
    {
        if ( nodeTuple().getOldNode().isEmpty() )
        {
            return false;
        }
        return enabledDisabledChanged() || !nodeTuple().getOldNode().get().replicas().equals( nodeTuple().getNewNode().replicas() );
    }

    @Value.Derived
    public boolean changeDisruptionBudget()
    {
        if ( nodeTuple().getOldNode().isEmpty() )
        {
            return true;
        }
        return scale() > 1 && changeScale();
    }

    @Value.Derived
    public boolean changeConfigMap()
    {
        return newDeployment() || !nodeTuple().getOldNode().get().config().equals( nodeTuple().getNewNode().config() );
    }

    @Value.Derived
    public boolean changeStatefulSet()
    {
        if ( newDeployment() || updateXp() )
        {
            return true;
        }

        if ( !nodeTuple().getOldNode().get().resources().cpu().equals( nodeTuple().getNewNode().resources().cpu() ) )
        {
            return true;
        }

        if ( !nodeTuple().getOldNode().get().resources().memory().equals( nodeTuple().getNewNode().resources().memory() ) )
        {
            return true;
        }

        if ( !nodeTuple().getOldNode().get().env().equals( nodeTuple().getNewNode().env() ) )
        {
            return true;
        }

        Optional<Properties> oldSystemProps = nodeTuple().getOldNode().get().configAsProperties( "system.properties" );
        Optional<Properties> newSystemProps = nodeTuple().getNewNode().configAsProperties( "system.properties" );

        if ( oldSystemProps.isPresent() && !oldSystemProps.equals( newSystemProps ) )
        {
            return true;
        }
        if ( newSystemProps.isPresent() && !newSystemProps.equals( oldSystemProps ) )
        {
            return true;
        }

        return false;
    }

    @Value.Check
    protected void check()
    {
        if ( nodeTuple().getOldNode().isPresent() )
        {
            XpDeploymentSpecChangeValidation.checkNode( nodeTuple().getOldNode(), nodeTuple().getNewNode() );
        }
        if ( newDeployment() )
        {
            Preconditions.checkState( !changeScale(), "Scale should never be true on new deployments" );
        }
    }
}
