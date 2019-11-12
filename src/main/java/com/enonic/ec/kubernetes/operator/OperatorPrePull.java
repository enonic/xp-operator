package com.enonic.ec.kubernetes.operator;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.runtime.StartupEvent;

import com.enonic.ec.kubernetes.common.client.DefaultClientProducer;
import com.enonic.ec.kubernetes.common.commands.ImmutableCombinedKubernetesCommand;
import com.enonic.ec.kubernetes.operator.prepull.ImmutablePrePullImages;

@ApplicationScoped
public class OperatorPrePull
{
    private final static Logger log = LoggerFactory.getLogger( OperatorPrePull.class );

    @Inject
    DefaultClientProducer defaultClientProducer;

    @ConfigProperty(name = "operator.prePull.versions", defaultValue = "")
    List<String> imageVersionPrePull;

    void onStartup( @Observes StartupEvent _ev )
    {
        if ( imageVersionPrePull.size() > 0 )
        {
            log.info( "Pre-Pulling images in cluster for versions: " + imageVersionPrePull );
            try
            {
                ImmutableCombinedKubernetesCommand.Builder commandBuilder = ImmutableCombinedKubernetesCommand.builder();

                ImmutablePrePullImages.builder().
                    defaultClient( defaultClientProducer.client() ).
                    addAllVersions( imageVersionPrePull ).
                    build().
                    addCommands( commandBuilder );

                commandBuilder.build().execute();
            }
            catch ( Exception e )
            {
                log.error( "Failed to pre pull images", e );
            }
        }
    }
}
