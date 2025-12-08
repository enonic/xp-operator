package com.enonic.kubernetes.operator.api.info;

import io.fabric8.kubernetes.client.KubernetesClientException;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class KubernetesExceptionMapper implements ExceptionMapper<KubernetesClientException> {
    @Override
    public Response toResponse(KubernetesClientException e) {
        return Response.status(e.getCode()).entity(e.getStatus()).build();
    }
}
