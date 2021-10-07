package com.enonic.kubernetes.client.apis;

import io.fabric8.kubernetes.api.model.Status;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.utils.ExponentialBackoffIntervalCalculator;
import io.fabric8.kubernetes.client.utils.Serialization;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

import static io.fabric8.kubernetes.client.dsl.base.OperationSupport.requestException;

public abstract class RawClient {

    private static final Logger logger = LoggerFactory.getLogger(RawClient.class);

    private final int requestRetryBackoffLimit;
    private final ExponentialBackoffIntervalCalculator retryIntervalCalculator;

    private final OkHttpClient client;
    private final String baseUrl;

    public RawClient(final OkHttpClient client, final Config config) {
        this.client = client;
        this.requestRetryBackoffLimit = config.getRequestRetryBackoffLimit();
        this.retryIntervalCalculator = new ExponentialBackoffIntervalCalculator(config.getRequestRetryBackoffInterval(), 5);
        this.baseUrl = config.getMasterUrl() + "apis/operator.enonic.cloud/v1alpha1";
    }

    protected String baseUrl() {
        return baseUrl;
    }

    protected HttpUrl.Builder requestBuilder(String... url) {
        StringBuilder sb = new StringBuilder(baseUrl());
        for (String s : url) {
            if (!s.startsWith("/")) {
                sb.append("/");
            }
            sb.append(s);
        }
        return HttpUrl.parse(sb.toString()).newBuilder();
    }

    protected <T> T request(HttpUrl.Builder httpBuilder, Class<T> type) {
        try {
            return request(new Request.Builder().url(httpBuilder.build()), type);
        } catch (KubernetesClientException e) {
            throw e;
        } catch (Exception e) {
            throw new KubernetesClientException("RawClient request failed", e);
        }
    }

    private <T> T request(Request.Builder requestBuilder, Class<T> type) throws IOException, InterruptedException {
        Request request = requestBuilder.build();
        Response response = retryWithExponentialBackoff(client, request);
        try (ResponseBody body = response.body()) {
            assertResponseCode(request, response);
            if (type != null) {
                try (InputStream bodyInputStream = body.byteStream()) {
                    return Serialization.unmarshal(bodyInputStream, type, null);
                }
            } else {
                return null;
            }
        } catch (Exception e) {
            if (e instanceof KubernetesClientException) {
                throw e;
            }
            throw requestException(request, e);
        } finally {
            if (response != null && response.body() != null) {
                response.body().close();
            }
        }
    }

    private Response retryWithExponentialBackoff(OkHttpClient client, Request request) throws InterruptedException, IOException {
        int numRetries = 0;
        long retryInterval;
        while (true) {
            try {
                Response response = client.newCall(request).execute();
                if (numRetries < requestRetryBackoffLimit && response.code() >= 500) {
                    retryInterval = retryIntervalCalculator.getInterval(numRetries);
                    logger.debug("HTTP operation on url: {} should be retried as the response code was {}, retrying after {} millis", request.url(), response.code(), retryInterval);
                } else {
                    return response;
                }
            } catch (IOException ie) {
                if (numRetries < requestRetryBackoffLimit) {
                    retryInterval = retryIntervalCalculator.getInterval(numRetries);
                    logger.debug(String.format("HTTP operation on url: %s should be retried after %d millis because of IOException", request.url(), retryInterval), ie);
                } else {
                    throw ie;
                }
            }
            Thread.sleep(retryInterval);
            numRetries++;
        }
    }

    private void assertResponseCode(Request request, Response response) {
        int statusCode = response.code();

        if (response.isSuccessful()) {
            return;
        } else {
            String message = response.message();
            Status status = null;
            try {
                status = Serialization.unmarshal(response.body().byteStream(), Status.class, null);
                message = status.getMessage();
            } catch (Exception e) {
                // Do nothing
            }
            throw new KubernetesClientException(message, statusCode, status);
        }
    }
}
