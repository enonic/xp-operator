package com.enonic.ec.kubernetes.operator.vhosts.spec;

import java.util.Collections;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.crd.vhost.spec.SpecCertificate;
import com.enonic.ec.kubernetes.operator.crd.certmanager.issuer.ImmutableIssuerResourceSpec;
import com.enonic.ec.kubernetes.operator.crd.certmanager.issuer.IssuerResourceSpec;

@Value.Immutable
public abstract class IssuerSpec
{
    protected abstract SpecCertificate certificate();

    @Value.Derived
    public IssuerResourceSpec spec()
    {
        if ( !certificate().selfSigned() )
        {
            throw new RuntimeException( "Only self signed certificates supported" );
        }
        return ImmutableIssuerResourceSpec.builder().selfSigned( Collections.emptyMap() ).build();
    }
}
