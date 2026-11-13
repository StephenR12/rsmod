package org.rsmod.content.bosses.magearena

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.magic.MageArenaTraining
import org.rsmod.api.combat.commons.npc.queueCombatRetaliate
import org.rsmod.api.config.refs.objs
import org.rsmod.api.hunt.Hunt
import org.rsmod.api.npc.isOutOfCombat
import org.rsmod.api.player.back
import org.rsmod.api.player.isOutOfCombat
import org.rsmod.api.player.isValidTarget
import org.rsmod.api.script.onAiApPlayer2
import org.rsmod.api.script.onAiOpPlayer2
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onModifyNpcHit
import org.rsmod.api.script.onNpcHit
import org.rsmod.api.script.onNpcTimer
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.type.refs.timer.TimerReferences
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.game.type.hunt.HuntVis
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object BattleMageTimers : TimerReferences() {
    val hunt = find("magearena_battlemage_hunt")
}

public class MageArenaScript
@Inject
constructor(
    private val hunt: Hunt,
    private val combat: MageArenaCombat,
    private val controller: MageArenaController,
    private val players: org.rsmod.game.entity.PlayerList,
) : PluginScript() {
    override fun ScriptContext.startup() {
        val godCapes =
            listOf(
                listOf(
                    objs.saradomin_cape,
                    objs.imbued_saradomin_cape,
                    objs.imbued_saradomin_cape_trouver,
                    objs.saradomin_max_cape,
                    objs.imbued_saradomin_max_cape,
                    objs.imbued_saradomin_max_cape_trouver,
                ),
                listOf(
                    objs.guthix_cape,
                    objs.imbued_guthix_cape,
                    objs.imbued_guthix_cape_trouver,
                    objs.guthix_max_cape,
                    objs.imbued_guthix_max_cape,
                    objs.imbued_guthix_max_cape_trouver,
                ),
                listOf(
                    objs.zamorak_cape,
                    objs.imbued_zamorak_cape,
                    objs.imbued_zamorak_cape_trouver,
                    objs.zamorak_max_cape,
                    objs.imbued_zamorak_max_cape,
                    objs.imbued_zamorak_max_cape_trouver,
                ),
            )
        fun startHunt(npc: Npc) {
            if (npc.id in 1610..1612) npc.timer(BattleMageTimers.hunt, 3)
        }
        onEvent<NpcStateEvents.Create> { startHunt(npc) }
        onEvent<NpcStateEvents.Respawn> { startHunt(npc) }
        for ((i, mage) in ArenaPeople.mages.withIndex()) {
            onAiOpPlayer2(mage) { combat.battleMageAttack(this, it.target, i) }
            onAiApPlayer2(mage) { combat.battleMageAttack(this, it.target, i) }
        }
        onNpcTimer(BattleMageTimers.hunt) {
            if (npc.hitpoints <= 0 || !npc.isOutOfCombat()) return@onNpcTimer
            val i = ArenaPeople.mages.indexOfFirst { it.id == npc.id }
            if (i < 0) return@onNpcTimer
            val target =
                hunt.findPlayers(npc.coords, 8, HuntVis.LineOfSight).firstOrNull { p ->
                    p.isValidTarget() &&
                        p.isOutOfCombat() &&
                        MageArenaTraining.inside(p.coords) &&
                        !controller.inChallenge(p) &&
                        godCapes[i].none { it.id == p.back?.id }
                } ?: return@onNpcTimer
            npc.queueCombatRetaliate(target)
        }
        MageArenaBosses.kolodionForms.forEach { form ->
            onEvent<org.rsmod.api.npc.events.NpcHitEvents.ResolveDamage>(form.npc.id) {
                val owner = controller.challenger(npc)
                if (
                    hit.type != org.rsmod.game.hit.HitType.Magic ||
                        owner == null ||
                        !hit.isFromPlayer ||
                        hit.resolvePlayerSource(players) != owner
                ) {
                    damage = 0
                } else if (
                    controller.state(npc).kolodionPhase < 4 && damage >= npc.hitpoints && damage > 0
                ) {
                    controller.state(npc).transition = true
                    damage = damage.coerceAtMost((npc.hitpoints - 1).coerceAtLeast(0))
                }
            }
            onAiOpPlayer2(form.npc) { combat.kolodionAttack(this, it.target) }
            onAiApPlayer2(form.npc) { combat.kolodionAttack(this, it.target) }
            onModifyNpcHit(form.npc) { controller.modifyKolodionHit(npc, hit) }
            onNpcHit(form.npc) { controller.onKolodionHit(npc, hit) }
        }
        listOf(MageArenaNpcs.zachariah, MageArenaNpcs.derwen, MageArenaNpcs.porazdir).forEach { type
            ->
            onAiOpPlayer2(type) { combat.followerAttack(this, it.target) }
            onAiApPlayer2(type) { combat.followerAttack(this, it.target) }
            onModifyNpcHit(type) { controller.modifyFollowerHit(npc, hit) }
        }
        onAiOpPlayer2(MageArenaNpcs.derwenEnergy) { combat.energyTick(this, it.target) }
        onAiApPlayer2(MageArenaNpcs.derwenEnergy) { combat.energyTick(this, it.target) }
        onOpHeld1(MageArenaObjs.enchantedSymbol) { controller.activateSymbol(this) }
        onEvent<NpcStateEvents.Delete> { controller.remove(npc) }
    }
}
