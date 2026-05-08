# Security Policy

## Reporting a vulnerability

Email **security@trendvidia.com** with a description, reproduction steps,
and the affected version(s) or commit(s). PGP key on request.

Please do **not** file public GitHub issues for vulnerabilities, and do
**not** post details in pull request comments.

You can expect:

- An acknowledgement within **3 business days**.
- A triage decision (accepted / not-a-vulnerability / needs-more-info)
  within **10 business days**.
- A coordinated fix on the timeline below.

## Scope

This policy covers `protowire-kotlin` — the Kotlin extensions
companion for the `protowire-java` port. Most vulnerabilities in the
codec layer are actually `protowire-java` bugs and are also in scope
at [`trendvidia/protowire-java`](https://github.com/trendvidia/protowire-java/blob/main/SECURITY.md).
You can file at either repo and we will coordinate.

In scope here:

- Bugs in the Kotlin extensions: incorrect coroutine context handling,
  DSL builders that produce malformed `Envelope` / `AppError` /
  `FieldError`, Flow operators that drop or duplicate items.
- Cross-port wire-format divergences exposed via the Kotlin surface.
- Any path where a Kotlin caller can produce wire output the Java port
  cannot, or vice versa.

Out of scope:

- Decoder safety — that's covered by the Java port's HARDENING.md
  conformance corpus (it tests the actual codec, not the Kotlin
  wrapper).
- Issues in `kotlinx.coroutines` itself — file at
  [`Kotlin/kotlinx.coroutines`](https://github.com/Kotlin/kotlinx.coroutines)
  and CC us.

## Coordinated disclosure

For vulnerabilities affecting **more than one port**, a **30-day
embargo** applies from the date we acknowledge your report (per the
upstream project's policy), extendable by mutual agreement when a fix
needs more time.

Single-port issues follow this port's own disclosure timeline,
typically 7–14 days, but always at least long enough for a fix to be
released.

## Hall of fame

Reporters who follow coordinated disclosure are credited in
`SECURITY-ADVISORY-*.md` advisories on the upstream repo and (with
permission) in the release notes. We do not currently run a paid
bug-bounty program.
