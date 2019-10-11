package com.enonic.ec.kubernetes.operator.commands;

import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.client.KubernetesClient;

import com.enonic.ec.kubernetes.common.commands.Command;
import com.enonic.ec.kubernetes.common.crd.XpDeployment.XpDeploymentResource;

import static com.enonic.ec.kubernetes.common.assertions.Assertions.assertNotNull;

public class CommandDeployXP
    implements Command<XpDeploymentResource>
{
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
        throws CommandCreateNamespace.NamespaceExists
    {
        String namespaceName = resource.getSpec().getFullProjectName();

        CommandCreateNamespace.newBuilder().
            client( client ).
            ownerReference( ownerReference ).
            name( namespaceName ).
            build().
            execute();

        CommandCreateConfigMap.newBuilder().
            client( client ).
            ownerReference( ownerReference ).
            name( resource.getSpec().getFullAppName() ).
            namespace( namespaceName ).
            labels( resource.getSpec().getDefaultLabels() ).
            data( resource.getSpec().getConfig() ).
            build().
            execute();

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
