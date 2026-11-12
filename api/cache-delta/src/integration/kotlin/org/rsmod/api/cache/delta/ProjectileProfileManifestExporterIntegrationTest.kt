package org.rsmod.api.cache.delta

import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ProjectileProfileManifestExporterIntegrationTest {
    @Test
    fun `exports every rsmod enriched projectile profile`() {
        val manifest =
            ProjectileProfileManifestExporter.export(
                cacheDirectory = Path.of(".data/cache/game"),
                symbolDirectory = Path.of(".data/symbols"),
                revision = "233.4-rsmod-enriched",
                archive = 2317,
                capturedAt = "2025-10-01T10:45:07.114859Z",
            )

        assertEquals(16, manifest.entries.size)
        assertEquals(16, manifest.entries.map(CacheManifestEntry::symbol).toSet().size)
        assertTrue(manifest.entries.all { it.domain == "projectiles" })
        assertTrue(manifest.entries.all { it.symbol != null })

        val arrow = manifest.entries.single { it.key == "0" }
        assertEquals("arrow", arrow.symbol)
        assertEquals("163", arrow.attributes["startHeight"])
        assertEquals("146", arrow.attributes["endHeight"])
        assertEquals("41", arrow.attributes["delay"])
        assertEquals("15", arrow.attributes["angle"])
        assertEquals("5", arrow.attributes["lengthAdjustment"])
        assertEquals("11", arrow.attributes["progress"])
        assertEquals("5", arrow.attributes["stepMultiplier"])
        assertEquals(listOf("client-contract-risk", "projectile-timing-risk"), arrow.riskTags)
        assertTrue(arrow.fingerprint.matches(Regex("sha256:[0-9a-f]{64}")))

        val last = manifest.entries.single { it.key == "15" }
        assertEquals("tumekens_shadow", last.symbol)
    }
}
