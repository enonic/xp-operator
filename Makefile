minikube-setup:
	minikube start
	kubectl apply -f src/main/kubernetes/operator/deployment/ec-operator.namespace.yaml
	kubectl apply -f src/main/kubernetes/operator/deployment/ec-operator.clusterrole.yaml
	kubectl apply -f src/main/kubernetes/operator/deployment/ec-operator.serviceaccount.yaml
	kubectl apply -f src/main/kubernetes/operator/deployment/ec-operator.clusterrolebinding.yaml

minikube-deploy:
	kubectl apply -f src/main/kubernetes/operator/crd/ec-operator.crd.xpdeployment.yaml
	./mvnw -DskipTests package
	bash -c 'eval $$(minikube docker-env) && docker build -f src/main/docker/Dockerfile.jvm -t enonic/kubernetes-operator .'

	-kubectl delete -f src/main/kubernetes/operator/deployment/ec-operator.deployment.yaml
	kubectl apply -f src/main/kubernetes/operator/deployment/ec-operator.deployment.yaml

minikube-port-forward:
	kubectl -n ec-system get pods | grep ec-operator | awk '{print $$1}' | xargs -I % kubectl -n ec-system port-forward % 8080

minikube-logs:
	kubectl -n ec-system get pods | grep ec-operator | awk '{print $$1}' | xargs kubectl -n ec-system logs -f

post:
	cat src/test/json/test.json | http -v -j POST :8080/

put:
	cat src/test/json/test.json | http -v -j PUT :8080/