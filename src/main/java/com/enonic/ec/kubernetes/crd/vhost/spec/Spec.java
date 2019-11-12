package com.enonic.ec.kubernetes.crd.vhost.spec;

import java.util.List;

import org.immutables.value.Value;
import org.wildfly.common.annotation.Nullable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import com.enonic.ec.kubernetes.common.Configuration;

@JsonDeserialize(builder = ImmutableSpec.Builder.class)
@Value.Immutable
public abstract class Spec
    extends Configuration
{
    public abstract String host();

    @Nullable
    public abstract SpecCertificate certificate(); // TODO: Fix nullable

    public abstract List<SpecMapping> mappings();
}
