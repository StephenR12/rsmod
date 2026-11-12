package org.rsmod.api.cache.delta

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CacheUpgradeBacklogGeneratorTest {
    @Test
    fun `generates one stable evidence-gated task for every cache change`() {
        val baseline =
            manifest(
                "233.4",
                2317,
                entry("vars", "8121", "pvp_area", "old-var", mapOf("scope" to "player")),
                entry("sounds", "300", "bell", "removed"),
            )
        val target =
            manifest(
                "234.1",
                2400,
                entry("interfaces", "500:12", "new_button", "added"),
                entry("vars", "8121", "pvp_area_v2", "new-var", mapOf("scope" to "player")),
            )

        val backlog = CacheUpgradeBacklogGenerator.generate(CacheDeltaEngine.diff(baseline, target))

        assertEquals(CacheUpgradeBacklogSummary(3, 2, 1, 0, 1, 1, 1), backlog.summary)
        assertEquals(
            listOf("interfaces:500:12", "sounds:300", "vars:8121"),
            backlog.tasks.map { "${it.domain}:${it.key}" },
        )
        assertEquals(3, backlog.tasks.map(CacheUpgradeTask::id).distinct().size)
        assertTrue(
            backlog.tasks.all { it.id.matches(Regex("cache-upgrade-[a-z0-9-]+-[a-f0-9]{32}")) }
        )

        val variableTask = backlog.tasks.last()
        assertEquals(CacheUpgradePriority.Critical, variableTask.priority)
        assertEquals(listOf("fingerprint", "symbol"), variableTask.changedFields)
        assertEquals(
            listOf("behavioral-risk", "protocol-risk", "typed-symbol-migration"),
            variableTask.riskTags,
        )
        assertTrue("risk-review:protocol-risk" in variableTask.evidenceRequirements)
        assertTrue("protocol-risk-verified" in variableTask.acceptanceCriteria)
        assertTrue("revision-233.4-regression-covered" in variableTask.acceptanceCriteria)
        assertTrue("target-234.1-contract-covered" in variableTask.acceptanceCriteria)

        val reversed =
            CacheUpgradeBacklogGenerator.generate(
                CacheDeltaEngine.diff(
                    baseline.copy(entries = baseline.entries.reversed()),
                    target.copy(entries = target.entries.reversed()),
                )
            )
        assertEquals(backlog, reversed)
        assertEquals(CacheDeltaJson.writeBacklog(backlog), CacheDeltaJson.writeBacklog(reversed))
    }

    @Test
    fun `rejects inconsistent reports before planning`() {
        val report =
            CacheDeltaEngine.diff(
                manifest("233.4", 2317),
                manifest("234.1", 2400, entry("objects", "1", "new_obj", "new")),
            )

        val error =
            assertThrows(IllegalArgumentException::class.java) {
                CacheUpgradeBacklogGenerator.generate(
                    report.copy(summary = report.summary.copy(added = 2))
                )
            }

        assertEquals("Cache delta added summary does not match its entries.", error.message)
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
    ): CacheManifestEntry = CacheManifestEntry(domain, key, symbol, fingerprint, attributes)
}
