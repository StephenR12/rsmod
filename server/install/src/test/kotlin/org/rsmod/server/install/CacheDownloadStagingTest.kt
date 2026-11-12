package org.rsmod.server.install

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class CacheDownloadStagingTest {
    @TempDir lateinit var tempDir: Path

    @Test
    fun `incomplete staged download preserves accepted cache and xteas`() {
        val targetCache = createCache(tempDir.resolve("vanilla"), "accepted")
        val targetXteas = write(tempDir.resolve("xteas.json"), "accepted-xteas")
        val stagedCache = tempDir.resolve("staged-vanilla")
        Files.createDirectories(stagedCache)
        write(stagedCache.resolve("main_file_cache.dat2"), "partial")
        val stagedXteas = write(tempDir.resolve("staged-xteas.json"), "new-xteas")

        assertThrows(IllegalArgumentException::class.java) {
            CacheDownloadStaging.install(
                stagedCache,
                stagedXteas,
                targetCache,
                targetXteas,
                verifyIdentity = { _, _ -> error("Incomplete download must not be verified.") },
            )
        }

        assertEquals("accepted", Files.readString(targetCache.resolve("main_file_cache.dat2")))
        assertEquals("accepted-xteas", Files.readString(targetXteas))
    }

    @Test
    fun `complete staged download replaces cache and xteas together`() {
        val targetCache = createCache(tempDir.resolve("vanilla"), "accepted")
        val targetXteas = write(tempDir.resolve("xteas.json"), "accepted-xteas")
        val stagedCache = createCache(tempDir.resolve("staged-vanilla"), "new")
        val stagedXteas = write(tempDir.resolve("staged-xteas.json"), "new-xteas")

        CacheDownloadStaging.install(
            stagedCache,
            stagedXteas,
            targetCache,
            targetXteas,
            verifyIdentity = { _, _ -> },
        )

        assertEquals("new", Files.readString(targetCache.resolve("main_file_cache.dat2")))
        assertEquals("new-xteas", Files.readString(targetXteas))
        assertFalse(Files.exists(stagedCache))
        assertFalse(Files.exists(stagedXteas))
    }

    @Test
    fun `startup recovery restores accepted pair after interrupted promotion`() {
        val targetCache = createCache(tempDir.resolve("vanilla"), "accepted")
        val targetXteas = write(tempDir.resolve("xteas.json"), "accepted-xteas")
        val backup = tempDir.resolve(".cache-backup-interrupted")
        Files.createDirectories(backup)
        write(backup.resolve("promotion-state"), "cacheExisted=true\nxteaExisted=true\n")
        Files.move(targetCache, backup.resolve("vanilla"))
        Files.move(targetXteas, backup.resolve("xteas.json"))
        createCache(targetCache, "uncommitted-new")

        CacheDownloadStaging.recoverInterruptedPromotion(targetCache, targetXteas)

        assertEquals("accepted", Files.readString(targetCache.resolve("main_file_cache.dat2")))
        assertEquals("accepted-xteas", Files.readString(targetXteas))
        assertFalse(Files.exists(backup))
    }

    @Test
    fun `startup recovery keeps committed pair and discards backup`() {
        val targetCache = createCache(tempDir.resolve("vanilla"), "new")
        val targetXteas = write(tempDir.resolve("xteas.json"), "new-xteas")
        val backup = tempDir.resolve(".cache-backup-committed")
        Files.createDirectories(backup)
        write(backup.resolve("promotion-state"), "cacheExisted=true\nxteaExisted=true\n")
        createCache(backup.resolve("vanilla"), "accepted")
        write(backup.resolve("xteas.json"), "accepted-xteas")
        write(backup.resolve("committed"), "")

        CacheDownloadStaging.recoverInterruptedPromotion(targetCache, targetXteas)

        assertEquals("new", Files.readString(targetCache.resolve("main_file_cache.dat2")))
        assertEquals("new-xteas", Files.readString(targetXteas))
        assertFalse(Files.exists(backup))
    }

    private fun createCache(directory: Path, marker: String): Path {
        Files.createDirectories(directory)
        write(directory.resolve("main_file_cache.dat2"), marker)
        write(directory.resolve("main_file_cache.idx255"), "index-255")
        write(directory.resolve("main_file_cache.idx0"), "index-0")
        return directory
    }

    private fun write(path: Path, contents: String): Path {
        Files.writeString(path, contents)
        return path
    }
}
