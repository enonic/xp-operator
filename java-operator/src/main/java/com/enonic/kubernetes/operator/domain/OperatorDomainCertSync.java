package com.enonic.kubernetes.operator.domain;

import com.enonic.kubernetes.client.v1.domain.Domain;
import com.enonic.kubernetes.kubernetes.Informers;
import com.enonic.kubernetes.kubernetes.Searchers;
import com.enonic.kubernetes.operator.helpers.InformerEventHandler;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.quarkus.runtime.StartupEvent;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This operator triggers ingress certificate configuration on domain changes
 */
@ApplicationScoped
public class OperatorDomainCertSync
    extends InformerEventHandler<Domain>
{
    @Inject
    OperatorIngressCertSync operatorIngressCertSync;

    @Inject
    Searchers searchers;

    @Inject
    Informers informers;

    void onStart( @Observes StartupEvent ev )
    {
        listen( informers.domainInformer() );
    }

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
        String host = domain.getSpec().getHost();
        return searchers.ingress().stream().
            filter( operatorIngressCertSync::ingressRelevant ).
            filter( ingress -> operatorIngressCertSync.ingressHosts( ingress ).contains( host ) ).
            collect( Collectors.toList() );
    }
}
