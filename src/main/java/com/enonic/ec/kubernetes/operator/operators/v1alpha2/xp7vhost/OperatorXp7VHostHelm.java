//package com.enonic.ec.kubernetes.operator.operators.v1alpha2.xp7vhost;
//
//import java.util.Optional;
//
//import javax.enterprise.context.ApplicationScoped;
//import javax.enterprise.event.Observes;
//import javax.inject.Inject;
//import javax.inject.Named;
//
//import io.fabric8.kubernetes.client.Watcher;
//import io.quarkus.runtime.StartupEvent;
//
//import com.enonic.ec.kubernetes.operator.operators.OperatorNamespaced;
//import com.enonic.ec.kubernetes.operator.common.DefaultClientProducer;
//import com.enonic.ec.kubernetes.operator.common.commands.ImmutableCombinedCommand;
//import com.enonic.ec.kubernetes.operator.helm.ChartRepository;
//
//@SuppressWarnings("OptionalGetWithoutIsPresent")
//@ApplicationScoped
//public class OperatorXp7VHostHelm
//    extends OperatorNamespaced
//{
//    @Inject
//    DefaultClientProducer defaultClientProducer;
//
//    @Inject
//    Xp7ConfigClientProducer configClientProducer;
//
//    @Inject
//    Xp7DeploymentCache xp7DeploymentCache;
//
//    @Inject
//    Xp7VHostCache xp7VHostCache;
//
//    @Inject
//    Xp7ConfigCache xp7ConfigCache;
//
//    @Inject
//    @Named("local")
//    ChartRepository chartRepository;
//
//    void onStartup( @Observes StartupEvent _ev )
//    {
//        // TODO: Enable
//        //xp7VHostCache.addWatcher( this::watchVHosts );
//    }
//
//    private void watchVHosts( final Watcher.Action action, final String s, final Optional<Xp7VHostResource> oldResource,
//                              final Optional<Xp7VHostResource> newResource )
//    {
//        Optional<ResourceInfoNamespaced<Xp7VHostResource, DiffXp7VHost>> i = getInfo( action, () -> ImmutableInfoXp7VHost.builder().
//            xpDeploymentCache( xp7DeploymentCache ).
//            oldResource( oldResource ).
//            newResource( newResource ).
//            build() );
//
//        i.ifPresent( info -> createCommands( ImmutableCombinedCommand.builder(), info ) );
//    }
//
//    protected void createCommands( ImmutableCombinedCommand.Builder commandBuilder,
//                                   ResourceInfoNamespaced<Xp7VHostResource, DiffXp7VHost> info )
//    {
//
//        ImmutableCommandXpVHostIngressTemplateApply.builder().
//            defaultClient( defaultClientProducer.client() ).
//            info( info ).
//            build().
//            addCommands( commandBuilder );
//
//        // Because multiple vHosts could potentially be deployed at the same time,
//        // lets use the stall function to let them accumulate before we update config
//        stallAndRunCommands( commandBuilder, () -> {
//            ImmutableCommandXpVHostConfigApply.builder().
//                xpConfigClient( configClientProducer.produce() ).
//                xpConfigCache( xp7ConfigCache ).
//                vHostCache( xp7VHostCache ).
//                info( info ).
//                build().
//                addCommands( commandBuilder );
//        } );
//    }
//}
