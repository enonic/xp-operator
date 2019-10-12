package com.enonic.ec.kubernetes.operator.commands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.client.KubernetesClient;

import com.enonic.ec.kubernetes.common.commands.CombinedCommand;
import com.enonic.ec.kubernetes.common.commands.Command;
import com.enonic.ec.kubernetes.common.crd.XpDeployment.XpDeploymentResource;

import static com.enonic.ec.kubernetes.common.assertions.Assertions.assertNotNull;

public class CommandDeployXP
    implements Command<XpDeploymentResource>
{
    private final Logger log = LoggerFactory.getLogger( CommandDeployXP.class );

    private final KubernetesClient client;

    private final XpDeploymentResource resource;

    private final OwnerReference ownerReference;

    private CommandDeployXP( final Builder builder )
    {
        client = assertNotNull( "client", builder.client );
        resource = assertNotNull( "resource", builder.resource );
        ownerReference = new OwnerReference( resource.getApiVersion(), true, true, resource.getKind(), resource.getMetadata().getName(),
                                             resource.getMetadata().getUid() );
    }

    public static Builder newBuilder()
    {
        return new Builder();
    }

    @Override
    public XpDeploymentResource execute()
    {
        log.info( "Creating XP deployment for resource with uid '" + resource.getMetadata().getUid() + "'" );
        String namespaceName = resource.getSpec().getFullProjectName();

        CombinedCommand.Builder commandBuilder = CombinedCommand.newBuilder();

        commandBuilder.add( CommandCreateNamespace.newBuilder().
            client( client ).
            ownerReference( ownerReference ).
            name( namespaceName ).
            labels( resource.getSpec().getDefaultLabels() ).
            build() );

        commandBuilder.add( CommandCreateConfigMap.newBuilder().
            client( client ).
            ownerReference( ownerReference ).
            name( resource.getSpec().getFullAppName() ).
            namespace( namespaceName ).
            labels( resource.getSpec().getDefaultLabels() ).
            data( resource.getSpec().getConfig() ).
            build() );

        Exception exception = commandBuilder.build().execute();

        if ( exception != null )
        {
            if ( exception instanceof CommandCreateNamespace.NamespaceExists )
            {
                // A namespace with this name already exists
                log.info( "Aborted because namespace '" + namespaceName + "' already exists" );
            }
            else
            {
                log.error( "Failed creating XP deployment", exception );
                log.info( "Cleaning up the failed deployment" );
                CommandDeleteNamespace.newBuilder().
                    client( client ).
                    name( namespaceName ).
                    build().
                    execute();
            }
        }

        return resource;
    }

    public static final class Builder
    {
        private KubernetesClient client;

        private XpDeploymentResource resource;

        private Builder()
        {
        }

        public Builder client( final KubernetesClient val )
        {
            client = val;
            return this;
        }

        public Builder resource( final XpDeploymentResource val )
        {
            resource = val;
            return this;
        }

        public CommandDeployXP build()
        {
            return new CommandDeployXP( this );
        }
    }
}
