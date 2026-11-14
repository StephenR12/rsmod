package org.rsmod.api.cache.delta

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import io.netty.buffer.Unpooled
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.TreeMap
import org.openrs2.cache.Cache
import org.openrs2.crypto.SymmetricKey
import org.rsmod.api.cache.Js5Archives
import org.rsmod.api.cache.map.MapArchive
import org.rsmod.api.cache.map.MapFile
import org.rsmod.api.cache.map.loc.MapLocDefinition
import org.rsmod.api.cache.map.loc.MapLocListDecoder
import org.rsmod.api.cache.map.tile.MapTileDecoder
import org.rsmod.api.cache.map.tile.MapTileDefinition
import org.rsmod.api.cache.util.InlineByteBuf
import org.rsmod.map.CoordGrid
import org.rsmod.map.square.MapSquareGrid
import org.rsmod.map.square.MapSquareKey

/** Inventories every terrain and XTEA-protected location map used by the server. */
public object MapSquareManifestExporter {
    private val mapper: ObjectMapper =
        JsonMapper.builder().addModule(KotlinModule.Builder().build()).build()

    public fun export(
        cacheDirectory: Path,
        xteaFile: Path,
        revision: String,
        archive: Int,
        capturedAt: String,
    ): CacheManifest {
        require(Files.isDirectory(cacheDirectory)) {
            "Cache directory does not exist: ${cacheDirectory.toAbsolutePath()}"
        }
        require(Files.isRegularFile(xteaFile)) {
            "XTEA file does not exist: ${xteaFile.toAbsolutePath()}"
        }
        val records = loadXteaRecords(xteaFile)
        return Cache.open(cacheDirectory).use { cache ->
            export(cache, records, revision, archive, capturedAt)
        }
    }

    internal fun loadXteaRecords(xteaFile: Path): List<XteaRecord> {
        require(Files.isRegularFile(xteaFile)) {
            "XTEA file does not exist: ${xteaFile.toAbsolutePath()}"
        }
        return Files.newInputStream(xteaFile).use {
            mapper.readValue<Array<XteaRecord>>(it).asList()
        }
    }

