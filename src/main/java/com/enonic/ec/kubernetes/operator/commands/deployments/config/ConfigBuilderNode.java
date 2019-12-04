package com.enonic.ec.kubernetes.operator.commands.deployments.config;

import java.util.Map;

import com.enonic.ec.kubernetes.operator.crd.deployment.spec.SpecNode;

public abstract class ConfigBuilderNode
    extends ConfigBuilder
{
    public abstract Map<String, String> create( SpecNode node );
}
