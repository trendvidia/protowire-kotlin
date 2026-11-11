// SPDX-License-Identifier: MIT
// Copyright (c) 2026 TrendVidia, LLC.
package org.protowire.kotlin

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.protowire.envelope.v1.FieldError
import org.protowire.pb.ProtoField
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

/**
 * The PB wrappers take `@ProtoField`-annotated objects, not generated
 * messages. protobuf-java is the oracle: a struct with FieldError's field
 * numbers must encode to the bytes FieldError encodes to.
 */
class PbExtensionsTest {

    class Err {
        @field:ProtoField(1) var field: String = ""
        @field:ProtoField(2) var code: String = ""
        @field:ProtoField(3) var message: String = ""
    }

    private val proto = FieldError.newBuilder().setField("amount").setCode("MIN_VALUE").setMessage("below minimum").build()

    @Test
    fun `pbMarshal writes what protobuf-java writes`() = runTest {
        val err = Err().apply { field = "amount"; code = "MIN_VALUE"; message = "below minimum" }
        assertContentEquals(proto.toByteArray(), pbMarshal(err))
    }

    @Test
    fun `pbUnmarshal reads protobuf-java's bytes`() = runTest {
        val err = pbUnmarshal<Err>(proto.toByteArray())
        assertEquals(listOf("amount", "MIN_VALUE", "below minimum"), listOf(err.field, err.code, err.message))
    }

    @Test
    fun `pbUnmarshalInto fills the instance it is given`() = runTest {
        val err = Err().apply { message = "kept unless overwritten" }
        pbUnmarshalInto(FieldError.newBuilder().setCode("REQUIRED").build().toByteArray(), err)
        assertEquals(listOf("", "REQUIRED", "kept unless overwritten"), listOf(err.field, err.code, err.message))
    }
}
