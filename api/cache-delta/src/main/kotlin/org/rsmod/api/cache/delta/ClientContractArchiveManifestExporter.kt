package org.rsmod.api.cache.delta

import java.nio.file.Files
import java.nio.file.Path
import java.util.TreeMap
import org.openrs2.cache.Cache
import org.openrs2.cache.Js5Index
import org.rsmod.api.cache.Js5Archives

/**
 * Inventories byte-oriented client contract archives that do not yet have semantic decoders.
 *
 * This intentionally records exact file bytes and JS5 index metadata without claiming semantic
 * parity for the underlying formats. A later decoder can enrich the attributes without changing the
 * stable domain/group/file identity.
 */
public object ClientContractArchiveManifestExporter {
    public data class ArchiveSpec(
        public val id: Int,
        public val domain: String,
        public val riskTags: List<String>,
    )

    public val archiveSpecs: List<ArchiveSpec> =
        listOf(
            ArchiveSpec(
                Js5Archives.ANIMS,
                "animation-frames",
                listOf("animation-contract-risk", "client-contract-risk"),
            ),
            ArchiveSpec(
                Js5Archives.BASES,
                "animation-bases",
                listOf("animation-contract-risk", "client-contract-risk"),
            ),
            ArchiveSpec(
                Js5Archives.INTERFACES,
                "interface-components",
                listOf("client-contract-risk", "interface-contract-risk"),
            ),
            ArchiveSpec(
                Js5Archives.SYNTHS,
                "sound-effects",
                listOf("audio-contract-risk", "client-contract-risk"),
            ),
            ArchiveSpec(
                Js5Archives.SONGS,
                "songs",
                listOf("audio-contract-risk", "client-contract-risk"),
            ),
            ArchiveSpec(
                Js5Archives.JINGLES,
                "jingles",
                listOf("audio-contract-risk", "client-contract-risk"),
            ),
            ArchiveSpec(
                Js5Archives.CLIENTSCRIPTS,
                "client-scripts",
                listOf("client-contract-risk", "script-contract-risk"),
            ),
            ArchiveSpec(
                Js5Archives.VORBIS,
                "vorbis-samples",
                listOf("audio-contract-risk", "client-contract-risk"),
            ),
            ArchiveSpec(
                Js5Archives.MIDIPATCHES,
                "midi-patches",
                listOf("audio-contract-risk", "client-contract-risk"),
            ),
            ArchiveSpec(
                Js5Archives.KEYFRAMESETS,
                "keyframe-sets",
                listOf("animation-contract-risk", "client-contract-risk"),
            ),
        )

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
        archiveSpecs
            .filter { cache.exists(it.id) }
            .forEach { spec ->
                cache.list(spec.id).asSequence().sortedBy(Js5Index.Group<*>::id).forEach { group ->
                    cache
                        .list(spec.id, group.id)
                        .asSequence()
                        .sortedBy(Js5Index.File::id)
                        .forEach { file ->
                            val data = cache.read(spec.id, group.id, file.id)
                            val bytes =
                                try {
                                    ByteArray(data.readableBytes()).also {
                                        data.getBytes(data.readerIndex(), it)
                                    }
                                } finally {
                                    data.release()
                                }
                            entries += entry(spec, group, file, bytes)
                        }
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

    internal fun entry(
        spec: ArchiveSpec,
        group: Js5Index.Group<*>,
        file: Js5Index.File,
        bytes: ByteArray,
    ): CacheManifestEntry {
        val attributes = TreeMap<String, String>()
        attributes["archiveId"] = spec.id.toString()
        attributes["groupId"] = group.id.toString()
        attributes["fileId"] = file.id.toString()
        attributes["groupVersion"] = group.version.toString()
        attributes["groupChecksum"] = group.checksum.toUInt().toString()
        attributes["groupUncompressedChecksum"] = group.uncompressedChecksum.toUInt().toString()
        attributes["groupLength"] = group.length.toString()
        attributes["groupUncompressedLength"] = group.uncompressedLength.toString()
        attributes["groupNameHash"] = group.nameHash.toString()
        attributes["fileNameHash"] = file.nameHash.toString()
        attributes["fileLength"] = bytes.size.toString()
        return CacheManifestEntry(
            domain = spec.domain,
            key = "${group.id}:${file.id}",
            fingerprint = CoreConfigManifestExporter.fingerprint(bytes),
            attributes = attributes,
            riskTags = spec.riskTags.sorted(),
        )
    }
}
