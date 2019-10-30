//package com.enonic.ec.kubernetes.operator.commands.plan;
//
//import java.util.Collections;
//import java.util.Optional;
//
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.Test;
//
//import com.enonic.ec.kubernetes.DeploymentFileTest;
//
//class XpNodeDeploymentPlanSimpleTest
//    extends DeploymentFileTest
//{
//    private XpNodeDeploymentDiff loadDiff( String oldDeployment, String newDeployment )
//    {
//        return loadDiff( Optional.of( oldDeployment ), newDeployment );
//    }
//
//    private XpNodeDeploymentDiff loadDiff( Optional<String> oldDeployment, String newDeployment )
//    {
//        return ImmutableXpNodeDeploymentDiff.builder().
//            oldDeployment( oldDeployment.map( this::loadResource ) ).
//            newDeployment( loadResource( newDeployment ) ).
//            build();
//    }
//
//    protected void loadDiffExpectIllegalState( String oldDeployment, String newDeployment, String expectedMessage )
//    {
//        RuntimeException e =
//            Assertions.assertThrows( RuntimeException.class, () -> loadDiff( Optional.of( oldDeployment ), newDeployment ) );
//        Assertions.assertEquals( expectedMessage, e.getMessage() );
//    }
//
//    private void assertDeploymentPlan( XpNodeDeploymentPlan plan, int scale, boolean changeScale, boolean changeDisruptionBudget,
//                                       boolean changeConfigMap, boolean changeStatefulSet )
//    {
//        Assertions.assertEquals( scale, plan.scale(), "scale is wrong" );
//        Assertions.assertEquals( changeScale, plan.changeScale(), "changeScale is wrong" );
//        Assertions.assertEquals( changeDisruptionBudget, plan.changeDisruptionBudget(), "changeDisruptionBudget is wrong" );
//        Assertions.assertEquals( changeConfigMap, plan.changeConfigMap(), "changeConfigMap is wrong" );
//        Assertions.assertEquals( changeStatefulSet, plan.changeStatefulSet(), "changeStatefulSet is wrong" );
//    }
//
//    private void simplePlanAssert( XpNodeDeploymentDiff diff )
//    {
//        Assertions.assertEquals( Collections.EMPTY_LIST, diff.nodesRemoved() );
//        Assertions.assertEquals( 1, diff.deploymentPlans().size() );
//    }
//
//    @Test
//    public void diffNewNull()
//    {
//        IllegalStateException e =
//            Assertions.assertThrows( IllegalStateException.class, () -> ImmutableXpNodeDeploymentDiff.builder().build() );
//        Assertions.assertEquals( "Cannot build XpNodeDeploymentDiff, some of required attributes are not set [newDeployment]",
//                                 e.getMessage() );
//    }
//
//    @Test
//    public void planOldNull()
//    {
//        XpNodeDeploymentDiff diff = loadDiff( Optional.empty(), "planOldNull_new.yaml" );
//        simplePlanAssert( diff );
//        assertDeploymentPlan( diff.deploymentPlans().get( 0 ), 1, false, true, true, true );
//    }
//
//    @Test
//    public void planInvalidChange()
//    {
//        loadDiffExpectIllegalState( "planInvalidChange_old.yaml", "planInvalidChange_new1.yaml", "cannot change 'cloud'" );
//        loadDiffExpectIllegalState( "planInvalidChange_old.yaml", "planInvalidChange_new2.yaml", "cannot change 'project'" );
//        loadDiffExpectIllegalState( "planInvalidChange_old.yaml", "planInvalidChange_new3.yaml", "cannot change 'name'" );
//        loadDiffExpectIllegalState( "planInvalidChange_old.yaml", "planInvalidChange_new4.yaml", "cannot change node 'disks'" );
//        loadDiffExpectIllegalState( "planInvalidChange_old.yaml", "planInvalidChange_new5.yaml", "cannot change 'sharedDisks'" );
//    }
//
//    @Test
//    public void planNewVersion()
//    {
//        XpNodeDeploymentDiff diff = loadDiff( "planFull_old.yaml", "planFull_newVersion.yaml" );
//        simplePlanAssert( diff );
//        assertDeploymentPlan( diff.deploymentPlans().get( 0 ), 1, false, false, false, true );
//    }
//
//    @Test
//    public void planNewEnableDisable()
//    {
//        XpNodeDeploymentDiff diff = loadDiff( "planFull_old.yaml", "planFull_newDisable.yaml" );
//        simplePlanAssert( diff );
//        assertDeploymentPlan( diff.deploymentPlans().get( 0 ), 0, true, false, false, false );
//
//        diff = loadDiff( "planFull_newDisable.yaml", "planFull_old.yaml" );
//        simplePlanAssert( diff );
//        assertDeploymentPlan( diff.deploymentPlans().get( 0 ), 1, true, false, false, false );
//    }
//
//    @Test
//    public void planNewScale()
//    {
//        XpNodeDeploymentDiff diff = loadDiff( "planFull_old.yaml", "planFull_newScale.yaml" );
//        simplePlanAssert( diff );
//        assertDeploymentPlan( diff.deploymentPlans().get( 0 ), 0, true, false, false, false );
//    }
//
//    @Test
//    public void planMoreResources()
//    {
//        XpNodeDeploymentDiff diff = loadDiff( "planFull_old.yaml", "planFull_newMoreResources.yaml" );
//        simplePlanAssert( diff );
//        assertDeploymentPlan( diff.deploymentPlans().get( 0 ), 1, false, false, false, true );
//    }
//
//    @Test
//    public void planNewConfig()
//    {
//        XpNodeDeploymentDiff diff = loadDiff( "planFull_old.yaml", "planFull_newConfig.yaml" );
//        simplePlanAssert( diff );
//        assertDeploymentPlan( diff.deploymentPlans().get( 0 ), 1, false, false, true, false );
//    }
//
//    @Test
//    public void planNewConfigSystem()
//    {
//        XpNodeDeploymentDiff diff = loadDiff( "planFull_old.yaml", "planFull_newConfigSystem.yaml" );
//        simplePlanAssert( diff );
//        assertDeploymentPlan( diff.deploymentPlans().get( 0 ), 1, false, false, true, true );
//    }
//
//    @Test
//    public void planNewEnv()
//    {
//        XpNodeDeploymentDiff diff = loadDiff( "planFull_old.yaml", "planFull_newEnv.yaml" );
//        simplePlanAssert( diff );
//        assertDeploymentPlan( diff.deploymentPlans().get( 0 ), 1, false, false, false, true );
//    }
//
//    @Test
//    public void planVHost()
//    {
//        XpNodeDeploymentDiff diff = loadDiff( "planFull_old.yaml", "planFull_newVHost1.yaml" );
//        simplePlanAssert( diff );
//        assertDeploymentPlan( diff.deploymentPlans().get( 0 ), 1, false, false, true, false );
//
//        diff = loadDiff( "planFull_old.yaml", "planFull_newVHost2.yaml" );
//        simplePlanAssert( diff );
//        assertDeploymentPlan( diff.deploymentPlans().get( 0 ), 1, false, false, true, false );
//    }
//}