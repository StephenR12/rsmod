package org.rsmod.content.skills.farming

import jakarta.inject.Inject
import java.util.WeakHashMap
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.registry.player.PlayerRegistry
import org.rsmod.api.registry.region.RegionRegistry
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.region.RegionRepository
import org.rsmod.api.repo.region.RegionTemplate
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.script.onPlayerTimer
import org.rsmod.api.type.refs.interf.InterfaceReferences
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.timer.TimerReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.region.Region
import org.rsmod.game.type.loc.LocType
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object TitheLocs : LocReferences() {
    val door = find("hosidius_tithe_farm_door")
    val table = find("tithe_plant_seed_table")
    val empty = find("hosidius_tithe_empty")
    val sacks = listOf(find("tithe_sack_of_fruit_empty"), find("tithe_sack_of_fruit"))
    val plants =
        ('a'..'c').map { crop ->
            (1..4).map { stage ->
                val name = "hosidius_tithe_${crop}_${stage}"
                val dry = find(if (stage == 4) name else name + "_dry")
                TitheStage(dry, if (stage == 4) null else find(name + "_wet"), find(name + "_dead"))
            }
        }
}

internal data class TitheStage(val dry: LocType, val wet: LocType?, val dead: LocType)

internal object TitheItems : ObjReferences() {
    val seeds = ('a'..'c').map { find("hosidius_tithe_seed_$it") }
    val fruits = ('a'..'c').map { find("hosidius_tithe_fruit_$it") }
    val fertiliser = find("hosidius_tithe_fertiliser")
    val outfit =
        listOf("hat", "torso", "legs", "feet").map { part ->
            listOf("male", "female").map { find("tithe_reward_${part}_$it") }
        }
}

internal object TitheBits : VarBitReferences() {
    val score = find("hosidius_tithe_score")
    val kind = find("hosidius_tithe_type")
    val points = find("hosidius_tithe_rewardpoints")
    val barbarianPlant = find("brut_farming_planting")
}

internal object TitheTimers : TimerReferences() {
    val growth = find("tithe_farm_growth")
}

internal object TitheInterfaces : InterfaceReferences() {
    val hud = find("hosidius_strip_farming")
}

