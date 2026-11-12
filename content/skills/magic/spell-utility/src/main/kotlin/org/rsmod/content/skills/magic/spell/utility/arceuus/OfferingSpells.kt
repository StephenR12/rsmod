package org.rsmod.content.skills.magic.spell.utility.arceuus

import jakarta.inject.Inject
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.timers
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.content.skills.prayer.remains.PrayerRemainsKind
import org.rsmod.content.skills.prayer.remains.PrayerRemainsRecipe
import org.rsmod.content.skills.prayer.remains.PrayerRemainsRecipes
import org.rsmod.game.entity.Player
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.spot.SpotanimType

internal var Player.offeringCooldown by intVarBit(varbits.arceuus_offering_cooldown)

public class OfferingSpells
@Inject
constructor(private val spells: MagicSpellRegistry, private val runes: MagicRuneManager) {
    public fun castDemonic(access: ProtectedAccess) {
        cast(
            access = access,
            spellObj = objs.spell_demonic_offering,
            kind = PrayerRemainsKind.DemonicAsh,
            spotanim = spotanims.demonic_offering_cast,
            emptyMessage = "You do not have any demonic ashes to offer.",
        )
    }

    public fun castSinister(access: ProtectedAccess) {
        cast(
            access = access,
            spellObj = objs.spell_sinister_offering,
            kind = PrayerRemainsKind.Bone,
            spotanim = spotanims.sinister_offering_cast,
            emptyMessage = "You do not have any bones to offer.",
        )
    }

    private fun cast(
        access: ProtectedAccess,
        spellObj: ObjType,
        kind: PrayerRemainsKind,
        spotanim: SpotanimType,
        emptyMessage: String,
    ) {
        with(access) {
            if (!canAccess() || player.offeringCooldown != 0) {
                return
            }
            val remains = eligibleRemains(kind)
            if (remains.isEmpty()) {
                mes(emptyMessage)
                return
            }

            val spell = requireNotNull(spells.getObjSpell(spellObj))
            if (runes.attemptCast(player, spell).isFailure()) {
                return
            }

            val transaction =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    for ((slot, recipe) in remains) {
                        delete(inventory, recipe.obj.id, 1, slot = slot)
                    }
                }
            check(transaction.success) { "Offering transaction failed after validation: $remains" }

            statAdvance(stats.prayer, remains.sumOf { (_, recipe) -> recipe.xp * XP_MULTIPLIER })
            statHeal(stats.prayer, remains.sumOf { (_, recipe) -> restorePoints(recipe) }, 0)
            if (vars[varbits.fountain_of_rune_active] == 0) {
                statAdvance(stats.magic, spell.castXp)
            }
            anim(seqs.human_cast_offering)
            spotanim(spotanim)
            player.offeringCooldown = 1
            softTimer(timers.magic_arceuus_offering_cooldown, CAST_INTERVAL_CYCLES)
        }
    }

    private fun ProtectedAccess.eligibleRemains(
        kind: PrayerRemainsKind
    ): List<Pair<Int, PrayerRemainsRecipe>> =
        player.inv
            .mapIndexedNotNull { slot, obj ->
                val recipe =
                    obj?.let { PrayerRemainsRecipes.byObj[it.id] } ?: return@mapIndexedNotNull null
                if (recipe.kind == kind) slot to recipe else null
            }
            .take(MAX_REMAINS)

    private fun restorePoints(recipe: PrayerRemainsRecipe): Int =
        when (recipe.obj) {
            objs.infernal_ashes,
            objs.ourg_bones,
            objs.dagannoth_bones,
            objs.hydra_bones,
            objs.superior_dragon_bones -> 2
            else -> 1
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
        return true
    }

    public companion object {
        public const val MAX_REMAINS: Int = 3
        public const val XP_MULTIPLIER: Int = 3
        public const val CAST_INTERVAL_CYCLES: Int = 8
        private const val A_KINGDOM_DIVIDED_COMPLETE: Int = 150
    }
}
