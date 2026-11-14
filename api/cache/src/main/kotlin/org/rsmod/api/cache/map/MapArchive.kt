package org.rsmod.api.cache.map

import io.netty.buffer.ByteBuf
import org.openrs2.cache.Cache
import org.openrs2.crypto.SymmetricKey
import org.rsmod.api.cache.Js5Archives
import org.rsmod.api.cache.util.readOrNull
import org.rsmod.map.square.MapSquareKey

public enum class MapFile(
    internal val prefix: Char,
    internal val file: Int,
    internal val server: Boolean = false,
) {
    Terrain('m', 0),
    Locations('l', 1),
    Npcs('n', 0, true),
    Objs('o', 1, true),
    Areas('a', 2, true),
}

/** Numeric map groups appeared in revision 238. Server overlays use a separate group per square. */
public class MapArchive(private val cache: Cache) {
    private val archive = Js5Archives.MAPS
    public val numeric: Boolean =
        cache.exists(archive) && cache.list(archive).asSequence().firstOrNull()?.nameHash == -1

    public fun address(square: MapSquareKey, type: MapFile): String =
        if (numeric) "${group(square, type)}/${type.file}" else name(square, type)

    public fun nativeSquares(): List<MapSquareKey> {
        check(numeric)
        // RuneLite's revision 238 RegionLoader ends before 25287, the native RGB palette group.
        return cache
            .list(archive)
            .asSequence()
            .filter { it.id in 0 until 65536 && it.id != 25287 }
            .map { MapSquareKey(it.id) }
            .toList()
    }

    public fun read(
        square: MapSquareKey,
        type: MapFile,
        key: SymmetricKey = SymmetricKey.ZERO,
    ): ByteBuf? =
        if (numeric) cache.readOrNull(archive, group(square, type), type.file)
        else cache.readOrNull(archive, name(square, type), 0, key)

    public fun write(
        square: MapSquareKey,
        type: MapFile,
        data: ByteBuf,
        key: SymmetricKey = SymmetricKey.ZERO,
    ) {
        if (numeric) cache.write(archive, group(square, type), type.file, data)
        else cache.write(archive, name(square, type), 0, data, key)
    }

    private fun group(square: MapSquareKey, type: MapFile): Int =
        if (type.server) square.id + 65536 else square.id

    private fun name(square: MapSquareKey, type: MapFile): String =
        "${type.prefix}${square.x}_${square.z}"
}
