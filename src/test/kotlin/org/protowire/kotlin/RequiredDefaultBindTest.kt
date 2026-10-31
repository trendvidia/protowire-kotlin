// SPDX-License-Identifier: MIT
// Copyright (c) 2026 TrendVidia, LLC.
package org.protowire.kotlin

import com.google.protobuf.ByteString
import com.google.protobuf.CodedOutputStream
import com.google.protobuf.DescriptorProtos.DescriptorProto
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto
import com.google.protobuf.DescriptorProtos.FieldOptions
import com.google.protobuf.DescriptorProtos.FileDescriptorProto
import com.google.protobuf.Descriptors.Descriptor
import com.google.protobuf.Descriptors.FileDescriptor
import com.google.protobuf.DynamicMessage
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Draft -01 "Required and Default": a schema with `(pxf.required) = true`
 * and `(pxf.default)` on one field is refused when it is bound, not when a
 * document omits the field.
 *
 * This module has no bind path of its own. The check is
 * `org.protowire.pxf.SchemaValidator` in protowire-java, and these tests
 * pin that it reaches Kotlin callers through each wrapper over the decoder.
 * The options are raw bytes, the way a descriptor loaded from a
 * FileDescriptorSet carries them, so no `.proto` compiler is involved.
 */
class RequiredDefaultBindTest {

    /** Sets the field, so only a bind-time check can refuse this document. */
    private val document = "f = \"set\"\n".toByteArray()

    private val exclusive = "(pxf.required) and (pxf.default) are exclusive on one field"

    @Test
    fun `pxfUnmarshal refuses the pair even when the document sets the field`() = runTest {
        val e = assertFailsWith<Exception> { pxfUnmarshal(document, descriptor(required = true)) }
        assertTrue(exclusive in e.message.orEmpty(), "got: ${e.message}")
    }

    @Test
    fun `pxfUnmarshalInto refuses the pair`() = runTest {
        val builder = DynamicMessage.newBuilder(descriptor(required = true))
        val e = assertFailsWith<Exception> { pxfUnmarshalInto(document, builder) }
        assertTrue(exclusive in e.message.orEmpty(), "got: ${e.message}")
    }

    @Test
    fun `pxfUnmarshalFull reports the pair as a Failure`() = runTest {
        val builder = DynamicMessage.newBuilder(descriptor(required = true))
        val result = assertIs<PxfResult.Failure>(pxfUnmarshalFull(document, builder))
        assertTrue(exclusive in result.cause.message.orEmpty(), "got: ${result.cause.message}")
    }

    @Test
    fun `required false beside a default stays accepted`() = runTest {
        val desc = descriptor(required = false)
        val f = desc.findFieldByName("f")
        assertEquals("set", pxfUnmarshal(document, desc).getField(f))
        // Omitted, the field takes its default, so the schema really did
        // bind. pxfUnmarshalFull because defaults are applied on that path.
        val omitted = assertIs<PxfResult.Success<DynamicMessage.Builder>>(
            pxfUnmarshalFull(ByteArray(0), DynamicMessage.newBuilder(desc)),
        )
        assertEquals("fallback", omitted.builder.getField(f))
    }

    /** One message `M` with a string field `f` carrying both options as raw bytes. */
    private fun descriptor(required: Boolean): Descriptor {
        val raw = ByteArrayOutputStream()
        CodedOutputStream.newInstance(raw).run {
            writeBool(EXT_REQUIRED, required)
            writeString(EXT_DEFAULT, "fallback")
            flush()
        }
        val field = FieldDescriptorProto.newBuilder()
            .setName("f")
            .setNumber(1)
            .setType(FieldDescriptorProto.Type.TYPE_STRING)
            .setOptions(FieldOptions.parseFrom(ByteString.copyFrom(raw.toByteArray())))
        val file = FileDescriptorProto.newBuilder()
            .setName("required_default.proto")
            .setPackage("required_default.v1")
            .setSyntax("proto3")
            .addMessageType(DescriptorProto.newBuilder().setName("M").addField(field))
            .build()
        return FileDescriptor.buildFrom(file, emptyArray()).findMessageTypeByName("M")
    }

    private companion object {
        // pxf/annotations.proto; the options are read off the wire by number.
        const val EXT_REQUIRED = 1314
        const val EXT_DEFAULT = 1315
    }
}
