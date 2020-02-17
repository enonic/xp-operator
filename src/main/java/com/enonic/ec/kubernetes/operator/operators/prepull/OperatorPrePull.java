package com.enonic.ec.kubernetes.operator.operators.prepull;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.runtime.StartupEvent;

import com.enonic.ec.kubernetes.operator.operators.common.Operator;
import com.enonic.ec.kubernetes.operator.operators.common.clients.Clients;
import com.enonic.ec.kubernetes.operator.operators.prepull.commands.ImmutableCommandPrePullImages;

@ApplicationScoped
public class OperatorPrePull
    extends Operator
{
    @Inject
    Clients clients;

    @ConfigProperty(name = "operator.prePull.versions", defaultValue = " ")
    List<String> imageVersionPrePull;

    void onStartup( @Observes StartupEvent _ev )
    {
        if ( imageVersionPrePull.size() > 0 && !imageVersionPrePull.get( 0 ).equals( " " ) )
        {
            runCommands( createCmdId(), commandBuilder -> ImmutableCommandPrePullImages.builder().
                clients( clients ).
                addAllVersions( imageVersionPrePull ).
                build().
                addCommands( commandBuilder ) );
        }
    }
}
