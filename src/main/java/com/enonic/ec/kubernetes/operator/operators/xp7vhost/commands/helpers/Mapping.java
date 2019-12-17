package com.enonic.ec.kubernetes.operator.operators.xp7vhost.commands.helpers;

import java.util.Optional;

import org.immutables.value.Value;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;

@Value.Immutable
public abstract class Mapping
{
    public abstract String host();

    public abstract String node();

    public abstract String source();

    public abstract String target();

    public abstract Optional<String> idProvider();

    @Value.Derived
    public String name()
    {
        return Hashing.sha512().hashString( host() + source(), Charsets.UTF_8 ).toString().substring( 0, 10 );
    }
}
