package com.enonic.ec.kubernetes.operator.commands.builders;

import java.util.Collections;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.extensions.HTTPIngressPath;
import io.fabric8.kubernetes.api.model.extensions.HTTPIngressRuleValue;
import io.fabric8.kubernetes.api.model.extensions.IngressBackend;
import io.fabric8.kubernetes.api.model.extensions.IngressRule;
import io.fabric8.kubernetes.api.model.extensions.IngressSpec;
import io.fabric8.kubernetes.api.model.extensions.IngressTLS;

import com.enonic.ec.kubernetes.common.commands.Command;
import com.enonic.ec.kubernetes.deployment.xpdeployment.XpDeploymentResourceSpecVhost;

@Value.Immutable
public abstract class IngressSpecBuilder
    implements Command<IngressSpec>
{

    protected abstract XpDeploymentResourceSpecVhost vhost();

    protected abstract String certificateSecretName();

    protected abstract String serviceName();

    protected abstract Integer servicePort();

    @Override
    public IngressSpec execute()
    {
        IngressSpec spec = new IngressSpec();
        IngressTLS tls = new IngressTLS();
        tls.setHosts( Collections.singletonList( vhost().host() ) );
        tls.setSecretName( certificateSecretName() );
        spec.setTls( Collections.singletonList( tls ) );

        HTTPIngressRuleValue http = new HTTPIngressRuleValue();
        http.setPaths( Collections.singletonList(
            new HTTPIngressPath( new IngressBackend( serviceName(), new IntOrString( servicePort() ) ), "/" ) ) );

        IngressRule ingressRule = new IngressRule();
        ingressRule.setHost( vhost().host() );
        ingressRule.setHttp( http );
        spec.setRules( Collections.singletonList( ingressRule ) );

        return spec;
    }
}
