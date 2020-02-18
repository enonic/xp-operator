package com.enonic.cloud.operator.helm.commands;

import java.util.Optional;

public interface ValueBuilder
{
    Optional<Object> buildOldValues();

    Optional<Object> buildNewValues();
}
