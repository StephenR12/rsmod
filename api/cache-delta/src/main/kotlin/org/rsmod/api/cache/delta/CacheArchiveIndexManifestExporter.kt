package org.rsmod.api.cache.delta

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.TreeMap
import org.openrs2.cache.Cache
import org.openrs2.cache.Js5Index

/**
 * Inventories the structural identity of every archive group in a JS5 cache.
 *
 * Unlike the semantic exporters, this does not read or interpret group payloads. The JS5 index
 * metadata is sufficient to detect added, removed, reordered, renamed, or replaced files, including
 * groups whose payloads require XTEA keys.
 */
public object CacheArchiveIndexManifestExporter {
    public fun export(
        cacheDirectory: Path,
        revision: String,
        archive: Int,
        capturedAt: String,
    ): CacheManifest {
        require(Files.isDirectory(cacheDirectory)) {
            "Cache directory does not exist: ${cacheDirectory.toAbsolutePath()}"
        }
        return Cache.open(cacheDirectory).use { cache ->
            export(cache, revision, archive, capturedAt)
        }
    }

    public fun export(
        cache: Cache,
        revision: String,
        archive: Int,
        capturedAt: String,
    ): CacheManifest {
        val entries = ArrayList<CacheManifestEntry>()
        cache.list().asSequence().sorted().forEach { archiveId ->
            cache.list(archiveId).asSequence().sortedBy(Js5Index.Group<*>::id).forEach { group ->
                entries += entry(archiveId, group)
            }
        }
        return CacheManifestNormalizer.normalize(
            CacheManifest(
                revision = revision,
                archive = archive,
                capturedAt = capturedAt,
                entries = entries,
            )
        )
    }

    internal fun entry(archiveId: Int, group: Js5Index.Group<*>): CacheManifestEntry {
        val files = group.asSequence().sortedBy(Js5Index.File::id).toList()
        val attributes = TreeMap<String, String>()
        attributes["archiveId"] = archiveId.toString()
        attributes["groupId"] = group.id.toString()
        attributes["groupVersion"] = group.version.toString()
        attributes["groupChecksum"] = group.checksum.toUInt().toString()
        attributes["groupUncompressedChecksum"] = group.uncompressedChecksum.toUInt().toString()
        attributes["groupLength"] = group.length.toString()
        attributes["groupUncompressedLength"] = group.uncompressedLength.toString()
        attributes["groupNameHash"] = group.nameHash.toString()
        attributes["fileCount"] = files.size.toString()
        attributes["files"] =
            files.joinToString(prefix = "[", postfix = "]") { "${it.id}:${it.nameHash}" }
        val canonical =
            attributes.entries.joinToString(separator = "\n") { (key, value) -> "$key=$value" }
        return CacheManifestEntry(
            domain = "cache-archive-index",
            key = "$archiveId:${group.id}",
            fingerprint =
                CoreConfigManifestExporter.fingerprint(
                    canonical.toByteArray(StandardCharsets.UTF_8)
                ),
            attributes = attributes,
            riskTags = listOf("archive-identity-risk", "client-contract-risk"),
        )
    }
}
