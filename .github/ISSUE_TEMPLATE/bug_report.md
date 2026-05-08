---
name: Bug report
about: Report a defect — incorrect Kotlin extension behavior, DSL builder bug, etc.
title: "bug: "
labels: bug
---

<!--
This module is a Kotlin extensions companion. Bugs that reproduce in
protowire-java directly belong at trendvidia/protowire-java.

Cross-port wire-equivalence regressions belong upstream at
trendvidia/protowire.

Security issues go to security@trendvidia.com (see SECURITY.md).
-->

## What happened

A clear description of the bug.

## How to reproduce

Smallest possible Kotlin snippet that triggers it.

```kotlin
import org.protowire.kotlin.*
// ...
```

## What you expected

What you thought should happen.

## Versions

- `protowire-kotlin` version (from `build.gradle.kts` or
  `dependencyInsight`):
- `org.protowire:{pxf,pb,sbe,envelope}` versions (transitively
  resolved):
- Kotlin version + JDK:
- OS / arch:
