package org.rsmod.api.cache.delta

import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ClientContractArchiveManifestExporterIntegrationTest {
    @Test
    fun `exports every selected client contract archive file`() {
        val manifest =
            ClientContractArchiveManifestExporter.export(
                cacheDirectory = Path.of(".data/cache/vanilla"),
                revision = "233.4",
                archive = 2317,
                capturedAt = "2025-10-01T10:45:07.114859Z",
            )

        val counts = manifest.entries.groupingBy(CacheManifestEntry::domain).eachCount()
        assertEquals(
            mapOf(
                "animation-bases" to 2_464,
                "animation-frames" to 175_868,
                "client-scripts" to 8_093,
                "interface-components" to 25_602,
                "jingles" to 311,
                "keyframe-sets" to 629,
                "midi-patches" to 155,
                "songs" to 830,
                "sound-effects" to 10_751,
                "vorbis-samples" to 501,
            ),
            counts,
        )
        assertEquals(
            manifest.entries.size,
            manifest.entries.map { it.domain to it.key }.toSet().size,
        )
        assertTrue(manifest.entries.all { it.fingerprint.matches(Regex("sha256:[0-9a-f]{64}")) })
        assertTrue(manifest.entries.all { it.symbol == null })
        assertTrue(manifest.entries.all { it.attributes["fileLength"]!!.toInt() >= 0 })
        assertTrue(manifest.entries.all { it.riskTags == it.riskTags.sorted() })

        val firstBase =
            manifest.entries.single { it.domain == "animation-bases" && it.key == "0:0" }
        assertEquals("1", firstBase.attributes["archiveId"])
        assertEquals("4154", firstBase.attributes["fileLength"])
        assertEquals(
            "sha256:0897c261e2367edfc680b1bef17cd3fc39c2029f2de1e1da59dec4b0247abdbb",
            firstBase.fingerprint,
        )
    }
}
