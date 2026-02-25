package com.enonic.kubernetes.operator.xp7deployment;

import com.enonic.kubernetes.client.v1.xp7deployment.Xp7Deployment;
import com.enonic.kubernetes.kubernetes.Clients;
import com.enonic.kubernetes.kubernetes.Informers;
import com.enonic.kubernetes.operator.helpers.InformerEventHandler;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.fabric8.kubernetes.api.model.rbac.RoleBindingBuilder;
import io.fabric8.kubernetes.api.model.rbac.RoleRef;
import io.fabric8.kubernetes.api.model.rbac.RoleRefBuilder;
import io.fabric8.kubernetes.api.model.rbac.Subject;
import io.fabric8.kubernetes.api.model.rbac.SubjectBuilder;
import io.quarkus.runtime.StartupEvent;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * This operator class ensures namespaces used by XP7Deployments are properly labeled
 * and have RoleBindings that grant the operator ServiceAccount permissions within those namespaces.
 */
@ApplicationScoped
public class OperatorXp7DeploymentNamespace
    extends InformerEventHandler<Xp7Deployment>
{
    private static final Logger log = Logger.getLogger(OperatorXp7DeploymentNamespace.class);
    private static final String MANAGED_LABEL_KEY = "operator.enonic.cloud/managed";
    private static final String MANAGED_LABEL_VALUE = "true";
    private static final String ROLEBINDING_NAME = "xp-operator-namespace-access";

    @Inject
    Clients clients;

    @Inject
    Informers informers;

    @ConfigProperty(name = "OPERATOR_NAME")
    Optional<String> operatorName;

    @ConfigProperty(name = "OPERATOR_NAMESPACE")
    Optional<String> operatorNamespace;

    void onStart(@Observes StartupEvent ev)
    {
        listen(informers.xp7DeploymentInformer());
    }

    @Override
    public void onNewAdd(final Xp7Deployment newResource)
    {
        handleNamespace(newResource);
    }

    @Override
    public void onUpdate(final Xp7Deployment oldResource, final Xp7Deployment newResource)
    {
        // Ensure namespace is still properly configured even on updates
        handleNamespace(newResource);
    }

    @Override
    public void onDelete(final Xp7Deployment oldResource, final boolean deletedFinalStateUnknown)
    {
        // Namespace cleanup is handled by OperatorDeleteAnnotation
    }

    private void handleNamespace(final Xp7Deployment deployment)
    {
        String namespace = deployment.getMetadata().getNamespace();
        
        if (!operatorName.isPresent() || !operatorNamespace.isPresent())
        {
            log.warn("OPERATOR_NAME or OPERATOR_NAMESPACE not configured, skipping namespace management for: " + namespace);
            return;
        }

        try
        {
            ensureNamespaceLabeled(namespace);
            ensureRoleBinding(namespace);
        }
        catch (Exception e)
        {
            log.error("Failed to configure namespace: " + namespace, e);
        }
    }

    private void ensureNamespaceLabeled(final String namespaceName)
    {
        Namespace namespace = clients.k8s().namespaces().withName(namespaceName).get();
        
        if (namespace == null)
        {
            log.warn("Namespace does not exist: " + namespaceName);
            return;
        }

        Map<String, String> labels = namespace.getMetadata().getLabels();
        if (labels == null)
        {
            labels = new HashMap<>();
        }

        if (!MANAGED_LABEL_VALUE.equals(labels.get(MANAGED_LABEL_KEY)))
        {
            labels.put(MANAGED_LABEL_KEY, MANAGED_LABEL_VALUE);
            
            Namespace updatedNamespace = new NamespaceBuilder(namespace)
                .editMetadata()
                    .withLabels(labels)
                .endMetadata()
                .build();

            clients.k8s().namespaces().withName(namespaceName).replace(updatedNamespace);
            log.info("Labeled namespace as managed: " + namespaceName);
        }
    }

    private void ensureRoleBinding(final String namespaceName)
    {
        String opName = operatorName.get();
        String opNamespace = operatorNamespace.get();
        String clusterRoleName = opName + "-namespace-role";

        RoleBinding existingRoleBinding = clients.k8s()
            .rbac()
            .roleBindings()
            .inNamespace(namespaceName)
            .withName(ROLEBINDING_NAME)
            .get();

        if (existingRoleBinding == null)
        {
            RoleRef roleRef = new RoleRefBuilder()
                .withApiGroup("rbac.authorization.k8s.io")
                .withKind("ClusterRole")
                .withName(clusterRoleName)
                .build();

            Subject subject = new SubjectBuilder()
                .withKind("ServiceAccount")
                .withName(opName)
                .withNamespace(opNamespace)
                .build();

            RoleBinding roleBinding = new RoleBindingBuilder()
                .withNewMetadata()
                    .withName(ROLEBINDING_NAME)
                    .withNamespace(namespaceName)
                .endMetadata()
                .withRoleRef(roleRef)
                .addToSubjects(subject)
                .build();

            clients.k8s().rbac().roleBindings().inNamespace(namespaceName).create(roleBinding);
            log.info("Created RoleBinding for operator in namespace: " + namespaceName);
        }
    }
}
