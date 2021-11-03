package com.enonic.kubernetes.apis.xp;

import com.enonic.kubernetes.apis.xp.service.*;
import com.enonic.kubernetes.client.v1.api.xp7.snapshots.Xp7MgmtSnapshotsList;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import okhttp3.*;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

public class XpClient
        extends EventSourceListener {
    private static final Logger log = LoggerFactory.getLogger(XpClient.class);

    private static final ObjectMapper mapper = new ObjectMapper();

    private final OkHttpClient restClient;

    private final OkHttpClient sseClient;

    private final EventSource eventSources;

    private final Set<Consumer<AppEvent>> onEventConsumers;

    private final Set<Consumer<Throwable>> onCloseConsumers;

    private final Function<String, Request.Builder> requestBuilder;

    private final XpClientParams params;

    private final Map<String, AppInfo> appMap;

    private final CountDownLatch onOpenLatch;

    private final MeterRegistry registry;

    private final Tags tags;

    public XpClient(final XpClientParams p) {
        log.debug(String.format("XP: Creating client for %s", p.url()));
        this.registry = p.registry();
        this.tags = Tags.of("namespace", p.namespace(), "nodeGroup", p.nodeGroup());
        this.appMap = new ConcurrentHashMap<>();
        this.params = p;

        registry.gauge("xp_apps_total", tags, appMap, Map::size);

        requestBuilder = url -> new Request.Builder().url(params.url() + url);

        onEventConsumers = new HashSet<>();
        onCloseConsumers = new HashSet<>();

        onOpenLatch = new CountDownLatch(1);

        sseClient = new OkHttpClient().newBuilder()
                .addInterceptor(new XpAuthenticator(params.username(), params.password()))
                .connectTimeout(params.timeout(), TimeUnit.MILLISECONDS)
                .callTimeout(params.timeout(), TimeUnit.MILLISECONDS)
                .readTimeout(0L, TimeUnit.MILLISECONDS)
                .build();

        restClient = new OkHttpClient().newBuilder()
                .addInterceptor(new XpAuthenticator(params.username(), params.password()))
                .connectTimeout(params.timeout(), TimeUnit.MILLISECONDS)
                .callTimeout(params.timeout(), TimeUnit.MILLISECONDS)
                .readTimeout(params.timeout(), TimeUnit.MILLISECONDS)
                .build();

        // Open up SSE
        eventSources = EventSources
                .createFactory(sseClient)
                .newEventSource(requestBuilder.apply("/app/events").build(), this);
    }

    public void waitForConnection(final long timeout)
            throws XpClientException {
        try {
            boolean connected = onOpenLatch.await(timeout, TimeUnit.MILLISECONDS);
            if (!connected) {
                throw new XpClientException(String.format("Timed out waiting for SSE connection on '%s'", params.url()));
            }
        } catch (InterruptedException e) {
            throw new XpClientException(String.format("Interrupted while waiting for SSE connection on '%s'", params.url()));
        }
    }

    private void openLatch() {
        if (onOpenLatch.getCount() > 0) {
            onOpenLatch.countDown();
        }
    }

    @Override
    public void onClosed(@NotNull EventSource eventSource) {
        log.debug(String.format(
                "XP: SSE event source closed from '%s' in NS '%s'",
                params.nodeGroup(),
                params.namespace()));
        closeClient(null);
    }

    @Override
    public void onEvent(@NotNull EventSource eventSource, @Nullable String id, @Nullable String eventType, @NotNull String eventData) {
        log.debug(String.format(
                "XP: SSE event '%s' received from '%s' in NS '%s': %s",
                eventType,
                params.nodeGroup(),
                params.namespace(),
                eventData));

        try {
            AppEventType type = AppEventType.fromEventName(eventType);
            switch (type) {
                case LIST:
                    mapper.readValue(eventData, AppEventList.class).
                            applications().
                            forEach(info -> handleAppInfo(type, info));
                    openLatch();
                    break;
                case INSTALLED:
                case STATE:
                    handleAppInfo(type, mapper.readValue(eventData, AppInfo.class));
                    break;
                case UNINSTALLED:
                    handleAppUninstall(type, mapper.readValue(eventData, AppKey.class));
                    break;
            }
        } catch (Exception e) {
            log.error(String.format(
                    "XP: SSE event failed to parse '%s' in NS '%s': %s",
                    params.nodeGroup(),
                    params.namespace(),
                    e.getMessage()));
            log.debug(e.getMessage(), e);
        }
    }

    @Override
    public void onFailure(@NotNull EventSource eventSource, @Nullable Throwable t, @Nullable Response response) {
        log.error(String.format(
                "XP: SSE event source error from '%s' in NS '%s': %s",
                params.nodeGroup(),
                params.namespace(),
                t == null ? null : t.getMessage()));
        closeClient(t);
    }

    @Override
    public void onOpen(@NotNull EventSource eventSource, @NotNull Response response) {
        log.info(String.format(
                "XP: SSE event source opened to '%s' in NS '%s'",
                params.nodeGroup(),
                params.namespace()));
    }

    private void handleAppInfo(final AppEventType type, final AppInfo info) {
        registry.counter("xp_apps_events", tags).increment();
        appMap.put(info.key(), info);
        onEventConsumers.forEach(consumer -> consumer.accept(ImmutableAppEvent.
                builder().
                namespace(params.namespace()).
                nodeGroup(params.nodeGroup()).
                type(type).
                key(info.key()).
                info(info).
                build()));
    }

    private void handleAppUninstall(final AppEventType type, final AppKey key) {
        registry.counter("xp_apps_events", tags).increment();
        appMap.remove(key.key());
        onEventConsumers.forEach(consumer -> consumer.accept(ImmutableAppEvent.
                builder().
                namespace(params.namespace()).
                nodeGroup(params.nodeGroup()).
                type(type).
                key(key.key()).
                build()));
    }

    public void closeClient(Throwable t) {
        if (t != null) {
            log.debug(t.getMessage(), t);
        }

        log.warn(String.format(
                "XP: SSE event source closed to '%s' in NS '%s'",
                params.nodeGroup(),
                params.namespace()));

        eventSources.cancel();
        sseClient.dispatcher().executorService().shutdown();
        restClient.dispatcher().executorService().shutdown();
        onCloseConsumers.forEach(c -> c.accept(t));
    }

    public void addEventListener(Consumer<AppEvent> eventListener) {
        onEventConsumers.add(eventListener);
        appList().forEach(a -> eventListener.accept(ImmutableAppEvent.
                builder().
                namespace(params.namespace()).
                nodeGroup(params.nodeGroup()).
                type(AppEventType.LIST).
                key(a.key()).
                info(a).
                build()));
    }

    public void addOnCloseListener(Consumer<Throwable> onCloseListener) {
        onCloseConsumers.add(onCloseListener);
    }

    public List<AppInfo> appList() {
        return new ArrayList<>(appMap.values());
    }

    public AppInstallResponse appInstall(AppInstallRequest req)
            throws XpClientException {
        registry.counter("xp_apps_install", tags).increment();
        try {
            Request request = requestBuilder.apply("/app/installUrl")
                    .post(RequestBody.create(MediaType.parse("application/json"), mapper.writeValueAsString(req)))
                    .build();
            Response response = restClient.newCall(request).execute();
            Preconditions.checkState(response.code() == 200, "Response code " + response.code());
            return mapper.readValue(response.body().bytes(), AppInstallResponse.class);
        } catch (IOException e) {
            registry.counter("xp_apps_error", tags).increment();
            throw new XpClientException(String.format("Failed installing app on '%s'", params.url()), e);
        }
    }

    private Response get(String url) throws XpClientException {
        return execute(requestBuilder.apply(url).get().build(), 200, "Failed GET " + url);
    }

    private Response execute(Request request, int code, String exceptionToThrow) throws XpClientException {
        try {
            Response response = restClient.newCall(request).execute();
            Preconditions.checkState(response.code() == code, "Response code " + response.code());
            return response;
        } catch (Exception e) {
            registry.counter("xp_apps_error", tags).increment();
            throw new XpClientException(exceptionToThrow, e);
        }
    }

    private void appOp(String op, AppKey req, String exceptionToThrow)
            throws XpClientException {
        try {
            Request request = requestBuilder.apply("/app/" + op)
                    .post(RequestBody.create(MediaType.parse("application/json"), mapper.writeValueAsString(req)))
                    .build();
            Response response = restClient.newCall(request).execute();
            Preconditions.checkState(response.code() == 204, "Response code " + response.code());
        } catch (Exception e) {
            registry.counter("xp_apps_error", tags).increment();
            throw new XpClientException(exceptionToThrow, e);
        }
    }

    public void appUninstall(AppKey req)
            throws XpClientException {
        registry.counter("xp_apps_uninstall", tags).increment();
        appOp("uninstall", req, String.format("Failed uninstalling app on '%s'", params.url()));
    }

    public void appStart(AppKey req)
            throws XpClientException {
        registry.counter("xp_apps_start", tags).increment();
        appOp("start", req, String.format("Failed starting app on '%s'", params.url()));
    }

    public void appStop(AppKey req)
            throws XpClientException {
        registry.counter("xp_apps_stop", tags).increment();
        appOp("stop", req, String.format("Failed stopping app on '%s'", params.url()));
    }

    public Xp7MgmtSnapshotsList snapshotList() throws XpClientException {
        try {
            Response response = get("/repo/snapshot/list");
            return mapper.readValue(response.body().bytes(), Xp7MgmtSnapshotsList.class);
        } catch (Exception e) {
            throw new XpClientException("Failed to list snapshots", e);
        }
    }

    public List<String> routesList() throws XpClientException {
        try {
            Response response = get("/cloud-utils/routes");
            return mapper.readValue(response.body().bytes(), new TypeReference<>() {
            });
        } catch (Exception e) {
            throw new XpClientException("Failed to list routes", e);
        }
    }

    public List<String> idProvidersList() throws XpClientException {
        try {
            Response response = get("/cloud-utils/idproviders");
            return mapper.readValue(response.body().bytes(), new TypeReference<>() {
            });
        } catch (Exception e) {
            throw new XpClientException("Failed to list idproviders", e);
        }
    }
}
