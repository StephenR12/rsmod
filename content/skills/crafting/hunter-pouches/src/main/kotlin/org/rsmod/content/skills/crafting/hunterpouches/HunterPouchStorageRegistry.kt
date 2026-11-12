package org.rsmod.content.skills.crafting.hunterpouches

import org.rsmod.api.config.refs.invs
import org.rsmod.api.config.refs.objs
import org.rsmod.game.type.inv.InvType
import org.rsmod.game.type.obj.ObjType

public enum class HunterPouchFamily(
    public val storage: InvType,
    public val singular: String,
    public val plural: String,
    public val accepted: Set<ObjType>,
) {
    Fur(
        invs.hunter_fur_pouch_storage,
        singular = "fur",
        plural = "furs",
        accepted =
            setOf(
                objs.larupia_fur,
                objs.graahk_fur,
                objs.kyatt_fur,
                objs.dark_kebbit_fur,
                objs.polar_kebbit_fur,
                objs.feldip_weasel_fur,
                objs.common_kebbit_fur,
                objs.desert_devil_fur,
                objs.spotted_kebbit_fur,
                objs.dashing_kebbit_fur,
                objs.fox_fur,
                objs.moonlight_antelope_fur,
                objs.sunlight_antelope_fur,
                objs.jaguar_fur,
            ),
    ),
    Meat(
        invs.hunter_meat_pouch_storage,
        singular = "meat",
        plural = "meats",
        accepted =
            setOf(
                objs.wild_kebbit_meat,
                objs.larupia_meat,
                objs.barb_tailed_kebbit_meat,
                objs.graahk_meat,
                objs.kyatt_meat,
                objs.pyre_fox_meat,
                objs.dashing_kebbit_meat,
                objs.sunlight_antelope_meat,
                objs.moonlight_antelope_meat,
            ),
    ),
}

public data class HunterPouchDescriptor(
    public val closed: ObjType,
    public val open: ObjType,
    public val capacity: Int,
    public val family: HunterPouchFamily,
) {
    public val variants: List<ObjType> = listOf(closed, open)

    public fun counterpart(type: ObjType): ObjType =
        when (type) {
            closed -> open
            open -> closed
            else -> error("$type is not a variant of $this")
        }
}

public class HunterPouchStorageRegistry {
    public val descriptors: List<HunterPouchDescriptor> =
        listOf(
            HunterPouchDescriptor(
                objs.small_fur_pouch,
                objs.small_fur_pouch_open,
                capacity = 14,
                HunterPouchFamily.Fur,
            ),
            HunterPouchDescriptor(
                objs.medium_fur_pouch,
                objs.medium_fur_pouch_open,
                capacity = 21,
                HunterPouchFamily.Fur,
            ),
            HunterPouchDescriptor(
                objs.large_fur_pouch,
                objs.large_fur_pouch_open,
                capacity = 28,
                HunterPouchFamily.Fur,
            ),
            HunterPouchDescriptor(
                objs.small_meat_pouch,
                objs.small_meat_pouch_open,
                capacity = 14,
                HunterPouchFamily.Meat,
            ),
            HunterPouchDescriptor(
                objs.large_meat_pouch,
                objs.large_meat_pouch_open,
                capacity = 28,
                HunterPouchFamily.Meat,
            ),
        )

    private val byType: Map<Int, HunterPouchDescriptor> =
        descriptors
            .flatMap { descriptor -> descriptor.variants.map { it.id to descriptor } }
            .toMap()

    public fun find(type: ObjType): HunterPouchDescriptor? = byType[type.id]

    public fun find(id: Int): HunterPouchDescriptor? = byType[id]
}
