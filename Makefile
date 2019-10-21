CERT_MANAGER_VERSION:=v0.10.1
LOCAL_OPERATOR_PORT:=8081

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
	./mvnw clean package
	bash -c 'eval $$(minikube docker-env) && docker build -f src/main/docker/Dockerfile.jvm -t enonic/kubernetes-operator .'
	kubectl apply -f src/main/kubernetes/operator/deployment/ec-operator.deployment.yaml
	-kubectl -n ec-system get pods | grep ec-operator | awk '{print $$1}' | xargs -I % kubectl -n ec-system delete pod %

minikube-operator-logs:
	kubectl -n ec-system get pods | grep ec-operator | awk '{print $$1}' | xargs -I % kubectl -n ec-system logs -f %

minikube-operator-port-forward:
	kubectl -n ec-system get pods | grep ec-operator | awk '{print $$1}' | xargs -I % kubectl -n ec-system port-forward % ${LOCAL_OPERATOR_PORT}:8080

post:
	cat src/test/example.json | http -v -j POST :${LOCAL_OPERATOR_PORT}/api