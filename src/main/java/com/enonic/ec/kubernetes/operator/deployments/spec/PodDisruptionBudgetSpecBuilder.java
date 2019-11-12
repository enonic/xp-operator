package com.enonic.ec.kubernetes.operator.deployments.spec;

import java.util.Map;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.policy.PodDisruptionBudgetSpec;

@Value.Immutable
public abstract class PodDisruptionBudgetSpecBuilder
{
    protected abstract Integer minAvailable();

    protected abstract Map<String, String> matchLabels();

    @Value.Derived
    public PodDisruptionBudgetSpec spec()
    {
        PodDisruptionBudgetSpec spec = new PodDisruptionBudgetSpec();
        spec.setMinAvailable( new IntOrString( minAvailable() ) );
        spec.setSelector( new LabelSelector( null, matchLabels() ) );
        return spec;
    }
}
