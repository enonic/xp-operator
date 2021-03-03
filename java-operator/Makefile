IMAGE:=enonic/ec-operator
KIND_CTX:=kind-kind
NFS_SERVER_NAME:=nfs_server

.DEFAULT_GOAL:=help

docker-build: ## Docker build image
	@./mvnw clean package
	@docker build -f src/main/docker/Dockerfile -t operatortmp .

docker-run: docker-build ## Docker run image
	@docker run --rm -it -v ${HOME}/.kube:/opt/jboss/.kube -p 8081:8080 operatortmp:latest

docker-push: docker-build ## Docker push image
	@docker tag operatortmp ${IMAGE}:$(shell ./get-version.sh)
	@docker push ${IMAGE}:$(shell ./get-version.sh)

namespace-create:
	@kubectl apply -f src/test/example-namespace.yaml

deployment-create: namespace-create
	@kubectl apply -f src/test/example-deployment.yaml

deployment-ingress:
	@kubectl apply -f src/test/example-ingress.yaml

deployment-delete:
	@kubectl delete -f src/test/example-deployment.yaml

deployment-supass:
	@kubectl -n mycloud-mysolution-myenv-myservice get secret su -o go-template="{{ .data.pass | base64decode }}"

deployment-drill:
	@drill -s -b src/test/drill.yml

help: ## Show help
	@echo "Helm chart ${CHART_NAME} makefile targets:"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-20s\033[0m %s\n", $$1, $$2}'