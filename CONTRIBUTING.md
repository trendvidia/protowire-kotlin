# Contributing to protowire-kotlin

Welcome — this is the Kotlin extensions companion for
[protowire-java](https://github.com/trendvidia/protowire-java), part
of the [protowire](https://protowire.org) language-neutral wire-format
toolkit. The codecs themselves live in the Java port; this artifact
adds the idiomatic Kotlin surface (suspend wrappers, DSL builders,
Flow extensions).

> **Steward integration is rolling out.** The governance described in
> [GOVERNANCE.md](GOVERNANCE.md) is the steady-state model. While
> Steward is being finalised, pull requests are reviewed by human
> maintainers in the conventional way — open a PR, expect review,
> iterate.

## Where bugs go

| Symptom | File against |
|---|---|
| Kotlin extension API ergonomics, suspend wrapper bug, DSL builder bug | `trendvidia/protowire-kotlin` |
| Bug that reproduces in `protowire-java` directly (calling the Java API from Java) | upstream [`trendvidia/protowire-java`](https://github.com/trendvidia/protowire-java) |
| The same input produces different output here vs another port | upstream [`trendvidia/protowire`](https://github.com/trendvidia/protowire) (cross-port wire-equivalence regression) |
| Spec / grammar / proto annotation question | upstream [`trendvidia/protowire`](https://github.com/trendvidia/protowire) |
| Decoder crash / hang / OOM on adversarial input | **email security@trendvidia.com**, do not file public issue (see [SECURITY.md](SECURITY.md)) |

## Toolchain

JDK 21+, Gradle 9.5 (managed by the wrapper), Kotlin 1.9+. Tested in
CI on:

- Ubuntu / temurin 21
- macOS / temurin 21
- Windows / temurin 21

Plus `-Werror`-equivalent settings on the Kotlin compiler keep the
build lint-clean.

## Local development

This is a Gradle composite build that depends on a sibling
[`protowire-java`](https://github.com/trendvidia/protowire-java)
checkout for the underlying codec jars (see
`settings.gradle.kts`'s `includeBuild("../protowire-java")`). Until
the Java artifacts are published to Maven Central (tracked in
ROADMAP 0.72.0 upstream), this is how dependent code resolves them.

```sh
git clone https://github.com/trendvidia/protowire-java.git ../protowire-java

./gradlew build
./gradlew test
```

## Sending changes

1. Open a draft PR early.
2. **Wire-impacting** changes belong in `protowire-java` first (or
   the upstream spec). This repo only adds Kotlin idioms; never
   re-implement codec logic here.
3. **Anything that adds a new public Kotlin extension** must be
   covered by a test under `src/test/kotlin/`.

## Code style

- Default Kotlin conventions; `.editorconfig` enforces 4-space
  indents.
- Public extensions get KDoc with at least one example.
- Match existing patterns: file-level `@file:JvmName("…Kotlin")`
  ensures Java callers see e.g. `PxfKotlin` rather than the synthetic
  `PxfExtensionsKt`.
- Suspend wrappers should dispatch to `Dispatchers.IO` for codec ops
  (they're CPU-bound but the Java side may allocate on the heap; IO
  pool prevents starving the default Kotlin dispatcher).

## What we don't accept

- Reimplementing codec logic. Call into `protowire-java`.
- New top-level dependencies beyond `kotlinx-coroutines`. The
  artifact must stay small and additive.

## Releases

This port releases in lockstep with the rest of the `protowire-*`
stack. The version line is `0.70.x` for the first coordinated public
release; the Kotlin module always pins to the same `0.70.x` Java
artifacts.

Cutting a release:

1. Bump `version` in `build.gradle.kts`.
2. Add a `## [X.Y.Z]` section to `CHANGELOG.md`.
3. Tag `vX.Y.Z` on `main`.
4. The `.github/workflows/publish.yml` workflow runs `./gradlew
   publishToMavenCentral --no-configuration-cache` against the
   Central Portal trusted-publisher integration.
