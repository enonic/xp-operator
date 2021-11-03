package com.enonic.kubernetes.client.v1;

import com.enonic.kubernetes.client.v1.domain.Domain;
import com.enonic.kubernetes.client.v1.xp7app.Xp7App;
import com.enonic.kubernetes.client.v1.xp7config.Xp7Config;
import com.enonic.kubernetes.client.v1.xp7deployment.Xp7Deployment;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

public interface CrdApi {
    MixedOperation<Domain, Domain.DomainList, Resource<Domain>> domains();

    MixedOperation<Xp7App, Xp7App.Xp7AppList, Resource<Xp7App>> xp7apps();

    MixedOperation<Xp7Config, Xp7Config.Xp7ConfigList, Resource<Xp7Config>> xp7configs();

    MixedOperation<Xp7Deployment, Xp7Deployment.Xp7DeploymentList, Resource<Xp7Deployment>> xp7deployments();
}
