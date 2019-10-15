minikube-setup:
	minikube start
	kubectl apply -f src/main/kubernetes/operator/deployment/ec-operator.namespace.yaml
	kubectl apply -f src/main/kubernetes/operator/deployment/ec-operator.clusterrole.yaml
	kubectl apply -f src/main/kubernetes/operator/deployment/ec-operator.serviceaccount.yaml
	kubectl apply -f src/main/kubernetes/operator/deployment/ec-operator.clusterrolebinding.yaml
	kubectl apply -f src/main/kubernetes/operator/crd/cert-manager.crd.issuer.yaml
	kubectl apply -f src/main/kubernetes/operator/crd/ec-operator.crd.xpdeployment.yaml

minikube-operator-deploy:
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