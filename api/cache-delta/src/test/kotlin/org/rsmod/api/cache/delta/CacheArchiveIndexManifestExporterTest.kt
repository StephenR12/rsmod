package org.rsmod.api.cache.delta

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.openrs2.cache.Js5Index
import org.openrs2.cache.Js5Protocol

class CacheArchiveIndexManifestExporterTest {
    @Test
    fun `entry fingerprints exact ordered group and file metadata`() {
        val index = Js5Index(Js5Protocol.VERSIONED)
        val group =
            index.createOrGet(7).apply {
                version = 11
                checksum = -1
                uncompressedChecksum = Int.MIN_VALUE
                length = 123
                uncompressedLength = 456
                nameHash = 789
                createOrGet(5).nameHash = 55
                createOrGet(1).nameHash = 11
            }

        val entry = CacheArchiveIndexManifestExporter.entry(2, group)

        assertEquals("cache-archive-index", entry.domain)
        assertEquals("2:7", entry.key)
        assertEquals("4294967295", entry.attributes["groupChecksum"])
        assertEquals("2147483648", entry.attributes["groupUncompressedChecksum"])
        assertEquals("2", entry.attributes["fileCount"])
        assertEquals("[1:11, 5:55]", entry.attributes["files"])
        assertEquals(listOf("archive-identity-risk", "client-contract-risk"), entry.riskTags)

        val changed = CacheArchiveIndexManifestExporter.entry(2, group.apply { version++ })
        assertNotEquals(entry.fingerprint, changed.fingerprint)
    }
}
