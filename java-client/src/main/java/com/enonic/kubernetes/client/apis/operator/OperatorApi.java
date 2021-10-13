package com.enonic.kubernetes.client.apis.operator;

import com.enonic.kubernetes.client.api.operator.OperatorVersion;

public interface OperatorApi {
    OperatorVersion version();
}
