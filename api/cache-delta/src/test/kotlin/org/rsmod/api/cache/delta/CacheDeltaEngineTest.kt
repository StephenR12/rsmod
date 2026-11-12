package org.rsmod.api.cache.delta

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class CacheDeltaEngineTest {
    @Test
    fun `diff is stable across source ordering and classifies every change`() {
        val baseline =
            manifest(
                "233.4",
                2317,
                entry("vars", "8121", "pvp_area", "old-var"),
                entry("objects", "100", "old_symbol", "same"),
                entry("locations", "200", "door", "old-door", mapOf("morphs" to "201")),
                entry("sounds", "300", "bell", "removed"),
            )
        val target =
            manifest(
                "234.1",
                2400,
                entry("interfaces", "500:12", "new_button", "added"),
                entry("objects", "100", "old_symbol", "same"),
                entry("locations", "200", "door", "new-door", mapOf("morphs" to "202")),
                entry("vars", "8121", "pvp_area_v2", "new-var"),
            )

        val report = CacheDeltaEngine.diff(baseline, target)

        assertEquals(CacheDeltaSummary(1, 1, 2, 1), report.summary)
        assertEquals(
            listOf(
                "interfaces:500:12:Added",
                "locations:200:Changed",
                "sounds:300:Removed",
                "vars:8121:Changed",
            ),
            report.changes.map { "${it.domain}:${it.key}:${it.kind}" },
        )
        assertEquals(
            listOf("client-contract-risk", "typed-symbol-migration"),
            report.changes[0].riskTags,
        )
        assertEquals(
            listOf("behavioral-risk", "map-collision-risk", "morph-change"),
            report.changes[1].riskTags,
        )
        assertEquals(
            listOf("behavioral-risk", "protocol-risk", "typed-symbol-migration"),
            report.changes[3].riskTags,
        )

        val reversed =
            CacheDeltaEngine.diff(
                baseline.copy(entries = baseline.entries.reversed()),
                target.copy(entries = target.entries.reversed()),
            )
        assertEquals(report, reversed)
        assertEquals(CacheDeltaJson.writeReport(report), CacheDeltaJson.writeReport(reversed))
    }

    @Test
    fun `normalizes attribute maps and explicit risk tags`() {
        val baseline = manifest("233.4", 2317)
        val target =
            manifest(
                "234.1",
                2400,
                entry(
                    domain = "npcs",
                    key = "42",
                    symbol = "guide",
                    fingerprint = "new",
                    attributes = linkedMapOf("z" to "last", "a" to "first"),
                    riskTags = listOf("drop-table-risk", "drop-table-risk", "combat-risk"),
                ),
            )

        val change = CacheDeltaEngine.diff(baseline, target).changes.single()

        assertEquals(listOf("a", "z"), change.after?.attributes?.keys?.toList())
        assertEquals(
            listOf("combat-risk", "drop-table-risk", "typed-symbol-migration"),
            change.riskTags,
        )
    }

    @Test
    fun `rejects duplicate domain keys before diffing`() {
        val duplicate =
            manifest(
                "233.4",
                2317,
                entry("objects", "100", "first", "a"),
                entry("objects", "100", "second", "b"),
            )

        val error =
            assertThrows(IllegalArgumentException::class.java) {
                CacheDeltaEngine.diff(duplicate, manifest("234.1", 2400))
            }

        assertEquals("baseline cache manifest contains duplicate key objects:100.", error.message)
    }

    @Test
    fun `rejects incomplete or unsupported manifest metadata`() {
        val target = manifest("234.1", 2400)

        assertThrows(IllegalArgumentException::class.java) {
            CacheDeltaEngine.diff(manifest("233.4", 2317).copy(schemaVersion = 2), target)
        }
        assertThrows(IllegalArgumentException::class.java) {
            CacheDeltaEngine.diff(manifest("", 2317), target)
        }
        assertThrows(IllegalArgumentException::class.java) {
            CacheDeltaEngine.diff(manifest("233.4", 2317, entry("objects", "1", "obj", "")), target)
        }
    }

    private fun manifest(
        revision: String,
        archive: Int,
        vararg entries: CacheManifestEntry,
    ): CacheManifest =
        CacheManifest(
            revision = revision,
            archive = archive,
            capturedAt = "$revision-capture",
            entries = entries.toList(),
        )

    private fun entry(
        domain: String,
        key: String,
        symbol: String,
        fingerprint: String,
        attributes: Map<String, String> = emptyMap(),
        riskTags: List<String> = emptyList(),
    ): CacheManifestEntry =
        CacheManifestEntry(domain, key, symbol, fingerprint, attributes, riskTags)
}
