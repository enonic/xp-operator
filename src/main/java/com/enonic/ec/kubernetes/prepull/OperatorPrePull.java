package com.enonic.ec.kubernetes.prepull;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.runtime.StartupEvent;

import com.enonic.ec.kubernetes.Operator;
import com.enonic.ec.kubernetes.common.client.DefaultClientProducer;
import com.enonic.ec.kubernetes.prepull.commands.ImmutableCommandPrePullImages;

@ApplicationScoped
public class OperatorPrePull
    extends Operator
{
    @Inject
    DefaultClientProducer defaultClientProducer;

    @ConfigProperty(name = "operator.prePull.versions", defaultValue = "")
    List<String> imageVersionPrePull;

    void onStartup( @Observes StartupEvent _ev )
    {
        if ( imageVersionPrePull.size() > 0 )
        {
            runCommands( commandBuilder -> {
                ImmutableCommandPrePullImages.builder().
                    defaultClient( defaultClientProducer.client() ).
                    addAllVersions( imageVersionPrePull ).
                    build().
                    addCommands( commandBuilder );
            } );
        }
    }
}
