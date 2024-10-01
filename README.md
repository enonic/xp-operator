<img align="right" src="https://raw.githubusercontent.com/enonic/xp/master/misc/logo.png">
<h1>Enonic XP Kubernetes Operator</h1>

The Enonic XP Kubernetes Operator is a piece of software runs on top of Kubernetes to help you manage your XP deployments.

- [Contents of this repository](#contents-of-this-repository)
- [Usage](#usage)
- [Development](#development)
  - [Requirements for local development](#requirements-for-local-development)
  - [Running and installing operator to kind cluster](#running-and-installing-operator-to-kind-cluster)
  - [Using custom XP docker image](#using-custom-xp-docker-image)
  - [Running operator in IDE](#running-operator-in-ide)
    - [Spoof hosts for SSE events](#spoof-hosts-for-sse-events)
    - [Enable dns records](#enable-dns-records)
- [Release](#release)


## Contents of this repository

This repository contains these various parts relating to the operator:

* [Enonic XP Kubernetes Operator](./java-operator)
* [Enonic XP Kubernetes Operator Helm Chart](./helm)
* [Enonic XP Kubernetes Operator Java Client](./java-client)
* [Enonic XP kubectl plugin](./kubectl-plugin)
* [Enonic XP Kubernetes Operator Documentation](./docs/index.adoc)

## Usage

Read about how to install and use the operator in the [documentation](./docs/index.adoc).

## Development

### Requirements for local development

* Java 11
* make
* yq
* [helm](https://helm.sh/docs/intro/install/)
* [docker](https://docs.docker.com/get-docker/)
* [kind](https://kind.sigs.k8s.io/)
* [kubectl](https://kubernetes.io/docs/tasks/tools/install-kubectl/)

### Building operator

```bash
make build
```


### Running and installing operator to kind cluster

Simply run:

```bash
make test
```

```
...
...
# Cluster setup done!
```

Then verify the operator is running:

```bash
make verify-kind
```
```
{
  "gitCommit": "2a2abb974a0c8ae75a23d79ed02236df1b04cc15",
  "gitTags": "",
  "buildDate": "2021-03-04T15:32:53+0100",
  "version": "0.16.0",
  "gitTreeState": "dirty"
}
```

Cluster need some time to start. 
After that you can apply a simple example:

```bash
kubectl apply -f kubernetes/example-simple.yaml
```

When you are done testing, you can delete the `kind` cluster:

```bash
make clean-kind
```

#### Using custom XP docker image

If you need to specify a custom XP docker image, you can do so by setting the `operator.charts.values.image.nameTemplate` property in
[values.yaml](helm%2Fsrc%2Ftest%2Fvalues.yaml)

```properties
operator.charts.values.image.nameTemplate=gcr.io/playground-186616/enonic-xp:nightly
operator.charts.values.image.pullPolicy=Always
```

### Running operator in IDE

Start kind cluster in dev mode:

```bash
export MY_IP=192.?.?.?
make dev
```

Start operator.

Use your IDE to run the `com.enonic.kubernetes.operator.helpers.Main` class and set the VM options to to:

```
-Doperator.charts.path=java-operator/src/main/helm -Doperator.charts.values.storage.shared.storageClassName=nfs -Dquarkus.http.ssl.certificate.file=kubernetes/kind/certs/tls.crt -Dquarkus.http.ssl.certificate.key-file=kubernetes/kind/certs/tls.key
```

Then verify it all works:

```bash
make verify-kind 
```
```
{
  "gitCommit": "2a2abb974a0c8ae75a23d79ed02236df1b04cc15",
  "gitTags": "",
  "buildDate": "2021-03-04T15:32:53+0100",
  "version": "0.16.0",
  "gitTreeState": "dirty"
}
```

#### Spoof hosts for SSE events

Add to your hosts file:

```
127.0.0.1 all.mycloud-mysolution-myenv-myservice.svc.cluster.local
```

And run proxy:

```bash
kubectl -n mycloud-mysolution-myenv-myservice port-forward main-0 4848
```

## Release

Make sure you workspace contains no uncommitted changes and then run:

```bash
VERSION=0.18.0 make release
```

That will update [gradle.properties](gradle.properties) version to `0.18.0` and push to GitHub. That will trigger a build. If that build succeeds, tag it for release:

```bash
git tag -a v0.18.0 -m "v0.18.0"
git push origin v0.18.0
```

This will trigger a release!

You now should update the version to next SNAPSHOT in [gradle.properties](gradle.properties)
```bash
yq -i -p=props -o=props '.version = "0.19.0-SNAPSHOT"' gradle.properties
```
