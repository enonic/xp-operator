package com.enonic.ec.kubernetes.operator.crd;

import javax.enterprise.event.Observes;

import io.fabric8.kubernetes.internal.KubernetesDeserializer;
import io.quarkus.runtime.StartupEvent;

import com.enonic.ec.kubernetes.operator.common.Configuration;
import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha1.app.V1alpha1Xp7App;
import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha1.config.V1alpha1Xp7Config;
import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha1.deployment.V1alpha1Xp7Deployment;
import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha1.vhost.V1alpha1Xp7VHost;
import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha2.config.V1alpha2Xp7Config;
import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha2.deployment.V1alpha2Xp7Deployment;
import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha2.vhost.V1alpha2Xp7VHost;

public class CrdSerializerInitialization
    extends Configuration
{

    void onStartup( @Observes StartupEvent _ev )
    {
        v1alpha1();
        v2alpha2();
    }

    private void v1alpha1()
    {
        // TODO: FIX RACE CONDITION
        String apiVersion = cfgStr( "operator.crd.group" ) + "/" + cfgStr( "operator.crd.v1alpha1.apiVersion" );
        KubernetesDeserializer.registerCustomKind( apiVersion, cfgStr( "operator.crd.apps.kind" ), V1alpha1Xp7App.class );
        KubernetesDeserializer.registerCustomKind( apiVersion, cfgStr( "operator.crd.configs.kind" ), V1alpha1Xp7Config.class );
        KubernetesDeserializer.registerCustomKind( apiVersion, cfgStr( "operator.crd.deployments.kind" ), V1alpha1Xp7Deployment.class );
        KubernetesDeserializer.registerCustomKind( apiVersion, cfgStr( "operator.crd.vhosts.kind" ), V1alpha1Xp7VHost.class );
    }

    private void v2alpha2()
    {
        String apiVersion = cfgStr( "operator.crd.group" ) + "/" + cfgStr( "operator.crd.v1alpha2.apiVersion" );
        KubernetesDeserializer.registerCustomKind( apiVersion, cfgStr( "operator.crd.configs.kind" ), V1alpha2Xp7Config.class );
        KubernetesDeserializer.registerCustomKind( apiVersion, cfgStr( "operator.crd.deployments.kind" ), V1alpha2Xp7Deployment.class );
        KubernetesDeserializer.registerCustomKind( apiVersion, cfgStr( "operator.crd.vhosts.kind" ), V1alpha2Xp7VHost.class );
    }
}
