package org.rsmod.content.skills.magic.spell.utility.arceuus

import jakarta.inject.Inject
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.projanims
import org.rsmod.api.config.refs.timers
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.npc.hit.queueHit
import org.rsmod.api.npc.isValidTarget
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.random.CoreRandom
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.script.onModifyNpcHit
import org.rsmod.api.script.onNpcTimer
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity.Companion.INVALID_SLOT
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.game.interact.InteractionNpcOp
import org.rsmod.game.interact.InteractionNpcT
import org.rsmod.game.interact.InteractionOp
import org.rsmod.game.type.proj.ProjAnimTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

public class ArceuusThrallScript
@Inject
constructor(
    private val spell: ArceuusThrallSpell,
    private val states: ThrallStateRepository,
    private val protectedAccess: ProtectedAccessLauncher,
    private val spells: MagicSpellRegistry,
    private val npcRepo: NpcRepository,
    private val worldRepo: WorldRepository,
    private val projAnimTypes: ProjAnimTypeList,
    private val collision: CollisionFlagMap,
    private val hitModifier: ThrallNpcHitModifier,
    @CoreRandom private val random: GameRandom,
) : PluginScript() {
    private lateinit var offensiveSpellIds: Set<Int>

    override fun ScriptContext.startup() {
        offensiveSpellIds = spells.offensiveCombatSpells().mapTo(hashSetOf()) { it.obj.id }
        for (recipe in ThrallRecipes.all) {
            onIfOverlayButton(recipe.component) {
                protectedAccess.launch(player) { spell.cast(this, recipe) }
            }
            onModifyNpcHit(recipe.npc) { hit.damage = 0 }
        }
        onNpcTimer(timers.magic_arceuus_thrall_attack) { processThrall() }
        onPlayerSoftTimer(timers.magic_arceuus_thrall_cooldown) { player.thrallCooldown = 0 }
        onEvent<NpcStateEvents.Delete> {
            val active = states.remove(npc) ?: return@onEvent
            clearDisplay(active.owner)
        }
        onPlayerLogout {
            val active = states.remove(player) ?: return@onPlayerLogout
            clearDisplay(player)
            if (active.npc.slotId != INVALID_SLOT) {
                npcRepo.del(active.npc, Int.MAX_VALUE)
            }
        }
    }

    private fun StandardNpcAccess.processThrall() {
        val active = states.active(npc) ?: return
        val owner = active.owner
        val target = owner.combatNpcTarget()
        if (target == null || !target.isValidTarget() || target === npc) {
            followOwner(owner)
            npc.timer(timers.magic_arceuus_thrall_attack, FOLLOW_POLL_CYCLES)
            return
        }

        val style = active.recipe.style
        if (
            !isWithinDistance(target, style.range) ||
                (style.range > 1 && !lineOfSight(npc.coords, target.bounds(), collision))
        ) {
            walk(target.coords)
            npc.timer(timers.magic_arceuus_thrall_attack, FOLLOW_POLL_CYCLES)
            return
        }

        npc.faceNpc(target)
        anim(style.attackSeq)
        val damage =
            if (target.visType.param(params.thrall_immunity) != 0) {
                0
            } else {
                random.of(active.recipe.maxHit + 1)
            }
        val delay = projectileDelay(style, target)
        target.queueHit(
                source = owner,
                delay = delay,
                type = style.hitType,
                damage = damage,
                modifier = hitModifier,
                sourceSecondary = active.recipe.spell,
                successful = true,
            )
        npc.timer(timers.magic_arceuus_thrall_attack, ATTACK_RATE_CYCLES)
    }

    private fun StandardNpcAccess.projectileDelay(style: ThrallStyle, target: Npc): Int {
        val projectile = style.projectile ?: return MELEE_IMPACT_DELAY
        val animation =
            worldRepo.projAnim(npc, target, projectile, projAnimTypes[projanims.magic_spell_low])
        if (style == ThrallStyle.Ghost) {
            target.spotanim(ThrallSpotanims.magicImpact, delay = animation.clientCycles)
            return (animation.serverCycles - MAGIC_EARLY_IMPACT_CYCLES).coerceAtLeast(1)
        }
        return animation.serverCycles
    }

    private fun StandardNpcAccess.followOwner(owner: Player) {
        if (!isWithinDistance(owner, OWNER_IDLE_DISTANCE)) {
            walk(owner.coords)
        }
    }

    private fun Player.combatNpcTarget(): Npc? =
        when (val current = interaction) {
            is InteractionNpcOp -> current.target.takeIf { current.op == InteractionOp.Op2 }
            is InteractionNpcT -> {
                val selected = current.objType?.let(spells::getObjSpell)
                current.target.takeIf { selected?.obj?.id in offensiveSpellIds }
            }
            else -> null
        }

    private fun clearDisplay(player: Player) {
        player.thrallActive = 0
    }

    public companion object {
        public const val ATTACK_RATE_CYCLES: Int = 4
        public const val FOLLOW_POLL_CYCLES: Int = 1
        public const val OWNER_IDLE_DISTANCE: Int = 2
        public const val MELEE_IMPACT_DELAY: Int = 1
        public const val MAGIC_EARLY_IMPACT_CYCLES: Int = 1
    }
}
