package com.enonic.ec.kubernetes.operator.crd.certmanager.issuer;

import io.fabric8.kubernetes.client.CustomResource;

public class IssuerResource
    extends CustomResource
{

    private IssuerResourceSpec spec;

    public IssuerResourceSpec getSpec()
    {
        return spec;
    }

    public void setSpec( IssuerResourceSpec spec )
    {
        this.spec = spec;
    }

}
