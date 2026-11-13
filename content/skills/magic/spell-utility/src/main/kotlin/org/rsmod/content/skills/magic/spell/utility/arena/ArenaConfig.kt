package org.rsmod.content.skills.magic.spell.utility.arena

import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.builders.map.obj.MapObjSpawnBuilder
import org.rsmod.api.type.editors.obj.ObjEditor
import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.api.type.builders.area.AreaBuilder
import org.rsmod.api.type.builders.map.area.MapAreaBuilder
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.refs.area.AreaReferences
import org.rsmod.api.type.refs.comp.ComponentReferences
import org.rsmod.api.type.refs.interf.InterfaceReferences
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.spot.SpotanimReferences
import org.rsmod.api.type.refs.timer.TimerReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.area.polygon.VertexCoord
import org.rsmod.map.CoordGrid

internal object ArenaLocs : LocReferences() {
    val doors = listOf("tele","alchem","enchant","grave").map { find("magictraining_${it}door") }
    val exit = find("magictraining_returndoor")
    val entrance = find("magictraining_temple_door")
    val stairsUp = listOf("base","base_mirror").map { find("magictraining_stairs_$it") }
    val stairsDown = listOf("top","top_mirror").map { find("magictraining_stairs_$it") }
    val bank = find("magictraining_bankchest")
    val cupboards = (0..5).map { i -> listOf("","_open").map { find("magictraining_alchem_cupboard$i$it") } }
    val coinCollector = find("magictraining_coin_collector")
    val shapes = (1..4).map { find("magictraining_enchan_shapepile$it") }
    val bones = (1..4).map { find("magictraining_bones_pile$it") }
    val hole = find("magictraining_encha_hole")
    val chute = find("magictraining_grave_foodshute")
}

internal object ArenaItems : ObjReferences() {
    val alchemy = listOf("leather_boots","adamant_kiteshield","adamant_med_helm","emerald","rune_longsword").map { find("magictraining_$it") }
    val coins = find("magictraining_coins")
    val shapes = listOf("cube","cylinder","icosahendron","pentamid").map { find("magictraining_enchan_$it") }
    val stone = find("magictraining_dragonstone")
    val orb = find("magictraining_enchan_shapeorb")
    val bones = (1..4).map { find("magictraining_bones$it") }
    val hats = listOf("dull","energised","full").map { find("magictraining_proghat_$it") }
    val book = find("magictraining_arenalorebook")
}

internal object ArenaNpcs : NpcReferences() {
    val rewards = find("magictraining_guard_rewards")
    val guides = listOf("tele","alchem","encha","grave").map { find("magictraining_guard_$it") }
    val statue = find("magictraining_guard_maze_incomplete")
    val solved = find("magictraining_guard_maze_complete")
}

internal object ArenaUi : InterfaceReferences() {
    val lobby = find("magictraining_main")
    val shop = find("magictraining_shop")
    val rooms = listOf("tele","alchem","encha","grave").map { find("magictraining_$it") }
}
internal object ArenaButtons : ComponentReferences() {
    val rewards = find("magictraining_shop:rewards")
    val confirm = find("magictraining_shop:confirm")
    val quantities = listOf("make1","make5","make10","makeall").map { find("magictraining_shop:$it") }
    val totals = listOf("tele","alch","ench","grave").map { find("magictraining_main:magictrain_${it}_points") }
    val roomPoints = listOf("tele","alchem","encha","grave").map { find("magictraining_$it:magictrain_${it}_pts") }
    val prices = (1..5).map { find("magictraining_alchem:magictrain_alchem_cost$it") }
    val free = (1..5).map { find("magictraining_alchem:magictrain_arrow$it") }
    val shape = listOf("laycube","laycyl","layico","laypent").map { find("magictraining_encha:magictrain_encha_$it") }
    val mazes = find("magictraining_tele:magictrain_maze_number")
}
internal object ArenaBits : VarBitReferences() {
    val selected = find("magictraining_shop")
    val orbs = find("magictraining_enchan_orbtotal")
    val shapes = find("magictraining_ench_convcount")
    val observing = find("magictraining_tele_camstat")
    val solved = find("magictraining_mazesolved")
}
internal object ArenaVars : VarpReferences() {
    val points = listOf("theatre","alchemy","enchantment","graveyard").map { find("mta_points_$it") }
    val quantity = find("makexcrafting")
    val displays = (1..4).map {find("if$it")}
    val boneCycle = find("mta_bone_cycle")
    val enchantRunes = find("mta_pending_enchantment")
    val graveRunes = find("mta_pending_graveyard")
    val streak = find("mta_maze_streak")
    val mazeActive = find("mta_maze_active")
}
internal object ArenaVarBuilds : VarpBuilder() {
    init { for (key in listOf("bone_cycle","pending_enchantment","pending_graveyard","maze_streak","maze_active","points_theatre","points_alchemy","points_enchantment","points_graveyard")) build("mta_$key") { transmitNever = true } }
}
internal object ArenaTimer : TimerReferences() { val tick = find("mta_room_tick") }
internal object ArenaSpots : SpotanimReferences() { val bones = (1..4).map { find("magictraining_bone_drop$it") } }
internal object ArenaAreas : AreaReferences() { val lobby = find("mta_lobby") }
internal object ArenaAreaBuilds : AreaBuilder(){ init { build("mta_lobby") } }
internal object ArenaMap : MapAreaBuilder() {
    override fun onPackMapTask() {
        area(ArenaAreas.lobby) {
            polygon(levels=0..1) {
                for ((x,z) in listOf(3352 to 3294,3375 to 3294,3375 to 3324,3352 to 3324)) vertex(VertexCoord.from(CoordGrid(x,z)))
            }
        }
    }
}

internal enum class ArenaRoom(val requiredLevel:Int,val cap:Int,val priceEnum:Int,val landing:CoordGrid) {
    Theatre(33,8000,2755,CoordGrid(3363,3317)),
    Alchemy(21,16000,2758,CoordGrid(3362,9624,2)),
    Enchantment(7,32000,2757,CoordGrid(3354,9640)),
    Graveyard(15,8000,2756,CoordGrid(3363,9641,1));

    fun contains(c:CoordGrid):Boolean = this!=Theatre && c.x in 3341..3384 && c.z in 9618..9662 && c.level==landing.level
}


internal object ArenaGuardianSpawns : MapNpcSpawnBuilder() {
    override fun onPackMapTask() { resourceFile<MageTrainingArena>("guardians.toml") }
}
internal object ArenaStoneSpawns : MapObjSpawnBuilder() {
    override fun onPackMapTask() { resourceFile<MageTrainingArena>("dragonstones.toml") }
}
internal object ArenaStoneRespawn : ObjEditor() {
    init { edit(ArenaItems.stone) { respawnRate = 800 } }
}

internal object ArenaStatues : NpcEditor() {
    init { for (statue in listOf(ArenaNpcs.statue,ArenaNpcs.solved)) edit(statue) { defaultMode = NpcMode.None } }
}
