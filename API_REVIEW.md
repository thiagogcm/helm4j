# helm4j API Review

Scope: user-facing API surface under `helm4j-api/src/main/java/dev/nthings/helm4j/` (everything not under `internal/`), measured against the stated goal of a *modern, idiomatic, high-level Helm API for Java developers*. Review only — no source files other than this one were modified.

---

## Verdict

**The skeleton is right; the ergonomics are not yet there.** helm4j gets the big structural decisions correct: a single discoverable entrypoint (`Helm.client()`), namespace partitioning (`repo()/chart()/release()`), immutable record models, sealed result hierarchies, and a clean `ServiceLoader`-based seam to the native runtime. A senior engineer picking this up will recognize the shape immediately and will appreciate that the gateway layer is genuinely hidden. As an *architecture*, it passes the bar.

**As an API to actually use, it has consistent, repeated friction that undermines the "idiomatic, high-level" claim.** The dominant pain is the `Consumer<Builder>` "spec" pattern applied uniformly to every operation: it is unusual in modern Java SDKs (AWS SDK v2, the Kubernetes client, Testcontainers all hand you a builder or a fluent call — they do not ask you to accept a callback), it produces a stutter (`spec -> spec.x().y()`), it makes partial application and request reuse awkward, and it has already forced the `show(mode, ref, spec -> {})` empty-lambda wart into the README's own quick-start. On top of that, the result model is split-brained: some operations return sealed `*Outcome`/`*Result` types you must `instanceof`/`switch` on, while sibling operations on the *same* namespace (`status`, `history`, `getValues`, ...) just return a plain record or throw `HelmException`. A caller cannot predict, without reading source, whether a given call communicates failure by return value or by exception.

**Secondary problems compound it:** `Helm.install(...).run()` duplicates `helm.release().install(...)` with a *different* builder type and a narrower option set, so there are two ways to install with different capabilities; `ReleaseOutcome` over-unifies five semantically unrelated results into one sealed type, so every `switch` must handle `UninstallSuccess` even when calling `install`; nullability is undocumented and `@Nullable`-free despite pervasive `null` use; `close()` is a no-op with no Javadoc saying so, training users to wonder what the lifecycle is; and there is at least one outright doc bug (`RollbackSuccess` has no `release()` method, but the README calls `r.release().version()`). None of this is fatal, and none of it requires touching the native runtime — but it needs a deliberate pass before this can be called idiomatic.

---

## Findings

Severity legend: **High** = actively misleads or obstructs a real user; **Medium** = friction/inconsistency a user will notice and resent; **Low** = polish.

### F1 — `Consumer<Builder>` spec pattern is the default everywhere — High
`ReleaseClient.java:16`, `ChartClient.java:17`, `RepoClient.java:16`, and the shared `NamespaceClient.buildAndInvoke` at `internal/api/NamespaceClient.java:17`.
Every operation is exposed as a pair `op(Request)` / `op(Consumer<Request.Builder> spec)`. The `Consumer` form is the one the README and tests use everywhere. It is not idiomatic modern Java — no major Java SDK asks the caller to *accept a callback* to configure a call. It reads `spec -> spec.releaseName("nginx").chart(ref)` (the parameter name is pure noise), it cannot be partially built and reused, and it cannot be composed. It also directly causes F2.
Why it matters: this is the single most-used shape in the whole library; if it feels foreign, the whole library feels foreign.

### F2 — `show(mode, ref, spec -> {})` empty-lambda wart — High
`ChartClient.java:35-44`; visible in `README.md:43` and `HelmClientApiTest.java:81`.
Because `show` has no `Consumer`-free overload for "no extra options," the README's own quick start contains `helm.chart().show(ShowMode.CHART, ChartRef.repo("bitnami/nginx"), spec -> {})`. An empty lambda passed to satisfy a signature is a textbook smell and it is in the *first page of docs*.

