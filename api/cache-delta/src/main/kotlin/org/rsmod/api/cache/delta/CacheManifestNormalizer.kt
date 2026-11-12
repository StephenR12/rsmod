package org.rsmod.api.cache.delta

import java.util.TreeMap

public object CacheManifestNormalizer {
    public fun normalize(manifest: CacheManifest, label: String = "cache"): CacheManifest {
        val indexed = index(manifest, label)
        return manifest.copy(entries = indexed.values.toList())
    }

    internal fun index(
        manifest: CacheManifest,
        label: String,
    ): Map<CacheManifestKey, CacheManifestEntry> {
        require(manifest.schemaVersion == CacheManifest.SCHEMA_VERSION) {
            "$label cache manifest has unsupported schema version ${manifest.schemaVersion}."
        }
        require(manifest.revision.isNotBlank()) { "$label cache manifest is missing revision." }
        require(manifest.archive >= 0) { "$label cache manifest has an invalid archive id." }
        require(manifest.capturedAt.isNotBlank()) {
            "$label cache manifest is missing capture time."
        }
        val indexed = sortedMapOf<CacheManifestKey, CacheManifestEntry>()
        for (entry in manifest.entries) {
            require(entry.domain.isNotBlank()) { "$label cache manifest contains a blank domain." }
            require(entry.key.isNotBlank()) { "$label cache manifest contains a blank key." }
            require(entry.fingerprint.isNotBlank()) {
                "$label cache manifest entry ${entry.domain}:${entry.key} has a blank fingerprint."
            }
            val normalized = entry.normalized()
            val previous = indexed.put(entry.manifestKey, normalized)
            require(previous == null) {
                "$label cache manifest contains duplicate key ${entry.domain}:${entry.key}."
            }
        }
        return indexed
    }

    private fun CacheManifestEntry.normalized(): CacheManifestEntry =
        copy(attributes = TreeMap(attributes), riskTags = riskTags.distinct().sorted())
}
