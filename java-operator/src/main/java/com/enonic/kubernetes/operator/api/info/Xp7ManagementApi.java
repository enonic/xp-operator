package com.enonic.kubernetes.operator.api.info;

import com.enonic.kubernetes.apis.xp.XpClient;
import com.enonic.kubernetes.apis.xp.XpClientCache;
import com.enonic.kubernetes.apis.xp.XpClientCacheKeyImpl;
import com.enonic.kubernetes.apis.xp.XpClientException;
import com.enonic.kubernetes.client.api.xp7.idproviders.Xp7MgmtIdProvidersList;
import com.enonic.kubernetes.client.api.xp7.routes.Xp7MgmtRoutesList;
import com.enonic.kubernetes.client.api.xp7.snapshots.Xp7MgmtSnapshotsList;
import com.enonic.kubernetes.client.v1alpha2.Xp7Deployment;
import com.enonic.kubernetes.kubernetes.Searchers;
import io.fabric8.kubernetes.api.model.StatusBuilder;
import io.fabric8.kubernetes.client.KubernetesClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import java.util.Optional;

import static com.enonic.kubernetes.kubernetes.Predicates.inNamespace;
import static com.enonic.kubernetes.kubernetes.Predicates.withName;
import static com.enonic.kubernetes.operator.v1alpha2xp7deployment.Predicates.running;
import static com.enonic.kubernetes.operator.v1alpha2xp7deployment.Predicates.withNodeGroup;

@ApplicationScoped
@Path("/apis/operator.enonic.cloud/v1alpha1")
public class Xp7ManagementApi {

    public static Logger logger = LoggerFactory.getLogger(Xp7ManagementApi.class);

    @Inject
    Searchers searchers;

    @Inject
    XpClientCache xpClientCache;

    private KubernetesClientException k8sException(int code, String message) {
        return new KubernetesClientException(message, code, new StatusBuilder()
                .withCode(code)
                .withMessage(message)
                .withReason(message)
                .build());
    }

    private XpClient getClient(final String namespace, final String name, final String nodeGroup) throws XpClientException {
        Optional<Xp7Deployment> xp7Deployment = searchers
                .xp7Deployment()
                .find(inNamespace(namespace), withName(name));

        if (xp7Deployment.isEmpty()) {
            throw k8sException(404, String.format("No Xp7Deployment in namespace '%s' with name '%s' not found", namespace, name));
        }

        if (!withNodeGroup(nodeGroup).test(xp7Deployment.get())) {
            throw k8sException(404, String.format("No Xp7Deployment in namespace '%s' with name '%s' does not have nodegroup %s", namespace, name, nodeGroup));
        }

        if (!running().test(xp7Deployment.get())) {
            throw k8sException(503, "Xp7Deployment not running");
        }

        return xpClientCache.getClient(XpClientCacheKeyImpl.of(namespace, name, nodeGroup));
    }

    @GET
    @Path("/xp7/{namespace}/{name}/{nodegroup}/mgmt/repo/snapshot/list")
    @Produces("application/json")
    public Xp7MgmtSnapshotsList snapshotList(
            @PathParam("namespace") final String namespace,
            @PathParam("name") final String name,
            @PathParam("nodegroup") final String nodeGroup)
            throws KubernetesClientException {
        try {
            return getClient(namespace, name, nodeGroup).snapshotList();
        } catch (Exception e) {
            logger.error("Failed listing snapshots", e);
            throw k8sException(500, e.getMessage());
        }
    }

    @GET
    @Path("/xp7/{namespace}/{name}/{nodegroup}/mgmt/cloud-utils/routes")
    @Produces("application/json")
    public Xp7MgmtRoutesList routesList(
            @PathParam("namespace") final String namespace,
            @PathParam("name") final String name,
            @PathParam("nodegroup") final String nodeGroup)
            throws KubernetesClientException {
        try {
            return new Xp7MgmtRoutesList().withResults(getClient(namespace, name, nodeGroup).routesList());
        } catch (Exception e) {
            logger.error("Failed listing routes", e);
            throw k8sException(500, e.getMessage());
        }
    }

    @GET
    @Path("/xp7/{namespace}/{name}/{nodegroup}/mgmt/cloud-utils/idproviders")
    @Produces("application/json")
    public Xp7MgmtIdProvidersList idProvidersList(
            @PathParam("namespace") final String namespace,
            @PathParam("name") final String name,
            @PathParam("nodegroup") final String nodeGroup)
            throws KubernetesClientException {
        try {
            return new Xp7MgmtIdProvidersList().withResults(getClient(namespace, name, nodeGroup).idProvidersList());
        } catch (Exception e) {
            logger.error("Failed listing idproviders", e);
            throw k8sException(500, e.getMessage());
        }
    }
}
