package com.enonic.kubernetes.client.apis.operator;

import com.enonic.kubernetes.client.api.operator.OperatorVersion;
import com.enonic.kubernetes.client.apis.RawClient;
import io.fabric8.kubernetes.client.Config;
import okhttp3.OkHttpClient;

public class OperatorApiImpl extends RawClient implements OperatorApi {

    public OperatorApiImpl(OkHttpClient client, Config config) {
        super(client, config);
    }

    @Override
    protected String baseUrl() {
        return String.format("%s/%s", super.baseUrl(), "operator");
    }

    public OperatorVersion version() {
        return request(requestBuilder("version"), OperatorVersion.class);
    }
}
