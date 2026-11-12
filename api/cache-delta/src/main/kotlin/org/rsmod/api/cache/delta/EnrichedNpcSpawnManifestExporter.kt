package org.rsmod.api.cache.delta

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.TreeMap
import org.openrs2.cache.Cache
import org.rsmod.api.cache.Js5Archives
import org.rsmod.api.cache.map.npc.MapNpcDefinition
import org.rsmod.api.cache.map.npc.MapNpcListDecoder
import org.rsmod.api.cache.types.npc.NpcTypeDecoder
import org.rsmod.api.cache.util.InlineByteBuf
import org.rsmod.api.cache.util.readOrNull
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.npc.UnpackedNpcType
import org.rsmod.map.square.MapSquareGrid
import org.rsmod.map.square.MapSquareKey

/**
 * Inventories the server-only `nX_Z` NPC placement groups added to an enriched cache.
 *
 * The vanilla OSRS cache does not contain these groups. RSMod writes them while enriching the
 * cache, and [org.rsmod.api.cache.map.GameMapDecoder] reads them when the world starts. Exporting
 * both the raw group contract and canonical individual placements makes the live server population
 * measurable and gives future cache upgrades stable, actionable NPC-spawn deltas.
 */
public object EnrichedNpcSpawnManifestExporter {
    public fun export(
        cacheDirectory: Path,
        xteaFile: Path,
        symbolDirectory: Path,
        revision: String,
        archive: Int,
        capturedAt: String,
    ): CacheManifest {
        require(Files.isDirectory(cacheDirectory)) {
            "Cache directory does not exist: ${cacheDirectory.toAbsolutePath()}"
        }
        val xteas = MapSquareManifestExporter.loadXteaRecords(xteaFile)
        val symbols = CoreConfigSymbols.load(symbolDirectory)
        return Cache.open(cacheDirectory).use { cache ->
            export(cache, xteas, symbols, revision, archive, capturedAt)
        }
    }

