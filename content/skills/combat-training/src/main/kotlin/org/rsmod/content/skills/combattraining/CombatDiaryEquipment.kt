package org.rsmod.content.skills.combattraining

import org.rsmod.api.combat.commons.npc.inSlayerWilderness
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.events.interact.HeldEquipEvents
import org.rsmod.api.player.hands
import org.rsmod.api.player.hat
import org.rsmod.api.player.legs
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.torso
import org.rsmod.api.player.worn.EquipmentChecks
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.entity.Player
import org.rsmod.game.type.obj.Wearpos
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object WarriorsGuildLocs : LocReferences() {
    val rangeDoor = find("ranging_guild_door")
    val campGates = listOf(find("lathastraining_gatel"), find("lathastraining_gater"))
    val frontDoor = find("warguild_door_front")
}

internal object TrainingCampVars : VarpReferences() {
    val quest = find("biohazard")
}

class CombatDiaryEquipment : PluginScript() {
    override fun ScriptContext.startup() {
        onEvent<HeldEquipEvents.WearposChange> {
            if (!player.members || player.worn[wearpos.slot]?.id != objType.id) return@onEvent
            val c = player.coords
            if (
                wearpos == Wearpos.Back &&
                    c.inSlayerWilderness() &&
                    ((objType.id in 4315..4413 && objType.id % 2 == 1) ||
                        objType.id in setOf(20211, 20214, 20217))
            ) {
                player.completeDiaryTask(DiaryTask.WildernessTeamCape)
            }
            when (objType.id) {
                11200 ->
                    if (c.inDwarvenMine()) player.completeDiaryTask(DiaryTask.DwarvenMineHelmet)
                6570,
                21295,
                24223,
                24224 -> if (c.inTzHaarCity()) player.completeDiaryTask(DiaryTask.TzHaarCityCape)
                12658 -> player.completeDiaryTask(DiaryTask.EquipUpgradedIban)
                else ->
                    if (wearpos in voidSlots && player.wearsVoidSet())
                        player.completeDiaryTask(DiaryTask.FullVoid)
            }
        }
        onOpLoc1(WarriorsGuildLocs.rangeDoor) {
            if (!player.members) return@onOpLoc1
            val entering = player.coords.z - player.coords.x > 780
            if (entering && player.stat(stats.ranged) < 40) {
                mes("You need a Ranged level of 40 to enter the Ranging Guild.")
                return@onOpLoc1
            }
            telejump(if (entering) CoordGrid(2658, 3437) else CoordGrid(2657, 3438))
            if (entering) player.completeDiaryTask(DiaryTask.EnterRangingGuild)
        }
        for (gate in WarriorsGuildLocs.campGates) onOpLoc1(gate) {
            if (!player.members) return@onOpLoc1
            val entering = player.coords.z <= 3356
            if (entering && player.vars[TrainingCampVars.quest] < 16) {
                mes("You need the king's permission to enter his training camp.")
                return@onOpLoc1
            }
            telejump(CoordGrid(it.loc.x, if (entering) 3357 else 3356))
            if (entering) player.completeDiaryTask(DiaryTask.EnterCombatTrainingCamp)
        }
        onOpLoc1(WarriorsGuildLocs.frontDoor) {
            if (!player.members) return@onOpLoc1
            if (player.coords.x < 2877) {
                telejump(CoordGrid(2877, 3546))
                return@onOpLoc1
            }
            val attack = player.statBase(stats.attack)
            val strength = player.statBase(stats.strength)
            if (attack + strength < 130 && maxOf(attack, strength) < 99) {
                mes(
                    "You need a combined Attack and Strength level of 130, or level 99 in either skill, to join the Warriors' Guild."
                )
                return@onOpLoc1
            }
            telejump(CoordGrid(2876, 3546))
            player.completeDiaryTask(DiaryTask.EnterWarriorsGuild)
        }
    }

    private fun Player.wearsVoidSet(): Boolean =
        with(EquipmentChecks) {
            (isVoidMageHelm(hat) || isVoidMeleeHelm(hat) || isVoidRangerHelm(hat)) &&
                isVoidTop(torso) &&
                isVoidRobe(legs) &&
                isVoidGloves(hands)
        }

    private fun CoordGrid.inDwarvenMine(): Boolean =
        level == 0 &&
            ((x in 3008..3064 && z in 9760..9855) ||
                (x in 2960..3007 && z in 9800..9855) ||
                (x in 2992..3007 && z in 9792..9799))

    private fun CoordGrid.inTzHaarCity(): Boolean = level == 0 && x in 2368..2559 && z in 5056..5183

    private companion object {
        val voidSlots = setOf(Wearpos.Hat, Wearpos.Torso, Wearpos.Legs, Wearpos.Hands)
    }
}
