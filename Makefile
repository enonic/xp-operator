CERT_MANAGER_VERSION:=v0.11.0
LOCAL_OPERATOR_PORT:=8080

mvn-dependencies:
	mvn versions:display-plugin-updates

minikube-start:
	minikube start
	kubectl config use-context minikube
	minikube addons enable ingress

minikube-certmanager:
	kubectl apply -f https://github.com/jetstack/cert-manager/releases/download/${CERT_MANAGER_VERSION}/cert-manager.yaml --validate=false

minikube-operator-setup:
	kubectl apply -f src/main/kubernetes/operator/deployment/ec-operator.namespace.yaml
	kubectl apply -f src/main/kubernetes/operator/deployment/ec-operator.clusterrole.yaml
	kubectl apply -f src/main/kubernetes/operator/deployment/ec-operator.serviceaccount.yaml
	kubectl apply -f src/main/kubernetes/operator/deployment/ec-operator.clusterrolebinding.yaml
	kubectl apply -f src/main/kubernetes/operator/crd/ec-operator.crd.xp.deployments.yaml
	kubectl apply -f src/main/kubernetes/operator/crd/ec-operator.crd.xp.vhosts.yaml

minikube-ingress-patch:
	./src/test/minikube/minikube.sh

minikube-linkerd:
	linkerd install | kubectl apply -f -
	bash -c 'until linkerd check; do echo "Waiting for linkerd..."; sleep 5; done'

minikube-setup-linkerd: minikube-start minikube-linkerd minikube-operator-setup minikube-certmanager minikube-ingress-patch
minikube-setup: minikube-start minikube-operator-setup minikube-certmanager minikube-ingress-patch

minikube-operator-deploy:
	kubectl apply -f src/main/kubernetes/operator/crd/ec-operator.crd.xp.deployments.yaml
	kubectl apply -f src/main/kubernetes/operator/crd/ec-operator.crd.xp.vhosts.yaml
	./mvnw clean package
	bash -c 'eval $$(minikube docker-env) && docker build -f src/main/docker/Dockerfile.jvm -t enonic/kubernetes-operator .'
	kubectl apply -f src/main/kubernetes/operator/deployment/ec-operator.deployment.yaml
	-kubectl -n ec-system get pods | grep ec-operator | awk '{print $$1}' | xargs -I % kubectl -n ec-system delete pod %

minikube-operator-logs:
	kubectl -n ec-system get pods | grep ec-operator | awk '{print $$1}' | xargs -I % kubectl -n ec-system logs -f %

minikube-operator-port-forward:
	kubectl -n ec-system get pods | grep ec-operator | awk '{print $$1}' | xargs -I % kubectl -n ec-system port-forward % ${LOCAL_OPERATOR_PORT}:8080

create-deployment:
	kubectl apply -f src/test/example-deployment.yaml

post-vhost:
	cat src/test/example-vhost.json | http -v -j POST :${LOCAL_OPERATOR_PORT}/api/$(shell http :${LOCAL_OPERATOR_PORT}/api | jq -r '.[0]')/vhosts