package com.enonic.ec.kubernetes.operator.commands.builders;

import java.util.Collections;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.common.commands.Command;
import com.enonic.ec.kubernetes.deployment.xpdeployment.XpDeploymentResourceSpecVhostCertificate;
import com.enonic.ec.kubernetes.operator.crd.certmanager.issuer.ImmutableIssuerResourceSpec;
import com.enonic.ec.kubernetes.operator.crd.certmanager.issuer.IssuerResourceSpec;

@Value.Immutable
public abstract class IssuerSpecBuilder
    implements Command<IssuerResourceSpec>
{

    protected abstract XpDeploymentResourceSpecVhostCertificate certificate();

    @Override
    public IssuerResourceSpec execute()
        throws Exception
    {
        if ( !certificate().selfSigned() )
        {
            throw new Exception( "Only self signed certificates supported" );
        }
        return ImmutableIssuerResourceSpec.builder().selfSigned( Collections.emptyMap() ).build();
    }
}