### F3 — Result vs. exception is inconsistent and unpredictable — High
`ReleaseClient.java`: `install/upgrade/uninstall/rollback` return the sealed `ReleaseOutcome` (which includes `ReleaseFailure`), but `status` returns a bare `StatusResult`, `history/list` return `ListResult`, and `getValues/getManifest/...` return bare records. `RepoClient.add` returns sealed `RepoAddResult` (with `RepoAddFailure`), but `RepoClient.update/list/remove/registryLogin/registryLogout` return bare types. `HelmException` (`errors/HelmException.java`) is the *other* failure channel.
Why it matters: a user cannot tell, without reading source, whether `release().status(...)` on a missing release returns something or throws — and whether `release().install(...)` failure is a `ReleaseFailure` value or a `HelmException`. Two failure channels, applied inconsistently, is the worst of both worlds. `ReleaseFailure`/`RepoAddFailure` even carry the *exact same* `(message, stage, operation)` triple as `HelmException` — the duplication is right there.

### F4 — `ReleaseOutcome` over-unifies unrelated operations — High
`release/ReleaseOutcome.java:4-5`: `permits ReleaseSuccess, ReleasePending, ReleaseFailure, UninstallSuccess, RollbackSuccess`.
`install` and `uninstall` and `rollback` all return the *same* sealed type. Per the README's own guidance (`README.md:69-86`), an exhaustive `switch` on the result of `install(...)` must still handle `UninstallSuccess` and `RollbackSuccess` — outcomes `install` can never produce. The compiler-enforced exhaustiveness that sealed types buy you is turned into a liability. `RollbackSuccess` and `UninstallSuccess` also have a different shape (`releaseName`/`revision`, no `ReleaseInfo`) from `ReleaseSuccess`, confirming they are not really the same domain.

### F5 — `Helm.install(...).run()` duplicates `helm.release().install(...)` with divergent capability — High
`Helm.java:26-110` (`InstallBuilder`) and `Helm.java:113-180` (`UpgradeBuilder`).
There are two ways to install: `Helm.install(chart).releaseName(...)...run()` and `helm.release().install(spec -> ...)`. They use *different builder types* (`Helm.InstallBuilder` vs `InstallRequest.Builder`) exposing *different* option subsets — `Helm.InstallBuilder` omits `waitForJobs`, `description`, `rollbackOnFailure`, `skipCrds`, `disableHooks`, `replace`, `generateName`, `subNotes`, `enableDns`, `takeOwnership`, `dependencyUpdate`, etc. A user who starts with `Helm.install(...)` and later needs `disableHooks` hits a wall and must rewrite against the other API. Two entrypoints, one a strict subset, is a maintenance and discoverability trap. `Helm.version()` (`Helm.java:36-40`) silently opens *and closes* a client per call — fine for a no-op `close()`, a latent bug the moment `close()` does anything.

### F6 — No `@Nullable`/`@NonNull` contract; pervasive nulls are silent — Medium
Every request record normalizes blanks to `null` (`InstallRequest.java:41-49`, `ChartSource.java:24-32`, `GetRequest.java:8-11`, etc.) and `ReleaseInfo` fields like `description`, `notes`, `firstDeployed` are nullable in practice. Nothing is annotated. `ReleaseFailure.stage/operation` are nullable (`ReleaseFailure.java:6` only null-checks `message`). A user destructuring `release.notes()` or `failure.stage()` gets an unannounced NPE risk and no IDE warning.

### F7 — `close()` is an undocumented no-op — Medium
`HelmClient.java:68-71`. The body comment explains it, but there is *no Javadoc* on the method, and the class is `AutoCloseable`. Users will wrap every call in `try (var helm = Helm.client())` (as the README does) believing it matters. Either it should be documented as currently a no-op with a forward-looking contract, or the lifecycle should be made real (see F5 — `Helm.version()` already assumes per-call clients are cheap).

### F8 — Thread-safety is unspecified — Medium
`HelmClient`, the three namespace clients, and the records are all effectively immutable/stateless, so the type is almost certainly safe to share across threads — but nothing says so. A server-side user (the stated use case) needs to know whether to pool clients or share one. The Kubernetes client and AWS SDK v2 both document this explicitly.

### F9 — `ChartRef` / `ChartSource` overlap and ceremony — Medium
`InstallRequest` carries both a `ChartRef chart` and a `ChartSource source`, and `ChartSource` holds `version`, `repositoryUrl`, `username`, `password`, TLS, etc. `Helm.InstallBuilder.version(...)` (`Helm.java:55-58`) routes into `source(s -> s.version(...))`. So "the chart and its version" is split across two objects, and `repositoryUrl` lives in `ChartSource` even though `RepoChartRef` is `repo/chart`. The merge logic (`ChartSource.merge`, `InstallRequest.Builder.build:217`) is non-obvious: builder-set `source(...)` is the *base*, `source(consumer)` mutations are *overrides*. A user setting both will be surprised.

