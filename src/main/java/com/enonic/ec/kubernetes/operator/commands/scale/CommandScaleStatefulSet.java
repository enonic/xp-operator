package com.enonic.ec.kubernetes.operator.commands.scale;

import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.KubernetesClient;

import com.enonic.ec.kubernetes.common.commands.Command;

@Value.Immutable
public abstract class CommandScaleStatefulSet
    implements Command<StatefulSet>
{
    private final static Logger log = LoggerFactory.getLogger( CommandScaleStatefulSet.class );

    protected abstract KubernetesClient client();

    protected abstract String name();

    protected abstract String namespace();

    protected abstract int scale();

    @Override
    public StatefulSet execute()
    {
        log.info( "Changing in Namespace '" + namespace() + "' StatefulSet '" + name() + "' scale to " + scale() );
        return client().apps().statefulSets().
            inNamespace( namespace() ).
            withName( name() ).
            scale( scale() );
    }
}
