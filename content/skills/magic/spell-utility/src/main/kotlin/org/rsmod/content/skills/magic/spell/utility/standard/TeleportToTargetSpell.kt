package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import kotlin.math.max
import org.rsmod.api.combat.commons.magic.MagicSpellType
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.synths
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.isValidTarget
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.map.CoordGrid

/** Cache-233 Teleport to Target casting behavior. */
public class TeleportToTargetSpell
@Inject
constructor(
    private val spells: MagicSpellRegistry,
    private val runes: MagicRuneManager,
    private val targets: BountyTargetRepository,
    private val cooldowns: UtilitySpellCooldownRepository,
) {
    public suspend fun cast(access: ProtectedAccess) {
        with(access) {
            if (!player.members) {
                mes("You need to be on a members' world to cast this spell.")
                return
            }
            if (vars[varbits.bounty_target_teleport] == 0) {
                mes("You have not unlocked this spell.")
                return
            }
            if (!cooldowns.isReady(player, mapClock)) {
                return
            }

            val location = targets.location(player)
            if (location == null) {
                mes("You can only cast this spell in Bounty Hunter.")
                return
            }
            if (
                location == BountyHunterLocation.Crater && combatElapsedTicks() < COMBAT_DELAY_TICKS
            ) {
                mes("You cannot use this spell until 12 seconds after combat.")
                return
            }

            val assignment = targets.assignment(player)
            if (assignment == null) {
                mes("You do not have a Bounty Hunter target.")
                return
            }
            if (assignment.teleportsUsed >= BountyTargetRepository.MAX_TELEPORTS_PER_TARGET) {
                mes("You have already teleported to this target five times.")
                return
            }

            val target = findUid(assignment.target)
            if (
                target == null ||
                    !target.isValidTarget() ||
                    targets.location(target) != BountyHunterLocation.Crater
            ) {
                mes("Your Bounty Hunter target is not inside the crater.")
                return
            }

            val spell = requireNotNull(spells.getObjSpell(objs.spell_teleport_to_target))
            check(spell.type == MagicSpellType.Teleport) { "Expected teleport spell: $spell" }
            check(spell.spellbook == null) {
                "Teleport to Target must be available on all spellbooks."
            }
            if (runes.attemptCast(player, spell).isFailure()) {
                return
            }

            stopAction()
            if (vars[varbits.fountain_of_rune_active] == 0) {
                statAdvance(stats.magic, spell.castXp)
            }
            anim(seqs.teleport_casting)
            spotanim(spotanims.teleport_casting)
            soundSynth(synths.teleport_all)
            cooldowns.markCast(player, mapClock + CAST_TICKS)
            delay(CAST_TICKS)

            val current = targets.assignment(player)
            val currentTarget = current?.let { findUid(it.target) }
            if (
                current == null ||
                    current.target != assignment.target ||
                    currentTarget == null ||
                    !currentTarget.isValidTarget() ||
                    targets.location(currentTarget) != BountyHunterLocation.Crater
            ) {
                mes("Your Bounty Hunter target is no longer available.")
                return
            }
            val destination = targetDestination(currentTarget.coords)
            spellTeleport(destination.coerceTargetPlane(currentTarget.coords))
            targets.recordTeleport(player)
        }
    }

    private fun ProtectedAccess.combatElapsedTicks(): Int {
        val lastCombat = max(vars[varps.lastcombat], vars[varps.lastcombat_pvp])
        return mapClock - lastCombat
    }

    private fun ProtectedAccess.targetDestination(target: CoordGrid): CoordGrid =
        mapFindSquareNone(target, minRadius = 0, maxRadius = TARGET_RADIUS) ?: target

    private fun CoordGrid.coerceTargetPlane(target: CoordGrid): CoordGrid =
        if (level == target.level) this else copy(level = target.level)

    private companion object {
        const val CAST_TICKS = 4
        const val COMBAT_DELAY_TICKS = 20
        const val TARGET_RADIUS = 12
    }
}
