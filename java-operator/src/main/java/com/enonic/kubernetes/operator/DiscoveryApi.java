package com.enonic.kubernetes.operator;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Produces;

@Controller("/apis")
public class DiscoveryApi
{
    @Get
    @Produces("application/json")
    public HttpResponse<?> get()
    {
        return HttpResponse.status( HttpStatus.NOT_ACCEPTABLE );
    }
}
