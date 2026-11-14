package org.rsmod.api.cache.map.obj

import io.netty.buffer.ByteBuf
import io.netty.buffer.PooledByteBufAllocator
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator
import org.openrs2.cache.Cache
import org.rsmod.api.cache.map.MapArchive
import org.rsmod.api.cache.map.MapFile
import org.rsmod.api.cache.util.EncoderContext
import org.rsmod.map.square.MapSquareKey

public object MapObjListEncoder {
    public fun encodeAll(
        cache: Cache,
        spawns: Map<MapSquareKey, MapObjListDefinition>,
        ctx: EncoderContext,
    ) {
        // Map obj spawns are a server-only group.
        if (ctx.clientOnly) {
            return
        }
        val buffer = PooledByteBufAllocator.DEFAULT.buffer()
        val archive = MapArchive(cache)
        for ((key, definition) in spawns) {
            val oldBuf = archive.read(key, MapFile.Objs)
            val newBuf = buffer.clear().apply { encode(definition, this) }
            if (newBuf != oldBuf) {
                archive.write(key, MapFile.Objs, newBuf)
            }
            oldBuf?.release()
        }
        buffer.release()
    }

    public fun encode(definition: MapObjListDefinition, data: ByteBuf): Unit =
        with(definition) {
            check(packedSpawns.size <= 65535) {
                "Map obj spawn size exceeds limit: ${packedSpawns.size} / 65535"
            }
            data.writeShort(packedSpawns.size)
            for (packed in packedSpawns.longIterator()) {
                data.writeLong(packed)
            }
        }
}