    internal fun export(
        cache: Cache,
        xteas: List<MapSquareManifestExporter.XteaRecord>,
        symbols: CoreConfigSymbols,
        revision: String,
        archive: Int,
        capturedAt: String,
    ): CacheManifest {
        require(
            xteas.map(MapSquareManifestExporter.XteaRecord::mapsquare).distinct().size == xteas.size
        ) {
            "XTEA input contains duplicate map-square IDs."
        }
        val npcTypes = NpcTypeDecoder.decodeAll(cache)
        val entries = ArrayList<CacheManifestEntry>()
        xteas.sortedBy(MapSquareManifestExporter.XteaRecord::mapsquare).forEach { record ->
            require(record.archive == Js5Archives.MAPS) {
                "Map square ${record.mapsquare} belongs to archive ${record.archive}, not ${Js5Archives.MAPS}."
            }
            val square = MapSquareKey(record.mapsquare)
            val group = "n${square.x}_${square.z}"
            val encoded = cache.readGroupBytesOrNull(group) ?: return@forEach
            entries += entries(square, encoded, symbols.npcs, npcTypes)
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

    internal fun entries(
        square: MapSquareKey,
        encoded: ByteArray,
        symbols: Map<Int, String>,
        npcTypes: NpcTypeList,
    ): List<CacheManifestEntry> {
        require(encoded.size >= COUNT_BYTES) {
            "NPC map group n${square.x}_${square.z} is shorter than its count header."
        }
        val definition = MapNpcListDecoder.decode(InlineByteBuf(encoded))
        val expectedLength = COUNT_BYTES + definition.packedSpawns.size * PACKED_SPAWN_BYTES
        require(encoded.size == expectedLength) {
            "NPC map group n${square.x}_${square.z} decoded ${definition.packedSpawns.size} " +
                "placements from $expectedLength bytes but contains ${encoded.size} bytes."
        }

        val placements =
            buildList(definition.packedSpawns.size) {
                    val iterator = definition.packedSpawns.intIterator()
                    while (iterator.hasNext()) {
                        add(MapNpcDefinition(iterator.nextInt()))
                    }
                }
                .sortedWith(
                    compareBy(
                        MapNpcDefinition::level,
                        MapNpcDefinition::localX,
                        MapNpcDefinition::localZ,
                        MapNpcDefinition::id,
                    )
                )
        val levels = IntArray(LEVEL_COUNT)
        val distinctNpcIds = sortedSetOf<Int>()
        placements.forEach {
            levels[it.level]++
            distinctNpcIds += it.id
        }

        val result = ArrayList<CacheManifestEntry>(placements.size + 1)
        result +=
            CacheManifestEntry(
                domain = "map-npc-groups",
                key = square.id.toString(),
                fingerprint = CoreConfigManifestExporter.fingerprint(encoded),
                attributes =
                    canonicalAttributes(
                        "archiveGroup" to "n${square.x}_${square.z}",
                        "fileLength" to encoded.size,
                        "mapSquareId" to square.id,
                        "mapSquareX" to square.x,
                        "mapSquareZ" to square.z,
                        "placements" to placements.size,
                        "distinctNpcIds" to distinctNpcIds.size,
                        "npcIds" to distinctNpcIds,
                        "placementsByLevel" to levels,
                    ),
                riskTags = listOf("map-spawn-risk", "npc-contract-risk"),
            )

        val duplicateOrdinals = HashMap<PlacementIdentity, Int>()
        placements.forEach { placement ->
            val identity = PlacementIdentity(square, placement)
            val ordinal = duplicateOrdinals.getOrDefault(identity, 0)
            duplicateOrdinals[identity] = ordinal + 1
            result +=
                placementEntry(identity, ordinal, symbols[placement.id], npcTypes[placement.id])
        }
        return result
    }

    private fun placementEntry(
        identity: PlacementIdentity,
        duplicateOrdinal: Int,
        symbol: String?,
        type: UnpackedNpcType?,
    ): CacheManifestEntry {
        val key = identity.key(duplicateOrdinal)
        val risks = sortedSetOf("map-spawn-risk", "npc-contract-risk")
        val attackable = type?.op?.any { it.equals("Attack", ignoreCase = true) } == true
        if (attackable) {
            risks += "combat-risk"
            risks += "drop-table-risk"
        }
        return CacheManifestEntry(
            domain = "npc-spawns",
            key = key,
            symbol = symbol,
            fingerprint =
                CoreConfigManifestExporter.fingerprint(key.toByteArray(StandardCharsets.UTF_8)),
            attributes =
                canonicalAttributes(
                    "archiveGroup" to "n${identity.square.x}_${identity.square.z}",
                    "mapSquareId" to identity.square.id,
                    "mapSquareX" to identity.square.x,
                    "mapSquareZ" to identity.square.z,
                    "npcId" to identity.npcId,
                    "name" to type?.name.orEmpty(),
                    "operations" to type?.op?.filterNotNull().orEmpty(),
                    "attackable" to attackable,
                    "combatStats" to
                        type
                            ?.let {
                                listOf(
                                    it.attack,
                                    it.strength,
                                    it.defence,
                                    it.hitpoints,
                                    it.ranged,
                                    it.magic,
                                )
                            }
                            .orEmpty(),
                    "localX" to identity.localX,
                    "localZ" to identity.localZ,
                    "x" to identity.globalX,
                    "z" to identity.globalZ,
                    "level" to identity.level,
                    "duplicateOrdinal" to duplicateOrdinal,
                ),
            riskTags = risks.toList(),
        )
    }

    private fun canonicalAttributes(vararg pairs: Pair<String, Any?>): Map<String, String> =
        TreeMap(
            pairs.associate { (key, value) ->
                key to SemanticConfigManifestExporter.canonical(value)
            }
        )

    private fun Cache.readGroupBytesOrNull(group: String): ByteArray? {
        val data = readOrNull(Js5Archives.MAPS, group) ?: return null
        return try {
            ByteArray(data.readableBytes()).also { data.getBytes(data.readerIndex(), it) }
        } finally {
            data.release()
        }
    }

    private data class PlacementIdentity(
        val square: MapSquareKey,
        val npcId: Int,
        val localX: Int,
        val localZ: Int,
        val level: Int,
    ) {
        constructor(
            square: MapSquareKey,
            placement: MapNpcDefinition,
        ) : this(square, placement.id, placement.localX, placement.localZ, placement.level)

        val globalX: Int
            get() = square.x * MapSquareGrid.LENGTH + localX

        val globalZ: Int
            get() = square.z * MapSquareGrid.LENGTH + localZ

        fun key(duplicateOrdinal: Int): String =
            "${square.id}:$level:$localX:$localZ:$npcId:$duplicateOrdinal"
    }

    private const val COUNT_BYTES = 2
    private const val PACKED_SPAWN_BYTES = 4
    private const val LEVEL_COUNT = 4
}
