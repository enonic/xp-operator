package com.enonic.kubernetes.helm.values;

import io.smallrye.config.ConfigMapping;

import java.util.Map;

@ConfigMapping(prefix = "operator.charts.values.backup.agent.env")
public interface ConfigMapper
{
    Map<String, String> map();
}
