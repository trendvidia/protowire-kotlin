// SPDX-License-Identifier: MIT
// Copyright (c) 2026 TrendVidia, LLC.
@file:JvmName("EnvelopeKotlin")

package org.protowire.kotlin

import com.google.protobuf.ByteString
import org.protowire.envelope.v1.AppError
import org.protowire.envelope.v1.Envelope
import org.protowire.envelope.v1.FieldError

/**
 * DSL builders for `org.protowire.envelope.v1.Envelope` and friends.
 * Idiomatic alternative to chained Java builder calls — composable,
 * type-safe, and visually closer to the wire-format payload.
 *
 * <p>Example:
 *
 * ```
 * val env = envelope {
 *     status = 402
 *     data = byteArrayOf(0xDE.toByte(), 0xAD.toByte(), 0xBE.toByte(), 0xEF.toByte())
 *     error {
 *         code = "INSUFFICIENT_FUNDS"
 *         message = "balance too low"
 *         args += listOf("$3.50", "$10.00")
 *         details += fieldError {
 *             field = "amount"
 *             code = "MIN_VALUE"
 *             message = "below minimum"
 *         }
 *         meta["request_id"] = "req-123"
 *     }
 * }
 * ```
 *
 * <p>Each DSL block writes through a typed builder; the surrounding
 * factory (`envelope`, `appError`, `fieldError`) calls `build()` on it.
 */

/** Top-level DSL: build an [Envelope]. */
public inline fun envelope(block: EnvelopeBuilder.() -> Unit): Envelope =
    EnvelopeBuilder().apply(block).build()

/** Top-level DSL: build a standalone [AppError]. */
public inline fun appError(block: AppErrorBuilder.() -> Unit): AppError =
    AppErrorBuilder().apply(block).build()

/** Top-level DSL: build a standalone [FieldError]. */
public inline fun fieldError(block: FieldErrorBuilder.() -> Unit): FieldError =
    FieldErrorBuilder().apply(block).build()

@DslMarker
public annotation class EnvelopeDsl

@EnvelopeDsl
public class EnvelopeBuilder {
    public var status: Int = 0
    public var transportError: String = ""
    public var data: ByteArray? = null
    private var error: AppError? = null

    /** Nested error block. The outer `Envelope.error` is set on `build()`. */
    public fun error(block: AppErrorBuilder.() -> Unit) {
        error = AppErrorBuilder().apply(block).build()
    }

    /** Set the error to a pre-built [AppError]. */
    public fun error(value: AppError) {
        error = value
    }

    public fun build(): Envelope = Envelope.newBuilder().also { b ->
        b.status = status
        if (transportError.isNotEmpty()) b.transportError = transportError
        data?.let { b.data = ByteString.copyFrom(it) }
        error?.let { b.error = it }
    }.build()
}

@EnvelopeDsl
public class AppErrorBuilder {
    public var code: String = ""
    public var message: String = ""
    public val args: MutableList<String> = mutableListOf()
    public val details: MutableList<FieldError> = mutableListOf()
    public val meta: MutableMap<String, String> = mutableMapOf()

    /** Append a field error built via DSL. */
    public fun detail(block: FieldErrorBuilder.() -> Unit) {
        details += FieldErrorBuilder().apply(block).build()
    }

    public fun build(): AppError = AppError.newBuilder().also { b ->
        b.code = code
        b.message = message
        args.forEach { b.addArgs(it) }
        details.forEach { b.addDetails(it) }
        meta.forEach { (k, v) -> b.putMetadata(k, v) }
    }.build()
}

@EnvelopeDsl
public class FieldErrorBuilder {
    public var field: String = ""
    public var code: String = ""
    public var message: String = ""
    public val args: MutableList<String> = mutableListOf()

    public fun build(): FieldError = FieldError.newBuilder().also { b ->
        b.field = field
        b.code = code
        b.message = message
        args.forEach { b.addArgs(it) }
    }.build()
}
