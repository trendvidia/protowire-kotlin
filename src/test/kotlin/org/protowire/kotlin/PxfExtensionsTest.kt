// SPDX-License-Identifier: MIT
// Copyright (c) 2026 TrendVidia, LLC.
package org.protowire.kotlin

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.protowire.envelope.v1.Envelope
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class PxfExtensionsTest {

    @Test
    fun `pxfMarshal then pxfUnmarshalInto round-trips an envelope`() = runTest {
        val original = envelope {
            status = 402
            error {
                code = "INSUFFICIENT_FUNDS"
                message = "balance too low"
            }
        }

        val text = pxfMarshal(original)
        val rebuilt = pxfUnmarshalInto(text, Envelope.newBuilder()).build()

        // PXF round-trip is lossless for the envelope shape.
        assertContentEquals(original.toByteArray(), rebuilt.toByteArray())
        assertEquals(original.status, rebuilt.status)
        assertEquals(original.error.code, rebuilt.error.code)
    }

    @Test
    fun `pxfUnmarshalFull surfaces field-presence on Success`() = runTest {
        // Empty envelope text → all fields absent (proto3 has no presence
        // tracking on scalars by default; PXF tracks input-side absence
        // independently via Pxf.unmarshalFull).
        val text = "status = 0\n".toByteArray()
        val result = pxfUnmarshalFull(text, Envelope.newBuilder())

        when (result) {
            is PxfResult.Success -> {
                assertEquals(0, result.builder.build().status)
                // `status` was set in the input (even though to default zero);
                // `error` was never mentioned at all.
                assertEquals(false, result.presence.isAbsent("status"))
                assertEquals(true,  result.presence.isAbsent("error"))
            }
            is PxfResult.Failure -> error("expected Success, got Failure: ${result.cause}")
        }
    }

    @Test
    fun `pxfUnmarshalFull returns Failure on garbage input`() = runTest {
        val result = pxfUnmarshalFull("this is not pxf".toByteArray(), Envelope.newBuilder())
        when (result) {
            is PxfResult.Success -> error("expected Failure, got Success")
            is PxfResult.Failure -> {
                // Sanity: we got an exception, not a silently-default builder.
                // The exact message depends on lexer/parser internals.
                assertEquals(true, result.cause.message?.isNotEmpty() ?: false)
            }
        }
    }

    @Test
    fun `fold helper takes both branches`() = runTest {
        val ok = pxfUnmarshalFull("status = 7\n".toByteArray(), Envelope.newBuilder())
        val errResult = pxfUnmarshalFull("garbage".toByteArray(), Envelope.newBuilder())

        val okMsg = ok.fold(onSuccess = { b, _ -> "ok ${b.build().status}" }, onFailure = { "fail" })
        val errMsg = errResult.fold(onSuccess = { _, _ -> "ok" }, onFailure = { "fail ${it::class.simpleName}" })

        assertEquals("ok 7", okMsg)
        assertEquals(true, errMsg.startsWith("fail "))
    }
}
