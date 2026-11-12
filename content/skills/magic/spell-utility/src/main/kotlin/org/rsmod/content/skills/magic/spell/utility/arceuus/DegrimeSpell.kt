package org.rsmod.content.skills.magic.spell.utility.arceuus

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.WeakHashMap
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.content.skills.herblore.HerbCleaningRecipe
import org.rsmod.content.skills.herblore.HerbCleaningRecipes
import org.rsmod.game.entity.Player

public class DegrimeSpell
@Inject
constructor(
    private val spells: MagicSpellRegistry,
    private val runes: MagicRuneManager,
    private val cooldowns: DegrimeCooldownRepository,
) {
    public fun cast(access: ProtectedAccess) {
        with(access) {
            if (!canAccess() || player.routeDestination.isNotEmpty()) {
                return
            }
            if (cooldowns.isActive(player)) {
                mes("Your last spell is still in progress!")
                return
            }
            val herbs = eligibleHerbs()
            if (herbs.isEmpty()) {
                mes("You do not have any herbs you can clean.")
                return
            }

            val spell = requireNotNull(spells.getObjSpell(objs.spell_degrime))
            if (runes.attemptCast(player, spell).isFailure()) {
                return
            }

            val transaction =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    for ((slot, recipe) in herbs) {
                        delete(inventory, recipe.grimy.id, 1, slot = slot)
                        add(inventory, recipe.clean.id, 1, slot = slot)
                    }
                }
            check(transaction.success) { "Degrime transaction failed after validation: $herbs" }

            val fineHerbloreXp = herbs.sumOf { (_, recipe) -> (recipe.xp * 10).toInt() / 2 }
            statAdvance(stats.herblore, fineHerbloreXp / 10.0)
            if (vars[varbits.fountain_of_rune_active] == 0) {
                statAdvance(stats.magic, spell.castXp)
            }
            anim(seqs.human_spellcast_degrime)
            spotanim(spotanims.degrime_cast)
            cooldowns.activate(player)
        }
    }

    private fun ProtectedAccess.eligibleHerbs(): List<Pair<Int, HerbCleaningRecipe>> {
        val level = player.stat(stats.herblore)
        return player.inv.mapIndexedNotNull { slot, obj ->
            val recipe =
                obj?.let { HerbCleaningRecipes.byGrimy[it.id] } ?: return@mapIndexedNotNull null
            if (level >= recipe.level) slot to recipe else null
        }
    }

    private fun ProtectedAccess.canAccess(): Boolean {
        if (!player.members) {
            mes("You need to be on a members' world to cast this spell.")
            return false
        }
        if (vars[varbits.a_kingdom_divided] < A_KINGDOM_DIVIDED_COMPLETE) {
            mes("You need to complete A Kingdom Divided to cast this spell.")
            return false
        }
        if (vars[varps.druidic_ritual] < DRUIDIC_RITUAL_COMPLETE) {
            mes("You need to complete Druidic Ritual before training Herblore.")
            return false
        }
        return true
    }

    public companion object {
        public const val CAST_INTERVAL_CYCLES: Int = 8
        private const val A_KINGDOM_DIVIDED_COMPLETE: Int = 150
        private const val DRUIDIC_RITUAL_COMPLETE: Int = 4
    }
}

@Singleton
public class DegrimeCooldownRepository {
    private val expiresAt = WeakHashMap<Player, Int>()

    public fun activate(player: Player) {
        expiresAt[player] = player.currentMapClock + DegrimeSpell.CAST_INTERVAL_CYCLES
    }

    public fun isActive(player: Player): Boolean = remainingCycles(player) > 0

    public fun clear(player: Player) {
        expiresAt.remove(player)
    }

    public fun remainingCycles(player: Player): Int {
        val expiry = expiresAt[player] ?: return 0
        val remaining = expiry - player.currentMapClock
        if (remaining <= 0) {
            expiresAt.remove(player)
            return 0
        }
        return remaining
    }
}
