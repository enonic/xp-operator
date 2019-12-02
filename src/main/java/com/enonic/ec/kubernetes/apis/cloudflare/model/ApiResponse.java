package com.enonic.ec.kubernetes.apis.cloudflare.model;

import java.util.List;

import org.wildfly.common.annotation.Nullable;

public abstract class ApiResponse<T>
{
    public abstract boolean success();

    public abstract List<String> messages();

    public abstract List<ApiResponseError> errors();

    @Nullable
    public abstract ApiResponseInfo result_info();

    public abstract T result();
}
