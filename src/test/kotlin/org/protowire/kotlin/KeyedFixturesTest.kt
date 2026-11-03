// SPDX-License-Identifier: MIT
// Copyright (c) 2026 TrendVidia, LLC.
package org.protowire.kotlin

import com.google.protobuf.CodedOutputStream
import com.google.protobuf.DescriptorProtos.DescriptorProto
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type
import com.google.protobuf.DescriptorProtos.FieldOptions
import com.google.protobuf.DescriptorProtos.FileDescriptorProto
import com.google.protobuf.DescriptorProtos.OneofDescriptorProto
import com.google.protobuf.Descriptors.Descriptor
import com.google.protobuf.Descriptors.FileDescriptor
import com.google.protobuf.DynamicMessage
import com.google.protobuf.Message
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * The spec repo's keyed repeated field fixtures (`testdata/keyed/`, protowire#116),
 * through this module's wrappers. Their README says a port inheriting the codec
 * verifies against the same files, and these are vendored from protowire-java
 * v1.4.0 under `src/test/resources/keyed/`.
 *
 * The `fmt-*.expected.pxf` outputs are not asserted: this module has no wrapper
 * over the formatter. Their inputs are still decoded.
 */
class KeyedFixturesTest {

    private val fixtures: List<File> =
        File(checkNotNull(javaClass.getResource("/keyed/README.md")).toURI()).parentFile
            .listFiles { f -> f.name.endsWith(".pxf") && !f.name.endsWith(".expected.pxf") }!!
            .sortedBy { it.name }

    private fun bodyOf(file: File): String = file.readLines()
        .filter { it.isNotBlank() && !it.startsWith("#") && !it.startsWith("@type") }
        .joinToString("\n", postfix = "\n")

    private fun descriptorOf(document: ByteArray): Descriptor {
        val type = checkNotNull(Regex("""^@type keyed\.v1\.(\w+)$""", RegexOption.MULTILINE).find(document.decodeToString()))
        return schema.findMessageTypeByName(type.groupValues[1])
    }

    private fun fixture(name: String): File = fixtures.single { it.name == name }

    @Test
    fun `every reject fixture is refused by both decoding wrappers`() = runTest {
        val rejects = fixtures.filter { it.name.startsWith("err-") }
        assertEquals(refusals.keys, rejects.map { it.name }.toSet(), "the fixture set changed")
        for (file in rejects) {
            val bytes = file.readBytes()
            val desc = descriptorOf(bytes)
            val because = refusals.getValue(file.name)
            val thrown = assertFailsWith<Exception>(file.name) { pxfUnmarshal(bytes, desc) }
            assertTrue(because in thrown.message.orEmpty(), "${file.name}: ${thrown.message}")
            val failed = assertIs<PxfResult.Failure>(pxfUnmarshalFull(bytes, DynamicMessage.newBuilder(desc)), file.name)
            assertTrue(because in failed.cause.message.orEmpty(), "${file.name}: ${failed.cause.message}")
        }
    }

    @Test
    fun `every accept fixture decodes through both decoding wrappers`() = runTest {
        val accepts = fixtures.filterNot { it.name.startsWith("err-") }
        assertEquals(9, accepts.size, "the fixture set changed; check this test still covers it")
        for (file in accepts) {
            val bytes = file.readBytes()
            val desc = descriptorOf(bytes)
            pxfUnmarshal(bytes, desc)
            assertIs<PxfResult.Success<DynamicMessage.Builder>>(
                pxfUnmarshalFull(bytes, DynamicMessage.newBuilder(desc)),
                file.name,
            )
        }
    }

    @Test
    fun `a keyed document keeps its entry order and fills the key field`() = runTest {
        val bytes = fixture("roundtrip-keyed.pxf").readBytes()
        val desc = descriptorOf(bytes)
        val children = desc.findFieldByName("children")
        val id = desc.findFieldByName("id")
        val m = pxfUnmarshal(bytes, desc)
        val ids = List(m.getRepeatedFieldCount(children)) { i ->
            (m.getRepeatedField(children, i) as DynamicMessage).getField(id)
        }
        assertEquals(listOf<Any>("greeting", "counter_row"), ids)
    }

    @Test
    fun `pxfMarshal writes keyed and quoted entries back as they were`() = runTest {
        for (name in listOf("roundtrip-keyed.pxf", "roundtrip-quoted.pxf")) {
            val file = fixture(name)
            val bytes = file.readBytes()
            val out = pxfMarshal(pxfUnmarshal(bytes, descriptorOf(bytes))).decodeToString()
            assertEquals(bodyOf(file), out.replace("\n\n", "\n"), name)
        }
    }

    @Test
    fun `the anonymous form is the same message as the keyed form`() = runTest {
        val keyed = fixture("roundtrip-keyed.pxf").readBytes()
        val anonymous = fixture("anonymous-equivalence.pxf").readBytes()
        assertEquals(pxfUnmarshal(keyed, descriptorOf(keyed)), pxfUnmarshal(anonymous, descriptorOf(anonymous)))
    }

