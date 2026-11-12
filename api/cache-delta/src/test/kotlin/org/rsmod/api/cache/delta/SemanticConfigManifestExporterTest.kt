package org.rsmod.api.cache.delta

import io.netty.buffer.Unpooled
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.game.type.proj.UnpackedProjAnimType
import org.rsmod.game.type.varbit.UnpackedVarBitType
import org.rsmod.game.type.varp.UnpackedVarpType
import org.rsmod.game.type.varp.VarpLifetime
import org.rsmod.game.type.varp.VarpTransmitLevel

class SemanticConfigManifestExporterTest {
    @Test
    fun `varclient entries preserve cache persistence and reject unknown opcodes`() {
        val transient = Unpooled.wrappedBuffer(byteArrayOf(0))
        val persistent = Unpooled.wrappedBuffer(byteArrayOf(2, 0))
        try {
            assertEquals(
                false,
                SemanticConfigManifestExporter.decodeVarClientPersistence(1, transient),
            )
            assertEquals(
                true,
                SemanticConfigManifestExporter.decodeVarClientPersistence(2, persistent),
            )
        } finally {
            transient.release()
            persistent.release()
        }

        val unknownOpcode = Unpooled.wrappedBuffer(byteArrayOf(3, 0))
        try {
            val error =
                assertThrows(IllegalStateException::class.java) {
                    SemanticConfigManifestExporter.decodeVarClientPersistence(3, unknownOpcode)
                }
            assertEquals("Varclient 3 has unsupported opcode 3.", error.message)
        } finally {
            unknownOpcode.release()
        }

        val entry =
            SemanticConfigManifestExporter.varClientEntry(
                id = 2,
                symbol = "persistent_setting",
                encoded = byteArrayOf(2, 0),
                persistent = true,
            )
        assertEquals("varclients", entry.domain)
        assertEquals("true", entry.attributes["persistent"])
        assertEquals(listOf("client-contract-risk", "state-transition-risk"), entry.riskTags)
    }

    @Test
    fun `varp and varbit entries preserve state and client contracts`() {
        val varp =
            UnpackedVarpType(
                bitProtect = true,
                clientCode = 42,
                internalScope = VarpLifetime.Perm,
                internalTransmit = VarpTransmitLevel.OnSetAlways,
                internalId = 10,
                internalName = "test_varp",
            )
        val varbit =
            UnpackedVarBitType(
                varpId = 10,
                internalVarp = varp,
                internalLsb = 3,
                internalMsb = 6,
                internalId = 11,
                internalName = "test_varbit",
            )

        val varpEntry =
            SemanticConfigManifestExporter.varpEntry(
                id = 10,
                type = varp,
                symbol = "test_varp",
                encoded = byteArrayOf(1),
            )
        val varbitEntry =
            SemanticConfigManifestExporter.varbitEntry(
                id = 11,
                type = varbit,
                symbol = "test_varbit",
                encoded = byteArrayOf(2),
            )

        assertEquals("42", varpEntry.attributes["clientCode"])
        assertEquals("Perm", varpEntry.attributes["scope"])
        assertEquals("10", varbitEntry.attributes["baseVar"])
        assertEquals("3", varbitEntry.attributes["leastSignificantBit"])
        assertEquals("6", varbitEntry.attributes["mostSignificantBit"])
        assertEquals("4", varbitEntry.attributes["width"])
        assertTrue("client-contract-risk" in varbitEntry.riskTags)
        assertTrue("state-transition-risk" in varbitEntry.riskTags)
    }

    @Test
    fun `canonical values are stable across map and set ordering`() {
        val first = mapOf(2 to linkedSetOf("z", "a"), 1 to intArrayOf(3, 2, 1))
        val second = linkedMapOf(1 to intArrayOf(3, 2, 1), 2 to linkedSetOf("a", "z"))

        assertEquals(
            SemanticConfigManifestExporter.canonical(first),
            SemanticConfigManifestExporter.canonical(second),
        )
        assertEquals("{1=[3, 2, 1], 2=[a, z]}", SemanticConfigManifestExporter.canonical(first))
    }

    @Test
    fun `projectile entry preserves every timing and trajectory field`() {
        val type =
            UnpackedProjAnimType(
                startHeight = 43,
                endHeight = 31,
                delay = 4,
                angle = 16,
                lengthAdjustment = -3,
                progress = 7,
                stepMultiplier = 11,
                internalId = 5,
                internalName = "bolt",
            )

        val entry =
            SemanticConfigManifestExporter.projectileEntry(
                id = 5,
                type = type,
                symbol = "bolt",
                encoded = byteArrayOf(1, 43, 0),
            )

        assertEquals("projectiles", entry.domain)
        assertEquals("5", entry.key)
        assertEquals("bolt", entry.symbol)
        assertEquals("43", entry.attributes["startHeight"])
        assertEquals("31", entry.attributes["endHeight"])
        assertEquals("4", entry.attributes["delay"])
        assertEquals("16", entry.attributes["angle"])
        assertEquals("-3", entry.attributes["lengthAdjustment"])
        assertEquals("7", entry.attributes["progress"])
        assertEquals("11", entry.attributes["stepMultiplier"])
        assertEquals(listOf("client-contract-risk", "projectile-timing-risk"), entry.riskTags)
    }
}
