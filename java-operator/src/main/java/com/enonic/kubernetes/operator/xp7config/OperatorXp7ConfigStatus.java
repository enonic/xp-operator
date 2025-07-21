package com.enonic.kubernetes.operator.xp7config;

import com.enonic.kubernetes.client.v1.xp7config.Xp7Config;
import com.enonic.kubernetes.client.v1.xp7config.Xp7ConfigStatus;
import com.enonic.kubernetes.kubernetes.Clients;
import com.enonic.kubernetes.kubernetes.InformerSearcher;
import com.enonic.kubernetes.kubernetes.Informers;
import com.enonic.kubernetes.kubernetes.Searchers;
import com.enonic.kubernetes.kubernetes.commands.K8sLogHelper;
import com.enonic.kubernetes.operator.helpers.InformerEventHandler;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Pod;
import io.quarkus.runtime.StartupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

import static com.enonic.kubernetes.common.Configuration.cfgStr;
import static com.enonic.kubernetes.kubernetes.Predicates.*;

@ApplicationScoped
public class OperatorXp7ConfigStatus extends InformerEventHandler<Pod> {
    private static final Logger log = LoggerFactory.getLogger(OperatorXp7ConfigStatus.class);

    private static final String ANNOTATION_KEY = "enonic.io/configReloaded";
    private static final String LAST_UPDATED_ANNOTATION = "enonic.io/lastUpdated";

    @Inject
    Clients clients;

    @Inject
    Searchers searchers;

    @Inject
    Informers informers;

    void onStart(@Observes StartupEvent ev) {
        listen(informers.podInformer());
    }

    @Override
    public void onNewAdd(Pod newPod) {
        handle();
    }

    @Override
    public void onUpdate(Pod oldPod, Pod newPod) {
        handle();
    }

    @Override
    public void onDelete(Pod pod, boolean b) {
        // Do nothing
    }

    private void handle() {
        InformerSearcher<Xp7Config> configs = searchers.xp7Config();

        configs.stream().forEach(config -> {
            String ns = config.getMetadata().getNamespace();
            String nodeGroup = config.getSpec().getNodeGroup();
            boolean isAllNodes = nodeGroup.equals(cfgStr("operator.charts.values.allNodesKey"));

            Optional<String> expectedTimestamp = searchers.configMap().stream()
                    .filter(inSameNamespaceAs(config))
                    .filter(cm -> cm.getMetadata().getName().equals(nodeGroup))
                    .map(this::getLastUpdatedTimestamp)
                    .filter(Objects::nonNull)
                    .findFirst();

            if (expectedTimestamp.isEmpty()) {
                log.debug("No lastUpdated annotation found for ConfigMap '{}'", nodeGroup);
                return;
            }

            List<Pod> relevantPods = searchers.pod().stream()
                    .filter(inSameNamespaceAs(config))
                    .filter(isEnonicManaged())
                    .filter(isAllNodes
                            ? pod -> true
                            : matchLabel(cfgStr("operator.charts.values.labelKeys.nodeGroup"), nodeGroup))
                    .collect(Collectors.toList());

            List<Pod> notUpdated = relevantPods.stream()
                    .filter(pod -> {
                        Map<String, String> annotations = Optional.ofNullable(pod.getMetadata().getAnnotations()).orElse(Collections.emptyMap());
                        String reloadedAt = annotations.get(ANNOTATION_KEY);
                        return reloadedAt == null || reloadedAt.compareTo(expectedTimestamp.get()) < 0;
                    })
                    .collect(Collectors.toList());

            if (notUpdated.isEmpty()) {
                markReady(config);
            } else {
                markPending(config, notUpdated);
            }
        });

    }

    private String getLastUpdatedTimestamp(ConfigMap cm) {
        return Optional.ofNullable(cm.getMetadata().getAnnotations())
                .map(a -> a.get(LAST_UPDATED_ANNOTATION))
                .orElse(null);
    }

    private void markReady(final Xp7Config xp7Config) {
        log.debug("markReady Xp7Config: {} in {}", xp7Config.getMetadata().getName(), xp7Config.getMetadata().getNamespace());

        K8sLogHelper.logEdit(clients.xp7Configs()
                .inNamespace(xp7Config.getMetadata().getNamespace())
                .withName(xp7Config.getMetadata().getName()), c -> c.withStatus(new Xp7ConfigStatus()
                .withState(Xp7ConfigStatus.State.READY)
                .withMessage("OK")));
    }

    private void markPending(final Xp7Config xp7Config, final List<Pod> notUpdatedPods) {
        String podNames = notUpdatedPods.stream()
                .map(p -> p.getMetadata().getName())
                .collect(Collectors.joining(", "));

        log.debug("markPending Xp7Config: {} in {} {}", xp7Config.getMetadata().getName(),
                xp7Config.getMetadata().getNamespace(), podNames);

        K8sLogHelper.logEdit(clients.xp7Configs()
                .inNamespace(xp7Config.getMetadata().getNamespace())
                .withName(xp7Config.getMetadata().getName()), c -> c.withStatus(new Xp7ConfigStatus()
                .withState(Xp7ConfigStatus.State.PENDING)
                .withMessage("Not loaded: " + podNames)));
    }
}
