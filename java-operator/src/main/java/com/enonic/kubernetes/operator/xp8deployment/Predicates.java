package com.enonic.kubernetes.operator.xp8deployment;

import com.enonic.kubernetes.crd.v1.Xp8Deployment;
import com.enonic.kubernetes.crd.v1.Xp8DeploymentStatus;
import io.fabric8.kubernetes.api.model.HasMetadata;

import java.util.function.Predicate;

import static com.enonic.kubernetes.kubernetes.Predicates.inSameNamespaceAs;

public class Predicates {
    public static Predicate<Xp8Deployment> running() {
        return deployment -> deployment.getStatus() != null &&
                deployment.getStatus().getState() == Xp8DeploymentStatus.State.RUNNING;
    }

    public static Predicate<Xp8Deployment> parent(HasMetadata r) {
        return d -> inSameNamespaceAs(r).test(d);
    }
}
