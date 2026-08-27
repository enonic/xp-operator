<h1>Enonic XP Kubernetes Operator Helm Chart</h1>

This chart bootstraps the [Enonic XP Kubernetes Operator](../) on a [Kubernetes](http://kubernetes.io) cluster using the [Helm](https://helm.sh) package manager.

- [Prerequisites](#prerequisites)
- [Quick start](#quick-start)
- [Configuration](#configuration)

## Prerequisites

- Kubernetes 1.17+
- Helm 3+

## Quick start

```bash
helm repo add enonic https://repo.enonic.com/helm
helm repo update
helm install --namespace kube-system xp-operator enonic/xp-operator
```

## Configuration

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
| `serviceAccount.create`     | Create the operator ServiceAccount                                    | `true`                             |
| `serviceAccount.name`       | Name of the ServiceAccount to use                                     | `""`                               |
| `rbac.create`               | Create the operator ClusterRoles and binding                          | `true`                             |
| `rbac.namespaceRoleName`    | ClusterRole bound in each managed namespace                           | `""`                               |
| `rbac.hazelcastClusterRoleName` | ClusterRole bound for Hazelcast discovery                             | `""`                               |
| `webhook.enabled`           | Create the webhook configurations                                     | `true`                             |
| `apiService.enabled`        | Create the APIService                                                 | `true`                             |
| `admissionPolicy.enabled`   | Create the ValidatingAdmissionPolicies                                | `true`                             |
| `tls.existingSecret`        | Existing kubernetes.io/tls Secret to mount                            | `""`                               |
| `tls.crt`                   | Base64 encoded server certificate                                     | `""`                               |
| `tls.key`                   | Base64 encoded private key                                            | `""`                               |
| `tls.caBundle`              | Base64 encoded CA for the APIService caBundle                         | `""`                               |
