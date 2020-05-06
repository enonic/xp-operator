LOCAL_OPERATOR_PORT:=8080
IMAGE:=gbbirkisson/ec-operator

docker-build:
	./mvnw clean package
	docker build -f src/main/docker/Dockerfile.jvm -t operatortmp .

docker-push: docker-build
	docker tag operatortmp ${IMAGE}:$(shell ./get-version.sh)
	docker push ${IMAGE}:$(shell ./get-version.sh)

mvn-dependencies:
	mvn versions:display-plugin-updates

crds-create:
	@ls --color=none src/main/kubernetes/operator/crd/ec-operator.crd.xp7.* | xargs -I {} kubectl apply -f {}

deployment-create:
	kubectl apply -f src/test/example-deployment.yaml

deployment-delete:
	kubectl delete -f src/test/example-deployment.yaml

deployment-supass:
	kubectl -n mycloud-myproject-qaxp get secrets su-pass -o jsonpath={.data.suPass} | base64 -d

post-admission-success:
	cat src/test/admission-success.json | http -v -j POST :${LOCAL_OPERATOR_PORT}/admission/validate

post-admission-fail:
	cat src/test/admission-fail.json | http -v -j POST :${LOCAL_OPERATOR_PORT}/admission/validate