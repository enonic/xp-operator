package com.enonic.kubernetes.apis.cloudflare.service.model;

import java.util.List;

import org.wildfly.common.annotation.Nullable;

public abstract class ApiResponse<T>
{
    public abstract boolean success();

    public abstract List<ApiResponseMessage> messages();

    public abstract List<ApiResponseMessage> errors();

    @Nullable
    public abstract ApiResponseInfo result_info();

    public abstract T result();
}
