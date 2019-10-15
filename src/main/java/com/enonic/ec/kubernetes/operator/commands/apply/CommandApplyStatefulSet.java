package com.enonic.ec.kubernetes.operator.commands.apply;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetSpec;
import io.fabric8.kubernetes.client.KubernetesClient;

import com.enonic.ec.kubernetes.operator.commands.common.CommandApplyResource;

import static com.enonic.ec.kubernetes.common.assertions.Assertions.assertNotNull;

public class CommandApplyStatefulSet
    extends CommandApplyResource<StatefulSet>
{
    private final static Logger log = LoggerFactory.getLogger( CommandApplyStatefulSet.class );

    private final KubernetesClient client;

    private final StatefulSetSpec spec;

    private CommandApplyStatefulSet( final Builder builder )
    {
        super( builder );
        client = assertNotNull( "client", builder.client );
        spec = assertNotNull( "spec", builder.spec );
    }

    public static Builder newBuilder()
    {
        return new Builder();
    }

    @Override
    protected StatefulSet fetchResource()
    {
        return client.apps().statefulSets().inNamespace( namespace ).withName( name ).get();
    }

    @Override
    protected StatefulSet apply( final ObjectMeta metadata )
    {
        StatefulSet statefulSet = new StatefulSet();
        statefulSet.setMetadata( metadata );
        statefulSet.setSpec( spec );
        return client.apps().statefulSets().inNamespace( namespace ).createOrReplace( statefulSet );
    }

    public static final class Builder
        extends CommandApplyResource.Builder<Builder>
    {
        private KubernetesClient client;

        private StatefulSetSpec spec;

        public Builder client( final KubernetesClient val )
        {
            client = val;
            return this;
        }

        public Builder spec( final StatefulSetSpec val )
        {
            spec = val;
            return this;
        }

        public CommandApplyStatefulSet build()
        {
            return new CommandApplyStatefulSet( this );
        }
    }
}
