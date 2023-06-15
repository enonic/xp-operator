DOCKER_IMAGE:=enonic/xp-operator

.PHONY: help build clean test
.DEFAULT_GOAL:=help

build-java: ## Build java modules
	# Building java modules ...
	@./gradlew build
	# Building java modules done!

build-docker: build-java ## Build docker image
	@docker build -f Dockerfile -t ${DOCKER_IMAGE}:$(shell ./get-version.sh) .

build: build-docker

release: ## Setup repo for release. Provide VERSION as env var i.e. 'VERSION=0.18 make release'
	# Checking prerequisites
	@test "${VERSION}" != "" || (echo 'You must provide a version!'; exit 1)
	@[[ ${VERSION} != v* ]] || (echo 'Version cannot start with "v"!'; exit 1)
	@test "$(shell git rev-parse --abbrev-ref HEAD)" == "master" || (echo 'You must be on the master branch!'; exit 1)
	@git diff --quiet || (echo 'Git repo is not clean, commit your changes first!'; exit 1)

	# Setting chart version
	@yq -i -p=props -o=props '.version = "${VERSION}"' gradle.properties

	# Creating and pushing release commit
	@git add gradle.properties
	@git commit -m "Set version to ${VERSION}"
	@git push origin master

	@echo
	@echo If build succeeds on github actions, tag and push to release:
	@echo '  $$' git tag -a v${VERSION} -m "\"v${VERSION}\""
	@echo '  $$' git push origin v${VERSION}

test: build-docker ## Run k8s kind cluster with operator installed
	# Start kind cluster
	@$(MAKE) -C kubernetes/kind --no-print-directory kind-up

	# Load docker image into cluster
	@kind load docker-image ${DOCKER_IMAGE}:$(shell ./get-version.sh)

	# Deploy operator
	@helm upgrade --install \
			--namespace kube-system \
			--values helm/src/test/values.yaml \
			xp-operator \
			./helm/build/libs/xp-operator-$(shell ./get-version.sh).tgz
	# Cluster setup done!
	@echo
	@echo Now you can setup a simple XP instance with:
	@echo '  $$' kubectl apply -f kubernetes/example-simple.yaml

dev:
	@$(MAKE) -C kubernetes/kind --no-print-directory kind-up kind-dev

verify-kind: ## Run k8s kind cluster setup is working
	@$(MAKE) -C kubernetes/kind --no-print-directory kind-network-info
	@echo "Operator info:"
	@kubectl get --raw='/apis/operator.enonic.cloud/v1/operator/version' | yq

clean-kind: ## Stop k8s kind cluster
	@$(MAKE) -C kubernetes/kind --no-print-directory kind-down

clean: clean-kind ## Clean up everything
	@./gradlew clean

help: ## Show help
	@echo "Makefile help:"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-15s\033[0m %s\n", $$1, $$2}'
