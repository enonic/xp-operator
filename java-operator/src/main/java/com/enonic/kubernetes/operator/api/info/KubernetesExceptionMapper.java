package com.enonic.kubernetes.operator.api.info;

import io.fabric8.kubernetes.client.KubernetesClientException;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.ExceptionHandler;

import javax.inject.Singleton;

@Produces
@Singleton
public class KubernetesExceptionMapper implements ExceptionHandler<KubernetesClientException, HttpResponse<?>> {
    @Override
    public HttpResponse<?> handle(HttpRequest request, KubernetesClientException e) {
        return HttpResponse.status( HttpStatus.valueOf( e.getCode() ) ).body( e.getStatus() );
    }
}
