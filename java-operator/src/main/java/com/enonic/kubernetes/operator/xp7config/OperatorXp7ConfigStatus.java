package com.enonic.kubernetes.operator.xp7config;

import com.enonic.kubernetes.client.v1.xp7config.Xp7Config;
import com.enonic.kubernetes.client.v1.xp7config.Xp7ConfigStatus;
import com.enonic.kubernetes.kubernetes.Clients;
import com.enonic.kubernetes.kubernetes.Informers;
import com.enonic.kubernetes.kubernetes.Searchers;
import com.enonic.kubernetes.kubernetes.commands.K8sLogHelper;
import com.enonic.kubernetes.operator.helpers.InformerEventHandler;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Pod;
import io.quarkus.runtime.StartupEvent;
import org.eclipse.microprofile.config.inject.ConfigProperty;
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

    @ConfigProperty(name = "operator.charts.values.annotationKeys.configMapUpdated")
    String configMapUpdatedAnnotation;

    @ConfigProperty(name = "operator.charts.values.annotationKeys.podConfigReloaded")
    String podConfigReloadedAnnotation;

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
        handle(newPod);
    }

    @Override
    public void onUpdate(Pod oldPod, Pod newPod) {
        handle(newPod);
    }

    @Override
    public void onDelete(Pod pod, boolean b) {
        // Do nothing
    }

    private void handle(Pod pod) {
        final String namespace = pod.getMetadata().getNamespace();

        List<ConfigMap> configMaps = searchers.configMap().stream()
                .filter(cm -> namespace.equals(cm.getMetadata().getNamespace()))
                .collect(Collectors.toList());

        List<Pod> allPods = searchers.pod().stream()
                .filter(p -> namespace.equals(p.getMetadata().getNamespace()))
                .filter(isEnonicManaged())
                .collect(Collectors.toList());

        List<Xp7Config> allConfigs = searchers.xp7Config().stream()
                .filter(cfg -> namespace.equals(cfg.getMetadata().getNamespace()))
                .collect(Collectors.toList());

        for (ConfigMap cm : configMaps) {
            String configMapName = cm.getMetadata().getName();
            String updatedAt = getLastUpdatedTimestamp(cm);

            List<Xp7Config> relatedConfigs = allConfigs.stream()
                    .filter(cfg -> cfg.getSpec().getNodeGroup().equals(configMapName))
                    .collect(Collectors.toList());

            log.info(String.format("Updating config map %s", configMapName));
            for (Xp7Config config : relatedConfigs) {

                log.info(String.format("Updating Xp7Config %s", config.getMetadata().getName()));

                if (!isEnonicManaged().test(cm)) {
                    markReady(config); // inherited behavior from events.sh implementation
                }

                if (updatedAt == null) {
                    continue;
                }

                boolean isAllNodes = config.getSpec().getNodeGroup().equals(cfgStr("operator.charts.values.allNodesKey"));

                List<Pod> relevantPods = allPods.stream()
                        .filter(isAllNodes
                                ? p -> true
                                : matchLabel(cfgStr("operator.charts.values.labelKeys.nodeGroup"), config.getSpec().getNodeGroup()))
                        .collect(Collectors.toList());

                List<Pod> notUpdated = relevantPods.stream()
                        .filter(p -> {
                            Map<String, String> annotations = Optional.ofNullable(p.getMetadata().getAnnotations()).orElse(Collections.emptyMap());
                            String reloadedAt = annotations.get(podConfigReloadedAnnotation);
                            return reloadedAt == null || reloadedAt.compareTo(updatedAt) < 0;
                        })
                        .collect(Collectors.toList());

                if (notUpdated.isEmpty()) {
                    markReady(config);
                } else {
                    markPending(config, notUpdated);
                }
            }
        }
    }

    private String getLastUpdatedTimestamp(ConfigMap cm) {
        return Optional.ofNullable(cm.getMetadata().getAnnotations())
                .map(a -> a.get(configMapUpdatedAnnotation))
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
