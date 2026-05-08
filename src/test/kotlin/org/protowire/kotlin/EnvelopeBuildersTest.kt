// SPDX-License-Identifier: MIT
// Copyright (c) 2026 TrendVidia, LLC.
package org.protowire.kotlin

import com.google.protobuf.ByteString
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EnvelopeBuildersTest {

    @Test
    fun `envelope DSL builds canonical fixture`() {
        // Mirrors the canonical envelope dump used in cross_envelope_check.sh.
        val env = envelope {
            status = 402
            data = byteArrayOf(0xDE.toByte(), 0xAD.toByte(), 0xBE.toByte(), 0xEF.toByte())
            error {
                code = "INSUFFICIENT_FUNDS"
                message = "balance too low"
                args += listOf("\$3.50", "\$10.00")
                detail {
                    field = "amount"
                    code = "MIN_VALUE"
                    message = "below minimum"
                    args += "10.00"
                }
                meta["request_id"] = "req-123"
            }
        }

        assertEquals(402, env.status)
        assertEquals(ByteString.copyFrom(byteArrayOf(0xDE.toByte(), 0xAD.toByte(), 0xBE.toByte(), 0xEF.toByte())), env.data)
        assertEquals("INSUFFICIENT_FUNDS", env.error.code)
        assertEquals("balance too low", env.error.message)
        assertEquals(listOf("\$3.50", "\$10.00"), env.error.argsList)
        assertEquals(1, env.error.detailsCount)
        assertEquals("amount", env.error.getDetails(0).field)
        assertEquals("req-123", env.error.metadataMap["request_id"])
    }

    @Test
    fun `appError DSL standalone`() {
        val ae = appError {
            code = "X"
            message = "y"
            args += "a"
            args += "b"
        }
        assertEquals("X", ae.code)
        assertEquals(listOf("a", "b"), ae.argsList)
    }

    @Test
    fun `default-value envelope encodes minimal`() {
        val env = envelope { /* nothing set */ }
        // Proto3 default-omission: status=0 omits, transport_error="" omits.
        assertEquals(0, env.serializedSize)
    }

    @Test
    fun `transport-error envelope`() {
        val env = envelope { transportError = "connection refused" }
        assertEquals("connection refused", env.transportError)
        assertTrue(env.serializedSize > 0)
    }
}
