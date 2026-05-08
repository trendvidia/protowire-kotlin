# Governance

`protowire-kotlin` is governed under the same constitution as the rest
of the `protowire-*` stack. The machine-readable source of truth lives
in the upstream spec repo at
[`governance.pxf`](https://github.com/trendvidia/protowire/blob/main/governance.pxf);
the human-readable preamble is at
[`GOVERNANCE.md`](https://github.com/trendvidia/protowire/blob/main/GOVERNANCE.md).

This file is a short pointer-doc. If anything below disagrees with the
upstream constitution, the upstream wins.

## Domain ownership

This repo's only domain vector is
[`protowire-kotlin`](https://github.com/trendvidia/protowire/blob/main/governance.pxf)
under the upstream `port-libraries` umbrella. Approval requirements:

| Path | Reviewer authority |
|---|---|
| `src/main/kotlin/` | port maintainers (`@trendvidia/maintainers`) |
| `src/test/kotlin/` | port maintainers |
| `build.gradle.kts`, `settings.gradle.kts`, `gradle/` | port maintainers |
| `.github/workflows/publish.yml` | maintainers only — controls Maven Central release surface |
| `.github/` (other) | port maintainers |

## What's enforced today vs (roadmap)

The Steward agent that enforces the constitution programmatically is
**rolling out**. Until it is live:

- Pull requests are reviewed by human maintainers.
- The `0.70.x` release line pins this artifact to the same
  `0.70.x` Java port artifacts; cross-port wire-equivalence is owned
  by the underlying Java port (and the upstream spec), not this
  Kotlin layer.
- Reputation-weighted voting, automatic escrow for risky changes, and
  the `manifesto.blocked_module_globs` restriction are all `(roadmap)`
  per the upstream `governance.pxf`.

## Stable surfaces

Everything in the `org.protowire.kotlin` package is part of the
SemVer contract. Anything in a sub-package ending in `.internal` is
not stable.

The wire contract — what bytes a given proto message produces — is
governed by the **upstream** spec, not this port. See
[`STABILITY.md`](https://github.com/trendvidia/protowire/blob/main/STABILITY.md)
upstream.
