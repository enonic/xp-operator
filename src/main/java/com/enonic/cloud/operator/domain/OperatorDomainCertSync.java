package com.enonic.cloud.operator.domain;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.extensions.Ingress;

import com.enonic.cloud.kubernetes.Searchers;
import com.enonic.cloud.kubernetes.model.v1alpha2.domain.Domain;
import com.enonic.cloud.operator.helpers.InformerEventHandler;

@Singleton
public class OperatorDomainCertSync
    extends InformerEventHandler<Domain>
{
    @Inject
    OperatorIngressCertSync operatorIngressCertSync;

    @Inject
    Searchers searchers;

    @Override
    protected void init()
    {

    }

    @Override
    protected void onNewAdd( final Domain newResource )
    {
        relevantIngresses( newResource ).forEach( operatorIngressCertSync::handle );
    }

    @Override
    public void onUpdate( final Domain oldResource, final Domain newResource )
    {
        relevantIngresses( newResource ).forEach( operatorIngressCertSync::handle );
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
        return searchers.ingress().query().
            filter( operatorIngressCertSync::ingressRelevant ).
            filter( ingress -> operatorIngressCertSync.ingressHosts( ingress ).contains( host ) ).
            list();
    }
}
