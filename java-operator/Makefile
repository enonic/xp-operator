.PHONY: help
.DEFAULT_GOAL:=help

validate: ## Run k8s kind cluster setup is working
	@$(MAKE) -C ../kubernetes/kind --no-print-directory kind-network-info
	@echo "Operator info:"
	@kubectl get --raw='/apis/operator.enonic.cloud/v1/operator/version' | jq

help: ## Show help
	@echo "Makefile help:"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-15s\033[0m %s\n", $$1, $$2}'
