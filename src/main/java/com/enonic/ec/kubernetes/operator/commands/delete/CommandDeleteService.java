package com.enonic.ec.kubernetes.operator.commands.delete;

import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.client.KubernetesClient;

import com.enonic.ec.kubernetes.common.commands.Command;

@Value.Immutable
public abstract class CommandDeleteService
    implements Command<Boolean>
{
    private final static Logger log = LoggerFactory.getLogger( CommandDeleteService.class );

    protected abstract KubernetesClient client();

    protected abstract String name();

    protected abstract String namespace();

    @Override
    public Boolean execute()
    {
        log.info( "Deleting in Namespace '" + namespace() + "' Service '" + name() );
        return client().services().inNamespace( namespace() ).withName( name() ).delete();
    }
}
