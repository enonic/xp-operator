package com.enonic.ec.kubernetes.crd.commands;

import java.util.Collections;

import org.immutables.value.Value;

import com.google.common.base.Preconditions;

import io.fabric8.kubernetes.api.model.OwnerReference;

import com.enonic.ec.kubernetes.common.Configuration;
import com.enonic.ec.kubernetes.common.commands.Command;
import com.enonic.ec.kubernetes.crd.deployment.ImmutableXpDeploymentNamingHelper;
import com.enonic.ec.kubernetes.crd.deployment.XpDeploymentNamingHelper;
import com.enonic.ec.kubernetes.crd.deployment.XpDeploymentResource;
import com.enonic.ec.kubernetes.crd.vhost.XpVHostResource;
import com.enonic.ec.kubernetes.crd.vhost.client.XpVHostClient;
import com.enonic.ec.kubernetes.crd.vhost.spec.Spec;


@Value.Immutable
public abstract class CommandCreateXpVHost
    extends Configuration
    implements Command<XpVHostResource>
{
    protected abstract XpVHostClient client();

    protected abstract String apiVersion();

    protected abstract XpDeploymentResource owner();

    protected abstract Spec spec();

    @Value.Derived
    protected XpDeploymentNamingHelper namingHelper()
    {
        return ImmutableXpDeploymentNamingHelper.builder().spec( owner().getSpec() ).build();
    }

    @Value.Check
    protected void check()
    {
        String operatorApiVersion = cfgStr( "operator.crd.xp.apiVersion" );
        Preconditions.checkState( apiVersion().equals( operatorApiVersion ), "Only version '" + operatorApiVersion + "' allowed" );
    }

    @Override
    public XpVHostResource execute()
    {
        XpVHostResource resource = new XpVHostResource();
        resource.setApiVersion( apiVersion() );
        resource.setKind( cfgStr( "operator.crd.xp.vhosts.kind" ) );
        resource.getMetadata().setName( spec().host().replace( ".", "-" ) );
        resource.getMetadata().setOwnerReferences( Collections.singletonList( createOwnerReference() ) );
        resource.setSpec( spec() );
        return client().client().inNamespace( namingHelper().defaultNamespaceName() ).createOrReplace( resource );
    }

    private OwnerReference createOwnerReference()
    {
        return new OwnerReference( owner().getApiVersion(), true, true, owner().getKind(), owner().getMetadata().getName(),
                                   owner().getMetadata().getUid() );
    }
}
