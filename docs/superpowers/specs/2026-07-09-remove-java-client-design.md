# Remove Java client (issue #799)

**Status:** Design approved, pending spec review
**Issue:** [enonic/xp-operator#799](https://github.com/enonic/xp-operator/issues/799) — *"Remove Java client / Use pure K8S API instead of wrapping java client"*
**Milestone:** 2.0.0 (breaking changes allowed)

## Problem

The repository ships a separate, published Gradle module `java-client`
(`com.enonic.kubernetes:client`) that wraps the fabric8 Kubernetes client. On
inspection it is really four distinct layers with very different value:

| Layer | Content | Used by operator? |
|-------|---------|-------------------|
| 1. CRD models | `Xp7Deployment/App/Config` (+ spec/status), generated from JSON schema | Yes, heavily (~60 imports) |
| 2. fabric8 wrapper | `EnonicKubernetesClient`, `CustomClient`, `CrdApi`, `Crd` | Only in `ClientsProducer` |
| 3. XP mgmt HTTP client | `Xp7Client`, `Xp7MgmtApi*`, `RawClient`, `HttpRequestRetrier` (+ DTOs) | No — operator has its own `apis/xp/XpClient` |
| 4. operator info HTTP client | `OperatorApi/Impl` (+ `OperatorVersion`) | No — operator has its own `operator/api/info` |

Key findings from investigation:

- **The wrapper (layer 2) is pure redundancy.** `CrdApiImpl.xp7apps()` is literally
  `k8sClient.resources(Xp7App.class, Xp7App.Xp7AppList.class)`. Modern fabric8
  supports typed custom resources directly via `@Group/@Version/@Kind` annotations.
- **The mgmt HTTP client (layer 3) is not a client to XP at all.** Its base URL is
  `{masterUrl}/apis/operator.enonic.cloud/v1/...` — it is a thin typed HTTP wrapper
  over the operator's *own* REST API (`Xp7ManagementApi`), reached through the k8s
  API server. The operator already re-exposes snapshots/idproviders/projects/webapps
  lists there. It is only ever called read-only (`list()`).
- **Schema is duplicated.** CRD schema exists both as `helm/src/main/crds/*.yaml`
  (installed to the cluster — the real source of truth) and as hand-maintained
  `java-client/.../schema/v1/xp7*/*.json` for code generation.
- **The only external consumer is `cloud-api`**, which pins the *old* `client:1.1.5`
  (operator is at 1.2.x) — so the coupling is already loose. `cloud-console` does not
  depend on the client. `cloud-api` uses layers 1, 2 and 3.

## Goals

- Remove the `java-client` module entirely from this repository.
- Operator talks to CRDs through the pure fabric8 API (`client.resources(...)`), no wrapper.
- Single source of truth per contract:
  - **CRD contract** → the CRD YAML shipped in the Helm chart.
  - **mgmt REST contract** → an OpenAPI document served by the operator.
- No published Java artifact from this repository.

## Non-goals

- The `cloud-api` migration itself (tracked separately in the `cloud-api` repo). This
  spec only fixes the contracts `cloud-api` must consume.
- Redesigning the mgmt endpoints' behaviour or paths.
- Fixing the pre-existing Helm `crds/` upgrade limitation (CRDs installed only on first install).

## Design

### CRD models — generate from the chart's CRD YAML, inside `java-operator`

- Source of truth: `helm/src/main/crds/{apps,configs,deployments}.yaml`.
- Generate Java types with the fabric8 **`java-generator`** Gradle plugin
  (`io.fabric8:java-generator-gradle-plugin`) as part of the `java-operator` build,
  reading the helm CRD resources (the build already wires
  `dependsOn(":helm:processHelmResources")`).
- This replaces **both** the hand-written `Xp7App/Config/Deployment` wrapper classes
  **and** the `jsonschema2pojo` step, and lets us delete the duplicate
  `schema/v1/xp7*/*.json` files.
- The operator accesses CRDs directly:
  `clients.k8s().resources(Xp7App.class, Xp7App.Xp7AppList.class)`.

**Naming reconciliation (primary risk).** The operator imports specific
`jsonschema2pojo`-produced names driven by `"javaName"` overrides:

```
Xp7App, Xp7AppStatus, Xp7AppStatusFields, Xp7AppStatusFieldsAppInfo,
Xp7Config, Xp7ConfigStatus,
Xp7Deployment, Xp7DeploymentStatus, Xp7DeploymentStatusFields,
Xp7DeploymentSpecNodeGroup, Xp7DeploymentSpecNodeGroupEnvVar, Xp7DeploymentSpecNodeGroupSidecar
```

`java-generator` derives class names from the CRD schema and will likely produce
different nested-type names and a group-derived package. Mitigation, in order of
preference:
1. Configure the generator (package override / naming) to match existing FQCNs where practical.
2. Where names cannot be preserved, do a mechanical find/replace across `java-operator`
   for the affected ~13 types.

Either way this must compile against the existing operator source before merge.

### mgmt / DTO contract — serve OpenAPI from the operator

- The mgmt DTOs (`Xp7MgmtIdProvider`, `Xp7MgmtProject` + nested `Xp7MgmtBranch`/`Xp7MgmtSite`,
  `Xp7MgmtSnapshotsList` + nested snapshot, `Xp7MgmtWebapp`, `OperatorVersion`) become
  **plain hand-written POJOs in `java-operator`** — they are tiny and flat. They are the
  *source* of the mgmt contract (return types of `Xp7ManagementApi` / `OperatorApi` and
  used by `apis/xp/XpClient`). The `jsonschema2pojo` schemas are deleted.
- Add `quarkus-smallrye-openapi` so the operator serves an OpenAPI document (`/q/openapi`)
  auto-generated from the existing JAX-RS resources and their return types.
- `cloud-api` generates its mgmt client/DTOs from this OpenAPI document (openapi-generator),
  instead of depending on a shared jar.
- `Xp7MgmtRoutesList` is dead (not referenced by any interface) — drop it.

### Remove the wrapper and HTTP clients

Delete: `EnonicKubernetesClient`, `DefaultEnonicKubernetesClient`, `CustomClient`,
`Clients`/`ClientsImpl` (client v1), `CrdApi`/`CrdApiImpl`, `Crd`, `CrdMappingProvider`,
`RawClient`, `HttpRequestRetrier`, `Xp7Client*`, `Xp7MgmtApi*`, the client's
`OperatorApi/OperatorApiImpl`, and their tests.

Operator wiring changes:
- `kubernetes/ClientsProducer` builds the `KubernetesClient` directly and supplies the
  three `MixedOperation`s via `k8s.resources(Xp7X.class, Xp7X.Xp7XList.class)`.
- `kubernetes/Clients` drops the `enonic()` method.
- `CrdMappingProvider` is not used by the operator (CRDs are registered via the
  `META-INF/services/...KubernetesResource` SPI + annotations, and fabric8 (de)serializes
  automatically). Ensure the SPI registration for the generated types still exists in
  `java-operator`.

### Remove the module

- Delete the `java-client` directory.
- `settings.gradle`: `include 'java-operator', 'helm'`.
- `java-operator/build.gradle`: drop `implementation project(":java-client")`; add the
  `java-generator` plugin and `quarkus-smallrye-openapi`.
- Remove the `README.md` link to the Java client.

### cloud-api impact (separate task, its own repo)

Recorded here as the contract `cloud-api` must migrate to:
- Generate CRD types with `java-generator` from the operator's published chart CRDs
  (`repo.enonic.com/helm/xp-operator-<version>.tgz` → `crds/*.yaml`).
- Generate the mgmt client from the operator's OpenAPI document.
- Access CRDs directly through fabric8 (`client.resources(Xp7App.class, ...)`), replacing
  the `EnonicKubernetesClient` wrapper.
- Its own CRD `Domain` continues to work — it already uses `client.resources(Domain.class, ...)`.

## Risks & verification

- **CRD class-name reconciliation** — see above; gate on a clean compile of `java-operator`.
- **`Crd` custom `equals/hashCode/toString` removed** — the generated types use fabric8's
  own equality. Verify the operator does not rely on value-equality of CRD objects
  (informer diffing, sets/maps keyed by CRD instances) before merge.
- **fabric8 CRD registration** — confirm the generated `CustomResource` types are
  discovered/serialised correctly at runtime (SPI file present, annotations intact).
- **OpenAPI completeness** — confirm `/q/openapi` includes all four mgmt list endpoints
  and the info/version endpoint with correct DTO schemas.

## Testing

- Unit tests: `JAVA_HOME=~/.sdkman/candidates/java/17.0.8.1-tem ./gradlew :java-operator:test`.
- Full integration: `make test` (build image + kind + deploy) to confirm CRUD on the CRDs
  and the mgmt endpoints still function end-to-end.
- Per project convention: for chart/template changes prefer 1–2 smoke assertions over
  comprehensive suites.

## Implementation notes (as built)

Reality found during implementation; supersedes the assumptions above where they differ.

- **`java-generator` is NOT a drop-in.** It generates a structurally different model, so
  the operator was adapted to the generated types (~34 files), not vice versa:
  - Package set to `com.enonic.kubernetes.crd.v1` via `packageOverrides = ['cloud.enonic.v1': 'com.enonic.kubernetes.crd.v1']`.
  - Requires `implementation 'io.fabric8:generator-annotations'`, and explicit
    `fabric8.kubernetes.clientapi` / `kubernetes.client` deps (were transitive via `java-client`).
  - Nested types are repackaged with short names (`Xp7DeploymentSpecNodeGroup` →
    `xp7deploymentspec.NodeGroups`, `…EnvVar` → `nodegroups.Env`, `Xp7*StatusFields` →
    `…status.Fields`, etc.). `state` remains a nested enum `Xp7*Status.State`.
  - No fluent `withX(...)`, no all-args constructors, no `Xp7XList` — migrated to `setX`,
    no-arg construction, and `KubernetesResourceList<Xp7X>` / single-arg `resources(Class)`.
  - **No value `equals`/`hashCode`.** Status/spec change-detection sites now compare
    `io.fabric8.kubernetes.client.utils.Serialization.asJson(...)` (deterministic via the
    generated `@JsonPropertyOrder`): `OperatorXp7DeploymentStatus`, `HandlerStatus`, `MutationApi`.
  - Added `default: []` to the array fields in `helm/src/main/crds/deployments.yaml` — the
    generator turns this into empty-list field initializers, restoring the old model's
    non-null collection behaviour (fixed 15+ NPE/validation test failures). Side effect:
    the shipped CRD now defaults those arrays to `[]`.
- **CRD registration:** added `java-operator/src/main/resources/META-INF/services/io.fabric8.kubernetes.api.model.KubernetesResource`
  listing the three generated `CustomResource` FQCNs.
- **DTOs** relocated as hand-written POJOs at their original FQCNs (packages still named
  `...client.v1.api...`; acceptable, can be renamed later).
- **OpenAPI implemented.** Adding `quarkus-smallrye-openapi` first broke the build/runtime:
  `smallrye-open-api-core` transitively drags `microprofile-config-api` 3.0, newer than the
  2.0.1 Quarkus 2.16.6 uses. config-api 3.0 breaks SmallRye Config's CDI integration — it
  surfaced as an ArC `AmbiguousResolutionException` for `Optional<ExemplarSampler>` at build
  and, at runtime, as `@ConfigProperty` fields silently injecting `null` (operator crashed on
  startup). Fixed cleanly by two changes: switched the platform from the deprecated
  `quarkus-universe-bom` to the core `quarkus-bom`, and pinned
  `microprofile-config-api:2.0.1` via `resolutionStrategy.force`. No bean-level workaround
  needed. The mgmt REST OpenAPI is generated at build
  (`quarkus.smallrye-openapi.store-schema-directory=build/openapi`) and committed as the
  cloud-api contract at `docs/openapi/operator-mgmt.yaml` (four mgmt list endpoints,
  `operator/version`, admission webhooks, all DTO schemas). Source of truth is the JAX-RS
  code; regenerate on mgmt API changes (a CI drift-check is a suggested follow-up).
- **JDK pin.** Gradle 8.13 does not run on JDK 25; added `.sdkmanrc` (`java=17.0.8.1-tem`) and
  the Makefile now derives `JAVA_HOME` from it so `make` is independent of the shell JDK. The
  `java-operator` `java{}` block uses a toolchain (17) for compile/test.
- **Verified:** `./gradlew clean build` green (`:java-operator:test` 57 pass + `quarkusBuild` +
  OpenAPI gen + helm package). Kind e2e green: image builds, operator deploys and boots clean
  with the `smallrye-openapi` feature, registers the generated CRDs, and reconciles a real
  `Xp7Deployment` to `status.state=RUNNING` (StatefulSet 1/1).

## Ready for cloud-api

- **CRD models:** generate from the operator's published chart CRDs (`crds/*.yaml`).
- **mgmt client:** generate from `docs/openapi/operator-mgmt.yaml` (openapi-generator).
- **CRUD:** fabric8 `client.resources(Xp7X.class)` directly; drop the `EnonicKubernetesClient` wrapper.
