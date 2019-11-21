package com.enonic.ec.kubernetes.crd.issuer;

import io.fabric8.kubernetes.client.CustomResource;

@SuppressWarnings("unused")
public class IssuerResource
    extends CustomResource
{
    private Object status;

    private IssuerResourceSpec spec;

    public IssuerResourceSpec getSpec()
    {
        return spec;
    }

    public void setSpec( IssuerResourceSpec spec )
    {
        this.spec = spec;
    }

    public Object getStatus()
    {
        return status;
    }

    public void setStatus( final Object status )
    {
        this.status = status;
    }
}