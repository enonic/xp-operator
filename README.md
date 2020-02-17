# ec-operator

## Debug with minikube

### Setup minikube

See https://github.com/enonic/ec-terraform/tree/master/clusters/minikube-dev

### Run operator locally

* Create jar run configuration with mvn pre step: `-DskipTests clean package`
* Run the project

## Create new release

```bash
./advance-version.sh
git commit -am "Bump version"
make docker-push
```