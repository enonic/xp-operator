IMAGE:=enonic/ec-operator
KIND_CTX:=kind-kind
NFS_SERVER_NAME:=nfs_server

.DEFAULT_GOAL:=help

docker-build: ## Docker build image
	@./mvnw clean package
	@docker build -f src/main/docker/Dockerfile -t operatortmp .

docker-run: docker-build ## Docker run image
	@docker run --rm -it -v ${HOME}/.kube:/opt/jboss/.kube -p 8081:8080 operatortmp:latest

docker-push: docker-build ## Docker push image
	@docker tag operatortmp ${IMAGE}:$(shell ./get-version.sh)
	@docker push ${IMAGE}:$(shell ./get-version.sh)

cluster-setup:
	# Ensure MY_IP is set
	@[ ! -z "${MY_IP}" ] || (echo "Set MY_IP variable to you host IP" && exit 1)

	# Create kind cluster
	@kind create cluster --image=kindest/node:v1.17.17 --config=src/test/kubernetes/kind-config.yaml

	# Start NFS server
	@(docker ps | grep ${NFS_SERVER_NAME}) || docker run -d \
		--name ${NFS_SERVER_NAME} \
		--network=kind \
		--privileged \
		-v $(shell pwd)/nfsshare:/nfsshare \
		-e SHARED_DIRECTORY=/nfsshare \
		itsthenetwork/nfs-server-alpine:latest
	
	# Set kubectl context to kind
	@kubectl config use-context ${KIND_CTX}

cluster-bootstrap:
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
	@ls --color=none src/test/kubernetes/crds/* | xargs -I {} kubectl apply -f {}

	# Apply K8s services
	@cat src/test/kubernetes/service.yaml | sed s%#IP#%${MY_IP}%g | kubectl apply -f -

cluster-ingress:
	# Ensure kubectl context
	@[ "$(shell kubectl config current-context)" == "${KIND_CTX}" ] || \
		(echo "Kubectl should be ${KIND_CTX}" && exit 1)
	
	# Create nginx-ingress
	@kubectl apply -f src/test/kubernetes/nginx-ingress.yaml

	# Wait for ingress
	@sleep 30
	@kubectl wait --namespace ingress-nginx \
		--for=condition=ready pod \
		--selector=app.kubernetes.io/component=controller \
		--timeout=120s

cluster: cluster-setup cluster-bootstrap cluster-ingress ## Development cluster setup everything

cluster-delete: ## Development cluster delete
	# Delete cluster
	-@kind delete cluster > /dev/null

	# Delete NFS server
	-@docker stop ${NFS_SERVER_NAME} > /dev/null
	-@docker rm ${NFS_SERVER_NAME} > /dev/null

namespace-create:
	@kubectl apply -f src/test/example-namespace.yaml

deployment-create: namespace-create
	@kubectl apply -f src/test/example-deployment.yaml

deployment-ingress:
	@kubectl apply -f src/test/example-ingress.yaml

deployment-delete:
	@kubectl delete -f src/test/example-deployment.yaml

deployment-supass:
	@kubectl -n mycloud-mysolution-myenv-myservice get secret su -o go-template="{{ .data.pass | base64decode }}"

deployment-drill:
	@drill -s -b src/test/drill.yml

help: ## Show help
	@echo "Helm chart ${CHART_NAME} makefile targets:"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-20s\033[0m %s\n", $$1, $$2}'