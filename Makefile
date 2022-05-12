DOCKER_IMAGE:=enonic/xp-operator

.PHONY: help
.DEFAULT_GOAL:=help

build-java: ## Build java modules
	# Building java modules ...
	@./mvnw clean package
	# Building java modules done!

build-docker: build-java ## Build docker image
	@TAG=$(shell ./.mvn/get-version) $(MAKE) -C docker --no-print-directory build

build-helm: ## Build helm chart
	# Building helm chart ...
	@$(MAKE) -C helm --no-print-directory package
	# Building helm chart done!

build: build-docker build-helm ## Build everything

validate: ## Build and validate everything
	# Validating helm chart ...
	@[ "$(shell bash -c 'cat helm/Chart.yaml | yq .appVersion')" == "$(shell ./.mvn/get-version)" ] || \
		(echo "Helm version mismatch: Chart.yaml appVersion! Aborting ..." && exit 1)
	@[ "$(shell bash -c 'cat helm/values.yaml | yq .image.tag')" == "$(shell ./.mvn/get-version)" ] || \
		(echo "Helm version mismatch: values.yaml image.tag! Aborting ..." && exit 1)
	# Validating helm chart done!

publish: build validate  ## Publish everything (env var ARTIFACTORY_USER and ARTIFACTORY_PASS required)
	# Check if docker is logged in
	@: | docker login | grep "Login Succeeded" || (echo "Not logged in to docker"; exit 1;)

	# Check if token is set ...
	@test "${ARTIFACTORY_USER}" || (echo "Set env variable ARTIFACTORY_USER"; exit 1;)
	@test "${ARTIFACTORY_PASS}" || (echo "Set env variable ARTIFACTORY_PASS"; exit 1;)

	# Validate version
	@[[ ! "$(shell ./.mvn/get-version)" =~ .*"SNAPSHOT" ]] || \
		(echo "Version is a SNAPSHOT! Aborting ... " && exit 1)
	
	# Validate that git is clean
	@git diff --quiet || (echo "Git repo is dirty! Aborting ..." && exit 1)

	# Validate
	@[[ "v$(shell ./.mvn/get-version)" == "$(shell git tag --points-at HEAD)" ]] || \
		(echo "Git tag does not match version! Aborting ..." && exit 1)

	@echo "# Starting release for $(shell ./.mvn/get-version) ..."

	# Build docker image ....
	@TAG=$(shell ./.mvn/get-version) $(MAKE) -C docker --no-print-directory buildx

	# Publishing helm chart ...
	@$(MAKE) -C helm --no-print-directory publish

	# Publishing docker image ...
	@TAG=$(shell ./.mvn/get-version) BUILD_ARGS=--push $(MAKE) -C docker --no-print-directory buildx
	@docker push ${DOCKER_IMAGE}:$(shell ./.mvn/get-version)

	# Publishing java-client ...
	@$(MAKE) -C java-client --no-print-directory publish

	# Push tag
	@git push origin "v$(shell ./.mvn/get-version)"

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
