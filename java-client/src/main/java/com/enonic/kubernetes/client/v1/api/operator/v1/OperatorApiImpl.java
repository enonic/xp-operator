package com.enonic.kubernetes.client.v1.api.operator.v1;

import com.enonic.kubernetes.client.RawClient;
import com.enonic.kubernetes.client.v1.api.operator.OperatorVersion;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.http.HttpClient;

public class OperatorApiImpl extends RawClient implements OperatorApi {

    public OperatorApiImpl(HttpClient client, Config config) {
        super(client, config, "v1");
    }

    @Override
    protected String baseUrl() {
        return String.format("%s/%s", super.baseUrl(), "operator");
    }

    public OperatorVersion version() {
        return request( requestUrl( "version"), OperatorVersion.class);
    }
}
