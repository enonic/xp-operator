# ec-operator

## TODO

* Create admission endpoint in operator for validation

## Debug with minikube

### Run operator locally
* Setup minikube: `make minikube-setup`
* Create jar run configuration with mvn pre step: `-DskipTests clean package`
* Run the project

### Run operator on minikube
* Setup minikube: `make minikube-setup`
* Deploy to minikube: `make minikube-operator-deploy`
* Port forward to operator: `make minikube-operator-port-forward`