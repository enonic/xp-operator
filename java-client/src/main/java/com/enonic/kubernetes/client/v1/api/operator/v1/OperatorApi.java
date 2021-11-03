package com.enonic.kubernetes.client.v1.api.operator.v1;

import com.enonic.kubernetes.client.v1.api.operator.OperatorVersion;

public interface OperatorApi {
    OperatorVersion version();
}
