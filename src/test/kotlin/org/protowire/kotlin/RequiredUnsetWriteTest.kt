// SPDX-License-Identifier: MIT
// Copyright (c) 2026 TrendVidia, LLC.
package org.protowire.kotlin

import com.google.protobuf.ByteString
import com.google.protobuf.CodedOutputStream
import com.google.protobuf.DescriptorProtos.DescriptorProto
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto
import com.google.protobuf.DescriptorProtos.FieldOptions
import com.google.protobuf.DescriptorProtos.FileDescriptorProto
import com.google.protobuf.DescriptorProtos.OneofDescriptorProto
import com.google.protobuf.Descriptors.Descriptor
import com.google.protobuf.Descriptors.FileDescriptor
import com.google.protobuf.DynamicMessage
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * An unset `optional` field marked `(pxf.required)` has no text: a reader
 * refuses it absent and refuses it null. protowire-go refuses to write it,
 * and protowire-java does the same from 2.0 (java#187). The text cases in
 * `GoConformanceTest` cover the unset annotated fields that can be written;
 * this pins the one that cannot, through `pxfMarshal`.
 */
class RequiredUnsetWriteTest {

    private val message = descriptor()
    private val n = message.findFieldByName("n")

    @Test
    fun `pxfMarshal refuses an unset required optional field as protowire-go does`() = runTest {
        val e = assertFailsWith<Exception> { pxfMarshal(DynamicMessage.newBuilder(message).build()) }
        assertTrue(
            "pxf: required_unset.v1.M.n is unset, and the field is (pxf.required)" in e.message.orEmpty(),
            "got: ${e.message}",
        )
    }

    /** The control: the same schema writes once the field is set, even to zero. */
    @Test
    fun `pxfMarshal writes the field once it is set`() = runTest {
        val msg = DynamicMessage.newBuilder(message).setField(n, 0).build()
        assertEquals("n = 0\n", pxfMarshal(msg).decodeToString())
    }

    /** `M { optional int32 n = 1 [(pxf.required) = true]; }`, the option as raw bytes. */
    private fun descriptor(): Descriptor {
        val raw = ByteArrayOutputStream()
        CodedOutputStream.newInstance(raw).run {
            writeBool(EXT_REQUIRED, true)
            flush()
        }
        val field = FieldDescriptorProto.newBuilder()
            .setName("n")
            .setNumber(1)
            .setLabel(FieldDescriptorProto.Label.LABEL_OPTIONAL)
            .setType(FieldDescriptorProto.Type.TYPE_INT32)
            .setProto3Optional(true)
            .setOneofIndex(0)
            .setOptions(FieldOptions.parseFrom(ByteString.copyFrom(raw.toByteArray())))
        val m = DescriptorProto.newBuilder()
            .setName("M")
            .addField(field)
            .addOneofDecl(OneofDescriptorProto.newBuilder().setName("_n"))
        val file = FileDescriptorProto.newBuilder()
            .setName("required_unset.proto")
            .setPackage("required_unset.v1")
            .setSyntax("proto3")
            .addMessageType(m)
            .build()
        return FileDescriptor.buildFrom(file, emptyArray()).findMessageTypeByName("M")
    }

    private companion object {
        // pxf/annotations.proto; the option is read off the wire by number.
        const val EXT_REQUIRED = 1314
    }
}
