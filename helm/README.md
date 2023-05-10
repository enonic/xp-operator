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
helm install --namespace kube-system enonic-operator enonic/xp-operator
```

## Configuration

The following table lists the configurable parameters of the xp-operator chart. For details look at [values.yaml](./values.yaml).

| Parameter            | Description                                                           | Default                            |
| -------------------- | --------------------------------------------------------------------- | ---------------------------------- |
| `image.repository`   | Operator image repository                                             | `enonic/xp-operator`               |
| `image.tag`          | Operator image tag                                                    | _see [values.yaml](./values.yaml)_ |
| `image.pullPolicy`   | Operator image pull policy                                            | `IfNotPresent`                     |
| `resources.limits`   | The resources limits for the Operator container                       | `{}`                               |
| `resources.requests` | The requested resources for the Operator container                    | `{}`                               |
| `labels`             | Additional labels for Operator pods                                   | `{}`                               |
| `annotations`        | Additional annotations for Operator pods                              | `{}`                               |
| `env`                | Environmental variables for the Operator container                    | `{}`                               |
| `secrets`            | Secrets mounted as environmental variables for the Operator container | `{}`                               |
| `config`             | Configuration for the Operator container                              | _see [values.yaml](./values.yaml)_ |
| `onlyDeployCrds`     | Only deploy the CRD definitions                                       | `false`                            |
