package org.rsmod.content.skills.firemaking

import jakarta.inject.Inject
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.controller.ControllerRepository
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.*
import org.rsmod.api.type.builders.controller.ControllerBuilder
import org.rsmod.api.type.refs.queue.QueueReferences
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Controller
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocEntity
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.obj.Obj
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.seq.SeqTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object ForesterFireType : ControllerBuilder() {
    val fire = build("foresters_campfire")
}

internal object ForesterQueue : QueueReferences() {
    val tend = find("foresters_campfire_tend")
}

private data class Tending(val pos: CoordGrid, val log: Int, val count: Int)

private data class BurningCampfire(val loc: LocInfo, var end: Int)

public class ForestersCampfire
@Inject
constructor(
    private val clock: MapClock,
    private val locs: LocRepository,
    private val objsRepo: ObjRepository,
    private val controllers: ControllerRepository,
    private val types: LocTypeList,
    private val seqs: SeqTypeList,
    private val xp: FiremakingXp,
) : PluginScript() {
    private val fires = mutableMapOf<CoordGrid, BurningCampfire>()
    private val campfires = (49927..49932).map(types::getValue)
    private val normalFires =
        listOf(
            firemaking_locs.fire,
            firemaking_locs.redFire,
            firemaking_locs.greenFire,
            firemaking_locs.blueFire,
            firemaking_locs.whiteFire,
            firemaking_locs.purpleFire,
        )
    private val burnTime = listOf(3, 3, 10, 17, 19, 22, 24, 26, 31, 32, 38, 45)
    private val sequences =
        listOf(10565, 10562, 10569, 10572, 10571, 10563, 10568, 10567, 10573, 10564, 10566, 10570)

    override fun ScriptContext.startup() {
        for ((i, fire) in normalFires.withIndex()) for (recipe in
            FiremakingRecipes.ordinary) onOpLocU(fire, recipe.log) {
            convert(it.loc, i, recipe, it.invSlot)
        }
        for (type in campfires) {
            onOpLoc1(type) { choose(it.loc) }
            onOpLoc2(type) {
                val left = (fires[it.loc.coords]?.end ?: clock.cycle) - clock.cycle
                mes(
                    when (left) {
                        in 0..58 -> "The embers glow softly."
                        in 59..118 -> "The flames flicker gently."
                        in 119..178 -> "The fire burns steadily."
                        in 179..238 -> "The fire burns brightly."
                        else -> "The roaring fire crackles invitingly."
                    }
                )
            }
            onOpLoc3(type) { anim(seqs.getValue(10083)) }
            for (recipe in FiremakingRecipes.ordinary) onOpLocU(type, recipe.log) {
                choose(it.loc, recipe)
            }
        }
        onPlayerQueueWithArgs<Tending>(ForesterQueue.tend) { tend(it.args) }
        onAiConTimer(ForesterFireType.fire) {
            val burning = fires[controller.coords]
            if (burning == null) {
                controllers.del(controller)
                return@onAiConTimer
            }
            val left = burning.end - clock.cycle
            if (left > 0) {
                controller.aiTimer(left)
                return@onAiConTimer
            }
            fires.remove(controller.coords)
            if (locs.findExact(controller.coords, types.getValue(burning.loc.id)) != null) {
                locs.del(burning.loc, Int.MAX_VALUE)
                objsRepo.add(
                    Obj.fromServer(clock, controller.coords, objs.ashes, 1),
                    constants.lootdrop_duration,
                )
            }
            controllers.del(controller)
        }
    }

    private fun ProtectedAccess.convert(
        loc: BoundLocInfo,
        colour: Int,
        recipe: FiremakingRecipe,
        slot: Int,
    ) {
        if (
            !eligible(recipe) ||
                inv[slot]?.id != recipe.log.id ||
                locs.findExact(loc.coords, normalFires[colour]) == null
        )
            return
        if (fires.keys.any { it.level == loc.level && it.chebyshevDistance(loc.coords) <= 5 }) {
            mes("There's a Forester's Campfire nearby, help tend to that one or move further away.")
            return
        }
        val fire =
            LocInfo(
                loc.layer,
                loc.coords,
                LocEntity(campfires[colour].id, loc.shapeId, loc.angleId),
            )
        if (!locs.add(fire, Int.MAX_VALUE)) return
        val i = FiremakingRecipes.ordinary.indexOf(recipe)
        val duration = 99 + burnTime[i]
        fires[loc.coords] = BurningCampfire(fire, clock.cycle + duration)
        val controller = Controller(ForesterFireType.fire, loc.coords)
        controllers.add(controller, Int.MAX_VALUE)
        controller.aiTimer(duration)
        objsRepo.cancelDelayed(loc.coords, objs.ashes)
        consume(recipe, slot, i)
    }

    private suspend fun ProtectedAccess.choose(
        loc: BoundLocInfo,
        selected: FiremakingRecipe? = null,
    ) {
        if (loc.coords !in fires) return
        val available =
            (selected?.let(::listOf) ?: FiremakingRecipes.ordinary).filter {
                it.log in inv && (!it.members || player.members)
            }
        // The native quantity interface has ten slots. A full mix of the twelve burnable logs
        // therefore needs a log selection before opening the quantity interface.
        val held =
            if (available.size <= 10) available
            else {
                val split =
                    menu(
                        "Choose a log group",
                        false,
                        listOf("Logs through maple", "Mahogany through redwood", "Cancel"),
                    )
                when (split) {
                    0 -> available.filter { it.level <= 45 }
                    1 -> available.filter { it.level > 45 }
                    else -> return
                }
            }
        if (held.isEmpty()) {
            mes("You need logs to tend the campfire.")
            return
        }
        val amount = held.maxOf { invTotal(inv, it.log) }
        val input =
            skillMulti("How many would you like to burn?", held.map { it.log }, amount, amount, 0)
        val choice = input.component.packed and 0xffff
        val recipe = held.getOrNull(choice - 14) ?: return
        ifClose()
        tend(Tending(loc.coords, recipe.log.id, input.subcomponent))
    }

    private fun ProtectedAccess.tend(request: Tending) {
        val fire = fires[request.pos] ?: return
        if (
            player.coords.level != request.pos.level ||
                player.coords.chebyshevDistance(request.pos) > 1 ||
                locs.findExact(request.pos, types.getValue(fire.loc.id)) == null
        )
            return
        val recipe = FiremakingRecipes.byLog[request.log] ?: return
        val slot = inv.indexOfFirst { it?.id == recipe.log.id }
        if (slot < 0 || !eligible(recipe)) return
        val i = FiremakingRecipes.ordinary.indexOf(recipe)
        consume(recipe, slot, i)
        fire.end = minOf(clock.cycle + 300, fire.end + burnTime[i])
        if (request.count > 1 && recipe.log in inv)
            weakQueue(ForesterQueue.tend, 9, request.copy(count = request.count - 1))
    }

    private fun ProtectedAccess.eligible(recipe: FiremakingRecipe): Boolean {
        if (recipe.members && !player.members) {
            mes("You need to be on a members' world to use these logs.")
            return false
        }
        if (stat(stats.firemaking) < recipe.level) {
            mes("You need level ${recipe.level} Firemaking to burn these logs.")
            return false
        }
        return true
    }

    private fun ProtectedAccess.consume(recipe: FiremakingRecipe, slot: Int, index: Int) {
        check(inv[slot]?.id == recipe.log.id)
        inv[slot] = null
        anim(seqs.getValue(sequences[index]))
        xp.advance(this, recipe.xp)
    }
}
