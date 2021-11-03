package com.enonic.kubernetes.client.v1.api.operator.v1;

import com.enonic.kubernetes.client.RawClient;
import com.enonic.kubernetes.client.v1.api.operator.OperatorVersion;
import io.fabric8.kubernetes.client.Config;
import okhttp3.OkHttpClient;

public class OperatorApiImpl extends RawClient implements OperatorApi {

    public OperatorApiImpl(OkHttpClient client, Config config) {
        super(client, config, "v1");
    }

    @Override
    protected String baseUrl() {
        return String.format("%s/%s", super.baseUrl(), "operator");
    }

    public OperatorVersion version() {
        return request(requestBuilder("version"), OperatorVersion.class);
    }
}
