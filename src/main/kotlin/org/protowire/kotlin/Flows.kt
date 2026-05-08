// SPDX-License-Identifier: MIT
// Copyright (c) 2026 TrendVidia, LLC.
@file:JvmName("FlowsKotlin")

package org.protowire.kotlin

import com.google.protobuf.Descriptors.Descriptor
import com.google.protobuf.DynamicMessage
import com.google.protobuf.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * `Flow<T>` adapters that decode a stream of raw byte arrays into typed
 * messages. The underlying codec calls run on `Dispatchers.IO` via
 * `flowOn`, so collectors on the main / default dispatcher don't block.
 *
 * <p>The Java port doesn't ship a native streaming reader yet — these
 * helpers are for the common case where a caller has a [Sequence] /
 * [Iterable] of pre-segmented payloads (e.g. one wire frame per Kafka
 * record, one envelope per Postgres `LISTEN` notification) and wants to
 * project them through a codec without writing the dispatcher boilerplate.
 *
 * <p>When the Java port grows true streaming reads, these helpers will
 * adapt to those source iterators in addition to the byte-array variants.
 */

/**
 * Converts each [ByteArray] into a [DynamicMessage] via [pxfUnmarshal].
 * Decoding errors propagate to the collector as exceptions; use
 * [kotlinx.coroutines.flow.catch] downstream to recover.
 */
public fun Flow<ByteArray>.unmarshalPxf(descriptor: Descriptor): Flow<DynamicMessage> =
    flow { collect { emit(pxfUnmarshal(it, descriptor)) } }
        .flowOn(Dispatchers.IO)

/**
 * Converts each [ByteArray] into a typed [Message] by decoding into a
 * fresh builder produced by [newBuilder] each call. Useful when the
 * input source has many records of the same type:
 *
 * ```
 * sourceBytes
 *     .unmarshalPxfTyped { MyMessage.newBuilder() }
 *     .map { it.build() }
 *     .collect { … }
 * ```
 */
public fun <B : Message.Builder> Flow<ByteArray>.unmarshalPxfTyped(newBuilder: () -> B): Flow<B> =
    flow { collect { emit(pxfUnmarshalInto(it, newBuilder())) } }
        .flowOn(Dispatchers.IO)

/**
 * Marshals each [Message] in the upstream flow back to PXF text bytes.
 * Useful as a sink-side transform when the producer hands typed
 * messages and the wire layer wants bytes.
 */
public fun Flow<Message>.marshalPxf(): Flow<ByteArray> =
    flow { collect { emit(pxfMarshal(it)) } }
        .flowOn(Dispatchers.IO)
