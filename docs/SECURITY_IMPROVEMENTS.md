# Security Improvements Summary

## Overview

This document provides a detailed comparison of the operator's permissions before and after implementing the security improvements.

## Before (Original Implementation)

### Single ClusterRole with Excessive Permissions

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: xp-operator
rules:
  - apiGroups: [ "" ]
    resources: [ "configmaps", "services", "serviceaccounts", "secrets", "namespaces", "events", "persistentvolumeclaims" ]
    verbs: [ "*" ]  # ❌ Wildcard on sensitive resources cluster-wide
  - apiGroups: [ "" ]
    resources: [ "pods" ]
    verbs: [ "watch", "list", "get" ]
  - apiGroups: [ "apps" ]
    resources: [ "statefulsets" ]
    verbs: [ "*" ]  # ❌ Full control over all statefulsets
  - apiGroups: [ "networking.k8s.io" ]
    resources: [ "ingresses" ]
    verbs: [ "*" ]  # ❌ Full control over all ingresses
  - apiGroups: [ "rbac.authorization.k8s.io" ]
    resources: [ "clusterrolebindings", "clusterroles", "roles", "rolebindings" ]
    verbs: [ "*" ]  # ❌ Full RBAC management cluster-wide
  - apiGroups: [ "enonic.cloud" ]
    resources: [ "xp7deployments", "xp7apps", "xp7configs" ]
    verbs: [ "*" ]
```

### Security Issues

1. **Excessive Scope**: Operator could access and modify resources in ANY namespace
2. **Secrets Exposure**: Full access to ALL secrets cluster-wide
3. **RBAC Escalation**: Could create/modify ClusterRoles and ClusterRoleBindings with arbitrary permissions
4. **No Boundaries**: No mechanism to restrict operator to only its managed namespaces
5. **Trust Requirement**: Third-party installations required near-cluster-admin permissions

## After (New Secure Implementation)

### 1. Minimal Operator ClusterRole (Cluster-Wide Scope)

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: xp-operator
rules:
  # Namespace management - operator creates and manages XP namespaces
  - apiGroups: [ "" ]
    resources: [ "namespaces" ]
    verbs: [ "get", "list", "watch", "create", "update", "patch", "delete" ]
  
  # RoleBinding management - operator binds the powerful role per-namespace
  - apiGroups: [ "rbac.authorization.k8s.io" ]
    resources: [ "rolebindings" ]
    verbs: [ "create", "update", "patch", "delete", "get", "list", "watch" ]
  
  # ✅ Bind verb on the powerful ClusterRole - allows delegation without escalation
  - apiGroups: [ "rbac.authorization.k8s.io" ]
    resources: [ "clusterroles" ]
    resourceNames: [ "xp-operator-namespace-role" ]  # ✅ Specific role only
    verbs: [ "bind" ]
  
  # CRD watchers - operator watches these resources cluster-wide
  - apiGroups: [ "enonic.cloud" ]
    resources: [ "xp7deployments", "xp7apps", "xp7configs" ]
    verbs: [ "get", "list", "watch", "update", "patch" ]
  
  - apiGroups: [ "enonic.cloud" ]
    resources: [ "xp7deployments/status", "xp7apps/status", "xp7configs/status" ]
    verbs: [ "get", "update", "patch" ]
  
  # Pod watching for operator functionality
  - apiGroups: [ "" ]
    resources: [ "pods" ]
    verbs: [ "watch", "list", "get" ]
```

### 2. Powerful Namespace-Scoped ClusterRole

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: xp-operator-namespace-role  # ✅ Bound via RoleBinding per-namespace
rules:
  - apiGroups: [ "" ]
    resources: [ "configmaps", "services", "serviceaccounts", "secrets", "events", "persistentvolumeclaims" ]
    verbs: [ "*" ]  # ✅ OK - only effective in bound namespaces
  
  - apiGroups: [ "apps" ]
    resources: [ "statefulsets" ]
    verbs: [ "*" ]  # ✅ OK - only effective in bound namespaces
  
  - apiGroups: [ "networking.k8s.io" ]
    resources: [ "ingresses" ]
    verbs: [ "*" ]  # ✅ OK - only effective in bound namespaces
  
  - apiGroups: [ "rbac.authorization.k8s.io" ]
    resources: [ "roles", "rolebindings" ]
    verbs: [ "*" ]  # ✅ Namespace-scoped RBAC only
  
  # ... (additional namespaced permissions)
```

### 3. ValidatingAdmissionPolicies (Security Enforcement)

#### VAP #1: Restrict RoleBinding Creation

```yaml
apiVersion: admissionregistration.k8s.io/v1
kind: ValidatingAdmissionPolicy
metadata:
  name: xp-operator-restrict-rolebinding
spec:
  failurePolicy: Fail
  matchConstraints:
    resourceRules:
      - apiGroups: [ "rbac.authorization.k8s.io" ]
        apiVersions: [ "v1" ]
        operations: [ "CREATE", "UPDATE" ]
        resources: [ "rolebindings" ]
  validations:
    - expression: >
        has(namespaceObject.metadata.labels) && 
        namespaceObject.metadata.labels.exists(k, k == 'operator.enonic.cloud/managed' && namespaceObject.metadata.labels[k] == 'true')
      message: "RoleBindings may only be created in namespaces labeled operator.enonic.cloud/managed=true"
