package com.enonic.ec.kubernetes.operator.commands.builders;

import java.util.Map;
import java.util.stream.Collectors;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.ServiceSpec;

import com.enonic.ec.kubernetes.common.Configuration;
import com.enonic.ec.kubernetes.common.commands.Command;

@Value.Immutable
public abstract class ServiceSpecBuilder
    extends Configuration
    implements Command<ServiceSpec>
{
    protected abstract Map<String, String> selector();

    protected abstract Map<String, Integer> ports();

    @Value.Default
    protected String clusterIP()
    {
        return "None";
    }

    @Value.Default
    protected Boolean publishNotReadyAddresses()
    {
        return false;
    }

    @Override
    public ServiceSpec execute()
    {
        ServiceSpec spec = new ServiceSpec();
        spec.setPorts( ports().entrySet().stream().map( e -> new ServicePort( e.getKey(), null, e.getValue(), null, null ) ).collect(
            Collectors.toList() ) );
        spec.setClusterIP( clusterIP() );
        spec.setSelector( selector() );
        spec.setPublishNotReadyAddresses( publishNotReadyAddresses() );
        return spec;
    }
}
