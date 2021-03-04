.PHONY: help
.DEFAULT_GOAL:=help

test: ## Run k8s kind cluster with operator installed
	@$(MAKE) -C .. --no-print-directory test

dev: ## Run k8s kind cluster setup for development
	@$(MAKE) -C ../kubernetes/kind --no-print-directory kind-up kind-dev

validate: ## Run k8s kind cluster setup is working
	@kubectl get --raw='/apis/operator.enonic.cloud/v1alpha1/internal/version' | jq

clean: ## Clean up everything
	@$(MAKE) -C .. --no-print-directory clean

help: ## Show help
	@echo "Makefile help:"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-15s\033[0m %s\n", $$1, $$2}'