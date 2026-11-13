package org.rsmod.content.skills.construction

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.queue.QueueReferences
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object FishingCranes : LocReferences() {
    val broken = find("piscarilius_fishingcrane_broken")
    val repaired = find("piscarilius_fishingcrane_repaired")
}

internal object CraneRepairQueue : QueueReferences() {
    val repair = find("piscarilius_crane_repair")
}

public class PiscariliusCranes @Inject constructor(private val locations: LocRepository) :
    PluginScript() {
    private class Repair(val target: BoundLocInfo, var finished: Boolean = false)

    private val jobs = mutableMapOf<CoordGrid, Repair>()
    private val hammers = listOf(objs.hammer, objs.imcando_hammer, objs.imcando_hammer_offhand)
    private val planks = listOf(objs.plank, objs.oak_plank, objs.teak_plank, objs.mahogany_plank)
    private val nails =
        listOf(
            objs.bronze_nails,
            objs.iron_nails,
            objs.steel_nails,
            objs.black_nails,
            objs.mithril_nails,
            objs.adamant_nails,
            objs.rune_nails,
        )

    override fun ScriptContext.startup() {
        onOpLoc1(FishingCranes.broken) {
            if (!player.members) return@onOpLoc1
            if (statBase(stats.crafting) < 30 || statBase(stats.construction) < 30) {
                mes("You need 30 Crafting and 30 Construction to repair the crane.")
                return@onOpLoc1
            }
            clearWeakQueue(CraneRepairQueue.repair)
            attempt(jobs.getOrPut(it.loc.coords) { Repair(it.loc) })
        }
        onPlayerQueueWithArgs<Repair>(CraneRepairQueue.repair) { attempt(it.args) }
    }

    private fun ProtectedAccess.attempt(job: Repair) {
        val loc = job.target
        if (
            !isWithinDistance(loc, 1) ||
                (!job.finished && locations.findExact(loc.coords, FishingCranes.broken) == null)
        )
            return
        val wood = planks.firstOrNull { invTotal(inv, it) >= 3 }
        if (
            hammers.none { it in worn || it in inv } ||
                wood == null ||
                nails.sumOf { invTotal(inv, it) } < 9
        ) {
            mes(
                "Bring a hammer, three matching planks and at least nine nails to repair the fishing crane."
            )
            return
        }
        faceLoc(loc)
        anim(seqs.human_hammer_hit)
        val level = maxOf(stat(stats.crafting), stat(stats.construction)).coerceAtMost(99)
        if (!job.finished && random.of(10000) >= level * 30) {
            val nail = nails.first { it in inv }
            invDel(inv, nail)
            mes("You bend a nail while repairing the crane.")
            weakQueue(CraneRepairQueue.repair, 10, job)
            return
        }
        val paid =
            player.invTransaction(inv) {
                val bag = select(inv)
                delete(bag, wood.id, 3)
                var needed = 9
                for (nail in nails) {
                    val count = minOf(needed, invTotal(inv, nail))
                    if (count > 0) {
                        delete(bag, nail.id, count)
                        needed -= count
                    }
                    if (needed == 0) break
                }
            }
        if (paid.failure) return
        if (!job.finished) {
            locations.change(loc, FishingCranes.repaired, 50 + random.of(51))
            job.finished = true
            jobs.remove(loc.coords)
        }
        statAdvance(stats.crafting, statBase(stats.crafting) * 4.0)
        statAdvance(stats.construction, statBase(stats.construction) * 4.0)
        player.completeDiaryTask(DiaryTask.RepairPiscariliusCrane)
        mes("You successfully repair the fishing crane.")
    }
}
