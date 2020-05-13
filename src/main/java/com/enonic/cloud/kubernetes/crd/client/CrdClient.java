package com.enonic.cloud.kubernetes.crd.client;

import org.immutables.value.Value;

import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

import com.enonic.cloud.common.annotations.Params;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha1.app.V1alpha1Xp7App;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha1.app.client.V1alpha1Xp7AppDoneable;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha1.app.client.V1alpha1Xp7AppList;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.config.V1alpha2Xp7Config;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.config.client.V1alpha2Xp7ConfigDoneable;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.config.client.V1alpha2Xp7ConfigList;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.deployment.V1alpha2Xp7Deployment;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.deployment.client.V1alpha2Xp7DeploymentDoneable;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.deployment.client.V1alpha2Xp7DeploymentList;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.vhost.V1alpha2Xp7VHost;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.vhost.client.V1alpha2Xp7VHostDoneable;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.vhost.client.V1alpha2Xp7VHostList;


@Value.Immutable
@Params
public abstract class CrdClient
{
    public abstract MixedOperation<V1alpha1Xp7App, V1alpha1Xp7AppList, V1alpha1Xp7AppDoneable, Resource<V1alpha1Xp7App, V1alpha1Xp7AppDoneable>> xp7Apps();

    public abstract MixedOperation<V1alpha2Xp7Config, V1alpha2Xp7ConfigList, V1alpha2Xp7ConfigDoneable, Resource<V1alpha2Xp7Config, V1alpha2Xp7ConfigDoneable>> xp7Configs();

    public abstract MixedOperation<V1alpha2Xp7Deployment, V1alpha2Xp7DeploymentList, V1alpha2Xp7DeploymentDoneable, Resource<V1alpha2Xp7Deployment, V1alpha2Xp7DeploymentDoneable>> xp7Deployments();

    public abstract MixedOperation<V1alpha2Xp7VHost, V1alpha2Xp7VHostList, V1alpha2Xp7VHostDoneable, Resource<V1alpha2Xp7VHost, V1alpha2Xp7VHostDoneable>> xp7VHosts();
}
