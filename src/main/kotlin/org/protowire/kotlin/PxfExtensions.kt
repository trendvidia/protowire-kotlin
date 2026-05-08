// SPDX-License-Identifier: MIT
// Copyright (c) 2026 TrendVidia, LLC.
@file:JvmName("PxfKotlin")

package org.protowire.kotlin

import com.google.protobuf.Descriptors.Descriptor
import com.google.protobuf.DynamicMessage
import com.google.protobuf.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.protowire.pxf.Pxf
import org.protowire.pxf.Result as JavaPxfResult

/**
 * Suspending wrappers around `org.protowire.pxf.Pxf`. Each delegates to the
 * Java entry point on `Dispatchers.IO`, so a coroutine on the main /
 * default dispatcher won't block its thread for the duration of an
 * unmarshal/marshal call.
 *
 * <p>For the typed-builder unmarshal path, see [pxfUnmarshalInto] —
 * mutates the supplied `Message.Builder` and returns it (as a fluent
 * `apply` does in pure Kotlin).
 */

/** Suspending [Pxf.unmarshal] returning a [DynamicMessage] for the supplied descriptor. */
public suspend fun pxfUnmarshal(data: ByteArray, descriptor: Descriptor): DynamicMessage =
    withContext(Dispatchers.IO) { Pxf.unmarshal(data, descriptor) }

/** Suspending [Pxf.unmarshal] into a typed builder. The same builder is returned for chaining. */
public suspend fun <B : Message.Builder> pxfUnmarshalInto(data: ByteArray, builder: B): B =
    withContext(Dispatchers.IO) {
        Pxf.unmarshal(data, builder)
        builder
    }

/** Suspending [Pxf.marshal] — Message → PXF text bytes. */
public suspend fun pxfMarshal(msg: Message): ByteArray =
    withContext(Dispatchers.IO) { Pxf.marshal(msg) }

/**
 * Suspending [Pxf.unmarshalFull] returning a [PxfResult] paired with the
 * built message. Captures field-presence metadata (set / null / absent)
 * which the throw-on-error variant discards — useful for validation flows
 * where "field absent" and "field set to default" must be distinguished.
 */
public suspend fun <B : Message.Builder> pxfUnmarshalFull(
    data: ByteArray,
    builder: B
): PxfResult<B> = withContext(Dispatchers.IO) {
    try {
        val javaResult: JavaPxfResult = Pxf.unmarshalFull(data, builder)
        PxfResult.Success(builder, FieldPresence(javaResult))
    } catch (e: Exception) {
        PxfResult.Failure(e)
    }
}
