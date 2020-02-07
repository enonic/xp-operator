package com.enonic.ec.kubernetes.operator.operators.v1alpha2.xp7vhost.helpers;

import java.util.Map;

import org.immutables.value.Value;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;

@Value.Immutable
public abstract class Mapping
{
    public abstract String host();

    public abstract String nodeGroup();

    public abstract String source();

    public abstract String target();

    public abstract Map<String, String> idProviders();

    @SuppressWarnings("UnstableApiUsage")
    @Value.Derived
    public String name()
    {
        return Hashing.sha512().hashString( host() + source(), Charsets.UTF_8 ).toString().substring( 0, 10 );
    }
}
