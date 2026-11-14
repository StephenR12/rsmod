package org.rsmod.api.cache.enricher

import jakarta.inject.Inject
import org.openrs2.buffer.use
import org.openrs2.cache.Cache
import org.rsmod.api.cache.enricher.map.area.AreaCacheEnricher
import org.rsmod.api.cache.enricher.map.area.EnrichedAreaConfig
import org.rsmod.api.cache.map.MapArchive
import org.rsmod.api.cache.map.MapFile
import org.rsmod.api.cache.map.area.MapAreaDecoder
import org.rsmod.api.cache.map.area.MapAreaDefinition
import org.rsmod.api.cache.map.area.MapAreaEncoder
import org.rsmod.api.cache.util.EncoderContext
import org.rsmod.api.cache.util.toInlineBuf
import org.rsmod.game.type.dbtable.DbTableTypeList
import org.rsmod.game.type.param.ParamTypeList
import org.rsmod.game.type.varp.VarpTypeList
import org.rsmod.map.square.MapSquareKey

public class CacheEnrichmentMaps
@Inject
constructor(
    private val paramTypes: ParamTypeList,
    private val varpTypes: VarpTypeList,
    private val dbTableTypes: DbTableTypeList,
    private val areaEnrichments: Set<AreaCacheEnricher>,
) {
    public fun encodeAll(dest: Cache) {
        val ctx = encoderContext()
        val areas = areaEnrichments.flatMap(AreaCacheEnricher::generate).collect(dest)
        MapAreaEncoder.encodeAll(dest, areas, ctx)
    }

    private fun Iterable<EnrichedAreaConfig>.collect(
        cache: Cache
    ): Map<MapSquareKey, MapAreaDefinition> {
        val merged = loadCacheAreas(cache)
        for (config in this) {
            merged.merge(config.square, config.areas, MapAreaDefinition::merge)
        }
        return merged
    }

    private fun Iterable<EnrichedAreaConfig>.loadCacheAreas(
        cache: Cache
    ): MutableMap<MapSquareKey, MapAreaDefinition> {
        val archive = MapArchive(cache)
        val areas = mutableMapOf<MapSquareKey, MapAreaDefinition>()
        for (config in this) {
            val buffer = archive.read(config.square, MapFile.Areas) ?: continue
            val area = buffer.use { MapAreaDecoder.decode(it.toInlineBuf()) }
            areas[config.square] = area
        }
        return areas
    }

    private fun encoderContext(): EncoderContext {
        return EncoderContext.server(
            params = paramTypes.filterTransmitKeys(),
            varps = varpTypes.filterTransmitKeys(),
            tables = dbTableTypes.filterTransmitKeys(),
        )
    }
}
