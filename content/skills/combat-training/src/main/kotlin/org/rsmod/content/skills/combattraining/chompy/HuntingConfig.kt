package org.rsmod.content.skills.combattraining.chompy

import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.diary.DiaryDifficulty
import org.rsmod.api.player.diary.DiaryRegion
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.api.type.editors.obj.ObjEditor
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.spot.SpotanimReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.npc.NpcMode

internal object HuntingObjects : ObjReferences() {
    val bellows = listOf("empty_ogre_bellows","filled_ogre_bellow1","filled_ogre_bellow2","filled_ogre_bellow3").map { find(it) }
    val bait = listOf("bloated_toad","100_jubbly_balloon_toad_brown","100_jubbly_balloon_toad_white").map { find(it) }
    val rocks = listOf(find("swamprocks1"),find("rock"))
    val hats = (1..18).map { find("cbhat$it") }
    val chick = find("chompybird_pet")
    val jubbly = find("100_jubbly_meat_raw")
}
internal object HuntingScenery : LocReferences() {
    val swamp = find("swampbubbles")
    val cave = find("rantzogrecaveentrance")
    val exits = listOf(find("rantzogrecaveexitr"),find("rantzogrecaveexitl"))
    val chest = find("chompybird_chest")
    val openChest = find("chompybird_chest_open")
}
internal object HuntingNpcs : NpcReferences() {
    val rantz = find("rantz") // Parent of the pre-quest and post-quest claim variants.
    val toad = find("toad")
    val baits = listOf("bloated_toad","100_jubbly_balloon_toad_brown","100_jubbly_balloon_toad_white").map { find(it) }
    val live = listOf(find("chompybird"),find("100_jubbly_bird"))
    val dead = listOf(find("chompybird_dead"),find("100_jubbly_bird_dead"))
}
internal object HuntingAnims : SeqReferences() {
    val fill = find("human_chompybird_ogrebellows")
    val inflate = find("chompy_toad_inflate")
    val burst = find("chompy_toad_explode")
    val land = listOf(find("chompy_update_fly_down"),find("jubbly_update_fly_down"))
    val fly = listOf(find("chompy_update_fly_up"),find("jubbly_update_fly_up"))
    val death = listOf(find("chompy_update_death"),find("jubbly_update_death"))
}
internal object HuntingArrows : SpotanimReferences() {
    val travel = find("ogre_arrow_travel")
    val launch = find("ogre_arrow_launch")
}
internal object HuntingVars : VarpReferences() {
    val kills = find("chompy_hunting_kills")
    val arrowDay = find("rantz_arrow_day")
    val wool = find("jubbly_wool_uses")
}
internal object HuntingVarDefs : VarpBuilder() {
    init {
        for (name in listOf("chompy_hunting_kills","rantz_arrow_day","jubbly_wool_uses"))
            build(name) { permanent = true; transmitNever = true }
    }
}
internal object HuntingBits : VarBitReferences() {
    val arrowsClaimed = find("western_rantz_arrows")
    val skrach = find("100_ogre_prog")
    val collected = (1..10).map { find("collection_item_cbhat$it") }
}
internal object HuntingObjConfigs : ObjEditor() {
    init {
        for (bait in HuntingObjects.bait) edit(bait) { param[params.no_bank] = 1 }
        for (arrow in listOf(objs.bronze_brutal,objs.iron_brutal,objs.steel_brutal,objs.black_brutal,objs.mithril_brutal,objs.adamant_brutal,objs.rune_brutal))
            edit(arrow) { param[params.proj_travel] = HuntingArrows.travel; param[params.proj_launch] = HuntingArrows.launch }
    }
}
internal object HuntingNpcConfigs : NpcEditor() {
    init {
        for ((i,bird) in HuntingNpcs.live.withIndex()) edit(bird) {
            hitpoints = if (i == 0) 10 else 20
            defence = if (i == 0) 3 else 6
            attack = 5
            strength = 5
            defaultMode = NpcMode.None
            param[params.death_anim] = HuntingAnims.death[i]
        }
        edit(HuntingNpcs.toad) { defaultMode=NpcMode.Wander; wanderRange=2 }
        for (npc in HuntingNpcs.baits + HuntingNpcs.dead) edit(npc) { defaultMode=NpcMode.None }
    }
}
internal object FeldipHuntingPopulation : MapNpcSpawnBuilder() {
    override fun onPackMapTask() { resourceFile<Rantz>("hunting-npcs.toml") }
}
internal fun Player.canHuntChompies() = members && vars[varps.big_chompy_bird_hunting] >= 65
internal fun Player.westernTier() = DiaryDifficulty.entries.lastOrNull { vars[DiaryRegion.Western.tier(it).reward] != 0 }
