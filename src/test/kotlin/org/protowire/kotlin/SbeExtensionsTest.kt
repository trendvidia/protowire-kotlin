// SPDX-License-Identifier: MIT
// Copyright (c) 2026 TrendVidia, LLC.
package org.protowire.kotlin

import com.google.protobuf.DescriptorProtos.DescriptorProto
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto
import com.google.protobuf.DescriptorProtos.FileDescriptorProto
import com.google.protobuf.DescriptorProtos.FileOptions
import com.google.protobuf.DescriptorProtos.MessageOptions
import com.google.protobuf.Descriptors.FileDescriptor
import com.google.protobuf.DynamicMessage
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.protowire.proto.sbe.Annotations
import org.protowire.sbe.Codec
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

/**
 * The suspending SBE wrappers on [Codec]. The oracle for the bytes is the
 * SBE layout written out by hand: an 8-byte little-endian header
 * (blockLength, templateId, schemaId, version), then the root block.
 */
class SbeExtensionsTest {

    /** `Quote { int64 price = 1; uint32 qty = 2; }`, schema 7, template 3. */
    private val file = FileDescriptor.buildFrom(
        FileDescriptorProto.newBuilder()
            .setName("sbe_wrappers.proto")
            .setPackage("sbe_wrappers.v1")
            .setSyntax("proto3")
            .setOptions(FileOptions.newBuilder().setExtension(Annotations.schemaId, 7).setExtension(Annotations.version, 0))
            .addMessageType(
                DescriptorProto.newBuilder()
                    .setName("Quote")
                    .setOptions(MessageOptions.newBuilder().setExtension(Annotations.templateId, 3))
                    .addField(field("price", 1, FieldDescriptorProto.Type.TYPE_INT64))
                    .addField(field("qty", 2, FieldDescriptorProto.Type.TYPE_UINT32)),
            )
            .build(),
        emptyArray(),
    )
    private val quote = file.findMessageTypeByName("Quote")
    private val codec = Codec.of(file)
    private val msg = DynamicMessage.newBuilder(quote)
        .setField(quote.findFieldByName("price"), -15_000L)
        .setField(quote.findFieldByName("qty"), 100)
        .build()

    private val wire: ByteArray = ByteBuffer.allocate(8 + 12).order(ByteOrder.LITTLE_ENDIAN)
        .putShort(12).putShort(3).putShort(7).putShort(0) // header
        .putLong(-15_000L).putInt(100) // root block
        .array()

    @Test
    fun `marshalSuspending writes the SBE layout`() = runTest {
        assertContentEquals(wire, codec.marshalSuspending(msg))
    }

    @Test
    fun `unmarshalSuspending fills the builder it is given`() = runTest {
        assertEquals(msg, codec.unmarshalSuspending(wire, DynamicMessage.newBuilder(quote)).build())
    }

    @Test
    fun `unmarshalDescriptorSuspending reads the message`() = runTest {
        assertEquals(msg, codec.unmarshalDescriptorSuspending(wire, quote))
    }

    @Test
    fun `viewSuspending reads fields in place`() = runTest {
        val view = codec.viewSuspending(wire)
        assertEquals(-15_000L, view.intValue("price"))
        assertEquals(100L, view.uintValue("qty"))
    }

    private fun field(name: String, number: Int, type: FieldDescriptorProto.Type) =
        FieldDescriptorProto.newBuilder().setName(name).setNumber(number).setType(type)
            .setLabel(FieldDescriptorProto.Label.LABEL_OPTIONAL)
}
