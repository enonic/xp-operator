minikube-setup:
	minikube start
	kubectl apply -f src/main/kubernetes/operator/deployment/ec-operator.namespace.yaml
	kubectl apply -f src/main/kubernetes/operator/deployment/ec-operator.clusterrole.yaml
	kubectl apply -f src/main/kubernetes/operator/deployment/ec-operator.serviceaccount.yaml
	kubectl apply -f src/main/kubernetes/operator/deployment/ec-operator.clusterrolebinding.yaml
	kubectl apply -f src/main/kubernetes/operator/crd/cert-manager.crd.issuer.yaml
	kubectl apply -f src/main/kubernetes/operator/crd/ec-operator.crd.xpdeployment.yaml

minikube-deploy-operator:
	kubectl apply -f src/main/kubernetes/operator/crd/ec-operator.crd.xpdeployment.yaml
	./mvnw -DskipTests clean package
	bash -c 'eval $$(minikube docker-env) && docker build -f src/main/docker/Dockerfile.jvm -t enonic/kubernetes-operator .'

	-kubectl delete -f src/main/kubernetes/operator/deployment/ec-operator.deployment.yaml
	kubectl apply -f src/main/kubernetes/operator/deployment/ec-operator.deployment.yaml

post:
	cat src/test/json/test.json | http -v -j POST :8080/api