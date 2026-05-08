// SPDX-License-Identifier: MIT
// Copyright (c) 2026 TrendVidia, LLC.
@file:JvmName("PbKotlin")

package org.protowire.kotlin

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.protowire.pb.Pb

/**
 * Suspending wrappers around `org.protowire.pb.Pb`. The Java port's PB
 * codec is a struct codec — operates on plain POJOs annotated with
 * `@ProtoField`, not on protobuf-generated [com.google.protobuf.Message]
 * subclasses. The Kotlin layer just adds `Dispatchers.IO` dispatch so
 * coroutines on the main thread don't block during marshal / unmarshal.
 *
 * <p>For protobuf-generated messages, see [pxfMarshal] / [pxfUnmarshal]
 * (PXF text path) or call `MessageLite.toByteArray()` /
 * `parseFrom(...)` directly — protobuf-java's own methods are already
 * thread-safe and short enough that suspending wrappers add no value.
 */

/** Suspending PB struct marshal — `@ProtoField`-annotated POJO → wire bytes. */
public suspend fun pbMarshal(obj: Any): ByteArray =
    withContext(Dispatchers.IO) { Pb.marshal(obj) }

/** Suspending PB struct unmarshal — wire bytes into an existing POJO instance. */
public suspend fun pbUnmarshalInto(data: ByteArray, dest: Any) {
    withContext(Dispatchers.IO) { Pb.unmarshal(data, dest) }
}

/** Suspending PB struct unmarshal — wire bytes → freshly-constructed POJO of [T]. */
public suspend inline fun <reified T : Any> pbUnmarshal(data: ByteArray): T =
    withContext(Dispatchers.IO) { Pb.unmarshal(data, T::class.java) }
