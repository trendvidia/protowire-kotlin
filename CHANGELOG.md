# Changelog

All notable changes to `protowire-kotlin` are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

The version number is kept aligned with the rest of the `protowire-*`
stack — releases bump in lockstep across language ports when the wire
format changes.

## [Unreleased]

### Added

- **Conformance against protowire-go.** `GoConformanceTest` holds what
  `pxfMarshal` writes to 60 goldens byte for byte, and checks that
  `pxfUnmarshal` reads them back. CI runs protowire-go v1.17.0 itself to show
  the goldens are what it writes.

### Changed

- **Built against protowire-java 2.0.1**, up from 1.4.0. Java now matches
  protowire-go v1.17.0. This module has no codec of its own, so everything
  below comes from that release. No wire-format change.
  - **protobuf-java 4 is required** (4.36.2 or later), up from 3.25.5. It comes
    in through this module's `api` dependencies, so a consumer that pins
    protobuf-java 3 has to move.
  - `pxfMarshal` refuses an unset `optional` field marked `(pxf.required)`,
    with protowire-go's text. It used to write a document that the full decode
    refuses.
  - Some documents parse as protowire-go parses them. URL-safe base64 in
    `b"…"` is accepted. Timestamps follow Go's RFC 3339, so `+02`, a missing
    second, and a lowercase `t` or `z` are refused. A precision suffix on a
    map key is refused.
  - `null` on an editions field with explicit presence leaves it unset. It
    used to be refused.
  - SBE refuses a value its primitive cannot hold. It used to truncate:
    `int32` 300 written as `int8` came out as 44. A message encoded with
    another schema's id is refused.

### Fixed

- **`pxfMarshal` writes an unset field with `(pxf.default)` or `(pxf.required)`
  so that it reads back** ([protowire-java#187]). It used to leave the field
  out. A reader then applied the default to a stored zero, or refused the
  document because a required field was missing. Now such a field is written
  as its zero value (`n = 0`, `rl = []`, `rm = {}`), or `null` for a
  message-typed field with a default, as protowire-go writes it. The spec's
  four `Annotated` text cases hold this; three of them fail on 1.4.0.

## [1.4.0] — 2026-11-03

Jumps from 1.0.0 to match `protowire-java`'s minor version, as the README
promises. This is the first release a consumer can resolve from Maven Central:
1.0.0's POM named artifacts that were never published there.

### Changed

- **Built against protowire-java 1.4.0** ([#17]), up from 0.70.0. This module
  has no codec of its own, so everything below comes from that release. No
  wire-format change, but some documents that decoded before now fail, and
  PXF text changes:
  - Refused now: a singular field or map key bound twice in one body, `null`
    on a field without explicit presence, and a raw line feed in a simple
    string.
  - `pxf.BigFloat` is read and written in binary as the spec defines it. The
    old power-of-ten form decoded to a different number in every other port.
  - Text follows protowire-go, so golden files will differ: `1h30m0s` for a
    Duration, `100` and `1e+21` for floats, `.5Z` for a Timestamp fraction.
    The old forms still decode.

### Fixed

- **The published POM resolves** ([#19]). It named `org.protowire:pxf` and its
  siblings, which were never on Maven Central. It now names
  `org.protowire:protowire-pxf` and the others, as Java publishes them.
- **The Flow helpers compile for a consumer without their own coroutines
  dependency.** `kotlinx-coroutines-core` was in the POM at runtime scope,
  though `Flow` is in this module's public signatures. It is compile scope now.
- **A schema with `(pxf.required) = true` and `(pxf.default)` on one field
  is refused when it is bound** ([#18]), as draft -01 now requires
  (protowire#453). Such a schema used to bind, and a document omitting the
  field decided what it meant. `(pxf.required) = false` beside a default
  stays accepted. The check is protowire-java's, not this module's, and
  arrives with the move to 1.4.0.

[protowire-java#187]: https://github.com/trendvidia/protowire-java/issues/187
[#17]: https://github.com/trendvidia/protowire-kotlin/issues/17
[#18]: https://github.com/trendvidia/protowire-kotlin/issues/18
[#19]: https://github.com/trendvidia/protowire-kotlin/issues/19

## [1.0.0] — 2026-05-13

First major-version cut. Lockstep with the protowire v1.0 spec
freeze line (STABILITY.md in the spec repo) and the underlying
`protowire-java` v1.0.1 (which carries the three v1.0 spec changes
plus the v1.0.0 javadoc fix).

### No Kotlin source changes

The Kotlin companion exposes a deliberately narrow surface over
`org.protowire.pxf` — `Pxf` (unmarshal entry points) and `Result`
(field presence). The v1.0 rename hit Java types
(`TableDirective` → `DatasetDirective`, `Result.tables()` →
`Result.datasets()`, etc.) that this companion never re-exported,
so the Kotlin API surface is unchanged.

Consumers who reach through the companion to Java types directly
need to follow the Java rename; the `FieldPresence` wrapper exposed
here is unaffected.

### Build

- Gradle version `0.70.0` → `1.0.0`.
- The composite-build dependency on `../protowire-java` continues
  to resolve at the sibling repo's checked-out commit (now v1.0.1);
  for published artifacts the Maven coordinate is
  `org.protowire:protowire-kotlin:1.0.0`.

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
