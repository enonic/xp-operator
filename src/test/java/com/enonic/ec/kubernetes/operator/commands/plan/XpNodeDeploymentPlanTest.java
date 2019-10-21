package com.enonic.ec.kubernetes.operator.commands.plan;

import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.enonic.ec.kubernetes.DeploymentFileTest;

class XpNodeDeploymentPlanTest
    extends DeploymentFileTest
{
    private XpNodeDeploymentDiff loadDiff( Optional<String> oldDeployment, String newDeployment )
    {
        return ImmutableXpNodeDeploymentDiff.builder().
            oldDeployment( oldDeployment.isPresent() ? Optional.of( loadResource( oldDeployment.get() ) ) : Optional.empty() ).
            newDeployment( loadResource( newDeployment ) ).
            build();
    }

    private void assertDeploymentPlan( XpNodeDeploymentPlan plan, boolean enabledDisabledChanged, boolean updateXp, boolean changeScale,
                                       boolean changeDisruptionBudget, boolean changeConfigMap, boolean changeStatefulSet )
    {
        Assertions.assertEquals( enabledDisabledChanged, plan.enabledDisabledChanged() );
        Assertions.assertEquals( updateXp, plan.updateXp() );
        Assertions.assertEquals( changeScale, plan.changeScale() );
        Assertions.assertEquals( changeDisruptionBudget, plan.changeDisruptionBudget() );
        Assertions.assertEquals( changeConfigMap, plan.changeConfigMap() );
        Assertions.assertEquals( changeStatefulSet, plan.changeStatefulSet() );
    }

    @Test
    public void diffNewNull()
    {
        IllegalStateException e =
            Assertions.assertThrows( IllegalStateException.class, () -> ImmutableXpNodeDeploymentDiff.builder().build() );
        Assertions.assertEquals( "Cannot build XpNodeDeploymentDiff, some of required attributes are not set [newDeployment]",
                                 e.getMessage() );
    }


    @Test
    public void planOldNullFull()
    {
        XpNodeDeploymentDiff diff = loadDiff( Optional.empty(), "planOldNullFull-new.yaml" );

        Assertions.assertEquals( Collections.EMPTY_LIST, diff.nodesRemoved() );
        Assertions.assertEquals( 1, diff.deploymentPlans().size() );
        XpNodeDeploymentPlan plan = diff.deploymentPlans().get( 0 );
        assertDeploymentPlan( plan, false, false, false, true, true, true );
    }
}