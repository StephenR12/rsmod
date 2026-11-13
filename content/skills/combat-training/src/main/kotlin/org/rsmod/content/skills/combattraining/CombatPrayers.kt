package org.rsmod.content.skills.combattraining

import jakarta.inject.Inject
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.hunt.Hunt
import org.rsmod.api.npc.hit.modifier.StandardNpcHitModifier
import org.rsmod.api.npc.hit.queueHit
import org.rsmod.api.npc.isValidTarget
import org.rsmod.api.player.disablePrayers
import org.rsmod.api.player.events.canHitNearbyPlayer
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.isValidTarget
import org.rsmod.api.player.mapMultiway
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.stat.statSub
import org.rsmod.api.random.GameRandom
import org.rsmod.api.script.onPlayerHit
import org.rsmod.api.type.refs.spot.SpotanimReferences
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.hit.HitType
import org.rsmod.game.type.hunt.HuntVis
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object RetributionSpots : SpotanimReferences() {
    val blast = find("retribution_spotanim")
}

public class CombatPrayers
@Inject
constructor(
    private val players: PlayerList,
    private val npcs: NpcList,
    private val areas: AreaChecker,
    private val hunt: Hunt,
    private val bus: EventBus,
    private val random: GameRandom,
    private val modifier: StandardNpcHitModifier,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onPlayerHit {
            if (damage <= 0 || hit.type == HitType.Typeless) return@onPlayerHit
            val attacker = if (hit.isFromPlayer) hit.resolvePlayerSource(players) else null
            if (
                attacker != null &&
                    attacker !== player &&
                    attacker.vars[varbits.smite] != 0 &&
                    damage >= 4
            ) {
                player.statSub(stats.prayer, damage / 4, 0)
                if (player.stat(stats.prayer) == 0) player.disablePrayers()
            }
            if (player.hitpoints > 0 || player.vars[varbits.retribution] == 0) return@onPlayerHit
            val npc = if (hit.isFromNpc) hit.resolveNpcSource(npcs) else null
            if (attacker == null && npc == null) return@onPlayerHit
            player.spotanim(RetributionSpots.blast)
            val max = player.statBase(stats.prayer) / 4
            if (!player.mapMultiway(areas)) {
                if (npc != null && npc.isValidTarget() && player.isWithinDistance(npc, 1))
                    strike(player, npc, max)
                if (
                    attacker != null &&
                        attacker !== player &&
                        attacker.isValidTarget() &&
                        player.isWithinDistance(attacker, 1)
                )
                    strike(player, attacker, max)
                return@onPlayerHit
            }
            for (target in hunt.findNpcs(player.coords, 1, HuntVis.LineOfSight)) {
                if (target.isValidTarget() && target.visType.op.any { it.equals("Attack", true) })
                    strike(player, target, max)
            }
            for (target in hunt.findPlayers(player.coords, 1, HuntVis.LineOfSight)) {
                if (
                    target !== player &&
                        target.isValidTarget() &&
                        (target === attacker || player.canHitNearbyPlayer(target, bus))
                )
                    strike(player, target, max)
            }
        }
    }

    private fun strike(player: Player, npc: Npc, max: Int) {
        npc.queueHit(player, 1, HitType.Typeless, random.of(0..max), modifier)
    }

    private fun strike(player: Player, target: Player, max: Int) {
        target.queueHit(player, 1, HitType.Typeless, random.of(0..max))
    }
}
