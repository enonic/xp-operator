package com.enonic.kubernetes.operator.xp8deployment;

import com.enonic.kubernetes.crd.v1.Xp8Deployment;
import com.enonic.kubernetes.crd.v1.xp8deploymentspec.NodeGroups;
import com.enonic.kubernetes.crd.v1.Xp8DeploymentStatus;
import io.fabric8.kubernetes.api.model.HasMetadata;

import java.util.function.Predicate;

import static com.enonic.kubernetes.common.Configuration.cfgStr;
import static com.enonic.kubernetes.kubernetes.Predicates.inSameNamespaceAs;

public class Predicates {
    public static Predicate<Xp8Deployment> running() {
        return deployment -> deployment
                .getStatus()
                .getState()
                .equals(Xp8DeploymentStatus.State.RUNNING);
    }

    public static Predicate<Xp8Deployment> parent(HasMetadata r) {
        return d -> inSameNamespaceAs(r).test(d);
    }

    public static Predicate<Xp8Deployment> withNodeGroup(String nodeGroup) {
        return deployment -> {
            if (nodeGroup.equals(cfgStr("operator.charts.values.allNodesKey"))) {
                return true;
            }
            if (deployment.getSpec() != null) {
                if (deployment.getSpec().getNodeGroups() != null) {
                    for (NodeGroups ng : deployment.getSpec().getNodeGroups()) {
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