### F10 — README documents a method that does not exist — Medium
`README.md:84`: `case RollbackSuccess r -> "rolled back to revision " + r.release().version()`. `RollbackSuccess` (`release/RollbackSuccess.java:4`) is `record RollbackSuccess(String releaseName, int revision)` — there is no `release()`, and even if there were, `ReleaseInfo` exposes `revision()`, not `version()`. The headline code sample does not compile.

### F11 — Naming inconsistencies — Low
`dryRunMode` on the request builders (`InstallRequest.Builder.dryRunMode`) vs `dryRun` on `Helm.InstallBuilder.dryRun` (`Helm.java:70`) and on `UninstallRequest` (`UninstallRequest.java:11` — a `boolean`, not the `DryRunMode` enum). `packageChart` (verb-noun) sits next to `pull`, `push`, `template`, `lint` (bare verbs). `searchRepo`/`searchHub` vs `repo()`/`chart()` namespace names. `forceReplace` vs Helm's own `--force`. `ChartClient.show(mode, chartReference, ...)` parameter is `chartReference` while the type and factory are `ChartRef`/`chart`.

### F12 — `ListResult<T>` is a thin, non-idiomatic wrapper — Low
`model/ListResult.java`. It exposes only `items()`, `size()`, `first()`. It is not `Iterable`, not streamable directly, has no `isEmpty()`. Users will write `result.items().stream()` constantly. Either make it `Iterable<T>` + add `stream()`/`isEmpty()`, or just return `List<T>` and drop the type.

### F13 — `boolean` soup in request records — Low
`InstallRequest` has 16 boolean components; `UpgradeRequest` has 14. Positional record construction is unreadable (`InstallRequest.java:219-244` is a 26-arg constructor call), and a caller reading a stored `InstallRequest` in a debugger sees a wall of `true/false`. This is acceptable behind a builder, but it argues strongly *for* always going through the builder and *against* exposing the canonical constructor as public API.

### F14 — `HelmException` lives in `errors` package; `RepoAddFailure`/`ReleaseFailure` do not — Low
`errors/HelmException.java` is alone in its package, while the domain-failure records sit in `repo`/`release`. Failure handling is scattered across three packages with no single place to discover it.

---

## Re-design proposal

Prioritization: **P0** = do before any "1.0 / idiomatic" claim; **P1** = strongly recommended next; **P2** = polish. "Breaking?" is relative to the current public surface.

### P0-1 — Return builders, drop the `Consumer<Builder>` spec pattern — **Breaking**

Make the request builder itself fluent and terminal. The namespace client hands back a builder whose terminal method executes. Keep one `op(Request)` overload for pre-built/reused requests.

**Before** (`ReleaseClient.java:16`, README:49-59):
```java
var install = helm.release().install(spec ->
    spec.releaseName("nginx")
        .chart(ChartRef.repo("bitnami/nginx"))
        .namespace("apps")
        .createNamespace(true));
```

**After:**
```java
// fluent + terminal: builder carries a back-reference to the gateway
var install = helm.release().install()
    .releaseName("nginx")
    .chart(ChartRef.repo("bitnami/nginx"))
    .namespace("apps")
    .createNamespace(true)
    .execute();                       // terminal

// still supported for reuse / pre-built requests:
InstallRequest req = InstallRequest.builder()....build();
var install2 = helm.release().install(req);
```

Signatures on `ReleaseClient`:
```java
public InstallInvocation install();                 // returns fluent invocation builder
public ReleaseResult install(InstallRequest request);
```
where `InstallInvocation extends InstallRequest.Builder` (or wraps it) and adds `ReleaseResult execute()`. This kills F1 *and* F2 (`show()` becomes `helm.chart().show(ChartRef.repo("bitnami/nginx")).execute()` — no empty lambda). `NamespaceClient.buildAndInvoke` is deleted.

### P0-2 — Split `ReleaseOutcome`; one result type per operation family — **Breaking**

Replace the single 5-permit `ReleaseOutcome` with operation-scoped sealed types so exhaustiveness is meaningful.

