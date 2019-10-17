CERT_MANAGER_VERSION:=v0.10.1

mvn-dependencies:
	mvn versions:display-plugin-updates

minikube-setup:
	minikube start
	minikube addons enable ingress
	kubectl apply -f src/main/kubernetes/operator/deployment/ec-operator.namespace.yaml
	kubectl apply -f src/main/kubernetes/operator/deployment/ec-operator.clusterrole.yaml
	kubectl apply -f src/main/kubernetes/operator/deployment/ec-operator.serviceaccount.yaml
	kubectl apply -f src/main/kubernetes/operator/deployment/ec-operator.clusterrolebinding.yaml
	kubectl apply -f src/main/kubernetes/operator/crd/ec-operator.crd.xpdeployment.yaml
	kubectl apply -f https://github.com/jetstack/cert-manager/releases/download/${CERT_MANAGER_VERSION}/cert-manager.yaml
	./src/test/minikube/minikube.sh

minikube-operator-deploy:
	kubectl apply -f src/main/kubernetes/operator/crd/ec-operator.crd.xpdeployment.yaml
	./mvnw -DskipTests clean package
	bash -c 'eval $$(minikube docker-env) && docker build -f src/main/docker/Dockerfile.jvm -t enonic/kubernetes-operator .'

	-kubectl delete -f src/main/kubernetes/operator/deployment/ec-operator.deployment.yaml
	kubectl apply -f src/main/kubernetes/operator/deployment/ec-operator.deployment.yaml

minikube-operator-port-forward:
	kubectl -n ec-system get pods | grep ec-operator | awk '{print $$1}' | xargs -I % kubectl -n ec-system port-forward % 8080

post:
	cat src/test/json/test.json | http -v -j POST :8080/api

put:
	cat src/test/json/test.json | http -v -j PUT :8080/api/4143b189-f371-4c58-b126-04a7adce257e

vhost:
	 http -v --verify=no https://$(shell minikube ip)/app host:company.com