```

#### VAP #2: Restrict Namespace Mutations

```yaml
apiVersion: admissionregistration.k8s.io/v1
kind: ValidatingAdmissionPolicy
metadata:
  name: xp-operator-restrict-namespace-mutations
spec:
  failurePolicy: Fail
  matchConstraints:
    resourceRules:
      - apiGroups: [ "" ]
        apiVersions: [ "v1" ]
        operations: [ "UPDATE", "PATCH", "DELETE" ]
        resources: [ "namespaces" ]
  matchConditions:
    - name: is-operator-sa
      expression: >
        request.userInfo.username == 'system:serviceaccount:kube-system:xp-operator'
  validations:
    - expression: >
        has(oldObject.metadata.labels) && 
        oldObject.metadata.labels.exists(k, k == 'operator.enonic.cloud/managed' && oldObject.metadata.labels[k] == 'true')
      message: "The operator may only update or delete namespaces labeled operator.enonic.cloud/managed=true"
```

### 4. Operator Lifecycle Changes

**New behavior when creating XP7Deployment:**

1. Namespace gets labeled: `operator.enonic.cloud/managed: "true"`
2. RoleBinding created in that namespace:
   ```yaml
   kind: RoleBinding
   metadata:
     name: xp-operator-namespace-access
     namespace: <xp-deployment-namespace>
   roleRef:
     kind: ClusterRole
     name: xp-operator-namespace-role
   subjects:
     - kind: ServiceAccount
       name: xp-operator
       namespace: kube-system
   ```
3. Operator gains full access ONLY in that specific namespace

## Permission Comparison

| Resource | Before | After (Cluster-Wide) | After (Per-Namespace) |
|----------|--------|---------------------|----------------------|
| **secrets** | `*` cluster-wide ❌ | None | `*` in managed NS ✅ |
| **configmaps** | `*` cluster-wide ❌ | None | `*` in managed NS ✅ |
| **services** | `*` cluster-wide ❌ | None | `*` in managed NS ✅ |
| **namespaces** | `*` cluster-wide ❌ | `create, update, patch, delete` with VAP restrictions ✅ | N/A |
| **statefulsets** | `*` cluster-wide ❌ | None | `*` in managed NS ✅ |
| **ingresses** | `*` cluster-wide ❌ | None | `*` in managed NS ✅ |
| **clusterroles** | `*` cluster-wide ❌ | `bind` on specific role only ✅ | `create, update, delete` in managed NS ✅ |
| **clusterrolebindings** | `*` cluster-wide ❌ | None | `create, update, delete` in managed NS ✅ |
| **rolebindings** | `*` cluster-wide ❌ | `create, update, delete` with VAP restrictions ✅ | `*` in managed NS ✅ |
| **XP CRDs** | `*` cluster-wide ✅ | `get, list, watch, update, patch` ✅ | `*` in managed NS ✅ |

## Security Benefits

### 1. Least Privilege
- Operator no longer has wildcard access to sensitive resources cluster-wide
- Can only access resources in namespaces it explicitly manages

### 2. Namespace Isolation
- Cannot access secrets, configmaps, or other sensitive data in arbitrary namespaces
- Each XP deployment's namespace is isolated from others

### 3. Prevention of Privilege Escalation
- Cannot create arbitrary ClusterRoles or ClusterRoleBindings
- `bind` verb on specific ClusterRole only - safe delegation pattern
- VAPs prevent unauthorized RoleBinding creation

### 4. Auditability
- Managed namespaces are clearly labeled
- All operator access is scoped to labeled namespaces
- VAP denials are logged for security monitoring

### 5. Third-Party Trust
- Third parties no longer need to grant near-cluster-admin permissions
- Clear security boundaries make risk assessment easier
- Reduced blast radius of compromised or misbehaving operator

## Migration Path

### For Existing Installations

1. **Upgrade operator**: Helm upgrade will apply new ClusterRoles and VAPs
2. **Existing deployments**: OperatorXp7DeploymentNamespace will label namespaces and create RoleBindings on next reconciliation
3. **No downtime**: Operator continues to function during migration

### For New Installations

1. Install operator with Helm (Kubernetes 1.30+ required)
2. Create XP7Deployment resources
3. Operator automatically manages namespace security

## Kubernetes Version Requirements

| Feature | Minimum Version | Status |
|---------|----------------|--------|
| ValidatingAdmissionPolicy | 1.30 | GA |
| `bind` verb on ClusterRoles | 1.18 | Stable |
| RoleBinding with ClusterRole | 1.8 | Stable |

## References

- [Kubernetes RBAC Authorization](https://kubernetes.io/docs/reference/access-authn-authz/rbac/)
- [ValidatingAdmissionPolicy](https://kubernetes.io/docs/reference/access-authn-authz/validating-admission-policy/)
- [Kubernetes Security Best Practices](https://kubernetes.io/docs/concepts/security/rbac-good-practices/)