**Before** (`release/ReleaseOutcome.java`):
```java
sealed interface ReleaseOutcome
    permits ReleaseSuccess, ReleasePending, ReleaseFailure, UninstallSuccess, RollbackSuccess {}
```

**After:**
```java
// install + upgrade share a shape, so they can share a result:
sealed interface ReleaseResult permits ReleaseSuccess, ReleasePending, ReleaseFailure {}
sealed interface UninstallResult permits UninstallSuccess, UninstallFailure {}
sealed interface RollbackResult  permits RollbackSuccess,  RollbackFailure  {}
```
`install()`/`upgrade()` return `ReleaseResult`; `uninstall()` returns `UninstallResult`; `rollback()` returns `RollbackResult`. A `switch` on an install result now has exactly three real cases. Fixes F4. (Also fixes the README F10 bug as a side effect, since `RollbackResult` is redesigned.)

### P0-3 — Pick ONE failure model and apply it everywhere — **Breaking**

Decide per-operation-class, then be 100% consistent. Recommended split:

- **Lifecycle mutations** (`install/upgrade/uninstall/rollback`, `repo add`): keep the sealed `*Result` with an explicit `*Failure` permit — these have rich, expected domain-failure states (a failed release is *data*, not an exception).
- **Everything else** (`status`, `history`, `list`, `get*`, `search*`, `show`, `template`, `lint`, `update`, `registryLogin/Logout`): return the value directly, throw `HelmException` on failure. Add a `ReleaseNotFoundException extends HelmException` for the common `status`/`get*` case so callers can catch it specifically.

Then **unify the failure carrier**: `ReleaseFailure`, `RepoAddFailure`, and `HelmException` all carry `(message, stage, operation)`. Give the `*Failure` records a single shared shape:
```java
package dev.nthings.helm4j.errors;            // co-locate with HelmException — fixes F14
public record HelmFailure(String message, String stage, String operation, Optional<String> hint) {}

// release/ReleaseFailure.java:
public record ReleaseFailure(HelmFailure failure) implements ReleaseResult {}
```
And make `HelmException` carry a `HelmFailure` too, so the value-channel and exception-channel speak the same vocabulary. Document the contract in package-info. Fixes F3, F14.

### P0-4 — Collapse `Helm.install/upgrade` into the namespace API — **Breaking**

Delete `Helm.InstallBuilder` and `Helm.UpgradeBuilder` (`Helm.java:43-180`). With P0-1, `helm.release().install()....execute()` is already fluent, so the one-shot builders add nothing but a divergent, capability-poor second path (F5). If a true zero-ceremony one-shot is wanted, add it as a thin, *full-capability* delegate that reuses the real builder:

**Before:**
```java
Helm.install(ChartRef.repo("bitnami/nginx")).releaseName("nginx").run();   // subset of options
helm.release().install(spec -> spec.releaseName("nginx").chart(...));      // full options
```
**After:**
```java
// one obvious way; if a static convenience is kept it delegates to the same builder:
try (var helm = Helm.client()) {
  helm.release().install().chart(ChartRef.repo("bitnami/nginx")).releaseName("nginx").execute();
}
```
Keep `Helm.client()` / `Helm.client(spec -> ...)` and `Helm.version()` — but make `Helm.version()`'s per-call open/close explicit in Javadoc, or cache it.

### P1-1 — Document and/or harden lifecycle & thread-safety — **Non-breaking**

`HelmClient` (F7, F8): add Javadoc:
```java
/**
 * {@code close()} is currently a no-op — native allocations are released per operation.
 * It is declared for forward compatibility; always use try-with-resources.
 * {@code HelmClient} and its namespace clients are immutable and safe to share across threads.
 */
@Override public void close() { /* ... */ }
```
This is free and removes a real source of user doubt. If pooling/real native lifecycle is ever added, the contract is already stated.

### P1-2 — Add nullability annotations + `Optional` returns on models — **Mostly non-breaking**

Adopt JSpecify (`org.jspecify.annotations`), mark the module `@NullMarked`, annotate the genuinely-nullable request/model fields `@Nullable` (F6). For *result* models that users read, prefer `Optional` accessors for the optional bits:
```java
public record ReleaseInfo(String name, String namespace, int revision, ReleaseStatus status, ...) {
  public Optional<String>  description()   { ... }
  public Optional<Instant> firstDeployed() { ... }
  public Optional<String>  notes()         { ... }
}
```
Changing accessor return types *is* breaking; if that is too costly for 1.0, at minimum ship `@Nullable` (non-breaking, IDE-visible) now and migrate accessors later.

