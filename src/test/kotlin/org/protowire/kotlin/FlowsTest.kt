// SPDX-License-Identifier: MIT
// Copyright (c) 2026 TrendVidia, LLC.
package org.protowire.kotlin

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.protowire.envelope.v1.Envelope
import kotlin.test.assertEquals

class FlowsTest {

    @Test
    fun `unmarshalPxfTyped projects byte flow into typed envelopes`() = runTest {
        val a = envelope { status = 1 }
        val b = envelope { status = 2 }
        val c = envelope { status = 3 }

        val texts = flowOf(pxfMarshal(a), pxfMarshal(b), pxfMarshal(c))
        val results = texts.unmarshalPxfTyped { Envelope.newBuilder() }.toList()

        assertEquals(listOf(1, 2, 3), results.map { it.build().status })
    }

    @Test
    fun `marshalPxf projects message flow back to bytes`() = runTest {
        val msgs = flowOf<com.google.protobuf.Message>(
            envelope { status = 1 },
            envelope { status = 2 }
        )
        val bytesList = msgs.marshalPxf().toList()
        assertEquals(2, bytesList.size)
        // Round-trip the first one to confirm it's actual PXF text.
        val rebuilt = pxfUnmarshalInto(bytesList[0], Envelope.newBuilder()).build()
        assertEquals(1, rebuilt.status)
    }
}
