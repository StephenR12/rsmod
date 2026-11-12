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
import org.rsmod.api.player.magic.ShadowVeilStateRepository
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.baseMagicLvl
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.game.entity.Player

internal var Player.shadowVeilCooldown by intVarBit(varbits.arceuus_shadow_veil_cooldown)
internal var Player.shadowVeilActive by intVarBit(varbits.arceuus_shadow_veil_active)

public class ShadowVeilSpell
@Inject
constructor(
    private val spells: MagicSpellRegistry,
    private val runes: MagicRuneManager,
    private val states: ShadowVeilStateRepository,
) {
    public fun cast(access: ProtectedAccess) {
        with(access) {
            if (!canAccess() || player.shadowVeilCooldown != 0) {
                return
            }
            val spell = requireNotNull(spells.getObjSpell(objs.spell_shadow_veil))
            if (runes.attemptCast(player, spell).isFailure()) {
                return
            }

            val duration = player.baseMagicLvl
            if (vars[varbits.fountain_of_rune_active] == 0) {
                statAdvance(stats.magic, spell.castXp)
            }
            anim(seqs.human_spellcast_shadow_veil)
            spotanim(spotanims.shadow_veil_cast)
            states.activate(player, duration)
            player.shadowVeilActive = 1
            player.shadowVeilCooldown = 1
            player.softTimer(timers.magic_arceuus_shadow_veil_expiry, duration)
            player.softTimer(timers.magic_arceuus_shadow_veil_cooldown, COOLDOWN_CYCLES)
            mes("<col=6800bf>Your thieving abilities have been enhanced.</col>")
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
        return true
    }

    public companion object {
        public const val COOLDOWN_CYCLES: Int = 50
        private const val A_KINGDOM_DIVIDED_COMPLETE: Int = 150
    }
}
