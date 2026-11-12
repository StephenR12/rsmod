package org.rsmod.api.cache.delta

public data class CacheManifest(
    public val schemaVersion: Int = SCHEMA_VERSION,
    public val revision: String,
    public val archive: Int,
    public val capturedAt: String,
    public val entries: List<CacheManifestEntry>,
) {
    public companion object {
        public const val SCHEMA_VERSION: Int = 1
    }
}

public data class CacheManifestEntry(
    public val domain: String,
    public val key: String,
    public val symbol: String? = null,
    public val fingerprint: String,
    public val attributes: Map<String, String> = emptyMap(),
    public val riskTags: List<String> = emptyList(),
)

internal data class CacheManifestKey(val domain: String, val key: String) :
    Comparable<CacheManifestKey> {
    override fun compareTo(other: CacheManifestKey): Int =
        compareValuesBy(this, other, CacheManifestKey::domain, CacheManifestKey::key)
}

internal val CacheManifestEntry.manifestKey: CacheManifestKey
    get() = CacheManifestKey(domain, key)
