package com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7vhost.commands;

import java.util.List;

import org.immutables.value.Value;

import com.google.common.base.Preconditions;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.kubernetes.client.KubernetesClient;

import com.enonic.ec.kubernetes.operator.common.Configuration;
import com.enonic.ec.kubernetes.operator.common.commands.CombinedCommandBuilder;
import com.enonic.ec.kubernetes.operator.common.commands.ImmutableCombinedCommand;
import com.enonic.ec.kubernetes.operator.helm.ChartRepository;
import com.enonic.ec.kubernetes.operator.helm.Helm;
import com.enonic.ec.kubernetes.operator.kubectl.apply.ImmutableCommandApplyIngress;
import com.enonic.ec.kubernetes.operator.kubectl.delete.ImmutableCommandDeleteIngress;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.ResourceInfoNamespaced;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7vhost.ImmutableXp7VHostValues;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7vhost.crd.Xp7VHostResource;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7vhost.crd.spec.Xp7VHostSpecCertificateAuthority;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7vhost.info.DiffXp7VHost;

@Value.Immutable
public abstract class CommandXpVHostIngressTemplateApply
    extends Configuration
    implements CombinedCommandBuilder
{
    private static String getIngressName( Xp7VHostResource resource )
    {
        return resource.getMetadata().getName();
    }

    protected abstract KubernetesClient defaultClient();

    protected abstract Helm helm();

    protected abstract ChartRepository chartRepository();

    protected abstract ResourceInfoNamespaced<Xp7VHostResource, DiffXp7VHost> info();

    @Override
    public void addCommands( final ImmutableCombinedCommand.Builder commandBuilder )
    {
        boolean skipIngress = info().resource().getSpec().skipIngress();
        if ( info().diff().diffSpec().skipIngressChanged() && skipIngress )
        {
            deleteIngress( commandBuilder );
        }
        else if ( !skipIngress )
        {
            applyIngress( commandBuilder );
        }
    }

    protected void applyIngress( final ImmutableCombinedCommand.Builder commandBuilder )
    {
        List<HasMetadata> l = helm().templateToObjects( chartRepository().get( "v1alpha1/xpvhost" ), createValues() );
        Preconditions.checkState( l.size() == 1, "Chart has to produce 1 object" );
        Preconditions.checkState( l.get( 0 ) instanceof Ingress, "Chart has to produce 1 ingress" );
        Ingress ingress = (Ingress) l.get( 0 );

        commandBuilder.addCommand( ImmutableCommandApplyIngress.builder().
            client( defaultClient() ).
            ownerReference( info().ownerReference() ).
            namespace( info().deploymentInfo().namespaceName() ).
            name( getIngressName( info().resource() ) ).
            annotations( ingress.getMetadata().getAnnotations() ).
            spec( ingress.getSpec() ).
            build() );
    }

    private Object createValues()
    {
        return ImmutableXp7VHostValues.builder().
            info( info() ).
            issuer( getIssuer( info().resource().getSpec().certificate().authority() ) ).
            build().
            values();
    }

    private void deleteIngress( final ImmutableCombinedCommand.Builder commandBuilder )
    {
        commandBuilder.addCommand( ImmutableCommandDeleteIngress.builder().
            client( defaultClient() ).
            namespace( info().deploymentInfo().namespaceName() ).
            name( getIngressName( info().resource() ) ).
            build() );
    }

    private String getIssuer( Xp7VHostSpecCertificateAuthority authority )
    {
        switch ( authority )
        {
            case SELF_SIGNED:
                return cfgStr( "operator.certissuer.selfsigned" );
            case LETS_ENCRYPT_STAGING:
                return cfgStr( "operator.certissuer.letsencrypt.staging" );
            case LETS_ENCRYPT_PROD:
                return cfgStr( "operator.certissuer.letsencrypt.prod" );
        }
        return "none";
    }
}
