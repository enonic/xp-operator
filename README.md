# ec-operator

## TODO

* This project assumes that cloud+project is always unique, is that the case?
* Create admission endpoint in operator for validation
* Fix tests

## Debug with minikube

### Run operator locally
* Setup minikube: `make minikube-setup`
* Create jar run configuration with mvn pre step: `-DskipTests clean package`
* Run the project

### Run operator on minikube
* Setup minikube: `make minikube-setup`
* Deploy to minikube: `make minikube-operator-deploy`
* Port forward to operator: `make minikube-operator-port-forward` (You can change the `LOCAL_OPERATOR_PORT` in the make file for a different port)
* View operator logs: `make minikube-operator-logs`
* Post sample json to operator: `make post`