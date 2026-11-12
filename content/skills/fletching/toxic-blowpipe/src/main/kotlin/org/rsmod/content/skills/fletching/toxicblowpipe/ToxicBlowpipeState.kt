package org.rsmod.content.skills.fletching.toxicblowpipe

import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.varobjs
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjType
import org.rsmod.utils.bits.getBits
import org.rsmod.utils.bits.withBits

/** The persistent resources stored in a toxic blowpipe's single packed item variable. */
public data class ToxicBlowpipeState(
    public val dart: ToxicBlowpipeDart? = null,
    public val dartCount: Int = 0,
    public val scales: Int = 0,
) {
    init {
        require(dartCount in 0..MAX_CAPACITY) { "Invalid blowpipe dart count: $dartCount" }
        require(scales in 0..MAX_CAPACITY) { "Invalid blowpipe scale count: $scales" }
        require((dart == null) == (dartCount == 0)) {
            "Blowpipe dart type and count must either both be empty or both be populated."
        }
    }

    public val isEmpty: Boolean
        get() = dartCount == 0 && scales == 0

    public fun pack(): Int =
        0.withBits(varobjs.snakeboss_blowpipe_darttype.bits, dart?.storageCode ?: 0)
            .withBits(varobjs.snakeboss_blowpipe_dartcount.bits, dartCount)
            .withBits(varobjs.snakeboss_blowpipe_flakes.bits, scales)

    public companion object {
        public const val MAX_CAPACITY: Int = 16_383

        public fun unpack(obj: InvObj?): ToxicBlowpipeState = unpack(obj?.vars ?: 0)

        public fun unpack(vars: Int): ToxicBlowpipeState {
            val dartCode = vars.getBits(varobjs.snakeboss_blowpipe_darttype.bits)
            val dartCount = vars.getBits(varobjs.snakeboss_blowpipe_dartcount.bits)
            val scales = vars.getBits(varobjs.snakeboss_blowpipe_flakes.bits)
            if (dartCode == 0 && dartCount == 0) {
                return ToxicBlowpipeState(scales = scales)
            }
            require(dartCode != 0 && dartCount != 0) {
                "Corrupt blowpipe dart state: type=$dartCode, count=$dartCount"
            }
            val dart =
                requireNotNull(ToxicBlowpipeDart.fromStorageCode(dartCode)) {
                    "Unknown blowpipe dart storage code: $dartCode"
                }
            return ToxicBlowpipeState(dart, dartCount, scales)
        }
    }
}

/**
 * Stable persisted codes for every unpoisoned dart accepted by the cache-233 toxic blowpipe.
 *
 * The workspace storage contract assigns codes in historical addition order: the original metal
 * darts, dragon, black, then amethyst. Cache and Wiki evidence do not expose Jagex's private code
 * mapping. These codes are deliberately explicit so a future cache migration can translate stored
 * state without relying on enum ordinals or item ids.
 */
public enum class ToxicBlowpipeDart(
    public val obj: ObjType,
    public val storageCode: Int,
    public val displayName: String,
) {
    Bronze(objs.bronze_dart, 1, "bronze darts"),
    Iron(objs.iron_dart, 2, "iron darts"),
    Steel(objs.steel_dart, 3, "steel darts"),
    Mithril(objs.mithril_dart, 4, "mithril darts"),
    Adamant(objs.adamant_dart, 5, "adamant darts"),
    Rune(objs.rune_dart, 6, "rune darts"),
    Dragon(objs.dragon_dart, 7, "dragon darts"),
    Black(objs.black_dart, 8, "black darts"),
    Amethyst(objs.amethyst_dart, 9, "amethyst darts");

    public companion object {
        public fun fromStorageCode(code: Int): ToxicBlowpipeDart? =
            entries.find { it.storageCode == code }

        public fun fromObj(type: ObjType): ToxicBlowpipeDart? = entries.find { it.obj == type }
    }
}
