package com.enonic.kubernetes.operator.xp8config;

import com.enonic.kubernetes.crd.v1.Xp8Config;
import com.enonic.kubernetes.crd.v1.Xp8ConfigStatus;
import com.enonic.kubernetes.kubernetes.Clients;
import com.enonic.kubernetes.kubernetes.Informers;
import com.enonic.kubernetes.kubernetes.Searchers;
import com.enonic.kubernetes.kubernetes.commands.K8sLogHelper;
import com.enonic.kubernetes.operator.helpers.InformerEventHandler;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Pod;
import io.micronaut.context.annotation.Value;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.runtime.server.event.ServerStartupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.enonic.kubernetes.common.Configuration.cfgStr;
import static com.enonic.kubernetes.kubernetes.Predicates.*;

@Singleton
public class OperatorXp8ConfigStatus extends InformerEventHandler<Pod> implements ApplicationEventListener<ServerStartupEvent> {
    private static final Logger log = LoggerFactory.getLogger(OperatorXp8ConfigStatus.class);

    @Value("${operator.charts.values.annotationKeys.configMapUpdated}")
    String configMapUpdatedAnnotation;

    @Value("${operator.charts.values.annotationKeys.podConfigReloaded}")
    String podConfigReloadedAnnotation;

    @Inject
    Clients clients;

    @Inject
    Searchers searchers;

    @Inject
    Informers informers;

    private static final Map<String, Object> NAMESPACE_LOGS = new ConcurrentHashMap<>();

    @Override
    public void onApplicationEvent(ServerStartupEvent event) {
        listen(informers.podInformer());
    }

    @Override
    public void onNewAdd(Pod newPod) {
        handle(newPod);
    }

    @Override
    public void onUpdate(Pod oldPod, Pod newPod) {
        if(Objects.equals( oldPod, newPod )) {
            return;
        }

        handle(newPod);
    }

    @Override
    public void onDelete(Pod pod, boolean b) {
        // Do nothing
    }

    private void handle(Pod pod) {
        if (!isEnonicManaged().test(pod)) {
            return;
        }

        final String namespace = pod.getMetadata().getNamespace();
        final Object lock = NAMESPACE_LOGS.computeIfAbsent(namespace, ns -> new Object());

        synchronized (lock) {
            handle(namespace);
        }
    }

    private void handle(String namespace) {
        List<ConfigMap> configMaps = searchers.configMap().stream()
                .filter(cm -> namespace.equals(cm.getMetadata().getNamespace()))
                .collect(Collectors.toList());

        List<Pod> allPods = searchers.pod().stream()
                .filter(p -> namespace.equals(p.getMetadata().getNamespace()))
                .filter(isEnonicManaged())
                .collect(Collectors.toList());

        List<Xp8Config> allConfigs = searchers.xp8Config().stream()
                .filter(cfg -> namespace.equals(cfg.getMetadata().getNamespace()))
                .collect(Collectors.toList());

        for (ConfigMap cm : configMaps) {
            String configMapName = cm.getMetadata().getName();
            String updatedAt = getLastUpdatedTimestamp(cm);

            List<Xp8Config> relatedConfigs = allConfigs.stream()
                    .filter(cfg -> cfg.getSpec().getNodeGroup().equals(configMapName) || cfg.getSpec().getNodeGroup().equals(cfgStr("operator.charts.values.allNodesKey")))
                    .collect(Collectors.toList());

            log.info(String.format("Updating config map %s", configMapName));
            for (Xp8Config config : relatedConfigs) {

                log.info(String.format("Updating Xp8Config %s", config.getMetadata().getName()));

                if (!isEnonicManaged().test(cm)) {
                    markReady(config); // inherited behavior from events.sh implementation
                }

                if (updatedAt == null) {
                    continue;
                }

                boolean isAllNodes = config.getSpec().getNodeGroup().equals(cfgStr("operator.charts.values.allNodesKey"));

                List<Pod> relevantPods = isAllNodes ? allPods : allPods.stream()
                        .filter(matchLabel(cfgStr("operator.charts.values.labelKeys.nodeGroup"), config.getSpec().getNodeGroup()))
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

    private void markReady(final Xp8Config xp8Config) {
        if(xp8Config.getStatus().getState() == Xp8ConfigStatus.State.READY) {
            return;
        }
        log.debug("markReady Xp8Config: {} in {}", xp8Config.getMetadata().getName(), xp8Config.getMetadata().getNamespace());

        K8sLogHelper.logEdit(clients.xp8Configs()
                .inNamespace(xp8Config.getMetadata().getNamespace())
                .withName(xp8Config.getMetadata().getName()), c -> {
                    Xp8ConfigStatus status = new Xp8ConfigStatus();
                    status.setState(Xp8ConfigStatus.State.READY);
                    status.setMessage("OK");
                    c.setStatus(status);
                    return c;
                });
    }

    private void markPending(final Xp8Config xp8Config, final List<Pod> notUpdatedPods) {
        if(xp8Config.getStatus().getState() == Xp8ConfigStatus.State.PENDING) {
            return;
        }
        String podNames = notUpdatedPods.stream()
                .map(p -> p.getMetadata().getName())
                .collect(Collectors.joining(", "));

        log.debug("markPending Xp8Config: {} in {} {}", xp8Config.getMetadata().getName(),
                xp8Config.getMetadata().getNamespace(), podNames);

        K8sLogHelper.logEdit(clients.xp8Configs()
                .inNamespace(xp8Config.getMetadata().getNamespace())
                .withName(xp8Config.getMetadata().getName()), c -> {
                    Xp8ConfigStatus status = new Xp8ConfigStatus();
                    status.setState(Xp8ConfigStatus.State.PENDING);
                    status.setMessage("Not loaded: " + podNames);
                    c.setStatus(status);
                    return c;
                });
    }
}
