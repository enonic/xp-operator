# ec-operator

## Debug with minikube

Setup minikube:

```bash
minikube start

kubectl apply -f src/main/kubernetes/operator/deployment/ec-operator.namespace.yml
kubectl apply -f src/main/kubernetes/operator/deployment/ec-operator.clusterrole.yaml
kubectl apply -f src/main/kubernetes/operator/deployment/ec-operator.serviceaccount.yaml
kubectl apply -f src/main/kubernetes/operator/deployment/ec-operator.clusterrolebinding.yaml
kubectl apply -f src/main/kubernetes/operator/crd/ec-operator.crd.xpdeployment.yaml
```

Create jar run configuration with mvn pre step: `-DskipTests clean package`


```bash

eval $(minikube docker-env)
./mvnw -DskipTests package
docker build -f src/main/docker/Dockerfile.jvm -t enonic/kubernetes-operator .

kubectl delete -f src/main/kubernetes/operator/deployment/ec-operator.deployment.yaml
kubectl apply -f src/main/kubernetes/operator/deployment/ec-operator.deployment.yaml
```