    @Test
    fun `duplicate keys in the anonymous form stay anonymous`() = runTest {
        val bytes = fixture("anonymous-duplicate-ok.pxf").readBytes()
        val out = pxfMarshal(pxfUnmarshal(bytes, descriptorOf(bytes))).decodeToString()
        assertTrue("children = [\n" in out, out)
    }

    @Test
    fun `the Flow adapters carry keyed documents both ways`() = runTest {
        val file = fixture("roundtrip-keyed.pxf")
        val bytes = file.readBytes()
        val decoded = flowOf(bytes).unmarshalPxf(descriptorOf(bytes)).single()
        assertEquals(pxfUnmarshal(bytes, descriptorOf(bytes)), decoded)
        val encoded = flowOf<Message>(decoded).marshalPxf().single().decodeToString()
        assertEquals(bodyOf(file), encoded.replace("\n\n", "\n"))
    }

    private companion object {
        /** Why each reject fixture is refused, so a refusal for another reason fails. */
        val refusals = mapOf(
            "err-duplicate-key.pxf" to "duplicate key \"greeting\" in keyed field \"children\"",
            "err-duplicate-key-spelling.pxf" to "duplicate key \"greeting\" in keyed field \"children\"",
            "err-key-conflict.pxf" to "conflicts with entry name \"greeting\" in keyed field \"children\"",
            "err-key-null.pxf" to "null on key field \"name\" conflicts with entry name \"alpha\" in keyed field \"tags\"",
            "err-key-oneof-sibling.pxf" to "field \"other\" shares oneof \"pick\" with key field \"name\"",
            "err-key-oneof-sibling-null.pxf" to "field \"other\" shares oneof \"pick\" with key field \"name\"",
            "err-empty-key.pxf" to "empty entry name in keyed field \"children\"",
            "err-empty-key-anonymous.pxf" to "explicit empty-string assignment to key field \"id\"",
            "err-quoted-name-unkeyed.pxf" to "quoted entry name \"a\" is only valid inside a keyed repeated field's block",
        )

        /** `(pxf.key)`; the option is read off the wire by number. */
        const val EXT_KEY = 1316

        /** `keyed.proto` beside the fixtures, built without a `.proto` compiler. */
        val schema: FileDescriptor = run {
            val file = FileDescriptorProto.newBuilder()
                .setName("keyed.proto")
                .setPackage("keyed.v1")
                .setSyntax("proto3")
                .addMessageType(
                    message("Node")
                        .addField(string("id", 1))
                        .addField(string("type", 2))
                        .addField(field("weight", 3).setType(Type.TYPE_INT32))
                        .addField(keyed("children", 4, "Node", key = "id")),
                )
                .addMessageType(
                    message("Region")
                        .addField(string("name", 1))
                        .addField(field("replicas", 2).setType(Type.TYPE_INT32)),
                )
                .addMessageType(
                    message("Deployment")
                        .addField(string("service", 1))
                        .addField(keyed("regions", 2, "Region", key = "name")),
                )
                .addMessageType(message("Plain").addField(string("id", 1)).addField(string("label", 2)))
                .addMessageType(
                    message("Doc").addField(
                        field("items", 1)
                            .setLabel(Label.LABEL_REPEATED)
                            .setType(Type.TYPE_MESSAGE)
                            .setTypeName(".keyed.v1.Plain"),
                    ),
                )
                .addMessageType(
                    message("Choice")
                        .addOneofDecl(OneofDescriptorProto.newBuilder().setName("pick"))
                        .addField(string("name", 1).setOneofIndex(0))
                        .addField(string("other", 2).setOneofIndex(0)),
                )
                .addMessageType(
                    message("Tag")
                        .addOneofDecl(OneofDescriptorProto.newBuilder().setName("_name"))
                        .addField(string("name", 1).setProto3Optional(true).setOneofIndex(0)),
                )
                .addMessageType(
                    message("Catalog")
                        .addField(keyed("choices", 1, "Choice", key = "name"))
                        .addField(keyed("tags", 2, "Tag", key = "name")),
                )
                .build()
            FileDescriptor.buildFrom(file, emptyArray())
        }

        fun message(name: String): DescriptorProto.Builder = DescriptorProto.newBuilder().setName(name)

        fun field(name: String, number: Int): FieldDescriptorProto.Builder =
            FieldDescriptorProto.newBuilder().setName(name).setNumber(number).setLabel(Label.LABEL_OPTIONAL)

        fun string(name: String, number: Int): FieldDescriptorProto.Builder =
            field(name, number).setType(Type.TYPE_STRING)

        fun keyed(name: String, number: Int, element: String, key: String): FieldDescriptorProto.Builder {
            val raw = ByteArrayOutputStream()
            CodedOutputStream.newInstance(raw).run {
                writeString(EXT_KEY, key)
                flush()
            }
            return field(name, number)
                .setLabel(Label.LABEL_REPEATED)
                .setType(Type.TYPE_MESSAGE)
                .setTypeName(".keyed.v1.$element")
                .setOptions(FieldOptions.parseFrom(raw.toByteArray()))
        }
    }
}
