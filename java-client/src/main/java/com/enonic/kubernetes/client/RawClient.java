package com.enonic.kubernetes.client;

import io.fabric8.kubernetes.api.model.Status;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.http.HttpClient;
import io.fabric8.kubernetes.client.http.HttpRequest;
import io.fabric8.kubernetes.client.http.HttpResponse;
import io.fabric8.kubernetes.client.utils.ExponentialBackoffIntervalCalculator;
import io.fabric8.kubernetes.client.utils.Serialization;
import okhttp3.HttpUrl;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;

public abstract class RawClient {

    private static final Logger logger = LoggerFactory.getLogger(RawClient.class);

    private final int requestRetryBackoffLimit;
    private final ExponentialBackoffIntervalCalculator retryIntervalCalculator;

    private final HttpClient client;
    private final String baseUrl;

    public RawClient(final HttpClient client, final Config config, String apiVersion) {
        this.client = client;
        this.requestRetryBackoffLimit = config.getRequestRetryBackoffLimit();
        this.retryIntervalCalculator = new ExponentialBackoffIntervalCalculator(config.getRequestRetryBackoffInterval(), 5);
        this.baseUrl = String.format("%s%s%s", config.getMasterUrl(), "apis/operator.enonic.cloud/", apiVersion);
    }

    protected String baseUrl() {
        return baseUrl;
    }

    protected HttpUrl.Builder requestUrlBuilder(String... url) {
        StringBuilder sb = new StringBuilder(baseUrl());
        for (String s : url) {
            if (!s.startsWith("/")) {
                sb.append("/");
            }
            sb.append(s);
        }
        return Objects.requireNonNull(HttpUrl.parse(sb.toString())).newBuilder();
    }

    protected <T> T request(HttpUrl.Builder httpBuilder, Class<T> type) {
        try {
            return request(client.newHttpRequestBuilder().url(httpBuilder.build().url()), type);
        } catch (KubernetesClientException e) {
            throw e;
        } catch (Exception e) {
            throw new KubernetesClientException("RawClient request failed", e);
        }
    }

    private <T> T request(HttpRequest.Builder requestBuilder, Class<T> type) throws IOException, InterruptedException {
        HttpRequest request = requestBuilder.build();
        return retryWithExponentialBackoff(client, request, type);
    }

    private <T> T retryWithExponentialBackoff(HttpClient client, HttpRequest request, Class<T> type) throws InterruptedException, IOException {
        int numRetries = 0;
        long retryInterval;
        while (true) {
            try {
                HttpResponse<T> response = client.send(request, type);
                if (numRetries < requestRetryBackoffLimit && response.code() >= 500) {
                    retryInterval = retryIntervalCalculator.getInterval(numRetries);
                    logger.debug("HTTP operation on url: {} should be retried as the response code was {}, retrying after {} millis", request.uri(), response.code(), retryInterval);
                } else {
                    return response.body();
                }
            } catch (IOException ie) {
                if (numRetries < requestRetryBackoffLimit) {
                    retryInterval = retryIntervalCalculator.getInterval(numRetries);
                    logger.debug(String.format("HTTP operation on url: %s should be retried after %d millis because of IOException", request.uri(), retryInterval), ie);
                } else {
                    throw ie;
                }
            }
            //noinspection BusyWait
            Thread.sleep(retryInterval);
            numRetries++;
        }
    }

    private void assertResponseCode(Response response) {
        int statusCode = response.code();

        if (!response.isSuccessful()) {
            String message = response.message();
            Status status = null;
            try {
                assert response.body() != null;
                status = Serialization.unmarshal(response.body().byteStream(), Status.class, null);
                message = status.getMessage();
            } catch (Exception e) {
                // Do nothing
            }

            if (Objects.equals(message, "")) {
                message = "Status code " + statusCode;
            }

            throw new KubernetesClientException(message, statusCode, status);
        }
    }
}
