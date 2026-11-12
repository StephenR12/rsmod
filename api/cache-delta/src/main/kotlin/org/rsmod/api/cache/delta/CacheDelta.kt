package org.rsmod.api.cache.delta

public enum class CacheDeltaKind {
    Added,
    Removed,
    Changed,
}

public data class CacheManifestReference(
    public val revision: String,
    public val archive: Int,
    public val capturedAt: String,
)

public data class CacheDeltaSummary(
    public val added: Int,
    public val removed: Int,
    public val changed: Int,
    public val unchanged: Int,
)

public data class CacheDeltaEntry(
    public val domain: String,
    public val key: String,
    public val kind: CacheDeltaKind,
    public val before: CacheManifestEntry? = null,
    public val after: CacheManifestEntry? = null,
    public val riskTags: List<String>,
)

public data class CacheDeltaReport(
    public val schemaVersion: Int = SCHEMA_VERSION,
    public val baseline: CacheManifestReference,
    public val target: CacheManifestReference,
    public val summary: CacheDeltaSummary,
    public val changes: List<CacheDeltaEntry>,
) {
    public companion object {
        public const val SCHEMA_VERSION: Int = 1
    }
}
