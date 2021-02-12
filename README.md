<h1>Enonic Kubernetes Operator Client</h1>

The Enonic Kubernetes Operator is a piece of software runs on top of Kubernetes to help you manage your XP deployments.

- [Documentation](#documentation)
- [Debug with minikube](#debug-with-minikube)
- [Create new release](#create-new-release)

## Documentation

For installation and usage read [the documentation](./docs/operator.adoc).

## Debug with minikube

TODO

## Create new release

```bash
./advance-version.sh
git commit -am "Bump version"
make docker-push
```