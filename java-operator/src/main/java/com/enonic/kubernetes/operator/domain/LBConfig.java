package com.enonic.kubernetes.operator.domain;


import java.util.Optional;

import javax.enterprise.inject.Default;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "dns.lb")
public interface LBConfig
{
    @WithName("cname")
    Optional<String> cname();

    @WithName("staticIp")
    Optional<String> staticIp();

    @WithName("service.name")
    String serviceName();

    @WithName("service.namespace")
    String serviceNamespace();

    @WithName("domain.wildcardCertificate")
    String domainWildcardCertificate();

}
