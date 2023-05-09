<img align="right" src="https://raw.githubusercontent.com/enonic/xp/master/misc/logo.png">
<h1>Enonic XP Kubernetes Operator</h1>

The Enonic XP Kubernetes Operator is a piece of software runs on top of Kubernetes to help you manage your XP deployments.

- [Contents of this repository](#contents-of-this-repository)
- [Usage](#usage)
- [Release](#release)

## Contents of this repository

This repository contains these various parts relating to the operator:

* [Enonic XP Kubernetes Operator](./java-operator)
* [Enonic XP Kubernetes Operator Helm Chart](./helm)
* [Enonic XP Kubernetes Operator Java Client](./java-client)
* [Enonic XP kubectl plugin](./kubectl-plugin)
* [Enonic XP Kubernetes Operator Documentation](./docs/index.adoc)

## Building

Prerequisites:
- Java 11 installed
- helm 3 installed https://helm.sh/docs/intro/install/


To build the operator, run:

```bash
make build
```

## Usage

Read about how to install and use the operator in the [documentation](./docs/index.adoc).

## Release

Make sure you workspace contains no uncommitted changes and then run:

```bash
$ VERSION=0.18 make publish
```

That will update versions to `0.18.0` and push to GitHub. That will trigger a build. If that build succeeds, tag it for release:

```bash
$ git tag -a v0.18 -m "v0.18.0"
$ git push origin v0.18.0
```

This will trigger a release!
