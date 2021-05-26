package com.enonic.kubernetes.apis.doh;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.enonic.kubernetes.apis.doh.service.DohResponse;
import com.enonic.kubernetes.apis.doh.service.DohService;

import static com.enonic.kubernetes.apis.ApiUtils.formatWebApplicationException;
import static com.enonic.kubernetes.common.SingletonAssert.singletonAssert;

@Singleton
public class DohServiceWrapper
{
    private static final Logger log = LoggerFactory.getLogger( DohServiceWrapper.class );

    private DohService doh;

    @Inject
    public DohServiceWrapper( @RestClient final DohService doh )
    {
        singletonAssert(this, "constructor");
        this.doh = doh;
    }

    public DohResponse query( final String type, final String name )
    {
        try
        {
            log.info( String.format( "DOH: QUERY (%s) %s", type, name ) );
            return doh.query( type, name );
        }
        catch ( WebApplicationException e )
        {
            throw formatWebApplicationException( e, "DOH: QUERY Failed" );
        }
    }
}
