IMAGE_BASE:=enonic/xp-operator
TAG?=refs/heads/dev

DOCKER_IMAGE:=$(shell echo ${IMAGE_BASE}:${TAG} | sed 's/refs\/tags\///g' | sed 's/refs\/heads\///g')

#BUILD_PLATFORMS:=linux/amd64,linux/arm64,linux/riscv64,linux/ppc64le,linux/s390x,linux/386,linux/arm/v7,linux/arm/v6
BUILD_PLATFORMS:=linux/amd64,linux/arm64,linux/ppc64le,linux/s390x,linux/arm/v7

#BUILD_ARGS?=--push
#BUILD_CACHE_ARGS?=--cache-from type=local,src=/tmp/.buildx-cache --cache-to type=local,dest=/tmp/.buildx-cache-new,mode=max

BUILD_ARGS?=
BUILD_CACHE_ARGS?=

BUILDER_NAME:="qemubuilder"

.DEFAULT_GOAL:= help
.PHONY:="builder-prepare builder-create buildx build help"

builder-prepare: ## Prepare multi arch builds. This needs to be done every time you reboot your machine!
	@docker run --rm --privileged docker/binfmt:a7996909642ee92942dcd6cff44b9b95f08dad64

builder-create: ## Create a new builder. This only needs to be done once!
	@docker buildx create --name ${BUILDER_NAME}
	@docker buildx use ${BUILDER_NAME}
	@docker buildx inspect --bootstrap

buildx: ## Build multiarch docker image
	# Building multiarch docker image ...
	@docker buildx build ${BUILD_ARGS} ${BUILD_CACHE_ARGS} \
		--platform ${BUILD_PLATFORMS} \
		-f Dockerfile \
		-t ${DOCKER_IMAGE} \
		..
	# Building multiarch docker image done!

build: ## Build docker image
	# Building docker image ...
	@docker build \
		-f Dockerfile \
		-t ${DOCKER_IMAGE} \
		..
	# Building docker image done!

help: ## Show target descriptions.
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'