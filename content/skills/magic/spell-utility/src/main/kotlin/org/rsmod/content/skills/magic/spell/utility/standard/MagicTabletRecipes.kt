package org.rsmod.content.skills.magic.spell.utility.standard

import org.rsmod.api.combat.commons.magic.MagicSpell
import org.rsmod.api.config.refs.objs
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.type.obj.ObjType

internal enum class TabletLectern(val spellbook:Int?) { House(null), Arceuus(3), Ancient(1), Lunar(2) }
internal data class TabletRecipe(val tablet:ObjType,val lectern:TabletLectern,val fixtures:Set<Int>,val level:Int,val xp:Double,val runes:List<MagicSpell.ObjRequirement>)

internal object MagicTabletRecipes:ObjReferences() {
    val darkEssence = find("arceuus_essence_block_dark")
    private fun recipe(symbol:String,lectern:TabletLectern,fixtures:Set<Int>,level:Int,xp:Double,vararg costs:Pair<ObjType,Int>) =
        TabletRecipe(find(symbol),lectern,fixtures,level,xp,costs.map { (obj,count) -> MagicSpell.ObjRequirement(obj,count,null) })

    // In the 233.4 recipes, several enchanting and lunar tablets differ from their parent spells.
    val all = listOf(
        recipe("poh_tablet_varrockteleport", TabletLectern.House, setOf(1, 2, 3, 4, 5, 6, 7, 8), 25, 35.0, objs.air_rune to 3, objs.fire_rune to 1, objs.law_rune to 1),
        recipe("poh_tablet_lumbridgeteleport", TabletLectern.House, setOf(2, 4, 6, 8), 31, 41.0, objs.air_rune to 3, objs.earth_rune to 1, objs.law_rune to 1),
        recipe("poh_tablet_faladorteleport", TabletLectern.House, setOf(2, 4, 6, 8), 37, 48.0, objs.air_rune to 3, objs.water_rune to 1, objs.law_rune to 1),
        recipe("poh_tablet_camelotteleport", TabletLectern.House, setOf(4, 6, 8), 45, 55.5, objs.air_rune to 5, objs.law_rune to 1),
        recipe("poh_tablet_kourendteleport", TabletLectern.House, setOf(4, 6, 8), 48, 58.0, objs.fire_rune to 1, objs.water_rune to 1, objs.law_rune to 2),
        recipe("poh_tablet_ardougneteleport", TabletLectern.House, setOf(4, 6, 8), 51, 61.0, objs.water_rune to 2, objs.law_rune to 2),
        recipe("poh_tablet_fortisteleport", TabletLectern.House, setOf(6, 8), 54, 64.0, objs.fire_rune to 1, objs.earth_rune to 1, objs.law_rune to 2),
        recipe("poh_tablet_watchtowerteleport", TabletLectern.House, setOf(6, 8), 58, 68.0, objs.earth_rune to 2, objs.law_rune to 2),
        recipe("poh_tablet_teleporttohouse", TabletLectern.House, setOf(6, 8), 40, 30.0, objs.air_rune to 1, objs.earth_rune to 1, objs.law_rune to 1),
        recipe("poh_tablet_enchantsapphire", TabletLectern.House, setOf(1, 2, 3, 4, 5, 6, 7, 8), 7, 13.2, objs.water_rune to 1, objs.cosmic_rune to 1),
        recipe("poh_tablet_enchantemerald", TabletLectern.House, setOf(3, 5, 7, 8), 27, 27.8, objs.air_rune to 3, objs.cosmic_rune to 1),
        recipe("poh_tablet_enchantruby", TabletLectern.House, setOf(5, 7, 8), 49, 44.3, objs.fire_rune to 5, objs.cosmic_rune to 1),
        recipe("poh_tablet_enchantdiamond", TabletLectern.House, setOf(5, 7, 8), 57, 50.3, objs.earth_rune to 10, objs.cosmic_rune to 1),
        recipe("poh_tablet_enchantdragonstone", TabletLectern.House, setOf(7, 8), 68, 58.5, objs.earth_rune to 15, objs.water_rune to 15, objs.cosmic_rune to 1),
        recipe("poh_tablet_enchantonyx", TabletLectern.House, setOf(7, 8), 87, 72.8, objs.earth_rune to 20, objs.fire_rune to 20, objs.cosmic_rune to 1),
        recipe("poh_tablet_bonestobananas", TabletLectern.House, setOf(3, 5, 7, 8), 15, 25.0, objs.earth_rune to 2, objs.water_rune to 2, objs.nature_rune to 1),
        recipe("poh_tablet_bonestopeaches", TabletLectern.House, setOf(7, 8), 60, 35.5, objs.earth_rune to 4, objs.water_rune to 4, objs.nature_rune to 2),
        recipe("teletab_lumbridge", TabletLectern.Arceuus, emptySet(), 6, 10.0, objs.earth_rune to 2, objs.law_rune to 1),
        recipe("teletab_draynor", TabletLectern.Arceuus, emptySet(), 17, 16.0, objs.earth_rune to 1, objs.water_rune to 1, objs.law_rune to 1),
        recipe("teletab_battlefront", TabletLectern.Arceuus, emptySet(), 23, 19.0, objs.earth_rune to 1, objs.fire_rune to 1, objs.law_rune to 1),
        recipe("teletab_mind_altar", TabletLectern.Arceuus, emptySet(), 28, 22.0, objs.mind_rune to 2, objs.law_rune to 1),
        recipe("teletab_salve", TabletLectern.Arceuus, emptySet(), 40, 30.0, objs.soul_rune to 2, objs.law_rune to 1),
        recipe("teletab_fenk", TabletLectern.Arceuus, emptySet(), 48, 50.0, objs.earth_rune to 1, objs.soul_rune to 1, objs.law_rune to 1),
        recipe("teletab_westardy", TabletLectern.Arceuus, emptySet(), 61, 68.0, objs.soul_rune to 2, objs.law_rune to 2),
        recipe("teletab_harmony", TabletLectern.Arceuus, emptySet(), 65, 74.0, objs.nature_rune to 1, objs.soul_rune to 1, objs.law_rune to 1),
        recipe("teletab_cemetery", TabletLectern.Arceuus, emptySet(), 71, 82.0, objs.blood_rune to 1, objs.soul_rune to 1, objs.law_rune to 1),
        recipe("teletab_barrows", TabletLectern.Arceuus, emptySet(), 83, 90.0, objs.blood_rune to 1, objs.soul_rune to 2, objs.law_rune to 2),
        recipe("teletab_ape", TabletLectern.Arceuus, emptySet(), 90, 100.0, objs.blood_rune to 2, objs.soul_rune to 2, objs.law_rune to 2),
        recipe("tablet_paddewa", TabletLectern.Ancient, emptySet(), 54, 64.0, objs.air_rune to 1, objs.fire_rune to 1, objs.law_rune to 2),
        recipe("tablet_senntisten", TabletLectern.Ancient, emptySet(), 60, 70.0, objs.soul_rune to 1, objs.law_rune to 2),
        recipe("tablet_kharyll", TabletLectern.Ancient, emptySet(), 66, 76.0, objs.blood_rune to 1, objs.law_rune to 2),
        recipe("tablet_lassar", TabletLectern.Ancient, emptySet(), 72, 82.0, objs.water_rune to 4, objs.law_rune to 2),
        recipe("tablet_dareeyak", TabletLectern.Ancient, emptySet(), 78, 88.0, objs.air_rune to 2, objs.fire_rune to 3, objs.law_rune to 2),
        recipe("tablet_carrallangar", TabletLectern.Ancient, emptySet(), 84, 94.0, objs.soul_rune to 2, objs.law_rune to 2),
        recipe("tablet_annakarl", TabletLectern.Ancient, emptySet(), 90, 100.0, objs.blood_rune to 2, objs.law_rune to 2),
        recipe("tablet_ghorrock", TabletLectern.Ancient, emptySet(), 96, 106.0, objs.water_rune to 8, objs.law_rune to 2),
        recipe("lunar_tablet_moonclan_teleport", TabletLectern.Lunar, emptySet(), 69, 66.0, objs.earth_rune to 2, objs.astral_rune to 2, objs.law_rune to 1),
        recipe("lunar_tablet_ourania_teleport", TabletLectern.Lunar, emptySet(), 71, 69.0, objs.earth_rune to 6, objs.astral_rune to 2, objs.law_rune to 1),
        recipe("lunar_tablet_waterbirth_teleport", TabletLectern.Lunar, emptySet(), 72, 71.0, objs.water_rune to 1, objs.astral_rune to 2, objs.law_rune to 1),
        recipe("lunar_tablet_barbarian_teleport", TabletLectern.Lunar, emptySet(), 75, 72.0, objs.fire_rune to 3, objs.astral_rune to 2, objs.law_rune to 2),
        recipe("lunar_tablet_khazard_teleport", TabletLectern.Lunar, emptySet(), 78, 76.0, objs.water_rune to 4, objs.astral_rune to 2, objs.law_rune to 2),
        recipe("lunar_tablet_fishing_guild_teleport", TabletLectern.Lunar, emptySet(), 85, 80.0, objs.water_rune to 10, objs.astral_rune to 3, objs.law_rune to 3),
        recipe("lunar_tablet_catherby_teleport", TabletLectern.Lunar, emptySet(), 87, 89.0, objs.water_rune to 10, objs.astral_rune to 3, objs.law_rune to 3),
        recipe("lunar_tablet_ice_plateau_teleport", TabletLectern.Lunar, emptySet(), 89, 92.0, objs.water_rune to 8, objs.astral_rune to 3, objs.law_rune to 3),
    )
}
