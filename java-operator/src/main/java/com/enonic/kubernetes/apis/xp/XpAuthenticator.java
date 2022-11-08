package com.enonic.kubernetes.apis.xp;

import okhttp3.Credentials;
import okhttp3.Interceptor;
import okhttp3.Response;

import java.io.IOException;

public class XpAuthenticator
    implements Interceptor
{
    private final String credentials;

    public XpAuthenticator( String user, String password )
    {
        credentials = Credentials.basic( user, password );
    }

    @Override
    public Response intercept( final Chain chain )
        throws IOException
    {
        return chain.proceed( chain.request()
            .newBuilder()
            .header( "Authorization", credentials )
            .build() );
    }
}
