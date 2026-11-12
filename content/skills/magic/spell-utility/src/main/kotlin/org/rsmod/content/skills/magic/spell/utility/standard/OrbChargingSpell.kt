package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.isInCombat
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.registry.loc.LocRegistry
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.obj.ObjType

public class OrbChargingSpell
@Inject
constructor(
    private val spells: MagicSpellRegistry,
    private val runes: MagicRuneManager,
    private val recipes: OrbChargingRecipeRegistry,
    private val locRegistry: LocRegistry,
    private val cooldowns: UtilitySpellCooldownRepository,
) {
    public fun castManual(access: ProtectedAccess, spellType: ObjType, target: BoundLocInfo) {
        with(access) {
            val recipe = recipes[spellType] ?: return
            if (target.id != recipe.obelisk.id) {
                mes("This spell can only be cast on the Obelisk of ${recipe.targetName}.")
                return
            }
            if (!player.members) {
                mes("You need to be on a members' world to cast this spell.")
                return
            }

            clearQueue(orb_charging_queues.auto)
            if (!cooldowns.isReady(player, mapClock)) {
                if (orb_charging_queues.manual !in player.queueList) {
                    val delay = cooldowns.cyclesUntilReady(player, mapClock)
                    queue(
                        orb_charging_queues.manual,
                        delay,
                        request(recipe, target.coords, mapClock + delay),
                    )
                }
                return
            }

            clearQueue(orb_charging_queues.manual)
            cast(this, recipe, target.coords)
        }
    }

    internal fun castQueued(access: ProtectedAccess, request: OrbChargingRequest) {
        with(access) {
            if (
                mapClock != request.expectedCycle ||
                    player.coords != request.origin ||
                    player.isInCombat() ||
                    !locRegistry.isValid(request.target, request.obelisk.id)
            ) {
                return
            }
            val recipe = recipes[request.spell] ?: return
            if (recipe.obelisk.id != request.obelisk.id || objs.unpowered_orb !in player.inv) {
                return
            }
            cast(this, recipe, request.target)
        }
    }

    private fun cast(
        access: ProtectedAccess,
        recipe: OrbChargingRecipe,
        target: org.rsmod.map.CoordGrid,
    ) {
        with(access) {
            if (!locRegistry.isValid(target, recipe.obelisk.id)) {
                return
            }
            val spell = requireNotNull(spells.getObjSpell(recipe.spell))
            if (!runes.canCastSpell(player, spell)) {
                return
            }
            val sourceSlot = player.inv.indexOfFirst { it?.id == objs.unpowered_orb.id }
            if (sourceSlot == -1) {
                return
            }
            check(spell.objReqs.any { it.obj.id == objs.unpowered_orb.id && it.count == 1 }) {
                "Orb spell must consume one unpowered orb through its cache contract: $spell"
            }

            val preflight = transform(sourceSlot, recipe.output, consumeSource = true, false)
            if (!preflight.success || runes.attemptCast(player, spell).isFailure()) {
                return
            }
            val committed = transform(sourceSlot, recipe.output, consumeSource = false, true)
            check(committed.success) { "Orb charging transaction failed after validation: $recipe" }

            if (vars[varbits.fountain_of_rune_active] == 0) {
                statAdvance(stats.magic, spell.castXp)
            }
            anim(seqs.human_castchargeorb)
            spotanim(recipe.spotanim)
            soundSynth(recipe.sound)
            cooldowns.markCast(player, mapClock + MANUAL_CAST_RATE)

            if (objs.unpowered_orb in player.inv) {
                clearQueue(orb_charging_queues.auto)
                queue(
                    orb_charging_queues.auto,
                    AUTO_CAST_RATE,
                    request(recipe, target, mapClock + AUTO_CAST_RATE),
                )
            }
        }
    }

    private fun ProtectedAccess.request(
        recipe: OrbChargingRecipe,
        target: org.rsmod.map.CoordGrid,
        expectedCycle: Int,
    ) =
        OrbChargingRequest(
            spell = recipe.spell,
            obelisk = recipe.obelisk,
            target = target,
            origin = player.coords,
            expectedCycle = expectedCycle,
        )

    private fun ProtectedAccess.transform(
        sourceSlot: Int,
        output: ObjType,
        consumeSource: Boolean,
        autoCommit: Boolean,
    ) =
        player.invTransaction(player.inv, autoCommit = autoCommit) {
            val inventory = select(player.inv)
            if (consumeSource) {
                delete(inventory, objs.unpowered_orb.id, count = 1, slot = sourceSlot)
            }
            add(inventory, output.id, count = 1, slot = sourceSlot)
        }

    private companion object {
        const val MANUAL_CAST_RATE = 3
        const val AUTO_CAST_RATE = 6
    }
}
