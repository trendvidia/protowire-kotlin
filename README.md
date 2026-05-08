# protowire-kotlin

Kotlin extensions companion for the [protowire-java](https://github.com/trendvidia/protowire-java) port.

This is **not a Kotlin reimplementation** of the protowire codec family — Kotlin code calls the Java port natively, and dual implementations would drift over time. Instead, this artifact adds the idiomatic Kotlin surface around the Java codecs (the same pattern Retrofit, OkHttp, Coil, and most modern JVM libraries use).

## What's here

### `suspend` wrappers — `PxfExtensions.kt`, `PbExtensions.kt`, `SbeExtensions.kt`

Each codec entry point gets a suspending counterpart that dispatches to `Dispatchers.IO`:

```kotlin
import org.protowire.kotlin.pxfMarshal
import org.protowire.kotlin.pxfUnmarshal

val msg = pxfUnmarshal(bytes, descriptor)        // Suspending — IO dispatcher
val text = pxfMarshal(msg)                       // Suspending — IO dispatcher

import org.protowire.kotlin.marshalSuspending
val sbe = sbeCodec.marshalSuspending(msg)        // Receiver-style for the descriptor-driven SBE Codec
```

### DSL builders — `EnvelopeBuilders.kt`

`Envelope` / `AppError` / `FieldError` get a typed Kotlin DSL:

```kotlin
import org.protowire.kotlin.envelope

val env = envelope {
    status = 402
    data = byteArrayOf(0xDE.toByte(), 0xAD.toByte(), 0xBE.toByte(), 0xEF.toByte())
    error {
        code = "INSUFFICIENT_FUNDS"
        message = "balance too low"
        args += listOf("$3.50", "$10.00")
        detail {
            field = "amount"
            code = "MIN_VALUE"
            message = "below minimum"
        }
        meta["request_id"] = "req-123"
    }
}
```

Strictly more readable than chained Java `Builder.setX(...).addY(...).putZ(...).build()`.

### Sealed `PxfResult<T>` — `PxfResult.kt`

Idiomatic alternative to the Java port's `Result` interface, paired with `pxfUnmarshalFull`:

```kotlin
when (val r = pxfUnmarshalFull(bytes, MyMessage.newBuilder())) {
    is PxfResult.Success -> {
        val msg = r.builder.build()
        if (r.presence.isAbsent("optional_field")) {
            // Field really wasn't supplied — distinct from "set to default"
        }
    }
    is PxfResult.Failure -> log.error("decode failed", r.cause)
}
```

`fold` / `getOrNull` / `getOrElse` for point-free style.

### `Flow<T>` extensions — `Flows.kt`

Adapt a flow of byte arrays to a flow of typed messages, with codec calls dispatched to `Dispatchers.IO`:

```kotlin
sourceBytes                                     // : Flow<ByteArray>
    .unmarshalPxfTyped { MyMessage.newBuilder() }
    .map { it.build() }
    .collect { handle(it) }
```

The Java port doesn't yet ship native streaming reads; these adapters bridge user-segmented byte streams (Kafka records, Postgres `LISTEN` notifications, file-segment iterators) into typed pipelines.

## Building

Composite-build setup: depends on [`protowire-java`](https://github.com/trendvidia/protowire-java) checked out as a sibling at `../protowire-java/`. The `settings.gradle.kts` `includeBuild` directive resolves the Java port's modules at build time.

```bash
./gradlew test
./gradlew jar
```

## Versioning

Locked in step with `protowire-java`'s minor version. The wire-format compatibility surface lives in [protowire's `STABILITY.md`](https://github.com/trendvidia/protowire/blob/main/STABILITY.md); this module's contract is "it composes with whatever Java port version is on the classpath at the same minor".

## Out of scope

- **Kotlin Multiplatform (KMP).** The protowire family already has a Swift port; a KMP-Common reimplementation would compete with both Java and Swift. KMP is on the roadmap below 1.0.0 only as an explicit out-of-scope. See [protowire's `ROADMAP.md`](https://github.com/trendvidia/protowire/blob/main/ROADMAP.md).
- **Lite-tier (Android) variants** of these helpers. The Java port's `*-android` modules are usable from Kotlin without any wrapper today; if real Android consumers report ergonomic gaps, a `protowire-kotlin-android` companion is one option for a follow-up.

## License

MIT. See [LICENSE](LICENSE).
