package org.rsmod.content.skills.prayer.remains

import jakarta.inject.Inject
import java.time.LocalDate
import java.time.ZoneOffset
import org.rsmod.api.config.refs.invs
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.diary.DiaryDifficulty
import org.rsmod.api.player.diary.DiaryRegion
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class Ectofuntus @Inject constructor(private val locations: LocRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        for ((bone, _) in EctoItems.recipes) onOpLocU(EctoLocs.loader, bone) {
            if (inv[it.invSlot]?.id == bone.id && load(bone.id)) scheduleGrinder(it.loc, bone.id)
        }
        onOpLoc1(EctoLocs.handle) { wind() }
        onOpLoc2(EctoLocs.handle) {
            mes(
                when (vars[EctoBits.stage]) {
                    1 -> "There are bones waiting to be ground."
                    2 -> "There is bonemeal waiting in the bin."
                    else -> "The grinder is empty."
                }
            )
        }
        onOpLoc1(EctoLocs.bin) { emptyBin() }
        onPlayerQueueWithArgs<GrindJob>(EctoQueues.grind) {
            val (loader, bone) = it.args
            if (!isWithinDistance(loader, 1) || !player.members) return@onPlayerQueueWithArgs
            val proceed =
                when (vars[EctoBits.stage]) {
                    0 -> load(bone)
                    1 -> wind()
                    2 -> emptyBin()
                    else -> false
                }
            if (proceed) scheduleGrinder(loader, bone)
        }
        for (pool in EctoLocs.pools) onOpLocU(pool, objs.bucket_empty) {
            if (inv[it.invSlot]?.id == objs.bucket_empty.id) fillBucket(it.loc)
        }
        onPlayerQueueWithArgs<BoundLocInfo>(EctoQueues.fill) {
            if (isWithinDistance(it.args, 1)) fillBucket(it.args)
        }
        onOpLoc1(EctoLocs.fountain) { worship() }
        onOpNpc1(EctoNpcs.disciple) { collect() }
        onOpNpc3(EctoNpcs.robin) { exchangeBones() }
        onOpNpc1(EctoNpcs.robin) { exchangeBones() }
        onOpLoc1(EctoLocs.trapdoor) { locations.change(it.loc, EctoLocs.openTrapdoor, 100) }
        onOpLoc2(EctoLocs.openTrapdoor) { locations.change(it.loc, EctoLocs.trapdoor, 100) }
        onOpLoc1(EctoLocs.openTrapdoor) { teleport(CoordGrid(3669, 9888, 3)) }
        onOpLoc1(EctoLocs.ladder) { teleport(CoordGrid(3653, 3519)) }
        onOpLoc1(EctoLocs.up) { teleport(CoordGrid(3667, 3521, 1)) }
        onOpLoc1(EctoLocs.down) { teleport(CoordGrid(3667, 3517)) }
        onOpLoc1(EctoLocs.cavernUp) { cavernStairs(it.loc, 1) }
        onOpLoc1(EctoLocs.cavernDown) { cavernStairs(it.loc, -1) }
    }

    private fun ProtectedAccess.cavernStairs(stairs: BoundLocInfo, levels: Int) {
        val east = stairs.angleId == 1
        val offset = if (east == (levels > 0)) 2 else -1
        teleport(CoordGrid(stairs.x + offset, stairs.z + 1, stairs.level + levels))
    }

    private fun ProtectedAccess.scheduleGrinder(loader: BoundLocInfo, bone: Int) {
        clearWeakQueue(EctoQueues.grind)
        weakQueue(EctoQueues.grind, 3, GrindJob(loader, bone))
    }

    private fun ProtectedAccess.load(bone: Int): Boolean {
        if (!player.members || vars[EctoBits.stage] != 0) return false
        val slot = inv.indexOfFirst { it?.id == bone }
        if (slot < 0) return false
        val input = EctoItems.inputs[bone] ?: return false
        if (!invDel(inv, input, slot = slot).success) return false
        vars[EctoVars.loaded] = bone
        vars[EctoBits.stage] = 1
        anim(EctoSeqs.load)
        return true
    }

    private fun ProtectedAccess.wind(): Boolean {
        if (!player.members || vars[EctoBits.stage] != 1) return false
        vars[EctoBits.stage] = 2
        anim(EctoSeqs.grind)
        return true
    }

    private fun ProtectedAccess.emptyBin(): Boolean {
        if (!player.members || vars[EctoBits.stage] != 2) return false
        val meal = EctoItems.byBone[vars[EctoVars.loaded]] ?: return false
        val slot = inv.indexOfFirst { it?.id == objs.pot_empty.id }
        if (slot < 0) {
            mes("You need an empty pot for the bonemeal.")
            return false
        }
        val result =
            player.invTransaction(inv) {
                val bag = select(inv)
                delete(bag, objs.pot_empty.id, 1, slot = slot)
                add(bag, meal.id, 1, slot = slot)
            }
        if (!result.success) return false
        vars[EctoBits.stage] = 0
        vars[EctoVars.loaded] = 0
        anim(EctoSeqs.empty)
        return true
    }

    private fun ProtectedAccess.fillBucket(pool: BoundLocInfo) {
        if (!player.members) return
        val slot = inv.indexOfFirst { it?.id == objs.bucket_empty.id }
        if (slot < 0) return
        val result =
            player.invTransaction(inv) {
                val bag = select(inv)
                delete(bag, objs.bucket_empty.id, 1, slot = slot)
                add(bag, EctoItems.slime.id, 1, slot = slot)
            }
        if (!result.success) return
        anim(EctoSeqs.fill)
        if (objs.bucket_empty in inv) weakQueue(EctoQueues.fill, 2, pool)
    }

    // The two original fields together hold an eight-bit count of worships. Each earns five tokens.
    private var ProtectedAccess.unclaimedWorships: Int
        get() = vars[EctoBits.tokensLow] or (vars[EctoBits.tokensHigh] shl 6)
        set(value) {
            vars[EctoBits.tokensLow] = value and 63
            vars[EctoBits.tokensHigh] = value ushr 6
        }

    private fun ProtectedAccess.worship() {
        if (!player.members || actionDelay > mapClock) return
        if (unclaimedWorships >= 200) {
            mes("There isn't room to put any more in.")
            return
        }
        val slot = inv.indexOfFirst { it?.id in EctoItems.byMeal }
        if (slot < 0 || EctoItems.slime !in inv) {
            mes("You need bonemeal and a bucket of slime.")
            return
        }
        val meal = requireNotNull(inv[slot])
        val bone = EctoItems.byMeal.getValue(meal.id)
        if (bone == objs.superior_dragon_bones && statBase(stats.prayer) < 70) {
            mes("You need a Prayer level of 70 to use these bones.")
            return
        }
        if (!zealotPreservesOffering()) {
            val result =
                player.invTransaction(inv) {
                    val bag = select(inv)
                    delete(bag, meal.id, 1, slot = slot)
                    delete(bag, EctoItems.slime.id, 1)
                    add(bag, objs.pot_empty.id, 1, slot = slot)
                    add(bag, objs.bucket_empty.id, 1)
                }
            if (!result.success) return
        }
        val xp =
            when (bone) {
                objs.burnt_jogre_bones -> 64.0
                objs.wyrmling_bones -> 120.0
                else -> PrayerRemainsRecipes.byObj.getValue(bone.id).xp * 4
            }
        unclaimedWorships++
        anim(EctoSeqs.pray)
        statAdvance(stats.prayer, xp)
        player.completeDiaryTask(DiaryTask.WorshipEctofuntus)
        mes("You worship the Ectofuntus.")
        actionDelay = mapClock + 2
    }

    private fun ProtectedAccess.collect() {
        if (!player.members) return
        if (EctoItems.ghostspeak.none { it in worn }) {
            mes("Wooo woooo wooooo woo.")
            return
        }
        val count = unclaimedWorships * 5
        if (count == 0) {
            val hard = vars[DiaryRegion.Morytania.tier(DiaryDifficulty.Hard).reward] != 0
            val ownsCrusher = listOfNotNull(inv, worn, player.invMap[invs.bank]).any { bag ->
                EctoItems.crusher in bag || EctoItems.crusherNecklace in bag
            }
            if (hard && !ownsCrusher) {
                if (invAdd(inv, EctoItems.crusher).success) mes("The ghost disciple gives you a bonecrusher.")
            } else {
                mes("You haven't earned any Ecto-tokens.")
            }
            return
        }
        if (invAdd(inv, EctoItems.tokens, count).success) {
            unclaimedWorships = 0
            mes("The ghost disciple gives you ${count} Ecto-tokens.")
        }
    }

    private fun ProtectedAccess.exchangeBones() {
        if (!player.members) return
        val tier =
            DiaryDifficulty.entries.lastOrNull { vars[DiaryRegion.Morytania.tier(it).reward] != 0 }
        val limit = (tier?.ordinal ?: 0) * 13
        if (limit == 0) {
            mes("Complete the Morytania Medium Diary to exchange bones.")
            return
        }
        val day = LocalDate.now(ZoneOffset.UTC).toEpochDay().toInt()
        val claimed = if (vars[EctoVars.claimDay] == day) vars[EctoBits.robinClaimed] else 0
        val remaining = (limit - claimed).coerceAtLeast(0)
        val slots =
            inv.indices
                .filter { inv[it]?.id in EctoItems.byBone }
                .take(minOf(remaining, inv.freeSpace()))
        if (slots.isEmpty()) {
            mes(
                if (remaining == 0) "You've claimed your allowance for today."
                else "Bring unnoted bones and room for the slime."
            )
            return
        }
        val result =
            player.invTransaction(inv) {
                val bag = select(inv)
                for (slot in slots) {
                    val bone = requireNotNull(inv[slot]).id
                    delete(bag, bone, 1, slot = slot)
                    add(bag, EctoItems.byBone.getValue(bone).id, 1, slot = slot)
                }
                add(bag, EctoItems.slime.id, slots.size)
            }
        if (result.success) {
            vars[EctoVars.claimDay] = day
            vars[EctoBits.robinClaimed] = claimed + slots.size
            mes("Robin exchanges your bones for bonemeal and slime.")
        }
    }

    private data class GrindJob(val loader: BoundLocInfo, val bone: Int)
}
