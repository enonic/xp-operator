package com.enonic.ec.kubernetes.operator.builders;

import java.util.Map;

import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.policy.PodDisruptionBudgetSpec;

import com.enonic.ec.kubernetes.common.Builder;

import static com.enonic.ec.kubernetes.common.assertions.Assertions.assertNotNull;

public class PodDisruptionBudgetSpecBuilder
    implements Builder<PodDisruptionBudgetSpec>
{
    private Integer minAvailable;

    private Map<String, String> matchLabels;

    private PodDisruptionBudgetSpecBuilder()
    {
    }

    public static PodDisruptionBudgetSpecBuilder newBuilder()
    {
        return new PodDisruptionBudgetSpecBuilder();
    }

    public PodDisruptionBudgetSpecBuilder minAvailable( Integer val )
    {
        minAvailable = val;
        return this;
    }

    public PodDisruptionBudgetSpecBuilder matchLabels( Map<String, String> val )
    {
        matchLabels = val;
        return this;
    }

    @Override
    public PodDisruptionBudgetSpec build()
    {
        minAvailable = assertNotNull( "minAvailable", minAvailable );
        matchLabels = assertNotNull( "matchLabels", matchLabels );

        PodDisruptionBudgetSpec spec = new PodDisruptionBudgetSpec();
        spec.setMinAvailable( new IntOrString( minAvailable ) );
        spec.setSelector( new LabelSelector( null, matchLabels ) );
        return spec;
    }
}
