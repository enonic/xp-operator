# Build & Test Guide

## Java requirements

Requires **Temurin JDK 17** (not GraalVM — causes `Type T not present` Gradle errors).

```bash
sdk use java 17.0.8.1-tem
# or set explicitly:
export JAVA_HOME=~/.sdkman/candidates/java/17.0.8.1-tem
```

## Unit tests (Java only, fast)

```bash
JAVA_HOME=~/.sdkman/candidates/java/17.0.8.1-tem ./gradlew :java-operator:test
```

Test fixtures live in `java-operator/src/test/resources/com/enonic/kubernetes/operator/helm/v1/xp7deployment/`:
- `*.yaml` — input CRDs
- `*_values.yaml` — expected Helm values output
- `*_result.yaml` — expected rendered Kubernetes manifests

## Full build (Docker image)

```bash
JAVA_HOME=~/.sdkman/candidates/java/17.0.8.1-tem make build-java
make build-docker
```

Or all at once: `make build` (runs build-java then build-docker).

## Integration test with kind cluster

Requires: `kind`, `kubectl`, `helm`, `docker`.

```bash
# Build + spin up kind cluster (k8s v1.31) + deploy operator
make test

# Tear down everything
make clean
```

`make test` does: `make build` → `kind create cluster` → `kind load docker-image` → `helm upgrade --install`.

After `make test` succeeds, verify the operator works by applying example resources:

```bash
# Minimal single-node XP deployment
kubectl apply -f kubernetes/example-simple.yaml

# Clustered deployment
kubectl apply -f kubernetes/example-cluster.yaml

# Clustered with shared storage
kubectl apply -f kubernetes/example-cluster-shared.yaml

# Watch rollout
kubectl get pods -n <namespace> -w

# Check operator logs
kubectl logs -n kube-system deploy/xp-operator -f
```

Expected result for `example-simple.yaml`:
- Namespace is created with label `enonic.managed=true`
- RoleBinding `xp-operator-namespace-access` is created in the namespace
- StatefulSet reaches `1/1 Ready`
- Pod has 2/2 containers running

To clean up a specific example:
```bash
kubectl delete -f kubernetes/example-simple.yaml
```

## kind cluster only (without full rebuild)

```bash
make -C kubernetes/kind kind-up   # create cluster
make -C kubernetes/kind kind-down # destroy cluster
```

## Helm chart only (no Java)

```bash
JAVA_HOME=~/.sdkman/candidates/java/17.0.8.1-tem ./gradlew :helm:build
helm template xp-operator ./helm/build/libs/xp-operator-*.tgz --debug
```
