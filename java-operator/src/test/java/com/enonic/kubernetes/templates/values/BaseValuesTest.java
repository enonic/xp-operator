package com.enonic.kubernetes.templates.values;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BaseValuesTest
{
    @Test
    @SuppressWarnings("unchecked")
    void overridesReplaceConfiguredValues()
    {
        final BaseValues values = new BaseValues( "cluster", Map.of( "operator.charts.values.pods.nodeGroupAntiAffinity", "true",
                                                                     "operator.charts.values.pods.imagePullSecretNames", "a,b",
                                                                     "operator.charts.values.resources.cpu.defaultRequests", "500m" ) );

        final Map<String, Object> pods = (Map<String, Object>) values.get( "pods" );
        final Map<String, Object> cpu = (Map<String, Object>) ( (Map<String, Object>) values.get( "resources" ) ).get( "cpu" );
        assertThat( pods.get( "nodeGroupAntiAffinity" ) ).isEqualTo( true );
        assertThat( (String[]) pods.get( "imagePullSecretNames" ) ).containsExactly( "a", "b" );
        assertThat( pods.get( "sysctlInitContainer" ) ).isEqualTo( true );
        assertThat( cpu.get( "defaultRequests" ) ).isEqualTo( "500m" );
    }

    @Test
    void overrideOutsideChartValuesIsRejected()
    {
        assertThatThrownBy( () -> new BaseValues( "cluster", Map.of( "pods.nodeGroupAntiAffinity", "true" ) ) ).hasMessage(
            "'pods.nodeGroupAntiAffinity' does not start with 'operator.charts.values.'" );
    }
}
