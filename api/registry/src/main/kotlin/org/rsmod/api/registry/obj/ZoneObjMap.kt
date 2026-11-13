package org.rsmod.api.registry.obj

import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import org.rsmod.map.zone.ZoneKey

@JvmInline
public value class ZoneObjMap(public val entries: Int2ObjectMap<ObjStackEntryList>) {
    public val activeZoneCount: Int
        get() = entries.size

    public constructor() : this(Int2ObjectOpenHashMap())

    public fun objCount(): Int = entries.values.sumOf { it.entries.size }

    /**
     * Item types already on the ground for this character, including personal untradeable drops.
     */
    public fun typesForReceiver(receiver: Long): Set<Int> = buildSet {
        for (zone in entries.values) for (obj in zone.entries) {
            if (obj.nullableReceiverId == receiver) add(obj.type)
        }
    }

    public fun getOrPut(key: ZoneKey): ObjStackEntryList =
        entries.computeIfAbsent(key.packed) { ObjStackEntryList() }

    public operator fun get(key: ZoneKey): ObjStackEntryList? =
        entries.getOrDefault(key.packed, null)
}
