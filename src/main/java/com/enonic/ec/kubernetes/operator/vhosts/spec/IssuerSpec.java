package com.enonic.ec.kubernetes.operator.vhosts.spec;

import java.util.Collections;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.crd.issuer.ImmutableIssuerResourceSpec;
import com.enonic.ec.kubernetes.crd.issuer.IssuerResourceSpec;
import com.enonic.ec.kubernetes.crd.vhost.spec.SpecCertificate;

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
