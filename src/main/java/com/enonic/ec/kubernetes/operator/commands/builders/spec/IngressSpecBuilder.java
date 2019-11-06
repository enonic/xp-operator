package com.enonic.ec.kubernetes.operator.commands.builders.spec;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.extensions.HTTPIngressPath;
import io.fabric8.kubernetes.api.model.extensions.HTTPIngressRuleValue;
import io.fabric8.kubernetes.api.model.extensions.IngressBackend;
import io.fabric8.kubernetes.api.model.extensions.IngressRule;
import io.fabric8.kubernetes.api.model.extensions.IngressSpec;
import io.fabric8.kubernetes.api.model.extensions.IngressTLS;

import com.enonic.ec.kubernetes.common.Configuration;
import com.enonic.ec.kubernetes.crd.deployment.vhost.VHost;
import com.enonic.ec.kubernetes.crd.deployment.vhost.VHostPath;

@Value.Immutable
public abstract class IngressSpecBuilder
    extends Configuration
{
    protected abstract VHost vhost();

    protected abstract Optional<String> certificateSecretName();

    @Value.Derived
    public IngressSpec spec()
    {
        IngressSpec spec = new IngressSpec();

        if ( certificateSecretName().isPresent() )
        {
            IngressTLS tls = new IngressTLS();
            tls.setHosts( Collections.singletonList( vhost().host() ) );
            tls.setSecretName( certificateSecretName().get() );
            spec.setTls( Collections.singletonList( tls ) );
        }

        HTTPIngressRuleValue http = new HTTPIngressRuleValue();

        IngressRule ingressRule = new IngressRule();
        ingressRule.setHost( vhost().host() );
        ingressRule.setHttp( http );
        spec.setRules( Collections.singletonList( ingressRule ) );

        List<HTTPIngressPath> paths = new LinkedList<>();
        for ( VHostPath path : vhost().vHostPaths() )
        {
            paths.add( new HTTPIngressPath(
                new IngressBackend( path.pathResourceName(), new IntOrString( cfgInt( "operator.deployment.xp.port.main.number" ) ) ),
                path.path() ) );
        }
        http.setPaths( paths );

        return spec;
    }
}
