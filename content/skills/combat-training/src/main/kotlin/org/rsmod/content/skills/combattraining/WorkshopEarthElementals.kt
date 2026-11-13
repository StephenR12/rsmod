package org.rsmod.content.skills.combattraining

import jakarta.inject.Inject
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.stats
import org.rsmod.api.death.NpcDropRoll
import org.rsmod.api.death.NpcDropRolls.item
import org.rsmod.api.death.NpcDropRolls.nothing
import org.rsmod.api.death.NpcDropRolls.sequence
import org.rsmod.api.death.NpcDropRolls.weighted
import org.rsmod.api.death.NpcDropTable
import org.rsmod.api.death.NpcDropTableRegistry
import org.rsmod.api.death.NpcHerbDropRolls
import org.rsmod.api.death.NpcWeightedDrop
import org.rsmod.api.npc.events.NpcHitEvents
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.opPlayer2
import org.rsmod.api.player.stat.stat
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.content.skills.mining.scripts.Mining
import org.rsmod.content.skills.mining.scripts.Mining.Companion.pickaxeAnim
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object WorkshopEarthNpcs : NpcReferences() {
    val roaming = find("elemental_earth")
    val ore = find("elem1_qip_earth_elemental_rock_version")
    val dormant = find("elem1_qip_earth_elemental_rock_version_rock")
}

internal object WorkshopEarthItems : ObjReferences() {
    val rock = find("swamprocks1")
    val ore = find("elemental_workshop_ore")
    val staff = find("staff_of_earth")
}

internal object WorkshopEarthAnims : SeqReferences() {
    val emerge = find("earth_elemental_rework_emerge")
    val attack = find("earth_elemental_rework_attack")
    val block = find("earth_elemental_rework_defend")
    val death = find("earth_elemental_rework_death")
}

internal object WorkshopEarthStats : NpcEditor() {
    init {
        for (form in
            listOf(
                WorkshopEarthNpcs.roaming,
                WorkshopEarthNpcs.ore,
                WorkshopEarthNpcs.dormant,
            )) edit(form) {
            hitpoints = 35
            attack = 20
            strength = 35
            defence = 35
            magic = 10
            ranged = 30
            respawnRate = if (form == WorkshopEarthNpcs.roaming) 50 else 5
            if (form == WorkshopEarthNpcs.dormant) defaultMode = NpcMode.None
            param[params.attackrate] = 6
            param[params.attack_anim] = WorkshopEarthAnims.attack
            param[params.defend_anim] = WorkshopEarthAnims.block
            param[params.death_anim] = WorkshopEarthAnims.death
            param[params.elemental_weakness_type] = constants.elemental_weakness_earth
            param[params.elemental_weakness_percent] = 50
        }
    }
}

class WorkshopEarthElementals
@Inject
constructor(
    private val types: NpcTypeList,
    private val items: ObjTypeList,
    private val interactions: AiPlayerInteractions,
    private val registry: NpcDropTableRegistry,
) : PluginScript() {
    override fun ScriptContext.startup() {
        val rock = item(WorkshopEarthItems.rock)
        val ore = NpcDropTable(sequence(rock, item(WorkshopEarthItems.ore)))
        registry.register(listOf(WorkshopEarthNpcs.dormant, WorkshopEarthNpcs.ore), ore)
        registry.register(WorkshopEarthNpcs.roaming, NpcDropTable(sequence(rock, roamingRoll())))
        onEvent<NpcHitEvents.ResolveDamage>(WorkshopEarthNpcs.dormant.id) {
            if (npc.visType.id == WorkshopEarthNpcs.dormant.id) damage = 0
        }
        onOpNpc1(WorkshopEarthNpcs.dormant) {
            val rockNpc = it.npc
            if (!player.members || rockNpc.visType.id != WorkshopEarthNpcs.dormant.id)
                return@onOpNpc1
            if (player.stat(stats.mining) < 20) {
                mes("You need a Mining level of 20 to mine this rock.")
                return@onOpNpc1
            }
            val pick = Mining.findPickaxe(player, items)
            if (pick == null) {
                mes("You need a pickaxe that you can use to mine this rock.")
                return@onOpNpc1
            }
            anim(items[pick].pickaxeAnim)
            val active = types[WorkshopEarthNpcs.ore]
            rockNpc.transmog(active, Int.MAX_VALUE)
            rockNpc.copyStats(active)
            rockNpc.anim(WorkshopEarthAnims.emerge)
            rockNpc.opPlayer2(player, interactions)
            mes("The rock springs to life!")
        }
    }

    private fun roamingRoll(): NpcDropRoll =
        weighted(
            NpcWeightedDrop(13, item(objs.earth_rune, 15..15)),
            NpcWeightedDrop(9, item(objs.nature_rune, 2..2)),
            NpcWeightedDrop(7, item(objs.chaos_rune, 2..2)),
            NpcWeightedDrop(6, item(objs.law_rune)),
            NpcWeightedDrop(4, item(objs.death_rune)),
            NpcWeightedDrop(3, item(objs.earth_rune, 20..20)),
            NpcWeightedDrop(1, item(objs.blood_rune)),
            NpcWeightedDrop(14, NpcHerbDropRolls.standard()),
            NpcWeightedDrop(36, item(objs.coins, 12..12)),
            NpcWeightedDrop(3, item(objs.coins, 42..42)),
            NpcWeightedDrop(2, item(WorkshopEarthItems.staff)),
            NpcWeightedDrop(2, standardCombatGems(CyclopsItems.chaosTalisman)),
            NpcWeightedDrop(28, nothing()),
        )
}
