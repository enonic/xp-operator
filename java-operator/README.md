<h1>Enonic Kubernetes Operator</h1>

The Enonic Kubernetes Operator is a piece of software runs on top of Kubernetes to help you manage your XP deployments.

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

Navigate to [./kubernetes/kind](../kubernetes/kind) and run these commands:

```console
$ export MY_IP=192.?.?.?
$ make cluster
```

### Starting operator

Use your IDE to run the `com.enonic.kubernetes.operator.helpers.Main` class and set the VM options to to:

```
-Doperator.charts.path=java-operator/src/main/helm -Doperator.charts.values.storage.shared.storageClassName=nfs -Dquarkus.http.ssl.certificate.file=kubernetes/kind/certs/tls.crt -Dquarkus.http.ssl.certificate.key-file=kubernetes/kind/certs/tls.key
```

### Verify it works

When its not up:

```console
$ kubectl get --raw='/apis/operator.enonic.cloud/v1alpha1'
Error from server (ServiceUnavailable): the server is currently unable to handle the request
```

When its up:

```console
$ kubectl get --raw='/apis/operator.enonic.cloud/v1alpha1'
{"apiVersion":"v1","kind":"APIResourceList","groupVersion":"operator.enonic.cloud/v1alpha","resources":[{"kind":"AdmissionReview","name":"validations","verbs":["create"],"version":"v1beta1","namespaced":false,"group":"admission.k8s.io","singularName":""},{"kind":"AdmissionReview","name":"mutations","verbs":["create"],"version":"v1beta1","namespaced":false,"group":"admission.k8s.io","singularName":""}]}
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
