package com.enonic.ec.kubernetes.operator.kubectl.newapply.mapping;

import java.util.Optional;

public interface ValueBuilder
{
    Optional<Object> buildOldValues();

    Optional<Object> buildNewValues();
}
