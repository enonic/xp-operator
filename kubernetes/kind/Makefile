K8S_VERSION:=v1.17.17
KIND_CTX:=kind-kind
NFS_SERVER_NAME:=nfs_server
NFS_SERVER_DIR:=/tmp/nfsshare

.PHONY: kind-setup kind-bootstrap kind-ingress kind-up kind-down help
.DEFAULT_GOAL:=help

kind-setup:
	# Ensure MY_IP is set
	@[ ! -z "${MY_IP}" ] || (echo "Set MY_IP variable to you host IP" && exit 1)

	# Create kind cluster
	@kind create cluster --image=kindest/node:${K8S_VERSION} --config=kind-config.yaml

	# Start NFS server
	@(docker ps | grep ${NFS_SERVER_NAME}) || docker run -d \
		--name ${NFS_SERVER_NAME} \
		--network=kind \
		--privileged \
		-v ${NFS_SERVER_DIR}:/nfsshare \
		-e SHARED_DIRECTORY=/nfsshare \
		itsthenetwork/nfs-server-alpine:latest
	
	# Set kubectl context to kind
	@kubectl config use-context ${KIND_CTX}

kind-bootstrap:
	# Ensure MY_IP is set
	@[ ! -z "${MY_IP}" ] || (echo "Set MY_IP variable to you host IP" && exit 1)

	# Ensure kubectl context
	@[ "$(shell kubectl config current-context)" == "${KIND_CTX}" ] || \
		(echo "Kubectl should be ${KIND_CTX}" && exit 1)

	# Setup NFS storage class
	@helm upgrade --install \
		--namespace kube-system \
		--set nfs.server=${NFS_SERVER_NAME} \
		--set nfs.path=/ \
		--set storageClass.name=nfs \
		--set storageClass.archiveOnDelete=false \
		--set storageClass.allowVolumeExpansion=true \
		--version 3.0.0 \
		nfs-client \
		nfs/nfs-subdir-external-provisioner

	# Apply CRDs
	@ls --color=none ../../helm/crds/* | xargs -I {} kubectl apply -f {}

	# Apply K8s services
	@cat operator-service.yaml | sed s%#IP#%${MY_IP}%g | kubectl apply -f -

kind-ingress:
	# Ensure kubectl context
	@[ "$(shell kubectl config current-context)" == "${KIND_CTX}" ] || \
		(echo "Kubectl should be ${KIND_CTX}" && exit 1)
	
	# Create nginx-ingress
	@kubectl apply -f nginx-ingress.yaml

	# Wait for ingress
	@sleep 30
	@kubectl wait --namespace ingress-nginx \
		--for=condition=ready pod \
		--selector=app.kubernetes.io/component=controller \
		--timeout=120s

kind-up: kind-setup kind-bootstrap kind-ingress ## Spin up development cluster

kind-down: ## Destroy development cluster
	# Delete cluster
	-@kind delete cluster > /dev/null

	# Delete NFS server
	-@docker stop ${NFS_SERVER_NAME} > /dev/null
	-@docker rm ${NFS_SERVER_NAME} > /dev/null

drill: ## Run drill
	@drill -s -b drill.yml

help: ## Show help
	@echo "Makefile help:"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-10s\033[0m %s\n", $$1, $$2}'