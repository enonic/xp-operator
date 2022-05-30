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

publish: ## Setup repo for release. Provide VERSION as env var i.e. 'VERSION=0.17.13 make release'
	# Checking prerequisites
	@test "${VERSION}" != "" || (echo 'You must provide a version!'; exit 1)
	@[[ ${VERSION} != v* ]] || (echo 'Version cannot start with "v"!'; exit 1)
	@test "$(shell git rev-parse --abbrev-ref HEAD)" == "master" || (echo 'You must be on the master branch!'; exit 1)
	@git diff --quiet || (echo 'Git repo is not clean, commit your changes first!'; exit 1)
	
	# Setting chart version
	@yq -i '.version = "${VERSION}"' helm/Chart.yaml
	@yq -i '.appVersion = "${VERSION}"' helm/Chart.yaml
	@yq -i '.image.tag = "${VERSION}"' helm/values.yaml

	# Setting java-client version
	@./mvnw -f java-client/ versions:set -DnewVersion=${VERSION} > /dev/null

	# Setting java-operator version
	@./mvnw -f java-operator/ versions:set -DnewVersion=${VERSION} > /dev/null

	# Setting parent pom version
	@./mvnw -f . versions:set -DnewVersion=${VERSION} > /dev/null

	# Creating and pushing release commit
	@git add helm/values.yaml helm/Chart.yaml pom.xml java-client/pom.xml java-operator/pom.xml
	@git commit -m "Set version to ${VERSION}"
	@git push origin master

	@echo
	@echo If build succeedes on github actions, tag and push to release:
	@echo '  $$' git tag -a v${VERSION} -m "\"v${VERSION}\""
	@echo '  $$' git push origin v${VERSION}

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
