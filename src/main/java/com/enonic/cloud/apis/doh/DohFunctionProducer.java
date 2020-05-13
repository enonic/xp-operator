package com.enonic.cloud.apis.doh;

import java.util.List;
import java.util.function.Function;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import com.enonic.cloud.apis.doh.service.DohAnswer;
import com.enonic.cloud.apis.doh.service.DohService;

@SuppressWarnings("CdiInjectionPointsInspection")
public class DohFunctionProducer
{
    @Produces
    @Singleton
    public Function<DohQueryParams, List<DohAnswer>> producerDohFunction( @RestClient DohService dohService )
    {
        return ( dohQueryParams -> dohService.query( dohQueryParams.type(), dohQueryParams.name() ).answers() );
    }
}
