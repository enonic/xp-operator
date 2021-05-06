package com.enonic.kubernetes.operator.v1alpha2xp7deployment;

import com.enonic.kubernetes.client.v1alpha2.Xp7Deployment;
import com.enonic.kubernetes.client.v1alpha2.xp7deployment.Xp7DeploymentStatus;
import io.fabric8.kubernetes.api.model.HasMetadata;

import java.util.function.Predicate;

import static com.enonic.kubernetes.kubernetes.Predicates.inSameNamespaceAs;

public class Predicates
{
    public static Predicate<Xp7Deployment> running()
    {
        return deployment -> deployment
            .getStatus()
            .getState()
            .equals( Xp7DeploymentStatus.State.RUNNING );
    }

    public static Predicate<Xp7Deployment> parent( HasMetadata r )
    {
        return d -> inSameNamespaceAs( r ).test( d );
    }


}
