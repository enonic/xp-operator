<h1>Enonic Kubernetes Operator Client</h1>

The Enonic Kubernetes Operator is a piece of software runs on top of Kubernetes to help you manage your XP deployments.

- [Documentation](./docs/operator.adoc)
- [Local development](#local-development)
  - [Requirements](#requirements)
  - [Setup](#setup)
  - [Starting K8s cluster](#starting-k8s-cluster)
  - [Starting operator](#starting-operator)
  - [Verify it works](#verify-it-works)
  - [Spoof hosts for SSE events](#spoof-hosts-for-sse-events)

## Local development

### Requirements

* [docker](https://docs.docker.com/get-docker/)
* [kind](https://kind.sigs.k8s.io/)
* [helm](https://helm.sh/docs/intro/install/)
* [kubectl](https://kubernetes.io/docs/tasks/tools/install-kubectl/)

### Setup

Kind does not support RWX storage classes by default, so we have to create one. We are using a helm chart for that so we need to add that repository:

```console
$ helm repo add nfs https://kubernetes-sigs.github.io/nfs-subdir-external-provisioner/
$ helm repo update
```

### Starting K8s cluster

```console
$ export MY_IP=192.?.?.?
$ make cluster
```

### Starting operator

Use your IDE to run the `com.enonic.cloud.operator.helpers.Main` class and set the environment to:

```
QUARKUS_HTTP_PORT=8081;OPERATOR_TASKS_SYNC_INTERVAL=30000;QUARKUS_HTTP_SSL_CERTIFICATE_FILE=src/test/kubernetes/certs/tls.crt;QUARKUS_HTTP_SSL_CERTIFICATE_KEY_FILE=src/test/kubernetes/certs/tls.key;OPERATOR_CHARTS_VALUES_STORAGE_SHARED_STORAGECLASSNAME=nfs
```

### Verify it works

```console
curl localhost:8001/apis/operator.enonic.cloud/v1alpha1
```

### Spoof hosts for SSE events

Add to your hosts file:

```
127.0.0.1 all.mycloud-mysolution-myenv-myservice.svc.cluster.local
```

And run proxy:

```console
kubectl -n mycloud-mysolution-myenv-myservice port-forward main-0 8080
```
