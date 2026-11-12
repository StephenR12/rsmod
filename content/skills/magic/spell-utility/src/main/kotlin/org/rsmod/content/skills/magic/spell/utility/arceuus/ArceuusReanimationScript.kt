package org.rsmod.content.skills.magic.spell.utility.arceuus

import jakarta.inject.Inject
import org.rsmod.api.combat.NvPCombat
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.config.refs.categories
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.queues
import org.rsmod.api.config.refs.stats
import org.rsmod.api.death.NpcDeath
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.npc.events.NpcKillEvents
import org.rsmod.api.player.interact.ObjTInteractionPolicyRegistry
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.ui.IfOverlayButtonT
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onAiApPlayer2
import org.rsmod.api.script.onAiOpPlayer2
import org.rsmod.api.script.onApObjT
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onIfOverlayButtonT
import org.rsmod.api.script.onNpcQueue
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.game.entity.PathingEntity.Companion.INVALID_SLOT
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.game.type.category.isType
import org.rsmod.game.type.comp.ComponentType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class ArceuusReanimationScript
@Inject
constructor(
    private val reanimation: ArceuusReanimation,
    private val states: ReanimationStateRepository,
    private val protectedAccess: ProtectedAccessLauncher,
    private val policies: ObjTInteractionPolicyRegistry,
    private val combat: NvPCombat,
    private val death: NpcDeath,
    private val npcRepo: NpcRepository,
) : PluginScript() {
    override fun ScriptContext.startup() {
        registerSpell(
            ReanimationComponents.basic,
            org.rsmod.api.config.refs.objs.spell_basic_reanimation,
        )
        registerSpell(
            ReanimationComponents.adept,
            org.rsmod.api.config.refs.objs.spell_adept_reanimation,
        )
        registerSpell(
            ReanimationComponents.expert,
            org.rsmod.api.config.refs.objs.spell_expert_reanimation,
        )
        registerSpell(
            ReanimationComponents.master,
            org.rsmod.api.config.refs.objs.spell_master_reanimation,
        )

        for (npc in ReanimationRecipes.npcTypes) {
            onAiOpPlayer2(npc) { attackIgnoringSingles(it.target) }
            onAiApPlayer2(npc) { attackIgnoringSingles(it.target) }
            onNpcQueue(npc, queues.death) { death.deathNoDrops(this) }
        }

        onEvent<NpcKillEvents.Credited> {
            val active = states.remove(npc) ?: return@onEvent
            if (active.owner != player) {
                return@onEvent
            }
            protectedAccess.launch(player) { statAdvance(stats.prayer, active.prayerXp) }
        }
        onEvent<NpcStateEvents.Delete> { states.remove(npc) }
        onPlayerLogout {
            val active = states.clear(player) ?: return@onPlayerLogout
            if (active.npc.slotId != INVALID_SLOT) {
                npcRepo.del(active.npc, Int.MAX_VALUE)
            }
        }
    }

    private fun ScriptContext.registerSpell(component: ComponentType, spell: ObjType) {
        onIfOverlayButtonT(component, org.rsmod.api.config.refs.components.inv_items) {
            castInventory(spell)
        }
        policies.register(component, range = GROUND_CAST_RANGE, checkLineOfSight = false)
        onApObjT(component) { reanimation.castGround(this, spell, it.obj) }
    }

    private fun IfOverlayButtonT.castInventory(spell: ObjType) {
        if (targetObj == null) {
            return
        }
        protectedAccess.launch(player) { reanimation.castInventory(this, spell, targetSlot) }
    }

    private fun StandardNpcAccess.attackIgnoringSingles(target: org.rsmod.game.entity.Player) {
        val attack = CombatAttack.NpcMelee(resolveMeleeAttackType())
        combat.attack(this, target, attack)
    }

    private fun StandardNpcAccess.resolveMeleeAttackType(): MeleeAttackType {
        val category = npc.visType.paramOrNull(params.npc_attack_type)
        return when {
            category.isType(categories.attacktype_stab) -> MeleeAttackType.Stab
            category.isType(categories.attacktype_slash) -> MeleeAttackType.Slash
            else -> MeleeAttackType.Crush
        }
    }

    public companion object {
        public const val GROUND_CAST_RANGE: Int = 10
    }
}
