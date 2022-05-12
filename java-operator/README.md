<h1>Enonic XP Kubernetes Operator</h1>

The Enonic XP Kubernetes Operator is a piece of software runs on top of Kubernetes to help you manage your XP deployments.

- [Requirements for local development](#requirements-for-local-development)
- [Running and installing operator to kind cluster](#running-and-installing-operator-to-kind-cluster)
- [Running operator in IDE](#running-operator-in-ide)
  - [Spoof hosts for SSE events](#spoof-hosts-for-sse-events)
  - [Enable dns records](#enable-dns-records)

## Requirements for local development

* [docker](https://docs.docker.com/get-docker/)
* [kind](https://kind.sigs.k8s.io/)
* [helm](https://helm.sh/docs/intro/install/)
* [kubectl](https://kubernetes.io/docs/tasks/tools/install-kubectl/)

## Running and installing operator to kind cluster

Simply run:

```console
$ make test
# Validating helm chart ...
# Validating helm chart done!
# Building java modules ...
...
# Cluster setup done!
```

Then verify the operator is running:

```console
$ make verify 
{
  "gitCommit": "2a2abb974a0c8ae75a23d79ed02236df1b04cc15",
  "gitTags": "",
  "buildDate": "2021-03-04T15:32:53+0100",
  "version": "0.16.0",
  "gitTreeState": "dirty"
}
```

## Running operator in IDE

Start kind cluster in dev mode:

```
$ export MY_IP=192.?.?.?
$ make dev
```

Start operator.

Use your IDE to run the `com.enonic.kubernetes.operator.helpers.Main` class and set the VM options to to:

```
-Doperator.charts.path=java-operator/src/main/helm -Doperator.charts.values.storage.shared.storageClassName=nfs -Dquarkus.http.ssl.certificate.file=kubernetes/kind/certs/tls.crt -Dquarkus.http.ssl.certificate.key-file=kubernetes/kind/certs/tls.key
```

Then verify it all works:

```console
$ make verify 
{
  "gitCommit": "2a2abb974a0c8ae75a23d79ed02236df1b04cc15",
  "gitTags": "",
  "buildDate": "2021-03-04T15:32:53+0100",
  "version": "0.16.0",
  "gitTreeState": "dirty"
}
```

### Spoof hosts for SSE events

Add to your hosts file:

```
127.0.0.1 all.mycloud-mysolution-myenv-myservice.svc.cluster.local
```

And run proxy:

```console
kubectl -n mycloud-mysolution-myenv-myservice port-forward main-0 4848
```

### Enable dns records

Set these in your IDE environment:

```
DNS_ENABLED=true;DNS_LB_STATICIP=192.168.0.69;DNS_CLOUDFLARE_APITOKEN=??
```
