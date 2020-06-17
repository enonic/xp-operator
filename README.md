# ec-operator

## Debug with minikube

### Setup minikube

Run minikube makefile in [minikube repo](https://vault.enonic.io/kubernetes/minikube):
```console
make start patch
```

Deploy required resources:
```console
make minikube-setup
```

## Create new release

```bash
./advance-version.sh
git commit -am "Bump version"
make docker-push
```