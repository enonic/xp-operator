package com.enonic.ec.kubernetes.operator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.client.KubernetesClient;

import com.enonic.ec.kubernetes.common.commands.CombinedCommand;
import com.enonic.ec.kubernetes.common.commands.Command;
import com.enonic.ec.kubernetes.deployment.XpDeployment.XpDeploymentResource;
import com.enonic.ec.kubernetes.operator.commands.apply.CommandApplyConfigMap;
import com.enonic.ec.kubernetes.operator.commands.apply.CommandApplyNamespace;
import com.enonic.ec.kubernetes.operator.crd.certmanager.issuer.IssuerClientProducer;

import static com.enonic.ec.kubernetes.common.assertions.Assertions.assertNotNull;

public class CommandDeployXP
    implements Command<XpDeploymentResource>
{
    private final static Logger log = LoggerFactory.getLogger( CommandDeployXP.class );

    private final KubernetesClient defaultClient;

    private final IssuerClientProducer.IssuerClient issuerClient;

    private final XpDeploymentResource resource;

    private final OwnerReference ownerReference;

    private CommandDeployXP( final Builder builder )
    {
        defaultClient = assertNotNull( "defaultClient", builder.defaultClient );
        issuerClient = assertNotNull( "issuerClient", builder.issuerClient );
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
        log.info( "Creating/Updating XP deployment for resource with uid '" + resource.getMetadata().getUid() + "'" );
        String namespaceName = resource.getSpec().getFullProjectName();

        // TODO: What to do if cloud+project of 2 different people have the same name??

        CombinedCommand.Builder commandBuilder = CombinedCommand.newBuilder();

        commandBuilder.add( CommandApplyNamespace.newBuilder().
            client( defaultClient ).
            ownerReference( ownerReference ).
            name( namespaceName ).
            build() );

        commandBuilder.add( CommandApplyConfigMap.newBuilder().
            client( defaultClient ).
            ownerReference( ownerReference ).
            name( resource.getSpec().getFullAppName() ).
            namespace( namespaceName ).
            labels( resource.getSpec().getDefaultLabels() ).
            data( resource.getSpec().getConfig() ).
            build() );

        Exception exception = commandBuilder.build().execute();

        if ( exception != null )
        {
            // TODO: What to do in this case? Update XP deployment with info maybe?
            log.error( "Failed creating XP deployment", exception );
        }

        return resource;
    }

    public static final class Builder
    {
        private KubernetesClient defaultClient;

        private XpDeploymentResource resource;

        private IssuerClientProducer.IssuerClient issuerClient;

        private Builder()
        {
        }

        public Builder defaultClient( final KubernetesClient val )
        {
            defaultClient = val;
            return this;
        }

        public Builder issuerClient( final IssuerClientProducer.IssuerClient val )
        {
            issuerClient = val;
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
