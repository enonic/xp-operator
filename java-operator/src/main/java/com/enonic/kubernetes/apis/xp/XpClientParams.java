package com.enonic.kubernetes.apis.xp;

import com.enonic.kubernetes.common.annotations.Params;
import io.micrometer.core.instrument.MeterRegistry;
import org.immutables.value.Value;

@Value.Immutable
@Params
public abstract class XpClientParams
{
    public abstract String namespace();

    public abstract String nodeGroup();

    @Value.Derived
    public String url()
    {
        return String.format( "http://%s.%s.svc.cluster.local:4848", nodeGroup(), namespace() );
    }

    public abstract String username();

    public abstract String password();

    public abstract MeterRegistry registry();

    @Value.Default
    public Long timeout()
    {
        return 5000L;
    }
}
