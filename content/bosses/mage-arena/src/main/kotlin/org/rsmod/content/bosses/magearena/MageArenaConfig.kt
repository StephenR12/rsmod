package org.rsmod.content.bosses.magearena

import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.type.npc.NpcType
import org.rsmod.map.CoordGrid

public object MageArenaNpcs : NpcReferences() {
    val kolodionHuman = find("kolhuman")
    val kolodionOgre = find("kologre")
    val kolodionSpider = find("kolspider")
    val kolodionGhost = find("kolethereal")
    val kolodionDemon = find("koldemon")
    val zachariah = find("ma2_boss_saradomin")
    val derwen = find("ma2_boss_guthix")
    val porazdir = find("ma2_boss_zamorak")
    val derwenEnergy = find("ma2_guthix_healer")
}

public object MageArenaObjs : ObjReferences() {
    val enchantedSymbol = find("ma2_symbol")
    val justiciarsHand = find("ma2_saradomin_heart")
    val entsRoots = find("ma2_guthix_heart")
    val demonsHeart = find("ma2_zamorak_heart")
    val saradominStaff = find("saradomin_staff")
    val guthixStaff = find("guthix_staff")
    val zamorakStaff = find("zamorak_staff")
    val staffOfLight = find("staff_of_light")
    val staffOfBalance = find("staff_of_balance")
    val staffOfTheDead = find("sotd")
    val toxicStaffOfTheDead = find("toxic_sotd_charged")
    val voidKnightMace = find("pest_void_knight_mace")
}

public data class KolodionForm(
    val npc: NpcType,
    val hitpoints: Int,
    val attack: Int,
    val strength: Int,
    val defence: Int,
    val magic: Int,
)

public object MageArenaBosses {
    public val kolodionForms: List<KolodionForm> =
        listOf(
            KolodionForm(MageArenaNpcs.kolodionHuman, 3, 15, 10, 20, 15),
            KolodionForm(MageArenaNpcs.kolodionOgre, 65, 55, 70, 72, 50),
            KolodionForm(MageArenaNpcs.kolodionSpider, 78, 69, 78, 47, 70),
            KolodionForm(MageArenaNpcs.kolodionGhost, 78, 28, 23, 58, 30),
            KolodionForm(MageArenaNpcs.kolodionDemon, 107, 85, 98, 105, 80),
        )

    public val kolodionIds: Set<Int> = kolodionForms.mapTo(mutableSetOf()) { it.npc.id }
    public val followerIds: Set<Int> =
        setOf(MageArenaNpcs.zachariah.id, MageArenaNpcs.derwen.id, MageArenaNpcs.porazdir.id)
    public val mageArena: CoordGrid = CoordGrid(3105, 3934, 0)
}
