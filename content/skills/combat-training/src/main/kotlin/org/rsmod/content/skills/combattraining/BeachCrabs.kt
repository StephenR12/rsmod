package org.rsmod.content.skills.combattraining

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.npc.queueCombatRetaliate
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.npc.isOutOfCombat
import org.rsmod.api.player.isInCombat
import org.rsmod.api.player.isValidTarget
import org.rsmod.api.repo.player.PlayerRepository
import org.rsmod.api.script.onAiTimer
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.timer.TimerReferences
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object CrabNpcs : NpcReferences() {
    val pairs =
        listOf(
                "horror_rockcrab",
                "horror_rockcrab_small",
                "kourend_rockcrab",
                "zeah_sandcrab",
                "zeah_sandcrab_small",
            )
            .map { find("${it}_inactive") to find(it) }
}

internal object CrabConfig : NpcEditor() {
    init {
        for ((rocks, crab) in CrabNpcs.pairs) {
            edit(rocks) {
                defaultMode = NpcMode.None
                timer = 2
                respawnRate = 50
            }
            edit(crab) { respawnRate = 50 }
        }
    }
}

internal object CrabTimers : TimerReferences() {
    val tolerance = find("beach_crab_tolerance")
}

internal object CrabSeqs : SeqReferences() {
    val reveal = find("horror_crab_reveal")
}

class BeachCrabs
@Inject
constructor(
    private val players: PlayerRepository,
    private val types: NpcTypeList,
    private val clock: MapClock,
) : PluginScript() {
    private data class Tolerance(val anchor: CoordGrid, val previous: CoordGrid?, val expires: Int)

    private val areas = mutableMapOf<Player, Tolerance>()

    override fun ScriptContext.startup() {
        for ((rocks, crab) in CrabNpcs.pairs) {
            onAiTimer(rocks) {
                if (npc.hitpoints == 0) return@onAiTimer
                val player =
                    players.findAll(ZoneKey.from(npc.coords), 1).firstOrNull {
                        it.members &&
                            it.isValidTarget() &&
                            !it.isInvisible &&
                            npc.isWithinDistance(it, 1) &&
                            (it.vars[varbits.multiway_indicator] != 0 || !it.isInCombat()) &&
                            aggressive(it)
                    } ?: return@onAiTimer
                val active = types[crab]
                npc.transmog(active, Int.MAX_VALUE)
                npc.copyStats(active)
                npc.anim(CrabSeqs.reveal)
                npc.queueCombatRetaliate(player)
            }
            onAiTimer(crab) {
                if (npc.id != rocks.id || npc.hitpoints == 0 || !npc.isOutOfCombat())
                    return@onAiTimer
                if (npc.coords != npc.spawnCoords) {
                    npc.walk(npc.spawnCoords)
                } else {
                    npc.resetTransmog()
                    npc.resetMode()
                }
            }
        }
        onPlayerSoftTimer(CrabTimers.tolerance) {
            val area = areas[player]
            if (
                area == null ||
                    (!player.isWithinDistance(area.anchor, 10) &&
                        (area.previous == null || !player.isWithinDistance(area.previous, 10)))
            ) {
                if (area != null && player.isWithinDistance(area.anchor, 25)) {
                    areas[player] = Tolerance(player.coords, area.anchor, clock + 1000)
                } else {
                    areas.remove(player)
                    player.clearSoftTimer(CrabTimers.tolerance)
                }
            }
        }
        onPlayerLogout { areas.remove(player) }
    }

    private fun aggressive(player: Player): Boolean {
        val area =
            areas.getOrPut(player) {
                player.softTimer(CrabTimers.tolerance, 1)
                Tolerance(player.coords, null, clock + 1000)
            }
        return clock.cycle < area.expires
    }
}
