package com.enonic.ec.kubernetes.operator.deployments.config;

import java.util.Map;

import com.enonic.ec.kubernetes.crd.deployment.spec.SpecNode;

public abstract class ConfigBuilderNode
    extends ConfigBuilder
{
    public abstract Map<String, String> create( SpecNode node );
}
