package com.enonic.ec.kubernetes.operator.commands.builders;

import java.util.Map;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.policy.PodDisruptionBudgetSpec;

import com.enonic.ec.kubernetes.common.commands.Command;

@Value.Immutable
public abstract class PodDisruptionBudgetSpecBuilder
    implements Command<PodDisruptionBudgetSpec>
{
    protected abstract Integer minAvailable();

    protected abstract Map<String, String> matchLabels();

    @Override
    public PodDisruptionBudgetSpec execute()
    {
        PodDisruptionBudgetSpec spec = new PodDisruptionBudgetSpec();
        spec.setMinAvailable( new IntOrString( minAvailable() ) );
        spec.setSelector( new LabelSelector( null, matchLabels() ) );
        return spec;
    }

}
