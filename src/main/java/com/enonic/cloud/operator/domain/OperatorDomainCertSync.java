package com.enonic.cloud.operator.domain;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.networking.v1beta1.Ingress;

import com.enonic.cloud.kubernetes.Searchers;
import com.enonic.cloud.kubernetes.model.v1alpha2.domain.Domain;
import com.enonic.cloud.operator.helpers.InformerEventHandler;

/**
 * This operator triggers ingress certificate configuration on domain changes
 */
@Singleton
public class OperatorDomainCertSync
    extends InformerEventHandler<Domain>
{
    @Inject
    OperatorIngressCertSync operatorIngressCertSync;

    @Inject
    Searchers searchers;

    @Override
    protected void onNewAdd( final Domain newResource )
    {
        handle( newResource );
    }

    @Override
    public void onUpdate( final Domain oldResource, final Domain newResource )
    {
        handle( newResource );
    }

    @Override
    public void onDelete( final Domain oldResource, final boolean b )
    {
        handle( oldResource );
    }

    private void handle( final Domain domain )
    {
        relevantIngresses( domain ).forEach( operatorIngressCertSync::handle );
    }

    private List<Ingress> relevantIngresses( final Domain domain )
    {
        String host = domain.getDomainSpec().getHost();
        return searchers.ingress().stream().
            filter( operatorIngressCertSync::ingressRelevant ).
            filter( ingress -> operatorIngressCertSync.ingressHosts( ingress ).contains( host ) ).
            collect( Collectors.toList() );
    }
}
