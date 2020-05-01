# ec-operator

## Debug with minikube

### Setup minikube

See https://github.com/enonic/ec-terraform

## Create new release

```bash
./advance-version.sh
git commit -am "Bump version"
make docker-push
```