package com.enonic.cloud.operator.dns.functions;

import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import org.immutables.value.Value;
import org.wildfly.common.annotation.Nullable;

import io.fabric8.kubernetes.api.model.extensions.Ingress;

import com.enonic.cloud.common.annotations.Params;
import com.enonic.cloud.operator.dns.functions.info.IngressEnabledHosts;

import static com.enonic.cloud.common.Configuration.cfgStr;

@Value.Immutable
@Params
public abstract class RelevantIngressPredicate
    implements Predicate<Ingress>
{
    public abstract Ingress ingress();

    protected abstract IngressEnabledHosts enabledHostsFunc();

    @Value.Derived
    protected Set<String> enabledHosts()
    {
        return enabledHostsFunc().apply( ingress() );
    }

    @Value.Derived
    @Nullable
    protected String heritage()
    {
        return getHeritage( ingress() );
    }

    @Override
    public boolean test( final Ingress ingress )
    {
        Set<String> testEnabledHosts = enabledHostsFunc().apply( ingress );
        testEnabledHosts.retainAll( enabledHosts() );

        if ( testEnabledHosts.isEmpty() )
        {
            return false;
        }

        return Objects.equals( heritage(), getHeritage( ingress ) );
    }

    private String getHeritage( Ingress ingress )
    {
        if ( ingress.getMetadata().getAnnotations() == null )
        {
            return null;
        }
        return ingress.getMetadata().getAnnotations().get( cfgStr( "dns.annotations.heritage" ) );
    }
}
