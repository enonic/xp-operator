package com.enonic.kubernetes.operator.xp7config;

import com.enonic.kubernetes.common.TaskRunner;
import com.enonic.kubernetes.kubernetes.ActionLimiter;
import com.enonic.kubernetes.kubernetes.Clients;
import com.enonic.kubernetes.kubernetes.Informers;
import com.enonic.kubernetes.operator.helpers.InformerEventHandler;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.quarkus.runtime.StartupEvent;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static com.enonic.kubernetes.kubernetes.Predicates.*;

@ApplicationScoped
public class OperatorConfigMapChangeMarker
    extends InformerEventHandler<ConfigMap>
{
    private static final Logger log = LoggerFactory.getLogger(OperatorConfigMapChangeMarker.class);

    @ConfigProperty(name = "operator.charts.values.annotationKeys.configMapUpdated")
    String configMapUpdatedAnnotation;

    @Inject
    Clients clients;

    @Inject
    Informers informers;

    @Inject
    TaskRunner taskRunner;

    ActionLimiter limiter;

    void onStart(@Observes StartupEvent ev) {
        limiter = new ActionLimiter(this.getClass().getSimpleName(), taskRunner, 1000L);
        listen(informers.configMapInformer());
    }

    @Override
    protected void onNewAdd(final ConfigMap newCm) {
        onCondition(newCm, cm -> {
            log.debug("onNew ConfigMap: {} in {}", cm.getMetadata().getNamespace(), cm.getMetadata().getName());
            handle(cm);
        }, isEnonicManaged());
    }

    @Override
    public void onUpdate(final ConfigMap oldCm, final ConfigMap newCm) {
        if (dataNotEquals(oldCm).and(isEnonicManaged()).test(newCm)) {
            log.debug("onUpdate ConfigMap: {} in {}", newCm.getMetadata().getNamespace(), newCm.getMetadata().getName());
            limiter.limit(newCm, this::handle);
        }
    }

    @Override
    public void onDelete(final ConfigMap oldCm, final boolean b) {
        // Do nothing
    }

    private void handle(final ConfigMap cm) {
        final String timestamp = Instant.now().toString();
        log.debug("Setting lastUpdated={} on ConfigMap: {} in {}", timestamp, cm.getMetadata().getName(), cm.getMetadata().getNamespace());

        clients.k8s().configMaps()
            .inNamespace(cm.getMetadata().getNamespace())
            .withName(cm.getMetadata().getName())
            .edit(c -> {
                Map<String, String> annotations = new HashMap<>();
                if (c.getMetadata().getAnnotations() != null) {
                    annotations.putAll(c.getMetadata().getAnnotations());
                }
                annotations.put( configMapUpdatedAnnotation, timestamp);

                return new ConfigMapBuilder(c)
                    .editMetadata()
                    .withAnnotations(annotations)
                    .endMetadata()
                    .build();
            });
    }
}
