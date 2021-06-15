package com.enonic.kubernetes.apis.xp;

public class XpClientException
    extends Exception
{
    public XpClientException( String message )
    {
        super( message );
    }

    public XpClientException( String message, Throwable cause )
    {
        super( message, cause );
    }
}
