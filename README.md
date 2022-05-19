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

## Usage

Read about how to install and use the operator in the [documentation](./docs/index.adoc).

## Release

Make sure you workspace contains no uncommited changes and then run:

```make
$ VERSION=0.17.15 make publish
```

That will update versions to `0.17.15` and push to github. That will trigger a build. If that build succeeds, tag it for release:

```
$ git tag -a v0.17.15 -m "v0.17.15"
$ git push origin v0.17.15
```

This will trigger a release!