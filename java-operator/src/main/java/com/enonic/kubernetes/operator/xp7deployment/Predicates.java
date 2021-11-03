package com.enonic.kubernetes.operator.xp7deployment;

import com.enonic.kubernetes.client.v1.xp7deployment.Xp7Deployment;
import com.enonic.kubernetes.client.v1.xp7deployment.Xp7DeploymentSpecNodeGroup;
import com.enonic.kubernetes.client.v1.xp7deployment.Xp7DeploymentStatus;
import io.fabric8.kubernetes.api.model.HasMetadata;

import java.util.function.Predicate;

import static com.enonic.kubernetes.common.Configuration.cfgStr;
import static com.enonic.kubernetes.kubernetes.Predicates.inSameNamespaceAs;

public class Predicates {
    public static Predicate<Xp7Deployment> running() {
        return deployment -> deployment
                .getStatus()
                .getState()
                .equals(Xp7DeploymentStatus.State.RUNNING);
    }

    public static Predicate<Xp7Deployment> parent(HasMetadata r) {
        return d -> inSameNamespaceAs(r).test(d);
    }

    public static Predicate<Xp7Deployment> withNodeGroup(String nodeGroup) {
        return deployment -> {
            if (nodeGroup.equals(cfgStr("operator.charts.values.allNodesKey"))) {
                return true;
            }
            if (deployment.getSpec() != null) {
                if (deployment.getSpec().getXp7DeploymentSpecNodeGroups() != null) {
                    for (Xp7DeploymentSpecNodeGroup ng : deployment.getSpec().getXp7DeploymentSpecNodeGroups()) {
                        if (ng.getName().equals(nodeGroup)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        };
    }
}
