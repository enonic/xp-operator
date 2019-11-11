package com.enonic.ec.kubernetes.operator.vhosts.spec;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.extensions.HTTPIngressPath;
import io.fabric8.kubernetes.api.model.extensions.HTTPIngressRuleValue;
import io.fabric8.kubernetes.api.model.extensions.IngressBackend;
import io.fabric8.kubernetes.api.model.extensions.IngressRule;
import io.fabric8.kubernetes.api.model.extensions.IngressTLS;

import com.enonic.ec.kubernetes.common.Configuration;
import com.enonic.ec.kubernetes.crd.vhost.spec.Spec;
import com.enonic.ec.kubernetes.crd.vhost.spec.SpecMapping;

@Value.Immutable
public abstract class IngressSpec
    extends Configuration
{
    protected abstract Spec vHostSpec();

    protected abstract Optional<String> certificateSecretName();

    protected abstract Function<SpecMapping, String> mappingResourceName();

    @Value.Derived
    public io.fabric8.kubernetes.api.model.extensions.IngressSpec spec()
    {
        io.fabric8.kubernetes.api.model.extensions.IngressSpec spec = new io.fabric8.kubernetes.api.model.extensions.IngressSpec();

        if ( certificateSecretName().isPresent() )
        {
            IngressTLS tls = new IngressTLS();
            tls.setHosts( Collections.singletonList( vHostSpec().host() ) );
            tls.setSecretName( certificateSecretName().get() );
            spec.setTls( Collections.singletonList( tls ) );
        }

        HTTPIngressRuleValue http = new HTTPIngressRuleValue();

        IngressRule ingressRule = new IngressRule();
        ingressRule.setHost( vHostSpec().host() );
        ingressRule.setHttp( http );
        spec.setRules( Collections.singletonList( ingressRule ) );

        List<HTTPIngressPath> paths = new LinkedList<>();
        for ( SpecMapping m : vHostSpec().mappings() )
        {
            paths.add( new HTTPIngressPath( new IngressBackend( mappingResourceName().apply( m ),
                                                                new IntOrString( cfgInt( "operator.deployment.xp.port.main.number" ) ) ),
                                            m.source() ) );
        }
        http.setPaths( paths );

        return spec;
    }
}
