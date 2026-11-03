// SPDX-License-Identifier: MIT
// Copyright (c) 2026 TrendVidia, LLC.
package org.protowire.kotlin

import com.google.protobuf.DescriptorProtos.FileDescriptorProto
import com.google.protobuf.DescriptorProtos.FileDescriptorSet
import com.google.protobuf.Descriptors.Descriptor
import com.google.protobuf.Descriptors.FileDescriptor
import com.google.protobuf.DynamicMessage
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import java.io.File
import kotlin.test.assertEquals

/**
 * What this module writes, against protowire-go, the reference. The cases are
 * the spec repo's `testdata/text/`, vendored under `src/test/resources/text/`:
 * protobuf bytes, and the PXF protowire-go writes for them. The bytes are read
 * with the stock protobuf runtime, so a difference is the marshaller's.
 *
 * `scripts/check_go_goldens.sh` runs in CI and shows that the goldens are what
 * the pinned protowire-go writes. To refresh them, copy `testdata/text/` at a
 * newer spec commit and move the pins in `.github/workflows/ci.yml`.
 */
class GoConformanceTest {

    private val cases: List<File> =
        File(checkNotNull(javaClass.getResource("/text/text.proto")).toURI()).resolveSibling("cases")
            .listFiles { f -> f.name.endsWith(".binpb") }!!
            .sortedBy { it.name }

    private fun nameOf(input: File) = input.name.removeSuffix(".binpb")

    private fun golden(input: File) = input.resolveSibling("${nameOf(input)}.expected.pxf")

    private fun descriptorOf(input: File): Descriptor =
        checkNotNull(schema.findMessageTypeByName(nameOf(input).substringBefore('.')))

    private fun eachCase(check: suspend (File) -> Unit): List<DynamicTest> {
        assertEquals(CASES, cases.size, "the corpus changed; update the count deliberately")
        return cases.map { input -> dynamicTest(nameOf(input)) { runTest { check(input) } } }
    }

    @TestFactory
    fun `pxfMarshal writes what protowire-go writes`() = eachCase { input ->
        val message = DynamicMessage.parseFrom(descriptorOf(input), input.readBytes())
        assertEquals(golden(input).readText(), pxfMarshal(message).decodeToString())
    }

    @TestFactory
    fun `pxfUnmarshal reads what protowire-go writes back to the same message`() = eachCase { input ->
        if (nameOf(input) !in NORMALISED) {
            val message = DynamicMessage.parseFrom(descriptorOf(input), input.readBytes())
            assertEquals(message, pxfUnmarshal(golden(input).readBytes(), descriptorOf(input)))
        }
    }

    /** Covers [NORMALISED] too: the stored form may change, the value may not. */
    @TestFactory
    fun `text protowire-go writes survives a round trip unchanged`() = eachCase { input ->
        val text = golden(input).readBytes()
        assertEquals(text.decodeToString(), pxfMarshal(pxfUnmarshal(text, descriptorOf(input))).decodeToString())
    }

    private companion object {
        const val CASES = 56

        /**
         * The same value comes back in another stored form, as it does from
         * protowire-go. A BigFloat literal is stored as a prec-bit significand,
         * and `dec = 25000` has no trace of the input's negative scale.
         */
        val NORMALISED = setOf(
            "WellKnown.big-float",
            "WellKnown.big-float-large",
            "WellKnown.big-float-negative",
            "WellKnown.decimal-negative-scale",
        )

        /** `text.binpb` holds the schema and every import, dependencies first. */
        val schema: FileDescriptor = run {
            val raw = checkNotNull(GoConformanceTest::class.java.getResourceAsStream("/text/text.binpb"))
            val built = mutableMapOf<String, FileDescriptor>()
            val files = raw.use { FileDescriptorSet.parseFrom(it) }.fileList
            files.forEach { file: FileDescriptorProto ->
                val imports = file.dependencyList.map(built::getValue).toTypedArray()
                built[file.name] = FileDescriptor.buildFrom(file, imports)
            }
            built.getValue("text.proto")
        }
    }
}
