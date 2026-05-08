# Changelog

All notable changes to `protowire-kotlin` are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

The version number is kept aligned with the rest of the `protowire-*`
stack — releases bump in lockstep across language ports when the wire
format changes.

## [Unreleased]

## [0.70.0]

Initial public release. The version number aligns this artifact with
the rest of the `protowire-*` stack, which targets the 0.70.x series
for the first coordinated public release.

### Added

- **Kotlin extensions companion** for [protowire-java](https://github.com/trendvidia/protowire-java).
  Not a reimplementation — Kotlin code calls the Java codecs natively.
- **`suspend` wrappers** for PXF / PB / SBE entry points (dispatching
  to `Dispatchers.IO`) — `pxfMarshal` / `pxfUnmarshal` / `pbMarshal` /
  `pbUnmarshal`, plus receiver-style `marshalSuspending` /
  `unmarshalSuspending` / `viewSuspending` on the descriptor-driven
  SBE `Codec`.
- **DSL builders** for `Envelope` / `AppError` / `FieldError`, with
  typed `error { … detail { … } }` blocks.
- **`Flow` extensions** for stream-decoding PXF documents and PB
  length-delimited records.
- **`PxfResult`** wrappers exposing the underlying Java
  `UnmarshalFull` presence-tracking surface as Kotlin idiom.
- **Maven Central distribution** as `org.protowire:protowire-kotlin`
  (depends transitively on `org.protowire:{pxf,pb,sbe,envelope}:0.70.0`).
- **Comprehensive CI matrix**: `./gradlew build` on Linux/macOS/Windows
  with JDK 21 (temurin). Weekly CodeQL SAST.
- **Governance scaffolding**: `LICENSE` (MIT), `CONTRIBUTING.md`,
  `SECURITY.md` (security@trendvidia.com), `GOVERNANCE.md`,
  `CODE_OF_CONDUCT.md`, `.github/CODEOWNERS`, issue + PR templates,
  Dependabot for gradle + GitHub Actions.
