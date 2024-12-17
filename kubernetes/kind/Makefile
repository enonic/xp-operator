K8S_VERSION:=v1.29.0
KIND_MTU:=$(shell docker network inspect bridge -f '{{ index .Options "com.docker.network.driver.mtu" }}')
KIND_CTX:=kind-kind

.PHONY: kind-setup kind-bootstrap kind-ingress kind-up kind-down help
.DEFAULT_GOAL:=help

kind-setup:
	# Create kind network
	@docker network create -o "com.docker.network.driver.mtu=${KIND_MTU}" kind

	# Create kind cluster
	@kind create cluster --image=kindest/node:${K8S_VERSION} --config=kind-config.yaml

	# Set kubectl context to kind
	@kubectl config use-context ${KIND_CTX}

kind-bootstrap:
	# Ensure kubectl context
	@[ "$(shell kubectl config current-context)" = "${KIND_CTX}" ] || \
		(echo "Kubectl should be ${KIND_CTX}" && exit 1)

	# Setup NFS server provisioner
	@helm repo add nfs-server-and-external-provisioner https://kubernetes-sigs.github.io/nfs-ganesha-server-and-external-provisioner/

	@helm upgrade --install \
		--namespace kube-system \
		--set persistence.enabled=true \
		--set persistence.size=1Gi \
		--set storageClass.name=nfs \
		--set storageClass.allowVolumeExpansion=true \
		--version 1.8.0 \
		nfs-server-provisioner \
		nfs-server-and-external-provisioner/nfs-server-provisioner

kind-dev: ## Setup cluster for development mode (env var MY_IP required)
	# Ensure MY_IP is set
	@[ ! -z "${MY_IP}" ] || (echo "Set MY_IP variable to you host IP" && exit 1)

	# Apply CRDs
	@ls --color=none ../../helm/crds/* | xargs -I {} kubectl apply -f {}

	# Apply K8s services
	@cat operator-service.yaml | sed s%#IP#%${MY_IP}%g | kubectl apply -f -

kind-ingress:
	# Ensure kubectl context
	@[ "$(shell kubectl config current-context)" = "${KIND_CTX}" ] || \
		(echo "Kubectl should be ${KIND_CTX}" && exit 1)

	# Create nginx-ingress
	@kubectl apply -f nginx-ingress.yaml

kind-network-info: ## Print out kind network info
	@echo -n 'Bridge network MTU: '
	@docker network inspect bridge -f '{{ index .Options "com.docker.network.driver.mtu" }}'
	@echo -n 'Kind network MTU: '
	@docker network inspect kind -f '{{ index .Options "com.docker.network.driver.mtu" }}'
	@echo 'Kind control plane MTU:'
	@docker exec -it kind-control-plane ip link show | grep -i '^[0-9]:'

kind-up: kind-setup kind-bootstrap kind-ingress ## Spin up development cluster

kind-down: ## Destroy development cluster
	# Delete cluster
	-@kind delete cluster > /dev/null

	# Delete kind network
	-@docker network rm kind

drill: ## Run drill
	@drill -s -b drill.yml

help: ## Show help
	@echo "Makefile help:"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-15s\033[0m %s\n", $$1, $$2}'
