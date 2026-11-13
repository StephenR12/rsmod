package org.rsmod.content.skills.thieving

import jakarta.inject.Inject
import org.rsmod.api.clue.Cache233ClueSteps
import org.rsmod.api.clue.ClueInventory
import org.rsmod.api.clue.ClueTier
import org.rsmod.api.config.refs.objs
import org.rsmod.api.player.diary.DiaryDifficulty
import org.rsmod.api.player.diary.DiaryRegion
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjType
import org.rsmod.map.CoordGrid

internal data class CastleDrop(val item: ObjType, val weight: Int, val standard: Int, val medium: Int, val hard: Int)

internal object CityChestGoods : ObjReferences() {
    val wire = find("dorgesh_wire")
    val oil = find("oil_lantern_unlit")
    val bullseye = find("bullseye_lantern_unlit")
    val helmet = find("cave_goblin_mining_helmet_unlit")
    val legs = find("dorgesh_frog_armour_bottoms")
    val top = find("dorgesh_frog_armour_top")
    val orb = find("dorgesh_light_bulb")
    val emptyOrb = find("dorgesh_lightbulb_nofilament")
    val rich = listOf(objs.uncut_sapphire,objs.uncut_emerald,objs.uncut_ruby,objs.uncut_diamond,objs.uncut_opal,objs.uncut_jade,objs.uncut_red_topaz,bullseye,helmet,legs,top,objs.iron_bar,wire,orb,emptyOrb)
    val average = listOf(objs.coins,objs.coins,objs.coins,oil,bullseye,helmet,wire)
    val oldak = listOf("airrune","mindrune","waterrune","earthrune","firerune","bodyrune","cosmicrune","chaosrune","lawrune","naturerune","deathrune","dorgesh_teleport_artifact","stafforb","dorgesh_wire","dorgesh_lightbulb_nofilament","dorgesh_light_bulb","air_talisman","water_talisman","earth_talisman","fire_talisman").map { find(it) }
    val zanik = listOf("coins","rope","cave_goblin_mining_helmet_unlit","dorgesh_frog_armour_top","dorgesh_frog_armour_bottoms","oil_lantern_unlit","bullseye_lantern_unlit","newcomer_map","dttd_bone_crossbow_bolt","dttd_bone_crossbow","dttd_bone_dagger","hammer","big_bones","ham_hood","ham_badge","ham_shirt","ham_robe","ham_gloves","ham_cloak","ham_boots","spade","bucket_empty","dorgesh_wire").map { find(it) }
    val rings = (0..5).map { find(if (it == 0) "ring_of_wealth_i" else "ring_of_wealth_i$it") }
    val potion = find("2doseprayerrestore")
    private fun drop(key: String, weight: Int, standard: Int, medium: Int, hard: Int) = CastleDrop(find(key),weight,standard,medium,hard)
    val castle = listOf(
        drop("naturerune",11,30,40,50), drop("cert_red_spiders_eggs",10,4,6,7),
        drop("lawrune",10,30,40,50),drop("cert_coal",8,15,20,25),drop("coins",6,3375,4500,5625),
        drop("cert_vile_ashes",3,11,15,18),drop("cert_uncut_diamond",3,2,3,3),drop("cert_uncut_diamond",2,3,5,6),
        drop("cert_uncut_emerald",2,7,10,12),drop("blighted_sack_icebarrage",2,9,13,16),
        drop("cert_iron_ore",1,30,40,50),drop("chaosrune",1,45,60,75),drop("deathrune",1,37,50,62),
        drop("cert_blighted_mantaray",1,15,20,25),drop("cert_blighted_anglerfish",1,11,15,18),
        drop("cert_uncut_sapphire",1,11,15,18),drop("2doseprayerrestore",2,1,1,1),drop("cert_dragonstone",1,2,2,2),
    )
}

/** The Zanik and Oldak houses use their own tables despite sharing the average chest type. */
internal class CityChestLoot @Inject constructor(private val clues:ClueInventory,private val steps:Cache233ClueSteps) {
    fun dorgesh(access:ProtectedAccess,rich:Boolean,coords:CoordGrid):InvObj = with(access) {
        val table = when {
            rich -> CityChestGoods.rich
            coords == CoordGrid(2706,5364,2) -> CityChestGoods.oldak
            coords == CoordGrid(2743,5350,2) || coords == CoordGrid(2746,5350,2) -> CityChestGoods.zanik
            else -> CityChestGoods.average
        }
        val index = random.of(table.size)
        val item = table[index]
        if (table === CityChestGoods.zanik && index == 0) clue(this,ClueTier.Easy)?.let { return InvObj(it) }
        val count = when {
            item.id == objs.coins.id -> random.of(1..250)
            item.id == CityChestGoods.wire.id && player.vars[DiaryRegion.Lumbridge.tier(DiaryDifficulty.Elite).complete] != 0 -> 2
            table === CityChestGoods.oldak && index < 6 -> random.of(10..19)
            table === CityChestGoods.oldak && index < 9 -> random.of(5..9)
            table === CityChestGoods.oldak && index < 11 -> random.of(2..4)
            table === CityChestGoods.zanik && index == 8 -> random.of(1..89)
            else -> 1
        }
        InvObj(item,count)
    }

    fun wilderness(access:ProtectedAccess):InvObj = with(access) {
        var roll = random.of(66)
        val drop = CityChestGoods.castle.first { roll -= it.weight; roll < 0 }
        if (drop.item.id == CityChestGoods.potion.id) {
            val chances = if (CityChestGoods.rings.any { it in worn }) 2 else 1
            if (random.of(3) < chances) clue(this,ClueTier.Hard)?.let { return InvObj(it) }
        }
        val count = when {
            player.vars[DiaryRegion.Wilderness.tier(DiaryDifficulty.Hard).complete] != 0 -> drop.hard
            player.vars[DiaryRegion.Wilderness.tier(DiaryDifficulty.Medium).complete] != 0 -> drop.medium
            else -> drop.standard
        }
        InvObj(drop.item,count)
    }

    private fun clue(access:ProtectedAccess,tier:ClueTier):ObjType? = with(access) {
        clues.reward(clues.state(player,tier),tier) { steps.select(tier,random.of(steps.steps(tier).size)) }.obj
    }
}
