package com.enonic.kubernetes.operator.xp8deployment;

import java.io.File;
import java.util.Map;

import org.junit.jupiter.api.Test;

import tools.jackson.dataformat.yaml.YAMLMapper;

import com.enonic.kubernetes.crd.v1.Xp8Deployment;
import com.enonic.kubernetes.templates.values.BaseValues;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Xp8DeploymentValueBuilderTest
{
    @Test
    void missingLogbackFileFailsRendering()
    {
        final Xp8Deployment deployment = YAMLMapper.builder().build().readValue(
            new File( "src/test/resources/com/enonic/kubernetes/operator/templates/v1/xp8deployment/simple.yaml" ), Xp8Deployment.class );
        final BaseValues baseValues = new BaseValues( "cluster", Map.of( "operator.charts.values.logbackFile", "does/not/exist.xml" ) );

        assertThatThrownBy(
            () -> new OperatorXp8DeploymentTemplate.Xp8DeploymentValueBuilder( baseValues, "xp-operator", "xp-operator-hazelcast" ).apply(
                deployment ) ).hasMessage( "Cannot read 'does/not/exist.xml'" );
    }
}
