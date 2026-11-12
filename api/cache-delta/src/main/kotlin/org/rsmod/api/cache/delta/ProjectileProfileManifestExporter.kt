package org.rsmod.api.cache.delta

import java.nio.file.Files
import java.nio.file.Path
import org.openrs2.cache.Cache

/** Exports the projectile profiles written into an rsmod-enriched runtime cache. */
public object ProjectileProfileManifestExporter {
    public fun export(
        cacheDirectory: Path,
        symbolDirectory: Path,
        revision: String,
        archive: Int,
        capturedAt: String,
    ): CacheManifest {
        require(Files.isDirectory(cacheDirectory)) {
            "Cache directory does not exist: ${cacheDirectory.toAbsolutePath()}"
        }
        val symbols = SemanticConfigSymbols.load(symbolDirectory).projectiles
        return Cache.open(cacheDirectory).use { cache ->
            CacheManifestNormalizer.normalize(
                CacheManifest(
                    revision = revision,
                    archive = archive,
                    capturedAt = capturedAt,
                    entries = SemanticConfigManifestExporter.exportProjectiles(cache, symbols),
                )
            )
        }
    }
}
