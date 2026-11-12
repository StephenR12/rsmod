package org.rsmod.api.cache.delta

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.game.type.obj.ObjTypeBuilder
import org.rsmod.game.type.util.ParamMap

class CoreConfigManifestExporterTest {
    @Test
    fun `object inventory preserves operations parameters symbols and stable fingerprint`() {
        val type =
            ObjTypeBuilder("test_item").apply {
                name = "Test item"
                desc = "A cache-backed test item."
                members = true
                wearpos1 = 3
                op[2] = "Take"
                iop[1] = "Wear"
                isubop2 = arrayOf("Inspect", "Configure")
                paramMap = ParamMap(mapOf(42 to "answer"))
            }
        val entry =
            CoreConfigManifestExporter.objectEntry(
                id = 100,
                type = type.build(100),
                symbol = "test_item",
                encoded = "abc".encodeToByteArray(),
            )

        assertEquals("objects", entry.domain)
        assertEquals("100", entry.key)
        assertEquals("test_item", entry.symbol)
        assertEquals(
            "sha256:ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            entry.fingerprint,
        )
        assertEquals("Take", entry.attributes["groundOp.3"])
        assertEquals("Wear", entry.attributes["inventoryOp.2"])
        assertEquals("Configure", entry.attributes["inventorySubOp.2.2"])
        assertEquals("answer", entry.attributes["param.42"])
        assertEquals("true", entry.attributes["members"])
        assertTrue("equipment-risk" in entry.riskTags)
        assertTrue("interaction-risk" in entry.riskTags)
        assertTrue("parameter-risk" in entry.riskTags)
    }

    @Test
    fun `manifest json is byte stable across entry attribute and risk ordering`() {
        val first =
            CacheManifestEntry(
                domain = "objects",
                key = "2",
                fingerprint = "sha256:b",
                attributes = linkedMapOf("z" to "last", "a" to "first"),
                riskTags = listOf("z-risk", "a-risk", "z-risk"),
            )
        val second = CacheManifestEntry(domain = "locations", key = "1", fingerprint = "sha256:a")
        val manifest =
            CacheManifest(
                revision = "233.4",
                archive = 2317,
                capturedAt = "2025-10-01T10:45:07.114859Z",
                entries = listOf(first, second),
            )

        val shuffled =
            manifest.copy(
                entries =
                    listOf(
                        second,
                        first.copy(
                            attributes = linkedMapOf("a" to "first", "z" to "last"),
                            riskTags = listOf("a-risk", "z-risk"),
                        ),
                    )
            )

        assertEquals(CacheDeltaJson.writeManifest(manifest), CacheDeltaJson.writeManifest(shuffled))
    }
}
