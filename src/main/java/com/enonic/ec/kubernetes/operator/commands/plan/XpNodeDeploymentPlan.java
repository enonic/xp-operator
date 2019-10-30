//package com.enonic.ec.kubernetes.operator.commands.plan;
//
//import java.util.Optional;
//import java.util.Properties;
//
//import org.immutables.value.Value;
//
//import com.google.common.base.Preconditions;
//
//import com.enonic.ec.kubernetes.deployment.xpdeployment.XpDeploymentSpecChangeValidation;
//import com.enonic.ec.kubernetes.deployment.xpdeployment.spec.SpecNode;
//
//@SuppressWarnings("OptionalGetWithoutIsPresent")
//@Value.Immutable
//public abstract class XpNodeDeploymentPlan
//{
//    protected abstract XpNodeDeploymentDiff.NodeTuple nodeTuple();
//
//    protected abstract boolean isEnabled();
//
//    protected abstract boolean isNewDeployment();
//
//    protected abstract boolean isChangingEnabledDisabled();
//
//    protected abstract boolean isUpdatingXp();
//
//    public abstract String xpVersion();
//
//    @Value.Derived
//    public SpecNode node()
//    {
//        return nodeTuple().getNewNode();
//    }
//
//    @Value.Derived
//    public int scale()
//    {
//        return isEnabled() ? nodeTuple().getNewNode().replicas() : 0;
//    }
//
//    @Value.Derived
//    public boolean changeScale()
//    {
//        if ( nodeTuple().getOldNode().isEmpty() )
//        {
//            return false;
//        }
//        return isChangingEnabledDisabled() || !nodeTuple().getOldNode().get().replicas().equals( nodeTuple().getNewNode().replicas() );
//    }
//
//    @Value.Derived
//    public boolean changeDisruptionBudget()
//    {
//        if ( nodeTuple().getOldNode().isEmpty() )
//        {
//            return true;
//        }
//        return scale() > 1 && changeScale();
//    }
//
//    @Value.Derived
//    public boolean changeConfigMap()
//    {
//        return isNewDeployment() || !nodeTuple().getOldNode().get().config().equals( nodeTuple().getNewNode().config() );
//    }
//
//    @Value.Derived
//    public boolean changeStatefulSet()
//    {
//        if ( isNewDeployment() || isUpdatingXp() )
//        {
//            return true;
//        }
//
//        if ( !nodeTuple().getOldNode().get().resources().cpu().equals( nodeTuple().getNewNode().resources().cpu() ) )
//        {
//            return true;
//        }
//
//        if ( !nodeTuple().getOldNode().get().resources().memory().equals( nodeTuple().getNewNode().resources().memory() ) )
//        {
//            return true;
//        }
//
//        if ( !nodeTuple().getOldNode().get().env().equals( nodeTuple().getNewNode().env() ) )
//        {
//            return true;
//        }
//
//        // TODO: Fix for all properties
//        Optional<Properties> oldSystemProps = nodeTuple().getOldNode().get().config().getAsProperties( "system.properties" );
//        Optional<Properties> newSystemProps = nodeTuple().getNewNode().config().getAsProperties( "system.properties" );
//
//        if ( oldSystemProps.isPresent() && !oldSystemProps.equals( newSystemProps ) )
//        {
//            return true;
//        }
//        return newSystemProps.isPresent() && !newSystemProps.equals( oldSystemProps );
//    }
//
//    @Value.Check
//    protected void check()
//    {
//        if ( nodeTuple().getOldNode().isPresent() )
//        {
//            XpDeploymentSpecChangeValidation.checkNode( nodeTuple().getOldNode(), nodeTuple().getNewNode() );
//        }
//        if ( isNewDeployment() )
//        {
//            Preconditions.checkState( !changeScale(), "Scale should never be true on new deployments" );
//        }
//    }
//}
