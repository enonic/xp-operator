package com.enonic.ec.kubernetes.operator.commands.builders.config;

import java.util.Map;

public interface ConfigBuilder
{
    Map<String, String> create( Map<String, String> defaultConfig );
}
