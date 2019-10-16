package com.enonic.ec.kubernetes.operator.commands.builders;

import java.util.Collections;
import java.util.Map;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.ServiceSpec;

import com.enonic.ec.kubernetes.common.commands.Command;

@Value.Immutable
public abstract class ServiceSpecBuilder
    implements Command<ServiceSpec>
{

    protected abstract Map<String, String> selector();

    @Override
    public ServiceSpec execute()
    {
        ServiceSpec spec = new ServiceSpec();
        spec.setPorts( Collections.singletonList( new ServicePort( "xp-main", null, 8080, null, null ) ) );
        spec.setClusterIP( "None" );
        spec.setSelector( selector() );
        return spec;
    }
}
