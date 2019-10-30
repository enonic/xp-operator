package com.enonic.ec.kubernetes.deployment.vhost;

import java.util.List;
import java.util.Optional;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.deployment.xpdeployment.spec.SpecVHostCertificate;

@Value.Immutable
public abstract class VHost
{
    public abstract String host();

    public abstract Optional<SpecVHostCertificate> certificate();

    public abstract List<VHostPath> vHostPaths();

    public abstract String vHostResourceName();

    @Override
    public String toString()
    {
        return host();
    }
}
