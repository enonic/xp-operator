<h1>Enonic XP Kubernetes Operator Helm Chart</h1>

This chart bootstraps the [Enonic XP Kubernetes Operator](../) on a [Kubernetes](http://kubernetes.io) cluster using the [Helm](https://helm.sh) package manager.

- [Prerequisites](#prerequisites)
- [Quick start](#quick-start)
- [Configuration](#configuration)

## Prerequisites

- Kubernetes 1.30+ (for ValidatingAdmissionPolicy support)
- Helm 3+

> **Note**: ValidatingAdmissionPolicy is a GA feature in Kubernetes 1.30+. The operator uses VAPs to enforce security boundaries and prevent privilege escalation. For clusters running Kubernetes < 1.30, you can still install the operator, but the VAP resources will need to be excluded from the installation.

## Quick start

```bash
helm repo add enonic https://repo.enonic.com/helm
helm repo update
helm install --namespace kube-system xp-operator enonic/xp-operator
```

## Configuration

### Security Model

The operator implements a least-privilege security model with the following components:

1. **Minimal Operator ClusterRole**: The operator runs with minimal cluster-wide permissions:
   - Manage namespaces (create, update, delete, label)
   - Manage RoleBindings cluster-wide
   - `bind` verb on the `namespace-role` ClusterRole (for safe delegation)
   - Watch CRDs cluster-wide (XP7Deployment, XP7App, XP7Config)

2. **Namespace-Scoped ClusterRole**: A powerful ClusterRole (`<release-name>-namespace-role`) that includes:
   - Full access to namespaced resources (secrets, configmaps, services, etc.)
   - Only effective in namespaces where the operator creates a RoleBinding
   - Includes permissions for Hazelcast ClusterRole/ClusterRoleBinding creation

3. **ValidatingAdmissionPolicies**: Two VAPs enforce security boundaries:
   - **VAP #1**: Prevents RoleBinding creation in namespaces without the `operator.enonic.cloud/managed=true` label
   - **VAP #2**: Prevents the operator from modifying or deleting namespaces it didn't create

4. **Operator Lifecycle**:
   - When an XP7Deployment is created, the operator labels its namespace as managed
   - The operator creates a RoleBinding in that namespace, granting itself the powerful role
   - The operator can only operate within namespaces it has explicitly labeled
   - VAPs prevent privilege escalation and unauthorized namespace access

This design ensures third-party installations can trust that the operator cannot:
- Access or modify resources in arbitrary namespaces
- Escalate its own privileges beyond what was granted at installation
- Delete or modify namespaces it didn't create

### Configuration Parameters

The following table lists the configurable parameters of the xp-operator chart. For details look at [values.yaml](./values.yaml).

| Parameter                   | Description                                                           | Default                            |
|-----------------------------|-----------------------------------------------------------------------|------------------------------------|
| `image.repository`          | Operator image repository                                             | `enonic/xp-operator`               |
| `image.tag`                 | Operator image tag                                                    | _see [values.yaml](./values.yaml)_ |
| `image.pullPolicy`          | Operator image pull policy                                            | `IfNotPresent`                     |
| `sysctlInitContainer.image` | Image of container to set vm.max_map_count                            | `busybox`                          |
| `waitForDnsDiscovery.image` | Image of container to wait XP deployment's service                    | `busybox`                          |
| `resources.limits`          | The resources limits for the Operator container                       | `{}`                               |
| `resources.requests`        | The requested resources for the Operator container                    | `{}`                               |
| `labels`                    | Additional labels for Operator pods                                   | `{}`                               |
| `annotations`               | Additional annotations for Operator pods                              | `{}`                               |
| `env`                       | Environmental variables for the Operator container                    | `{}`                               |
| `secrets`                   | Secrets mounted as environmental variables for the Operator container | `{}`                               |
| `config`                    | Configuration for the Operator container                              | _see [values.yaml](./values.yaml)_ |
| `onlyDeployCrds`            | Only deploy the CRD definitions                                       | `false`                            |