public class TitheFarm
@Inject
constructor(
    private val petRates:org.rsmod.content.skills.pets.SkillPetRates,
    private val regions: RegionRepository,
    private val registry: RegionRegistry,
    private val players: PlayerRegistry,
    private val locs: LocRepository,
) : PluginScript() {
    private data class Plot(
        val loc: BoundLocInfo,
        val crop: Int,
        var next: Int,
        var stage: Int = 0,
        var wet: Boolean = false,
        var dead: Boolean = false,
        var fertilised: Boolean = false,
    ) {
        val ticks: Int
            get() = if (fertilised) 50 else 100

        fun appearance(): LocType {
            val types = TitheLocs.plants[crop][stage]
            return when {
                dead -> types.dead
                wet -> requireNotNull(types.wet)
                else -> types.dry
            }
        }
    }

    private data class Farm(
        val region: Region,
        val plots: MutableMap<CoordGrid, Plot> = mutableMapOf(),
    )

    private val farms = WeakHashMap<Player, Farm>()
    private val template =
        RegionTemplate.create {
            copyAllLevels(225, 435) {
                zoneWidth = 6
                zoneLength = 5
                regionZoneX = 1
                regionZoneZ = 1
            }
        }

    override fun ScriptContext.startup() {
        onOpLoc1(TitheLocs.table) { chooseSeeds() }
        onOpLoc1(TitheLocs.door) {
            if (!player.members) return@onOpLoc1
            if (it.loc.coords == DOOR) enter()
            else if (
                ownsFarm() &&
                    choice2(
                        "Leave and discard remaining seeds, fruit and progress?",
                        true,
                        "Stay in the farm.",
                        false,
                    )
            ) {
                finish()
                telejump(LOBBY)
            }
        }
        for ((i, seed) in TitheItems.seeds.withIndex()) onOpLocU(TitheLocs.empty, seed) {
            plant(it.loc, i)
        }
        for (stages in TitheLocs.plants) for ((i, stage) in stages.withIndex()) {
            if (stage.wet != null) {
                onOpLoc2(stage.dry) { water(it.loc) }
                for (can in
                    FarmingObjs.wateringCans.drop(1) +
                        org.rsmod.api.config.refs.objs.gricollers_can) onOpLocU(stage.dry, can) {
                    water(it.loc, it.invSlot)
                }
                for (type in listOf(stage.dry, stage.wet)) onOpLocU(type, TitheItems.fertiliser) {
                    fertilise(it.loc)
                }
            }
            if (i == 3) onOpLoc1(stage.dry) { harvest(it.loc) }
            onOpLoc1(stage.dead) { clear(it.loc) }
        }
        for (sack in TitheLocs.sacks) {
            onOpLoc1(sack) { deposit() }
            for (fruit in TitheItems.fruits) onOpLocU(sack, fruit) { deposit() }
        }
        onPlayerTimer(TitheTimers.growth) {
            val farm =
                farms[player]
                    ?: run {
                        player.clearTimer(TitheTimers.growth)
                        return@onPlayerTimer
                    }
            if (!ownsFarm()) {
                finish()
                return@onPlayerTimer
            }
            for (plot in farm.plots.values) {
                if (plot.dead || mapClock < plot.next) continue
                if (!plot.wet || plot.stage == 3) plot.dead = true
                else {
                    plot.stage++
                    plot.wet = false
                }
                plot.next += plot.ticks
                locs.change(plot.loc, plot.appearance(), Int.MAX_VALUE)
            }
            timer(TitheTimers.growth, 1)
        }
        onPlayerLogout {
            val farm = farms.remove(player) ?: return@onPlayerLogout
            if (registry[player.coords] === farm.region) players.relocateOnLogout(player, LOBBY)
        }
    }

    private suspend fun ProtectedAccess.chooseSeeds() {
        if (!player.members || coords.chebyshevDistance(LOBBY) > 8) return
        val crop = menu("Choose seeds", true, names.map { "$it seeds" })
        if (crop !in names.indices) return
        if (stat(stats.farming) < levels[crop]) {
            mes("You need a Farming level of ${levels[crop]} to take these seeds.")
            return
        }
        val other =
            TitheItems.seeds.indices.any {
                it != crop && (TitheItems.seeds[it] in inv || TitheItems.fruits[it] in inv)
            }
        if (other || (vars[TitheBits.score] > 0 && vars[TitheBits.kind] != crop + 1)) {
            mes("Finish your current batch before taking another type of seed.")
            return
        }
        val amount =
            countDialog("How many seeds would you like?")
                .coerceIn(0, 10_000 - invTotal(inv, TitheItems.seeds[crop]).coerceAtMost(10_000))
        if (amount > 0 && invAdd(inv, TitheItems.seeds[crop], amount).failure)
            mes("You need room for the seeds.")
    }

    private fun ProtectedAccess.enter() {
        if (ownsFarm()) return
        val crop =
            TitheItems.seeds.indices.firstOrNull {
                TitheItems.seeds[it] in inv || TitheItems.fruits[it] in inv
            }
                ?: (vars[TitheBits.kind] - 1).takeIf {
                    it in names.indices && vars[TitheBits.score] > 0
                }
        if (crop == null) {
            mes("Take some seeds from the table before entering the farm.")
            return
        }
        val region =
            regions.add(template)
                ?: run {
                    mes("The farm is currently full.")
                    return
                }
        region.reclaimFromCycle = mapClock + 5
        farms[player] = Farm(region)
        vars[TitheBits.kind] = crop + 1
        if (TitheItems.fertiliser !in inv) invAdd(inv, TitheItems.fertiliser)
        telejump(region.normal[LANDING])
        ifOpenOverlay(TitheInterfaces.hud)
        timer(TitheTimers.growth, 1)
    }

    private fun ProtectedAccess.ownsFarm(): Boolean =
        farms[player]?.region?.let { registry[coords] === it } == true

    private fun ProtectedAccess.plot(loc: BoundLocInfo): Plot? =
        farms[player]?.takeIf { registry[coords] === it.region }?.plots?.get(loc.coords)

    private fun ProtectedAccess.plant(loc: BoundLocInfo, crop: Int) {
        if (!ownsFarm() || farms.getValue(player).plots.containsKey(loc.coords)) return
        if (stat(stats.farming) < levels[crop]) {
            mes("You need a Farming level of ${levels[crop]} to plant these seeds.")
            return
        }
        if (FarmingObjs.dibber !in inv && vars[TitheBits.barbarianPlant] < 2) {
            mes("You need a seed dibber to plant these seeds.")
            return
        }
        if (vars[TitheBits.kind] != crop + 1 || invDel(inv, TitheItems.seeds[crop]).failure) return
        val plot = Plot(loc, crop, mapClock + 100)
        farms.getValue(player).plots[loc.coords] = plot
        locs.change(loc, plot.appearance(), Int.MAX_VALUE)
        anim(FarmingSeqs.plant)
        if (crop == 2) player.completeDiaryTask(DiaryTask.PlantTitheLogavano)
    }

    private fun ProtectedAccess.water(loc: BoundLocInfo, selected: Int? = null) {
        val plot = plot(loc) ?: return
        if (plot.dead || plot.stage == 3 || plot.wet) return
        val slot = selected ?: inv.mapSlots { candidate, _ -> hasWater(candidate) }.minOrNull()
        if (slot == null || !useWater(slot)) {
            mes("You need a watering can with water in it.")
            return
        }
        plot.wet = true
        locs.change(loc, plot.appearance(), Int.MAX_VALUE)
        anim(FarmingSeqs.water)
    }

    private fun ProtectedAccess.fertilise(loc: BoundLocInfo) {
        val plot = plot(loc) ?: return
        if (plot.dead || plot.stage == 3 || plot.fertilised) return
        if (TitheItems.fertiliser !in inv) return
        plot.fertilised = true
        plot.next = mapClock + ((plot.next - mapClock).coerceAtLeast(1) + 1) / 2
        mes("You fertilise the plant. It will grow twice as fast.")
    }

    private fun ProtectedAccess.clear(loc: BoundLocInfo) {
        val plot = plot(loc) ?: return
        if (!plot.dead) return
        farms.getValue(player).plots.remove(loc.coords)
        locs.change(loc, TitheLocs.empty, Int.MAX_VALUE)
        anim(FarmingSeqs.harvest)
    }

    private fun ProtectedAccess.harvest(loc: BoundLocInfo) {
        val plot = plot(loc) ?: return
        if (plot.dead || plot.stage != 3) return
        if (invAdd(inv, TitheItems.fruits[plot.crop]).failure) {
            mes("You need room for the fruit.")
            return
        }
        farms.getValue(player).plots.remove(loc.coords)
        locs.change(loc, TitheLocs.empty, Int.MAX_VALUE)
        anim(FarmingSeqs.harvest)
        farmingXp(harvestXp[plot.crop])
    }

    private fun ProtectedAccess.deposit() {
        if (!ownsFarm()) return
        val crop = vars[TitheBits.kind] - 1
        if (crop !in names.indices) return
        val fruit = TitheItems.fruits[crop]
        val previous = vars[TitheBits.score]
        val amount = minOf(100 - previous, invTotal(inv, fruit))
        if (amount <= 0) {
            mes("You have no fruit to deposit.")
            return
        }
        if (invDel(inv, fruit, amount).failure) return
        repeat(amount){petRates.roll(player,stats.farming,20661,7494389)}
        val score = previous + amount
        val bonusFruit = (score - 74).coerceAtLeast(0) - (previous - 74).coerceAtLeast(0)
        val bonus = if (previous < 75 && score >= 75) 250 else 0
        val points = score / 3 - previous / 3 + if (score == 100) 2 else 0
        vars[TitheBits.points] = (vars[TitheBits.points] + points).coerceAtMost(16000)
        vars[TitheBits.score] = score % 100
        farmingXp((amount + bonusFruit) * 10 * harvestXp[crop])
        if (bonus > 0) statAdvance(stats.farming, bonus * harvestXp[crop])
        mes("You deposit $amount fruit in the sack.")
    }

    private fun ProtectedAccess.finish() {
        farms.remove(player)
        player.clearTimer(TitheTimers.growth)
        for (item in TitheItems.seeds + TitheItems.fruits + TitheItems.fertiliser) {
            val count = invTotal(inv, item)
            if (count > 0) invDel(inv, item, count)
        }
        vars[TitheBits.score] = 0
        vars[TitheBits.kind] = 0
        ifCloseSub(TitheInterfaces.hud)
    }

    internal companion object {
        val DOOR = CoordGrid(1805, 3501)
        val LOBBY = CoordGrid(1804, 3501)
        val LANDING = CoordGrid(1806, 3501)
        private val names = listOf("Golovanova", "Bologano", "Logavano")
        private val levels = listOf(34, 54, 74)
        private val harvestXp = listOf(6.0, 14.0, 23.0)
    }
}

internal fun ProtectedAccess.farmingXp(base: Double) {
    val weights = listOf(4, 8, 6, 2)
    val equipped =
        TitheItems.outfit.indices.filter { piece -> TitheItems.outfit[piece].any { it in worn } }
    val perMille = equipped.sumOf { weights[it] } + if (equipped.size == 4) 5 else 0
    statAdvance(stats.farming, base * (1000 + perMille) / 1000)
}
