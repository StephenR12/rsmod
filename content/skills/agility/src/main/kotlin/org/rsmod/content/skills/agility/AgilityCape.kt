package org.rsmod.content.skills.agility

import jakarta.inject.Inject
import java.time.LocalDate
import java.time.ZoneOffset
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.*
import org.rsmod.api.type.editors.obj.ObjEditor
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.content.skills.magic.spell.utility.standard.StaminaEffectRepository
import org.rsmod.content.skills.skillcapes.SkillcapeBoost
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object AgilityCapeItems : ObjReferences() {
    val worn = listOf(find("skillcape_agility_worn"), find("skillcape_agility_trimmed_worn"))
    val carried = listOf(objs.agility_skillcape, objs.agility_skillcape_t)
}

internal object AgilityCapeTransforms : ObjEditor() {
    init {
        for ((index, cape) in AgilityCapeItems.carried.withIndex()) {
            val worn = AgilityCapeItems.worn[index]
            edit(cape) { transformlink = worn }
            edit(worn) {
                transformlink = cape
                transformtemplate = objs.template_for_transform
                param[params.graceful_restore_rate] = 300
                param[params.statreq1_skill] = stats.agility
                param[params.statreq1_level] = 99
            }
        }
    }
}

public class AgilityCape
@Inject
constructor(private val stamina: StaminaEffectRepository, private val boost: SkillcapeBoost) :
    PluginScript() {
    override fun ScriptContext.startup() {
        for (cape in AgilityCapeItems.carried) onOpHeld3(cape) { refill() }
        for (cape in AgilityCapeItems.worn) {
            onOpWorn2(cape) { boost.activate(player, stats.agility) }
            onOpWorn3(cape) { refill() }
        }
    }

    public fun restoreEnergy(access: ProtectedAccess): Unit = with(access) { refill() }

    private fun ProtectedAccess.refill() {
        if (!player.members) return
        val today = LocalDate.now(ZoneOffset.UTC).toEpochDay().toInt()
        if (vars[LapVars.capeDay] == today) {
            mes("Your cape's stamina boost will recharge tomorrow.")
            return
        }
        player.runEnergy = 10000
        stamina.activate(player, maxOf(100, stamina.remainingCycles(player)))
        vars[LapVars.capeDay] = today
        mes("Your cape restores your run energy.")
    }
}
