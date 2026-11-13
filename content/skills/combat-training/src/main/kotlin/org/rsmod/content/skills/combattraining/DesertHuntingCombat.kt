package org.rsmod.content.skills.combattraining

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.player.queueCombatRetaliate
import org.rsmod.api.combat.formulas.AccuracyFormulae
import org.rsmod.api.config.refs.stats
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.npc.events.NpcMeleeAlternative
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.isValidTarget
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statSub
import org.rsmod.api.script.*
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.spot.SpotanimReferences
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object DesertHuntAnims : SeqReferences() {
    val dust = find("dustdevil_attack")
    val arrow = find("contact_locust_bow_attack")
    val lance = find("contact_locust_lance_attack")
}

internal object DesertHuntEffects : SpotanimReferences() {
    val cloud = find("slayer_dustdevil_dust")
}

public class DesertHuntingCombat
@Inject
constructor(private val items: ObjTypeList, private val accuracy: AccuracyFormulae) :
    PluginScript() {
    override fun ScriptContext.startup() {
        for (type in HuntingGroundNpcs.dustDevils) onEvent<NpcMeleeAlternative>(type.id) {
            if (dustProtection(player, items)) return@onEvent
            for (skill in listOf(stats.attack, stats.strength, stats.ranged, stats.magic)) player
                .statSub(skill, player.stat(skill), 0)
            for (skill in listOf(stats.agility, stats.defence, stats.prayer)) player.statSub(
                skill,
                player.stat(skill) / 2,
                0,
            )
            player.spotanim(DesertHuntEffects.cloud)
            attack = NpcMeleeAlternative.Attack(DesertHuntAnims.dust, HitType.Typeless, 16)
        }
        for (type in HuntingGroundNpcs.archers) {
            onAiApPlayer2(type) { shoot(it.target) }
            onAiOpPlayer2(type) { shoot(it.target) }
        }
    }

    private fun StandardNpcAccess.shoot(target: Player) {
        if (!target.isValidTarget()) {
            resetMode()
            return
        }
        if (actionDelay > mapClock || !isWithinDistance(target, npc.visType.attackRange)) return
        actionDelay = mapClock + 6
        anim(DesertHuntAnims.arrow)
        val damage = if (accuracy.rollRangedAccuracy(npc, target, random)) random.of(0..22) else 0
        target.queueCombatRetaliate(npc)
        target.queueHit(npc, 2, HitType.Ranged, damage)
    }
}
