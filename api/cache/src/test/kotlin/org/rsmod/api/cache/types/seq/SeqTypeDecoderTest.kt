package org.rsmod.api.cache.types.seq

import io.netty.buffer.Unpooled
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SeqTypeDecoderTest {
    @Test
    fun `opcode 16 decodes signed vertical offset`() {
        val positive = SeqTypeDecoder.decode(Unpooled.wrappedBuffer(byteArrayOf(16, 12, 0)))
        val negative = SeqTypeDecoder.decode(Unpooled.wrappedBuffer(byteArrayOf(16, -12, 0)))

        assertEquals(12, positive.verticalOffset)
        assertEquals(-12, negative.verticalOffset)
    }
}
