DOCKER_IMAGE:=enonic/ec-operator

.PHONY: help
.DEFAULT_GOAL:=help

mvn-build: ## Build java modules
	@./mvnw clean package

docker-build: mvn-build ## Docker build image
	@docker build -f docker/Dockerfile -t ${DOCKER_IMAGE}:$(shell ./.mvn/get-version) .

docker-run: docker-build ## Docker run image
	@docker run --rm -it \
		--net=host \
		-e QUARKUS_HTTP_INSECURE_REQUESTS=enabled \
		-v ${HOME}/.kube:/opt/jboss/.kube:ro \
		${DOCKER_IMAGE}:$(shell ./.mvn/get-version)

validate: ## Build and validate everything
	@[ "$(shell bash -c 'cat helm/Chart.yaml | yq -r .appVersion')" == "$(shell ./.mvn/get-version)" ] || (echo "Helm version mismatch: Chart.yaml appVersion" && exit 1)
	@[ "$(shell bash -c 'cat helm/values.yaml | yq -r .image.tag')" == "$(shell ./.mvn/get-version)" ] || (echo "Helm version mismatch: values.yaml image.tag" && exit 1)

release: validate ## Create a release of the operator
	# Push operator client
	@echo docker push ${DOCKER_IMAGE}:$(shell ./.mvn/get-version)
	# Push helm chart

help: ## Show help
	@echo "Enonic Kubernetes Operator:"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-10s\033[0m %s\n", $$1, $$2}'