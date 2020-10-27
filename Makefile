LOCAL_OPERATOR_PORT:=8080
IMAGE:=enonic/ec-operator

docker-build:
	./mvnw clean package
	docker build -f src/main/docker/Dockerfile -t operatortmp .

docker-run: docker-build
	docker run -it -v ${HOME}/.kube:/opt/jboss/.kube -v ${HOME}/.minikube:${HOME}/.minikube -p 8081:8080 operatortmp:latest

docker-push: docker-build
	docker tag operatortmp ${IMAGE}:$(shell ./get-version.sh)
	docker push ${IMAGE}:$(shell ./get-version.sh)

mvn-dependencies:
	mvn versions:display-plugin-updates

minikube-setup:
	@kubectl apply -f src/test/kubernetes/proxy.yaml
	@ls --color=none src/test/kubernetes/crd/* | xargs -I {} kubectl apply -f {}

namespace-create:
	@kubectl apply -f src/test/example-namespace.yaml

deployment-create: namespace-create
	@kubectl apply -f src/test/example-deployment.yaml

deployment-delete:
	@kubectl delete -f src/test/example-deployment.yaml

deployment-supass:
	@kubectl -n mycloud-mysolution-myenv-myservice get secrets su -o jsonpath={.data.pass} | base64 -d