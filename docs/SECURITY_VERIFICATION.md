# Security Model Verification Guide

This guide explains how to verify that the operator's security model is working correctly.

## Prerequisites

- Kubernetes 1.30+ cluster (for ValidatingAdmissionPolicy support)
- kubectl configured to access the cluster
- Helm 3+ installed

## Installation

Install the operator with the new security model:

```bash
helm upgrade --install \
  --namespace kube-system \
  xp-operator \
  ./helm/build/libs/xp-operator-<version>.tgz
```

## Verification Steps

### 1. Verify ClusterRoles are created

Check that both ClusterRoles exist:

```bash
# Minimal operator ClusterRole
kubectl get clusterrole xp-operator -o yaml

# Powerful namespace-scoped ClusterRole
kubectl get clusterrole xp-operator-namespace-role -o yaml
```

Expected: The `xp-operator` ClusterRole should only have cluster-scoped permissions (namespaces, rolebindings, bind on clusterroles, CRD watching). The `xp-operator-namespace-role` should have wildcard permissions on namespaced resources.

### 2. Verify ValidatingAdmissionPolicies are created

```bash
kubectl get validatingadmissionpolicy
```

Expected output:
```
NAME                                        AGE
xp-operator-restrict-namespace-mutations    1m
xp-operator-restrict-rolebinding            1m
```

### 3. Create a test namespace and XP7Deployment

```bash
# Create a namespace
kubectl create namespace test-xp-deployment

# Apply an XP7Deployment
kubectl apply -f - <<EOF
apiVersion: enonic.cloud/v1
kind: Xp7Deployment
metadata:
  name: test-deployment
  namespace: test-xp-deployment
spec:
  enabled: true
  xpVersion: "7.13.0"
  nodeGroups:
    - name: main
      replicas: 1
      master: true
      data: true
      resources:
        cpu: 1
        memory: 1Gi
        disk: 1Gi
EOF
```

### 4. Verify namespace is labeled as managed

```bash
kubectl get namespace test-xp-deployment -o jsonpath='{.metadata.labels}'
```

Expected: Should include `"operator.enonic.cloud/managed":"true"`

### 5. Verify RoleBinding is created

```bash
kubectl get rolebinding -n test-xp-deployment xp-operator-namespace-access -o yaml
```

Expected: RoleBinding should bind the `xp-operator` ServiceAccount to the `xp-operator-namespace-role` ClusterRole.

### 6. Test negative case: Try to create RoleBinding in unmanaged namespace

Create an unmanaged namespace:

```bash
kubectl create namespace unmanaged-test
```

Try to create a RoleBinding manually (as cluster admin):

```bash
kubectl apply -f - <<EOF
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: test-rolebinding
  namespace: unmanaged-test
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: xp-operator-namespace-role
subjects:
  - kind: ServiceAccount
    name: xp-operator
    namespace: kube-system
EOF
```

Expected: The ValidatingAdmissionPolicy should **DENY** this operation with message: "RoleBindings may only be created in namespaces labeled operator.enonic.cloud/managed=true"

### 7. Test negative case: Try to modify unmanaged namespace as operator SA

This requires impersonating the operator ServiceAccount:

```bash
kubectl auth can-i update namespace unmanaged-test \
  --as=system:serviceaccount:kube-system:xp-operator
```

Expected: Should return "yes" (the operator has the permission)

But actually trying to update it should be blocked:

```bash
kubectl label namespace unmanaged-test test=value \
  --as=system:serviceaccount:kube-system:xp-operator
```

Expected: The ValidatingAdmissionPolicy should **DENY** this with message: "The operator may only update or delete namespaces labeled operator.enonic.cloud/managed=true"

### 8. Test positive case: Operator can modify managed namespace

```bash
kubectl label namespace test-xp-deployment test=value \
  --as=system:serviceaccount:kube-system:xp-operator
```

Expected: Should **SUCCEED** because the namespace is labeled as managed.

## Cleanup

```bash
kubectl delete namespace test-xp-deployment
kubectl delete namespace unmanaged-test
```

## Security Model Summary

The security model ensures:

1. **Minimal cluster-wide permissions**: The operator only has permissions needed for cluster-wide operations (namespace management, RoleBinding creation, CRD watching).

2. **Namespace isolation**: The operator only gains full access to namespaces it explicitly manages (labeled with `operator.enonic.cloud/managed=true`).

3. **VAP enforcement**: ValidatingAdmissionPolicies prevent:
   - Creating RoleBindings in unmanaged namespaces
   - Modifying or deleting unmanaged namespaces
   - Privilege escalation attempts

4. **Safe delegation**: The `bind` verb on the specific `namespace-role` ClusterRole allows the operator to delegate permissions without holding them directly, following Kubernetes best practices.

## Troubleshooting

### VAPs not working

If ValidatingAdmissionPolicies are not enforcing:
- Verify your Kubernetes version is 1.30+: `kubectl version`
- Check if the ValidatingAdmissionPolicy feature is enabled: `kubectl api-resources | grep validatingadmissionpolicy`
- Check VAP status: `kubectl get validatingadmissionpolicy -o yaml`

### Operator can't create resources in managed namespace

If the operator fails to create resources in a managed namespace:
- Verify the RoleBinding exists: `kubectl get rolebinding -n <namespace> xp-operator-namespace-access`
- Check operator logs: `kubectl logs -n kube-system deployment/xp-operator`
- Verify the namespace has the managed label: `kubectl get namespace <namespace> --show-labels`
