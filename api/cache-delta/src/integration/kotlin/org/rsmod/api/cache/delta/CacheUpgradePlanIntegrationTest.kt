package org.rsmod.api.cache.delta

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class CacheUpgradePlanIntegrationTest {
    @TempDir lateinit var tempDir: Path

    @Test
    fun `production command writes a byte-stable atomic backlog`() {
        val baselinePath = tempDir.resolve("baseline.json")
        val targetPath = tempDir.resolve("target.json")
        val firstOutput = tempDir.resolve("first-backlog.json")
        val secondOutput = tempDir.resolve("second-backlog.json")
        CacheDeltaJson.writeManifest(
            baselinePath,
            manifest("233.4", 2317, entry("locations", "200", "old-door", "before")),
        )
        CacheDeltaJson.writeManifest(
            targetPath,
            manifest(
                "234.1",
                2400,
                entry("locations", "200", "new-door", "after"),
                entry("objects", "300", "new-object", "added"),
            ),
        )

        val firstResult = runPlan(baselinePath, targetPath, firstOutput)
        val secondResult = runPlan(baselinePath, targetPath, secondOutput)

        assertEquals(0, firstResult)
        assertEquals(0, secondResult)
        assertEquals(Files.readString(firstOutput), Files.readString(secondOutput))
        val backlog = CacheDeltaJson.readBacklog(firstOutput)
        assertEquals(CacheUpgradeBacklogSummary(2, 1, 1, 0, 1, 0, 1), backlog.summary)
        assertEquals(
            listOf("locations:200", "objects:300"),
            backlog.tasks.map { "${it.domain}:${it.key}" },
        )
        assertTrue(
            backlog.tasks.all { "official-wiki-or-update-evidence" in it.evidenceRequirements }
        )
        assertTrue(
            backlog.tasks.all { "full-unit-and-integration-gates-pass" in it.acceptanceCriteria }
        )
    }

    private fun runPlan(baseline: Path, target: Path, output: Path): Int =
        CacheUpgradePlanMain.run(
            arrayOf("plan-upgrade", baseline.toString(), target.toString(), output.toString())
        )

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
    ): CacheManifestEntry =
        CacheManifestEntry(
            domain = domain,
            key = key,
            symbol = symbol,
            fingerprint = fingerprint,
            riskTags =
                when (domain) {
                    "locations" -> listOf("map-collision-risk")
                    else -> listOf("item-contract-risk")
                },
        )
}