    internal fun export(
        cache: Cache,
        xteas: List<XteaRecord>,
        revision: String,
        archive: Int,
        capturedAt: String,
    ): CacheManifest {
        require(xteas.map(XteaRecord::mapsquare).distinct().size == xteas.size) {
            "XTEA input contains duplicate map-square IDs."
        }
        val maps = MapArchive(cache)
        val entries = ArrayList<CacheManifestEntry>(xteas.size * 2)
        if (maps.numeric) {
            for (square in maps.nativeSquares()) {
                entries +=
                    terrainEntry(square, maps.readBytes(square, MapFile.Terrain), numeric = true)
                entries += locationEntry(square, maps.readBytes(square, MapFile.Locations), null)
            }
        } else {
            xteas.sortedBy(XteaRecord::mapsquare).forEach { record ->
                require(record.key.size == 4) {
                    "Map square ${record.mapsquare} must have exactly four XTEA words."
                }
                val square = MapSquareKey(record.mapsquare)
                val name = "${square.x}_${square.z}"
                require(record.archive == Js5Archives.MAPS) {
                    "Map square ${record.mapsquare} belongs to archive ${record.archive}, not ${Js5Archives.MAPS}."
                }
                require(record.name == "l$name") {
                    "Map square ${record.mapsquare} has XTEA group name '${record.name}', expected 'l$name'."
                }
                val terrain = maps.readBytes(square, MapFile.Terrain)
                val locations =
                    maps.readBytes(
                        square,
                        MapFile.Locations,
                        SymmetricKey.fromIntArray(record.key.toIntArray()),
                    )
                entries += terrainEntry(square, terrain)
                entries += locationEntry(square, locations, record)
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

    internal fun terrainEntry(
        square: MapSquareKey,
        encoded: ByteArray,
        numeric: Boolean = false,
    ): CacheManifestEntry {
        val data = Unpooled.wrappedBuffer(encoded)
        var decodedBytes = 0
        val definition =
            try {
                MapTileDecoder.decode(data).also { decodedBytes = data.readerIndex() }
            } finally {
                data.release()
            }
        val rules = definition.rules.values
        return entry(
            domain = "map-terrain",
            square = square,
            encoded = encoded,
            attributes =
                mapOf(
                    "archiveGroup" to if (numeric) "${square.id}/0" else "m${square.x}_${square.z}",
                    "tiles" to TILE_COUNT,
                    "decodedBytes" to decodedBytes,
                    "trailingBytes" to encoded.size - decodedBytes,
                    "explicitHeights" to
                        definition.tileHeights.values.count { it != Int.MIN_VALUE },
                    "overlays" to definition.overlays.size,
                    "underlays" to definition.underlays.size,
                    "blockedTiles" to rules.countSet(MapTileDefinition.BLOCK_MAP_SQUARE),
                    "linkBelowTiles" to rules.countSet(MapTileDefinition.LINK_BELOW),
                    "removeRoofTiles" to rules.countSet(MapTileDefinition.REMOVE_ROOFS),
                    "visibleBelowTiles" to rules.countSet(MapTileDefinition.VISIBLE_BELOW),
                    "notLowDetailTiles" to rules.countSet(MapTileDefinition.NOT_LOW_DETAIL),
                ),
            risks = listOf("client-contract-risk", "map-collision-risk"),
        )
    }

    internal fun locationEntry(
        square: MapSquareKey,
        encoded: ByteArray,
        xtea: XteaRecord?,
    ): CacheManifestEntry {
        val definition = MapLocListDecoder.decode(InlineByteBuf(encoded))
        val levels = IntArray(CoordGrid.LEVEL_COUNT)
        val ids = HashSet<Int>()
        val shapes = HashSet<Int>()
        val iterator = definition.spawns.longIterator()
        while (iterator.hasNext()) {
            val location = MapLocDefinition(iterator.nextLong())
            levels[location.level]++
            ids += location.id
            shapes += location.shape
        }
        return entry(
            domain = "map-locations",
            square = square,
            encoded = encoded,
            attributes =
                mapOf(
                    "archiveGroup" to
                        if (xtea == null) "${square.id}/1" else "l${square.x}_${square.z}",
                    "archiveGroupId" to (xtea?.group ?: square.id),
                    "xteaProtected" to (xtea != null),
                    "xteaKeyFingerprint" to
                        xtea?.let {
                            CoreConfigManifestExporter.fingerprint(
                                it.key.joinToString(",").toByteArray(StandardCharsets.UTF_8)
                            )
                        },
                    "placements" to definition.spawns.size,
                    "distinctLocationIds" to ids.size,
                    "distinctShapes" to shapes.size,
                    "placementsByLevel" to levels,
                ),
            risks = listOf("location-contract-risk", "map-collision-risk"),
        )
    }

    private fun entry(
        domain: String,
        square: MapSquareKey,
        encoded: ByteArray,
        attributes: Map<String, Any?>,
        risks: List<String>,
    ): CacheManifestEntry {
        val canonical =
            TreeMap(attributes.mapValues { SemanticConfigManifestExporter.canonical(it.value) })
        canonical["mapSquareId"] = square.id.toString()
        canonical["mapSquareX"] = square.x.toString()
        canonical["mapSquareZ"] = square.z.toString()
        canonical["fileLength"] = encoded.size.toString()
        return CacheManifestEntry(
            domain = domain,
            key = square.id.toString(),
            fingerprint = CoreConfigManifestExporter.fingerprint(encoded),
            attributes = canonical,
            riskTags = risks.sorted(),
        )
    }

    private fun Collection<Byte>.countSet(flag: Int): Int = count { it.toInt() and flag != 0 }

    private fun MapArchive.readBytes(
        square: MapSquareKey,
        type: MapFile,
        key: SymmetricKey = SymmetricKey.ZERO,
    ): ByteArray {
        val data = requireNotNull(read(square, type, key)) { "Missing $type for $square" }
        return try {
            ByteArray(data.readableBytes()).also { data.getBytes(data.readerIndex(), it) }
        } finally {
            data.release()
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    internal data class XteaRecord(
        val archive: Int = -1,
        val group: Int = -1,
        val name: String = "",
        val mapsquare: Int = 0,
        val key: List<Int> = emptyList(),
    )

    private const val TILE_COUNT =
        CoordGrid.LEVEL_COUNT * MapSquareGrid.LENGTH * MapSquareGrid.LENGTH
}
