package com.enonic.kubernetes.operator.xp8deployment;

import org.junit.jupiter.api.Test;

import com.enonic.kubernetes.crd.v1.Xp8Deployment;
import com.enonic.kubernetes.crd.v1.Xp8DeploymentStatus;

import static org.assertj.core.api.Assertions.assertThat;

class PredicatesTest
{
    @Test
    void runningWithoutStatus()
    {
        assertThat( Predicates.running().test( new Xp8Deployment() ) ).isFalse();
    }

    @Test
    void running()
    {
        final Xp8Deployment deployment = new Xp8Deployment();
        final Xp8DeploymentStatus status = new Xp8DeploymentStatus();
        status.setState( Xp8DeploymentStatus.State.RUNNING );
        deployment.setStatus( status );

        assertThat( Predicates.running().test( deployment ) ).isTrue();
    }
}
