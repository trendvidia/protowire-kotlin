// SPDX-License-Identifier: MIT
// Copyright (c) 2026 TrendVidia, LLC.
package org.protowire.kotlin

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Test
import org.protowire.envelope.Decoder
import org.protowire.envelope.Encoder
import org.protowire.envelope.v1.Envelope
import org.protowire.envelope.v1.FieldError
import org.protowire.pxf.DatasetReader
import org.protowire.pxf.DecodeLimits
import org.protowire.pxf.MarshalOptions
import org.protowire.pxf.Pxf
import org.protowire.pxf.UnmarshalOptions
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * The README's "Calling the Java API directly" examples, run as written.
 * Options and stream readers have no Kotlin wrapper (#30), so these pin the
 * Java calls the README tells Kotlin callers to make: a rename in
 * protowire-java fails here before the README goes stale.
 */
class JavaOnlyApisTest {

    private val desc = FieldError.getDescriptor()
    private val err = FieldError.newBuilder().setField("amount").setCode("MIN_VALUE").build()

    @Test
    fun `decode options reach the Java call`() = runTest {
        val bytes = pxfMarshal(err)
        val opts = UnmarshalOptions.defaults()
            .withLimits(DecodeLimits.defaults().withMaxMessageSize(8))
        val e = assertFailsWith<Exception> { withContext(Dispatchers.IO) { opts.unmarshal(bytes, desc) } }
        assertTrue("exceeds MaxMessageSize=8" in e.message.orEmpty(), "got: ${e.message}")
        // The same bytes decode under the defaults, so the limit did the refusing.
        assertEquals(err, FieldError.parseFrom(pxfUnmarshal(bytes, desc).toByteArray()))
    }

    @Test
    fun `encode options reach the Java call`() = runTest {
        val text = withContext(Dispatchers.IO) {
            MarshalOptions.defaults().withEmitDefaults(true).marshal(err)
        }.decodeToString()
        assertTrue("message = \"\"" in text, "got: $text")
        assertTrue("message" !in pxfMarshal(err).decodeToString())
    }

    private fun envelopes(input: InputStream): Flow<Envelope> = flow {
        input.use {
            val decoder = Decoder(it)
            while (true) emit(decoder.decode() ?: break)
        }
    }.flowOn(Dispatchers.IO)

    @Test
    fun `an envelope stream becomes a Flow`() = runTest {
        val sent = listOf(envelope { status = 200 }, envelope { status = 404; transportError = "gone" })
        val out = ByteArrayOutputStream()
        Encoder(out).let { enc -> sent.forEach(enc::encode) }
        assertEquals(sent, envelopes(ByteArrayInputStream(out.toByteArray())).toList())
    }

    private fun rows(input: InputStream): Flow<FieldError> = flow {
        input.use {
            val reader = DatasetReader(it)
            while (true) {
                val row = FieldError.newBuilder()
                if (!reader.scan(row)) break
                emit(row.build())
            }
        }
    }.flowOn(Dispatchers.IO)

    @Test
    fun `a dataset becomes a Flow of rows`() = runTest {
        val sent = listOf(err, FieldError.newBuilder().setField("currency").setCode("REQUIRED").build())
        val columns = listOf("field", "code")
        val doc = StringBuilder()
        Pxf.appendDatasetHeader(doc, desc, columns)
        sent.forEach { Pxf.appendDatasetRow(doc, it, columns) }
        assertEquals(sent, rows(ByteArrayInputStream(doc.toString().toByteArray())).toList())
    }
}
