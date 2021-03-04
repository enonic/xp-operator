DOCKER_IMAGE:=enonic/xp-operator

.PHONY: help
.DEFAULT_GOAL:=help

build-java: ## Build java modules
	# Building java modules ...
	@./mvnw clean package
	# Building java modules done!

build-docker: build-java ## Build docker image
	# Building docker image ...
	@docker build -f docker/Dockerfile -t ${DOCKER_IMAGE}:$(shell ./.mvn/get-version) .
	# Building docker image done!

build-helm: ## Build helm chart
	# Building helm chart ...
	@$(MAKE) -C helm --no-print-directory package
	# Building helm chart done!

build: build-docker build-helm ## Build everything

validate: ## Build and validate everything
	# Validating helm chart ...
	@[ "$(shell bash -c 'cat helm/Chart.yaml | yq -r .appVersion')" == "$(shell ./.mvn/get-version)" ] || \
		(echo "Helm version mismatch: Chart.yaml appVersion" && exit 1)
	@[ "$(shell bash -c 'cat helm/values.yaml | yq -r .image.tag')" == "$(shell ./.mvn/get-version)" ] || \
		(echo "Helm version mismatch: values.yaml image.tag" && exit 1)
	# Validating helm chart done!

publish: build validate ## Publish everything (env var ARTIFACTORY_TOKEN required)
	@echo "# Starting release for $(shell ./.mvn/get-version) ..."

	# Publishing helm chart ...
	#@$(MAKE) -C helm --no-print-directory publish

	# Publishing docker image ...
	#@echo docker push ${DOCKER_IMAGE}:$(shell ./.mvn/get-version)

	# Publishing java-client ...
	#@$(MAKE) -C java-client --no-print-directory publish

test: validate build-docker ## Run k8s kind cluster with operator installed
	# Start kind cluster
	@$(MAKE) -C kubernetes/kind --no-print-directory kind-up

	# Load docker image into cluster
	@kind load docker-image ${DOCKER_IMAGE}:$(shell ./.mvn/get-version)

	# Deploy operator
	@$(MAKE) -C helm --no-print-directory install
	# Cluster setup done!

clean: ## Clean up everything
	# Clean java modules
	@./mvnw clean

	# Clean helm chart
	@$(MAKE) -C helm --no-print-directory clean

	# Clean kind cluster
	@$(MAKE) -C kubernetes/kind --no-print-directory kind-down

help: ## Show help
	@echo "Makefile help:"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-15s\033[0m %s\n", $$1, $$2}'