### P1-3 — Make `ListResult<T>` idiomatic — **Non-breaking (additive)**

`model/ListResult.java` (F12):
```java
public record ListResult<T>(List<T> items) implements Iterable<T> {
  public boolean isEmpty()      { return items.isEmpty(); }
  public Stream<T> stream()     { return items.stream(); }
  public Iterator<T> iterator() { return items.iterator(); }
  public Optional<T> first()    { ... }   // keep
}
```
Purely additive — no existing call breaks.

### P1-4 — Add a no-arg `show` / collapse the show signature — **Non-breaking (additive)**

Even independent of P0-1, add overloads so the README never needs `spec -> {}` (F2):
```java
public ShowResult show(ShowMode mode, ChartRef ref);                       // new
public ShowResult show(ShowMode mode, ChartRef ref, ShowRequest request);  // keep
```

### P2-1 — Naming cleanup — **Breaking (renames)**

Batch into the P0 break (F11): `dryRunMode` → `dryRun` everywhere (and make `UninstallRequest.dryRun` use `DryRunMode` for consistency, or rename it `dryRunOnly`); `packageChart` → `package_`-free verb `pack` or keep but document; `show(mode, chartReference, ...)` param → `chart`. Pick `searchRepo`/`searchHub` *or* `repo().search()` style and commit.

### P2-2 — Reconsider `ChartRef` + `ChartSource` split — **Breaking**

Fold version resolution into `ChartRef` subtypes where it belongs (`RepoChartRef(repo, chart, version)`, `OciChartRef(uri, version)`) and keep `ChartSource` strictly for *transport/auth* (TLS, credentials, `plainHttp`). Then a request has a `ChartRef` (what chart, what version) and an optional `ChartSource` (how to fetch it) with no overlapping `version`/`repositoryUrl`. Reduces F9. This is the most invasive change — schedule it deliberately, possibly post-1.0.

---

## Migration sketch

The P0 changes are breaking by nature; land them together in one pre-1.0 minor so users migrate once.

1. **Branch the result types first (P0-2, P0-3).** Add the new `ReleaseResult`/`UninstallResult`/`RollbackResult` and `HelmFailure` alongside the old `ReleaseOutcome`; have the gateway adapters in `helm4j-native` map to the new types. The `internal/gateway` interfaces change return types (`ReleaseGateway.install` → `ReleaseResult`) — this is an internal SPI, so only `helm4j-native` is affected, and the task brief explicitly allows reading/adjusting that seam. Delete `ReleaseOutcome` once nothing references it.
2. **Introduce fluent invocations (P0-1) as additive overloads**, e.g. `install()` returning the new invocation type, while temporarily keeping `install(Consumer<...>)` `@Deprecated(forRemoval=true)` for one release so existing code compiles with warnings. Remove the `Consumer` overloads and `NamespaceClient.buildAndInvoke` in the following release.
3. **Delete `Helm.InstallBuilder`/`UpgradeBuilder` (P0-4)** in the same release the `Consumer` overloads go — by then `helm.release().install()....execute()` is the one fluent path. Provide a short migration table in the README mapping each removed `Helm.install(...)` method to its `InstallRequest.Builder` equivalent.
4. **Ship P1-1 (Javadoc), P1-3 (`ListResult`), P1-4 (`show` overload) immediately** — they are non-breaking and can go in a patch release ahead of the big break to reduce churn.
5. **P1-2 nullability:** ship `@Nullable` annotations now (non-breaking); defer the `Optional`-accessor change to the same breaking release as P0, listed in the changelog under "model accessor signatures."
6. **P2-2 (`ChartRef`/`ChartSource`)** is large enough to defer to its own release after 1.0; gate it behind a clear deprecation cycle on `ChartSource.version()`/`repositoryUrl()`.
7. **Fix the README (F10) and the `show(... spec -> {})` sample (F2) right now** — these are doc-only and should not wait for any code change.

Net: two breaking releases (`0.x` → `0.x+1` deprecations, `0.x+1` → `0.x+2` removals) plus one non-breaking patch, then `1.0`. Every breaking change is mechanical and scriptable for downstream users; none requires touching the native runtime.
