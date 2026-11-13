package org.rsmod.content.skills.combattraining

import jakarta.inject.Inject
import org.rsmod.api.combat.manager.PlayerAttackManager
import org.rsmod.api.combat.weapon.WeaponSpeeds
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

object DummyLocs : LocReferences() {
    val dummy = find("sworddummy")
    val varrockDummy = find("fai_varrock_sword_dummy")
}

class TrainingDummies
@Inject
constructor(
    private val objTypes: ObjTypeList,
    private val attacks: PlayerAttackManager,
    private val speeds: WeaponSpeeds,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(DummyLocs.varrockDummy) { practise() }
        onOpLoc2(DummyLocs.dummy) { practise() }
    }

    private fun ProtectedAccess.practise() {
        if (player.statBase(stats.attack) >= 8) {
            mes("There is nothing more you can learn from hitting a dummy.")
            return
        }
        if (attacks.isAttackDelayed(player)) return
        attacks.setNextAttackDelay(player, speeds.base(player))
        val weapon = objTypes.getOrNull(player.righthand)
        anim(weapon?.paramOrNull(params.attack_anim_stance1) ?: seqs.human_unarmedpunch)
        player.statAdvance(stats.attack, 5.0)
    }
}
