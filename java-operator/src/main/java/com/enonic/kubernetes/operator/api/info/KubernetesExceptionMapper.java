package com.enonic.kubernetes.operator.api.info;

import io.fabric8.kubernetes.client.KubernetesClientException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class KubernetesExceptionMapper implements ExceptionMapper<KubernetesClientException> {
    @Override
    public Response toResponse(KubernetesClientException e) {
        return Response.status(e.getCode()).entity(e.getStatus()).build();
    }
}
