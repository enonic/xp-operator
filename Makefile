CERT_MANAGER_VERSION:=v0.11.0
LOCAL_OPERATOR_PORT:=8080
IMAGE:=gbbirkisson/ec-operator

docker-build:
	./mvnw clean package
	docker build -f src/main/docker/Dockerfile.jvm -t operatortmp .

docker-push: docker-build
	docker tag operatortmp ${IMAGE}:$(shell ./get-version.sh)
	docker push ${IMAGE}:$(shell ./get-version.sh)

mvn-dependencies:
	mvn versions:display-plugin-updates

minikube-start:
	minikube start
	kubectl config use-context minikube
	minikube addons enable ingress

minikube-certmanager:
	kubectl apply -f https://github.com/jetstack/cert-manager/releases/download/${CERT_MANAGER_VERSION}/cert-manager.yaml --validate=false

minikube-certmanager-issuers:
	kubectl apply -f src/main/kubernetes/certmanager/certmanager.clusterissuers.yaml

minikube-operator-setup:
	kubectl apply -f src/main/kubernetes/operator/deployment/ec-operator.dep.namespace.yaml
	kubectl apply -f src/main/kubernetes/operator/crd/ec-operator.crd.xp7.deployments.yaml
	kubectl apply -f src/main/kubernetes/operator/crd/ec-operator.crd.xp7.vhosts.yaml

minikube-ingress-patch:
	-./src/test/minikube/minikube.sh

minikube-linkerd:
	linkerd install | kubectl apply -f -
	bash -c 'until linkerd check; do echo "Waiting for linkerd..."; sleep 5; done'

minikube-setup-linkerd: minikube-start minikube-certmanager minikube-linkerd minikube-operator-setup minikube-ingress-patch minikube-certmanager-issuers
minikube-setup: minikube-start minikube-certmanager minikube-operator-setup minikube-ingress-patch minikube-certmanager-issuers

minikube-operator-deploy:
	./mvnw clean package
	bash -c 'eval $$(minikube docker-env) && docker build -f src/main/docker/Dockerfile.jvm -t enonic/kubernetes-operator .'
	kubectl apply -f src/main/kubernetes/operator/deployment/ec-operator.dep.serviceaccount.yaml
	kubectl apply -f src/main/kubernetes/operator/deployment/ec-operator.dep.configmap.yaml
	kubectl apply -f src/main/kubernetes/operator/deployment/ec-operator.dep.admission.cert.yaml
	kubectl apply -f src/main/kubernetes/operator/deployment/ec-operator.dep.deployment.yaml
	sleep 5 # Let certmanager create the certificate
	kubectl -n ec-system get secrets ec-operator-webhook-tls -o jsonpath='{.data.ca\.crt}' | xargs -I % sed s#{CA_CERT}#%#g src/main/kubernetes/operator/deployment/ec-operator.dep.api.yaml | kubectl apply -f -
	-kubectl -n ec-system get pods | grep ec-operator | awk '{print $$1}' | xargs -I % kubectl -n ec-system delete pod %

minikube-operator-logs:
	kubectl -n ec-system get pods | grep ec-operator | awk '{print $$1}' | xargs -I % kubectl -n ec-system logs -f %

minikube-operator-port-forward:
	kubectl -n ec-system get pods | grep ec-operator | awk '{print $$1}' | xargs -I % kubectl -n ec-system port-forward % ${LOCAL_OPERATOR_PORT}:8080

create-deployment:
	kubectl apply -f src/test/example-deployment.yaml

post-admission-success:
	cat src/test/admission-success.json | http -v -j POST :${LOCAL_OPERATOR_PORT}/admission/validate

post-admission-fail:
	cat src/test/admission-fail.json | http -v -j POST :${LOCAL_OPERATOR_PORT}/admission/validate