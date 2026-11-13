package org.rsmod.content.skills.runecraft

import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.back
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.hat
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.advanced.onWearposChange
import org.rsmod.api.script.onOpHeld4
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.game.entity.Player
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.Wearpos
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class RuneTempleScript @jakarta.inject.Inject internal constructor(private val rift: RiftRound) :
    PluginScript() {
    private val attunements =
        TempleObjects.tiaras.values.toList() +
            listOf(TempleObjects.elementalTiara, TempleObjects.catalyticTiara)

    override fun ScriptContext.startup() {
        for (temple in RuneTemple.entries) registerTemple(temple)
        for (hat in RunecraftObjs.raimentsHats) {
            for ((index, tiara) in attunements.withIndex()) {
                onOpHeldU(hat, tiara) {
                    if (player.members && inv.contains(hat) && inv.contains(tiara)) {
                        vars[TempleVars.attuned] = index + 1
                        player.refreshRuins()
                        mes("You attune the hat to the ${ocName(tiara)}.")
                    }
                }
            }
            onOpHeld4(hat) {
                val tiara = attunements.getOrNull(vars[TempleVars.attuned] - 1)
                mes(
                    if (tiara == null) "Your hat isn't attuned."
                    else "Your hat is attuned to the ${ocName(tiara)}."
                )
            }
        }
        onPlayerLogin { player.refreshRuins() }
        onWearposChange {
            if (wearpos == Wearpos.Hat || wearpos == Wearpos.Back) player.refreshRuins()
        }
    }

    private fun ScriptContext.registerTemple(temple: RuneTemple) {
        val ruins = TempleLocs.ruins.getValue(temple)
        val talisman = TempleObjects.talismans.getValue(temple)
        val tiara = TempleObjects.tiaras.getValue(temple)
        val groupTalisman =
            if (temple.elemental) TempleObjects.elementalTalisman
            else TempleObjects.catalyticTalisman
        val groupTiara =
            if (temple.elemental) TempleObjects.elementalTiara else TempleObjects.catalyticTiara
        onOpLoc1(ruins) {
            if (it.loc.coords == temple.outside && player.canEnterWorn(temple)) enter(temple)
        }
        for (key in listOf(talisman, groupTalisman)) {
            onOpLocU(ruins, key) {
                if (
                    it.loc.coords == temple.outside &&
                        inv[it.invSlot]?.id == key.id &&
                        (key == talisman || player.members)
                )
                    enter(temple)
            }
        }
        onOpLoc1(TempleLocs.exits.getValue(temple)) {
            if (rift.returnFromAltar(this, temple)) return@onOpLoc1
            val anchor = temple.outside
            // Choose a free tile beside the ruins, taking their full 3x3 footprint into account.
            val landing =
                (-1..3)
                    .flatMap { x ->
                        (-1..3)
                            .filter { z -> x !in 0..2 || z !in 0..2 }
                            .map { z -> anchor.translate(x, z) }
                    }
                    .firstOrNull { !mapBlocked(it) }
            if (landing != null) teleport(landing)
        }
        registerTiara(temple, objs.tiara, talisman, tiara, temple.tiaraXp)
        registerTiara(
            temple,
            TempleObjects.goldTiara,
            groupTalisman,
            groupTiara,
            if (temple.elemental) 40.0 else 60.0,
        )
    }

    private fun ProtectedAccess.enter(temple: RuneTemple) {
        if (temple.recipe.members && !player.members) return
        val landing = temple.inside
        if (mapBlocked(landing)) return
        teleport(landing)
        mes("You feel a powerful force take hold of you.")
        if (temple == RuneTemple.Chaos) player.completeDiaryTask(DiaryTask.EnterChaosTemple)
    }

    private fun Player.canEnterWorn(temple: RuneTemple): Boolean {
        if (temple.recipe.members && !members) return false
        if (
            members &&
                back?.id in
                    listOf(
                        objs.runecrafting_skillcape.id,
                        objs.runecrafting_skillcape_t.id,
                        objs.max_cape_worn.id,
                    )
        )
            return true
        val worn =
            if (members && RunecraftObjs.raimentsHats.any { it.id == hat?.id }) {
                attunements.getOrNull(vars[TempleVars.attuned] - 1)?.id
            } else hat?.id
        val group =
            if (temple.elemental) TempleObjects.elementalTiara else TempleObjects.catalyticTiara
        return worn == TempleObjects.tiaras.getValue(temple).id || members && worn == group.id
    }

    private fun Player.refreshRuins() {
        for (temple in RuneTemple.entries) {
            val flag = TempleVars.enter.getValue(temple)
            val enabled = if (canEnterWorn(temple)) 1 else 0
            if (vars[flag] != enabled) VarPlayerIntMapSetter.set(this, flag, enabled)
        }
    }

    private fun ScriptContext.registerTiara(
        temple: RuneTemple,
        blank: ObjType,
        talisman: ObjType,
        result: ObjType,
        xp: Double,
    ) {
        for (item in listOf(blank, talisman)) {
            onOpLocU(temple.recipe.altar, item) {
                if (
                    vars[TempleVars.playingGuardiansOfTheRift] != 0 ||
                        inv[it.invSlot]?.id != item.id
                )
                    return@onOpLocU
                if (!player.members && (temple.recipe.members || blank == TempleObjects.goldTiara))
                    return@onOpLocU
                val slot =
                    if (item == blank) it.invSlot
                    else inv.indexOfFirst { obj -> obj?.id == blank.id }
                if (slot < 0 || !inv.contains(talisman)) {
                    mes("You need a ${ocName(blank)} and a ${ocName(talisman)}.")
                    return@onOpLocU
                }
                val transaction =
                    player.invTransaction(inv) {
                        val inventory = select(inv)
                        delete(inventory, blank.id, 1, slot = slot)
                        delete(
                            inventory,
                            talisman.id,
                            1,
                            slot = if (item == talisman) it.invSlot else null,
                        )
                        add(inventory, result.id, 1, slot = slot)
                    }
                if (transaction.success) {
                    if (temple == RuneTemple.Mind && blank == objs.tiara) {
                        player.completeDiaryTask(DiaryTask.MakeMindTiara)
                    }
                    anim(RunecraftSeqs.craft)
                    statAdvance(stats.runecrafting, xp)
                    mes("You bind the talisman to the tiara.")
                }
            }
        }
    }
}
