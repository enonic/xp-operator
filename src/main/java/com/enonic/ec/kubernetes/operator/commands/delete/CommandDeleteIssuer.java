package com.enonic.ec.kubernetes.operator.commands.delete;

import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.enonic.ec.kubernetes.common.commands.Command;
import com.enonic.ec.kubernetes.operator.crd.certmanager.issuer.IssuerClientProducer;

@Value.Immutable
public abstract class CommandDeleteIssuer
    implements Command<Boolean>
{
    private final static Logger log = LoggerFactory.getLogger( CommandDeleteIssuer.class );

    protected abstract IssuerClientProducer.IssuerClient client();

    protected abstract String name();

    protected abstract String namespace();

    @Override
    public Boolean execute()
    {
        log.info( "Deleting in Namespace '" + namespace() + "' Issuer '" + name() );
        return client().getClient().inNamespace( namespace() ).withName( name() ).delete();
    }
}
