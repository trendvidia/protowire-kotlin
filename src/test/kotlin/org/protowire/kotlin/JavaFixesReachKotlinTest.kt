// SPDX-License-Identifier: MIT
// Copyright (c) 2026 TrendVidia, LLC.
package org.protowire.kotlin

import com.google.protobuf.ByteString
import com.google.protobuf.DescriptorProtos.DescriptorProto
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type
import com.google.protobuf.DescriptorProtos.FileDescriptorProto
import com.google.protobuf.DescriptorProtos.MessageOptions
import com.google.protobuf.Descriptors.Descriptor
import com.google.protobuf.Descriptors.FileDescriptor
import com.google.protobuf.DynamicMessage
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.protowire.proto.pxf.BigFloat
import org.protowire.proto.pxf.Bignum
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * The protowire-java fixes that say "covers Kotlin" (java#102, #103, #104
 * and #105), checked through this module's wrappers. Each rule is tested in
 * Java. What these pin is that the Java release this module resolves
 * carries it, so a dependency that fell behind fails here.
 *
 * The cases come from the spec corpora Java vendors under
 * `pxf/src/test/resources/corpus/`.
 */
class JavaFixesReachKotlinTest {

    private val message = descriptor()

    /** A document breaking the rule is refused by both decoding wrappers. */
    private suspend fun assertRefused(document: String, because: String) {
        val bytes = document.toByteArray()
        val thrown = assertFailsWith<Exception> { pxfUnmarshal(bytes, message) }
        assertTrue(because in thrown.message.orEmpty(), "got: ${thrown.message}")
        val failed = assertIs<PxfResult.Failure>(pxfUnmarshalFull(bytes, DynamicMessage.newBuilder(message)))
        assertTrue(because in failed.cause.message.orEmpty(), "got: ${failed.cause.message}")
    }

    @Test
    fun `the schema decodes a document breaking none of the rules`() = runTest {
        val m = pxfUnmarshal("s = \"a\"\nlabels = { a: \"1\"\n b: \"2\" }\n".toByteArray(), message)
        assertEquals("a", m.getField(message.findFieldByName("s")))
        assertEquals(2, m.getRepeatedFieldCount(message.findFieldByName("labels")))
    }

    @Test
    fun `java 102 null on a plain scalar is refused`() = runTest {
        assertRefused("s = null\n", "null")
    }

    @Test
    fun `java 102 a raw line feed in a simple string is refused`() = runTest {
        assertRefused("s = \"a\nb\"\n", "newline")
    }

    @Test
    fun `java 103 a singular field bound twice is refused`() = runTest {
        assertRefused("s = \"a\"\ns = \"b\"\n", "\"s\"")
    }

    @Test
    fun `java 105 a map key bound twice is refused`() = runTest {
        assertRefused("labels = { a: \"1\"\n a: \"2\" }\n", "\"a\"")
    }

    /** Corpus case `enc-2pow1024-prec64`: 2^1024 at 64 bits. */
    @Test
    fun `java 104 a BigFloat exponent is written as a power of two`() = runTest {
        val value = BigFloat.newBuilder()
            .setMantissa(ByteString.copyFrom(byteArrayOf(1)))
            .setExponent(1024)
            .setPrec(64)
            .build()
        val msg = DynamicMessage.newBuilder(message)
            .setField(message.findFieldByName("bf"), value)
            .build()
        assertEquals("bf = 1.79769313486231590773e+308p64", pxfMarshal(msg).decodeToString().trim())
    }

    /** The other direction: the literal reads back as 2^63 × 2^961, not a power of ten. */
    @Test
    fun `java 104 a BigFloat literal is read as a power of two`() = runTest {
        val bf = message.findFieldByName("bf")
        val decoded = pxfUnmarshal("bf = 1.79769313486231590773e+308p64\n".toByteArray(), message)
        val value = BigFloat.parseFrom((decoded.getField(bf) as DynamicMessage).toByteArray())
        assertEquals(64, value.prec)
        assertEquals(961, value.exponent)
        assertEquals("8000000000000000", value.mantissa.toByteArray().toHexString())
    }

    /** `M { string s; map<string, string> labels; pxf.BigFloat bf; }`, built without a `.proto` compiler. */
    private fun descriptor(): Descriptor {
        val entry = DescriptorProto.newBuilder()
            .setName("LabelsEntry")
            .setOptions(MessageOptions.newBuilder().setMapEntry(true))
            .addField(field("key", 1).setType(Type.TYPE_STRING))
            .addField(field("value", 2).setType(Type.TYPE_STRING))
        val m = DescriptorProto.newBuilder()
            .setName("M")
            .addNestedType(entry)
            .addField(field("s", 1).setType(Type.TYPE_STRING))
            .addField(
                field("labels", 2)
                    .setLabel(Label.LABEL_REPEATED)
                    .setType(Type.TYPE_MESSAGE)
                    .setTypeName(".java_fixes.v1.M.LabelsEntry"),
            )
            .addField(field("bf", 3).setType(Type.TYPE_MESSAGE).setTypeName(".pxf.BigFloat"))
        val file = FileDescriptorProto.newBuilder()
            .setName("java_fixes.proto")
            .setPackage("java_fixes.v1")
            .setSyntax("proto3")
            .addDependency(Bignum.getDescriptor().name)
            .addMessageType(m)
            .build()
        return FileDescriptor.buildFrom(file, arrayOf(Bignum.getDescriptor())).findMessageTypeByName("M")
    }

    private fun field(name: String, number: Int): FieldDescriptorProto.Builder =
        FieldDescriptorProto.newBuilder().setName(name).setNumber(number).setLabel(Label.LABEL_OPTIONAL)
}
