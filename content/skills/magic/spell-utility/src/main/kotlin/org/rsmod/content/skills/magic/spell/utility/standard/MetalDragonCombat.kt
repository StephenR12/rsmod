package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.player.queueCombatRetaliate
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.combat.formulas.AccuracyFormulae
import org.rsmod.api.combat.formulas.MaxHitFormulae
import org.rsmod.api.config.refs.huntmodes
import org.rsmod.api.config.refs.params
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.isValidTarget
import org.rsmod.api.player.lefthand
import org.rsmod.api.script.onAiApPlayer2
import org.rsmod.api.script.onAiOpPlayer2
import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object MetalDragons : NpcReferences() {
    val common =
        listOf("bronze", "iron", "steel").flatMap { metal ->
            listOf(
                find("${metal}_dragon"),
                find("${metal}_dragon_strongholdcave"),
                find("kourend_${metal}_dragon"),
            )
        }
    val mithril = find("brut_mithril_dragon")
}

internal object MetalDragonStats : NpcEditor() {
    init {
        for (type in MetalDragons.common) edit(type) {
            attackRange = 8
            huntMode = huntmodes.aggressive_ranged
            huntRange = 6
            param[params.attackrate] = 4
            param[params.defence_slash] = 70
            param[params.defence_crush] = 70
            param[params.defence_magic] = 30
            param[params.defence_light] = 90
            param[params.defence_standard] = 90
            param[params.defence_heavy] = 10
        }
    }
}

internal object MetalDragonAnims : SeqReferences() {
    val bite = find("dragon_attack")
    val ranged = find("dragon_ranged_attacks")
    val cast = find("brut_dragon_casting")
}

public class MetalDragonCombat
@Inject
constructor(
    private val accuracy: AccuracyFormulae,
    private val maxHits: MaxHitFormulae,
    private val fire: StandardDragonfireDamageResolver,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (type in MetalDragons.common + MetalDragons.mithril) {
            onAiApPlayer2(type) { attack(it.target) }
            onAiOpPlayer2(type) { attack(it.target) }
        }
    }

    private fun StandardNpcAccess.attack(target: Player) {
        if (!target.isValidTarget()) {
            resetMode()
            return
        }
        if (actionDelay > mapClock || !isWithinDistance(target, npc.visType.attackRange)) return
        val adjacent = isWithinDistance(target, 1)
        val mithril = npc.visType.id == MetalDragons.mithril.id
        val style =
            when {
                mithril -> {
                    val normal =
                        if (random.of(5) >= 3) HitType.Magic
                        else if (adjacent) HitType.Melee else HitType.Ranged
                    if (random.of(5) == 0) HitType.Typeless else normal
                }
                !adjacent || random.of(3) == 0 -> HitType.Typeless
                else -> HitType.Melee
            }
        actionDelay = mapClock + 4
        val damage =
            when (style) {
                HitType.Typeless -> {
                    anim(ChromaticSequences.breathe)
                    val shield = target.lefthand?.id in dragonfireShieldIds
                    val resisted = !shield && !accuracy.rollMagicAccuracy(npc, target, random)
                    fire.rollDamage(target, DragonfireBreath.Metallic, resisted, shield, random)
                }
                HitType.Melee -> {
                    anim(MetalDragonAnims.bite)
                    val attackType = if (mithril) MeleeAttackType.Stab else MeleeAttackType.Slash
                    if (accuracy.rollMeleeAccuracy(npc, target, attackType, random))
                        random.of(0..maxHits.getMeleeMaxHit(npc, target, attackType))
                    else 0
                }
                HitType.Ranged -> {
                    anim(MetalDragonAnims.ranged)
                    if (accuracy.rollRangedAccuracy(npc, target, random)) random.of(0..22) else 0
                }
                HitType.Magic -> {
                    anim(MetalDragonAnims.cast)
                    if (accuracy.rollMagicAccuracy(npc, target, random)) random.of(0..18) else 0
                }
            }
        target.queueCombatRetaliate(npc)
        target.queueHit(npc, if (style == HitType.Melee) 1 else 2, style, damage)
    }
}
