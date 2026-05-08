// SPDX-License-Identifier: MIT
// Copyright (c) 2026 TrendVidia, LLC.
@file:JvmName("SbeKotlin")

package org.protowire.kotlin

import com.google.protobuf.Descriptors.Descriptor
import com.google.protobuf.DynamicMessage
import com.google.protobuf.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.protowire.sbe.Codec
import org.protowire.sbe.View

/**
 * Suspending wrappers around the descriptor-driven SBE [Codec]. Each
 * dispatches to `Dispatchers.IO` — useful for low-latency call paths
 * where the marshal/unmarshal call shouldn't block the calling thread
 * for the duration of a copy + descriptor walk.
 *
 * <p>The lite-tier SBE story (codegen-emitted typed `<Message>SbeCodec`)
 * is callable from Kotlin without any wrapper; just use it directly.
 * This module's wrappers exist for the descriptor-driven flow only.
 */

/** Suspending [Codec.marshal] — typed [Message] → SBE wire bytes. */
public suspend fun Codec.marshalSuspending(msg: Message): ByteArray =
    withContext(Dispatchers.IO) { marshal(msg) }

/** Suspending [Codec.unmarshal] — SBE wire bytes into a typed builder. Returns the builder. */
public suspend fun <B : Message.Builder> Codec.unmarshalSuspending(data: ByteArray, builder: B): B =
    withContext(Dispatchers.IO) {
        unmarshal(data, builder)
        builder
    }

/** Suspending [Codec.unmarshalDescriptor] — SBE wire bytes → [DynamicMessage] of the given descriptor. */
public suspend fun Codec.unmarshalDescriptorSuspending(
    data: ByteArray,
    desc: Descriptor
): DynamicMessage = withContext(Dispatchers.IO) { unmarshalDescriptor(data, desc) }

/** Suspending [Codec.view] — SBE wire bytes → zero-allocation [View]. */
public suspend fun Codec.viewSuspending(data: ByteArray): View =
    withContext(Dispatchers.IO) { view(data) }
