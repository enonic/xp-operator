package com.enonic.cloud.operator.operators.v1alpha2.xp7deployment;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.rbac.PolicyRule;
import io.fabric8.kubernetes.api.model.rbac.Role;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.fabric8.kubernetes.api.model.rbac.RoleRef;
import io.fabric8.kubernetes.api.model.rbac.Subject;
import io.fabric8.kubernetes.client.Watcher;
import io.quarkus.runtime.StartupEvent;

import com.enonic.cloud.operator.crd.xp7.v1alpha2.deployment.V1alpha2Xp7Deployment;
import com.enonic.cloud.operator.helm.ChartRepository;
import com.enonic.cloud.operator.helm.Helm;
import com.enonic.cloud.operator.helm.commands.ImmutableHelmKubeCmdBuilder;
import com.enonic.cloud.operator.kubectl.ImmutableKubeCmd;
import com.enonic.cloud.operator.kubectl.base.ImmutableKubeCommandOptions;
import com.enonic.cloud.operator.operators.common.OperatorNamespaced;
import com.enonic.cloud.operator.operators.common.cache.Caches;
import com.enonic.cloud.operator.operators.common.clients.Clients;
import com.enonic.cloud.operator.operators.v1alpha2.xp7deployment.info.ImmutableInfoXp7Deployment;
import com.enonic.cloud.operator.operators.v1alpha2.xp7deployment.info.InfoXp7Deployment;

import static com.enonic.cloud.operator.operators.common.BackupRestore.isBeingRestored;

@Singleton
public class OperatorXp7Deployments
    extends OperatorNamespaced
{
    private static final Logger log = LoggerFactory.getLogger( OperatorXp7Deployments.class );

    @Inject
    Clients clients;

    @Inject
    Caches caches;

    @Inject
    Helm helm;

    @ConfigProperty(name = "operator.helm.imageTemplate")
    String imageTemplate;

    @Inject
    @Named("local")
    ChartRepository chartRepository;

    @Inject
    @Named("baseValues")
    Map<String, Object> baseValues;

    void onStartup( @Observes StartupEvent _ev )
    {
        caches.getDeploymentCache().addEventListener( this::watch );
        log.info( "Started listening for Xp7Deployment events" );
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private void watch( final String actionId, final Watcher.Action action, final Optional<V1alpha2Xp7Deployment> oldResource,
                        final Optional<V1alpha2Xp7Deployment> newResource )
    {
        InfoXp7Deployment info = ImmutableInfoXp7Deployment.builder().
            oldResource( oldResource ).
            newResource( newResource ).
            build();

        if(isBeingRestored(actionId, action, info.resource())) {
            // This is a backup restore, just ignore
            return;
        }

        runCommands( actionId, commandBuilder -> {
            if ( action == Watcher.Action.DELETED )
            {
                // Deployment namespace and all children will automatically be deleted
                return;
            }

            if ( action == Watcher.Action.ADDED )
            {
                // Create namespace
                ImmutableKubeCmd.builder().
                    clients( clients ).
                    resource( createNamespace( info ) ).
                    options( ImmutableKubeCommandOptions.builder().neverOverwrite( true ).build() ).
                    build().
                    apply( commandBuilder );

                // Create SA rules
                ServiceAccount sa = getServiceAccount();
                if ( sa != null )
                {
                    // Create role && role binding
                    Role role = createRole( info.namespaceName() );
                    RoleBinding roleBinding = createRoleBinding( role, sa );

                    ImmutableKubeCmd.builder().
                        clients( clients ).
                        resource( role ).
                        build().
                        apply( commandBuilder );

                    ImmutableKubeCmd.builder().
                        clients( clients ).
                        resource( roleBinding ).
                        build().
                        apply( commandBuilder );
                }

                // Create su pass
                ImmutableKubeCmd.builder().
                    clients( clients ).
                    namespace( info.namespaceName() ).
                    resource( createSecret() ).
                    options( ImmutableKubeCommandOptions.builder().neverOverwrite( true ).build() ).
                    build().
                    apply( commandBuilder );
            }

            if ( action == Watcher.Action.ADDED || action == Watcher.Action.MODIFIED )
            {
                // Apply chart
                ImmutableHelmKubeCmdBuilder.builder().
                    clients( clients ).
                    helm( helm ).
                    chart( chartRepository.get( "v1alpha2/xp7deployment" ) ).
                    namespace( info.namespaceName() ).
                    valueBuilder( ImmutableXp7DeploymentValues.builder().
                        baseValues( baseValues ).
                        imageTemplate( imageTemplate ).
                        info( info ).
                        build() ).
                    build().
                    addCommands( commandBuilder );
            }
        } );
    }

    private Namespace createNamespace( InfoXp7Deployment info )
    {
        ObjectMeta metaData = new ObjectMeta();
        metaData.setName( info.namespaceName() );
        metaData.setOwnerReferences( Collections.singletonList( info.ownerReference() ) );
        metaData.setAnnotations( Map.of( cfgStr( "operator.helm.charts.Values.labels.managed" ), "true" ) );
        Namespace namespace = new Namespace();
        namespace.setMetadata( metaData );
        return namespace;
    }

    private ServiceAccount getServiceAccount()
    {
        return clients.getDefaultClient().
            serviceAccounts().
            inNamespace( cfgStr( "operator.deployment.adminServiceAccount.namespace" ) ).
            withName( cfgStr( "operator.deployment.adminServiceAccount.name" ) ).
            get();
    }

    private Role createRole( String namespace )
    {
        ObjectMeta metaData = new ObjectMeta();
        metaData.setName( "read-secrets" );
        metaData.setNamespace( namespace );

        Role role = new Role();
        role.setMetadata( metaData );

        PolicyRule rule = new PolicyRule();
        rule.setApiGroups( Collections.singletonList( "*" ) );
        rule.setResources( Collections.singletonList( "secrets" ) );
        rule.setVerbs( Collections.singletonList( "get" ) );
        role.setRules( Collections.singletonList( rule ) );

        return role;
    }

    private RoleBinding createRoleBinding( Role role, ServiceAccount sa )
    {
        String saName = sa.getMetadata().getName();
        ObjectMeta metaData = new ObjectMeta();
        metaData.setName( saName + "-read-secrets" );
        metaData.setNamespace( role.getMetadata().getNamespace() );

        RoleBinding rb = new RoleBinding();
        rb.setMetadata( metaData );

        rb.setSubjects( Collections.singletonList(
            new Subject( null, "ServiceAccount", sa.getMetadata().getName(), sa.getMetadata().getNamespace() ) ) );
        rb.setRoleRef( new RoleRef( null, "Role", role.getMetadata().getName() ) );

        return rb;
    }

    @SuppressWarnings("UnstableApiUsage")
    private Secret createSecret()
    {
        String password = generateSuPassword();
        String passHash = Hashing.sha512().hashString( password, Charsets.UTF_8 ).toString();

        ObjectMeta metaData = new ObjectMeta();
        metaData.setAnnotations( Map.of( cfgStr( "operator.helm.charts.Values.labels.managed" ), "true" ) );
        metaData.setName( "su" );
        Secret secret = new Secret();
        secret.setMetadata( metaData );
        secret.setData( Map.of( "pass", BaseEncoding.base64().encode( password.getBytes() ), "passHash",
                                BaseEncoding.base64().encode( passHash.getBytes() ) ) );

        return secret;
    }

    private String generateSuPassword()
    {
        Random random = new Random();
        final byte[] buffer = new byte[20];
        random.nextBytes( buffer );
        return BaseEncoding.base64Url().omitPadding().encode( buffer );
    }
}
