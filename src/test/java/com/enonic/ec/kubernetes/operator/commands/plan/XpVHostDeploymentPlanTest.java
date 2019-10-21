package com.enonic.ec.kubernetes.operator.commands.plan;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.enonic.ec.kubernetes.DeploymentFileTest;
import com.enonic.ec.kubernetes.deployment.vhost.VhostPath;

class XpVHostDeploymentPlanTest
    extends DeploymentFileTest
{

    private ImmutableXpVhostDeploymentDiff loadDiff( String oldDeployment, String newDeployment )
    {
        return loadDiff( Optional.of( oldDeployment ), newDeployment );
    }

    private ImmutableXpVhostDeploymentDiff loadDiff( Optional<String> oldDeployment, String newDeployment )
    {
        return ImmutableXpVhostDeploymentDiff.builder().
            oldDeployment( oldDeployment.map( this::loadResource ) ).
            newDeployment( loadResource( newDeployment ) ).
            build();
    }

    private void assertVHostPlan( XpVhostDeploymentPlan plan, String host, boolean certificate, List<String> newPaths,
                                  List<String> pathsToDelete, boolean changeIssuer, boolean changeIngress )
    {
        Assertions.assertEquals( host, plan.vhost().host() );
        Assertions.assertEquals( certificate, plan.vhost().certificate().isPresent(), "certificate is wrong" );
        Assertions.assertEquals( newPaths, plan.newPaths().stream().map( VhostPath::path ).collect( Collectors.toList() ) );
        Assertions.assertEquals( pathsToDelete, plan.pathsToDelete().stream().map( VhostPath::path ).collect( Collectors.toList() ) );
        Assertions.assertEquals( changeIssuer, plan.changeIssuer(), "changeIssuer is wrong" );
        Assertions.assertEquals( changeIngress, plan.changeIngress(), "changeIngress is wrong" );
    }

    @Test
    public void diffNewNull()
    {
        IllegalStateException e =
            Assertions.assertThrows( IllegalStateException.class, () -> ImmutableXpVhostDeploymentDiff.builder().build() );
        Assertions.assertEquals( "Cannot build XpVhostDeploymentDiff, some of required attributes are not set [newDeployment]",
                                 e.getMessage() );
    }

    @Test
    public void planOldNull()
    {
        ImmutableXpVhostDeploymentDiff diff = loadDiff( Optional.empty(), "planOldNull_new.yaml" );
        Assertions.assertEquals( 1, diff.deploymentPlans().size() );
        XpVhostDeploymentPlan plan1 = diff.deploymentPlans().get( 0 );
        assertVHostPlan( plan1, "company.com", true, Arrays.asList( "/admin" ), Arrays.asList(), true, true );
    }

    @Test
    public void planAddVHost1()
    {
        ImmutableXpVhostDeploymentDiff diff = loadDiff( "planFull_old.yaml", "planFull_newVHost1.yaml" );
        Assertions.assertEquals( 2, diff.deploymentPlans().size() );

        XpVhostDeploymentPlan plan1 = diff.deploymentPlans().get( 0 );
        assertVHostPlan( plan1, "company1.com", false, Arrays.asList( "/asdf" ), Arrays.asList(), false, true );

        XpVhostDeploymentPlan plan2 = diff.deploymentPlans().get( 1 );
        assertVHostPlan( plan2, "company.com", true, Arrays.asList( "/admin" ), Arrays.asList(), false, false );
    }

    @Test
    public void planAddVHost2()
    {
        ImmutableXpVhostDeploymentDiff diff = loadDiff( "planFull_old.yaml", "planFull_newVHost2.yaml" );
        Assertions.assertEquals( 1, diff.deploymentPlans().size() );
        Assertions.assertEquals( 1, diff.vHostsRemoved().size() );

        XpVhostDeploymentPlan plan1 = diff.deploymentPlans().get( 0 );
        assertVHostPlan( plan1, "company1.com", true, Arrays.asList( "/asdf" ), Arrays.asList(), true, true );
    }

    @Test
    public void planAddVHost3()
    {
        ImmutableXpVhostDeploymentDiff diff = loadDiff( "planFull_newVHost1.yaml", "planFull_newVHost2.yaml" );
        Assertions.assertEquals( 1, diff.deploymentPlans().size() );
        Assertions.assertEquals( 1, diff.vHostsRemoved().size() );

        XpVhostDeploymentPlan plan1 = diff.deploymentPlans().get( 0 );
        assertVHostPlan( plan1, "company1.com", true, Arrays.asList( "/asdf" ), Arrays.asList(), true, true );
    }

    @Test
    public void planAddVHost4()
    {
        ImmutableXpVhostDeploymentDiff diff = loadDiff( "planFull_newVHost2.yaml", "planFull_newVHost1.yaml" );
        Assertions.assertEquals( 2, diff.deploymentPlans().size() );

        XpVhostDeploymentPlan plan2 = diff.deploymentPlans().get( 0 );
        assertVHostPlan( plan2, "company.com", true, Arrays.asList( "/admin" ), Arrays.asList(), true, true );

        XpVhostDeploymentPlan plan1 = diff.deploymentPlans().get( 1 );
        assertVHostPlan( plan1, "company1.com", false, Arrays.asList( "/asdf" ), Arrays.asList(), false, true );
    }
}