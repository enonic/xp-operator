CHART_NAME:=xp-operator
REPOSITORY:=https://repo.enonic.com/helm

.DEFAULT_GOAL:=help
.PHONY: test template clean package publish help

install: ## Install chart into cluster
	@helm upgrade --install \
		--namespace kube-system \
		--values test/values.yaml \
		xp-operator \
		.

template: ## Test chart by templating
	@helm template --namespace kube-system .

clean: ## Clean up build files
	# Clean up target folder ...
	@[ -d "target" ] && rm -r target || true

package: clean ## Package chart
	# Run test (template) ...
	@$(MAKE) template > /dev/null
	
	# Package ...
	@helm package . -d target > /dev/null

publish: package ## Publish chart (env var ARTIFACTORY_USER and ARTIFACTORY_PASS required)
	# Setup environment ...
	@test ${ARTIFACTORY_USER} || (echo "Set env variable ARTIFACTORY_USER"; exit 1;)
	@test ${ARTIFACTORY_PASS} || (echo "Set env variable ARTIFACTORY_PASS"; exit 1;)
	@$(eval FILE := $(shell find target/ -name '*.tgz'))
	@$(eval URL := ${REPOSITORY}/${CHART_NAME}/`basename ${FILE}`)
	@$(eval MD5 := $(shell openssl md5 ${FILE} | cut -d ' ' -f 2))
	@$(eval SHA1 := $(shell openssl sha1 ${FILE} | cut -d ' ' -f 2))
	@$(eval SHA256 := $(shell openssl sha256 ${FILE} | cut -d ' ' -f 2))

	# Check if version already exists ...
	@$(eval EXISTS := $(shell curl -s -f -u "${ARTIFACTORY_USER}:${ARTIFACTORY_PASS}" ${URL}.sha256 > /dev/null && echo "1" || echo "0"))
	@if [ ${EXISTS} == "1" ]; then echo "Artifact $(shell basename ${FILE}) already exists!"; exit 1; fi;

	# Upload artifact ...
	@curl -u "${ARTIFACTORY_USER}:${ARTIFACTORY_PASS}" \
	-H "X-Checksum-MD5:${MD5}" \
	-H "X-Checksum-SHA1:${SHA1}" \
	-H "X-Checksum-SHA256:${SHA256}" \
	-X PUT \
	-T ${FILE} \
	${URL}

help: ## Show help
	@echo "Makefile help:"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-15s\033[0m %s\n", $$1, $$2}'
