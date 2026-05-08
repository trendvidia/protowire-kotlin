// SPDX-License-Identifier: MIT
// Copyright (c) 2026 TrendVidia, LLC.
package org.protowire.kotlin

import org.protowire.pxf.Result as JavaPxfResult

/**
 * Sealed-class wrapper over the Java port's [JavaPxfResult] / thrown-
 * exception split. Idiomatic Kotlin for "either I succeeded with a typed
 * value plus presence metadata, or I failed with an exception".
 *
 * <p>Use [pxfUnmarshalFull] to obtain one. Pattern-match via:
 *
 * ```
 * when (val r = pxfUnmarshalFull(bytes, MyMessage.newBuilder())) {
 *     is PxfResult.Success -> {
 *         val msg = r.builder.build()
 *         if (r.presence.isAbsent("optional_field")) { ... }
 *     }
 *     is PxfResult.Failure -> log.error("decode failed", r.cause)
 * }
 * ```
 *
 * <p>Or use the [fold] / [getOrNull] / [getOrElse] helpers below for
 * point-free style.
 */
public sealed class PxfResult<out T> {

    /** Decode succeeded. [builder] holds the populated builder; [presence] tracks field presence. */
    public data class Success<T>(val builder: T, val presence: FieldPresence) : PxfResult<T>()

    /** Decode failed with [cause]. */
    public data class Failure(val cause: Throwable) : PxfResult<Nothing>()
}

/** Field-presence metadata from a successful unmarshal. Mirrors [JavaPxfResult]'s view. */
public class FieldPresence internal constructor(private val raw: JavaPxfResult) {
    /** True iff the field was set to the PXF `null` sentinel. */
    public fun isNull(path: String): Boolean = raw.isNull(path)

    /** True iff the field was absent from the input (not even set to null). */
    public fun isAbsent(path: String): Boolean = raw.isAbsent(path)

    /** True iff the field was set to a non-null value. */
    public fun isSet(path: String): Boolean = raw.isSet(path)

    /** Dotted paths of every field set to null. */
    public fun nullFields(): List<String> = raw.nullFields()
}

/**
 * Folds a [PxfResult] to a single value, taking branches for both
 * success and failure. Idiomatic Kotlin alternative to a `when`.
 */
public inline fun <T, R> PxfResult<T>.fold(
    onSuccess: (T, FieldPresence) -> R,
    onFailure: (Throwable) -> R
): R = when (this) {
    is PxfResult.Success -> onSuccess(builder, presence)
    is PxfResult.Failure -> onFailure(cause)
}

/** Returns the success value, or null on failure. */
public fun <T> PxfResult<T>.getOrNull(): T? = when (this) {
    is PxfResult.Success -> builder
    is PxfResult.Failure -> null
}

/** Returns the success value, or the result of [block] on failure. */
public inline fun <T> PxfResult<T>.getOrElse(block: (Throwable) -> T): T = when (this) {
    is PxfResult.Success -> builder
    is PxfResult.Failure -> block(cause)
}
