package org.rsmod.content.skills.construction

import jakarta.inject.Inject
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.minigames.ContactAmy
import org.rsmod.api.player.minigames.MahoganyContract
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.*
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.timer.TimerReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.content.skills.construction.CrystalSaw.Companion.charges
import org.rsmod.content.skills.construction.CrystalSaw.Companion.useCharge
import org.rsmod.game.entity.Player
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.obj.ObjType
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object HomeContractors : NpcReferences() {
    val offices =
        listOf(
                "con_contractor",
                "con_contractor_varrock",
                "con_contractor_ardougne",
                "con_contractor_hosidius",
            )
            .map { find(it) }
    val intro =
        listOf(
                "con_contractor_1op",
                "con_contractor_varrock_1op",
                "con_contractor_ardougne_1op",
                "con_contractor_hosidius_1op",
            )
            .map { find(it) }
    val introduced =
        listOf(
                "con_contractor_3op",
                "con_contractor_varrock_2op",
                "con_contractor_ardougne_2op",
                "con_contractor_hosidius_2op",
            )
            .map { find(it) }
    val clients = (1..11).map { find("con_contract_client_$it") } + find("falador_man4")
}

internal object ContractBits : VarBitReferences() {
    val discussed = find("con_contract_discussed")
}

internal object ContractTimers : TimerReferences() {
    val display = find("mahogany_homes_display")
}

internal object ContractVars : VarpReferences() {
    val tier = find("mahogany_homes_tier")
    val points = find("mahogany_homes_points")
    val sack = find("mahogany_homes_sack")
    val furniture = find("mahogany_homes_furniture")
    val display = find("construction_contract_building_display")
    val hosidius = find("poh_hosidius_style_unlocked")
}

internal object ContractSaves : VarpBuilder() {
    init {
        build("poh_hosidius_style_unlocked") { transmitNever = true }
        for (n in listOf("tier", "points", "sack", "furniture")) build("mahogany_homes_$n") {
            transmitNever = true
        }
    }
}

internal enum class ContractTier(
    val level: Int,
    val plank: ObjType,
    val plankXp: Double,
    val repairXp: Double,
    val bonus: Double,
) {
    Beginner(1, objs.plank, 22.5, 127.5, 500.0),
    Novice(20, objs.oak_plank, 48.0, 160.0, 1250.0),
    Adept(50, objs.teak_plank, 72.0, 190.0, 2250.0),
    Expert(70, objs.mahogany_plank, 112.0, 240.0, 2750.0);

    val points
        get() = ordinal + 2
}

internal data class ContractHouse(
    val id: Int,
    val name: String,
    val area: String,
    val bounds: List<Int>,
    val costs: List<Int>,
    val layouts: List<List<Int>>,
) {
    val city
        get() =
            when (area.substringBefore('_')) {
                "ardy" -> "East Ardougne"
                "hosidius" -> "Hosidius"
                "falador" -> "Falador"
                else -> "Varrock"
            }

    fun contains(tile: CoordGrid) =
        tile.level <= 1 && tile.x in bounds[0]..bounds[1] && tile.z in bounds[2]..bounds[3]
}

public class MahoganyHomes @Inject constructor(private val locTypes: LocTypeList) : PluginScript() {
    private val houses =
        requireNotNull(javaClass.getResourceAsStream("mahogany-homes.tsv"))
            .bufferedReader()
            .useLines { lines ->
                lines
                    .mapIndexed { index, line ->
                        val fields = line.split('\t')
                        fun ints(value: String) = value.split(',').map(String::toInt)
                        ContractHouse(
                            index + 1,
                            fields[0],
                            fields[1],
                            ints(fields[2]),
                            ints(fields[3]),
                            fields.drop(4).map(::ints),
                        )
                    }
                    .toList()
            }
    private val hammers = listOf(objs.hammer, objs.imcando_hammer, objs.imcando_hammer_offhand)

    private data class Fixture(
        val slot: Int,
        val house: ContractHouse,
        val repair: Boolean,
        val name: String,
    )

    private val fixtures = mutableMapOf<Int, Fixture>()

    override fun ScriptContext.startup() {
        onProtectedEvent<ContactAmy>(ContactAmy.id) { offer(false, false) }
        for ((i, office) in HomeContractors.introduced.withIndex()) {
            onOpNpc1(HomeContractors.intro[i]) { offer(i == 0, false) }
            onOpNpc1(office) { offer(i == 0, false) }
            onOpNpc3(office) { offer(i == 0, false) }
            onOpNpc4(office) { offer(i == 0, true) }
        }
        for (house in houses) onOpNpc1(HomeContractors.clients[house.id - 1]) { visit(house) }
        val actions = mutableSetOf<Pair<Int, Int>>()
        for (base in locTypes.values) {
            val match = HOTSPOT.matchEntire(base.internalName.orEmpty()) ?: continue
            val house = houses.single { it.area == match.groupValues[1] }
            val slot = match.groupValues[2].toInt() - 1
            val fixture =
                Fixture(
                    slot,
                    house,
                    base.multiLoc.size == 3,
                    locTypes[base.multiLoc[0].toInt() and 65535]?.name ?: "furniture",
                )
            fixtures[base.id] = fixture
            for (index in listOf(1, 3, 4)) {
                val id = base.multiLoc.getOrNull(index)?.toInt()?.and(65535) ?: continue
                val visual = locTypes[id] ?: continue
                when (index) {
                    1 -> if (actions.add(id to 3)) onOpLoc3(visual) { work(it.loc.id, 1) }
                    3 -> if (actions.add(id to 2)) onOpLoc2(visual) { work(it.loc.id, 3) }
                    4 -> if (actions.add(id to 4)) onOpLoc4(visual) { work(it.loc.id, 4) }
                }
            }
        }
        onPlayerLogin { refresh(player) }
        onPlayerSoftTimer(ContractTimers.display) { refresh(player) }
    }

    private fun refresh(player: Player) {
        val house = houses.getOrNull(player.vars[MahoganyContract.client] - 1)
        val display =
            if (house?.contains(player.coords) == true) player.vars[ContractVars.furniture] else 0
        if (player.vars[ContractVars.display] != display)
            VarPlayerIntMapSetter.set(player, ContractVars.display, display)
        if (house != null) player.softTimer(ContractTimers.display, 1)
    }

    private suspend fun ProtectedAccess.offer(amy: Boolean, last: Boolean) {
        if (!player.members) return
        if (vars[ContractBits.discussed] == 0 && !amy) {
            mes("Speak to Amy in Falador first to join Mahogany Homes.")
            return
        }
        val active = houses.getOrNull(vars[MahoganyContract.client] - 1)
        if (active != null) {
            mes("You still have a contract for ${active.name} in ${active.city}.")
            return
        }
        val old = vars[ContractVars.tier] - 1
        val tier =
            if (last && old in ContractTier.entries.indices) ContractTier.entries[old]
            else {
                val selection =
                    menu(
                        "Choose a Mahogany Homes contract",
                        true,
                        ContractTier.entries.map { "${it.name} (level ${it.level})" },
                    )
                ContractTier.entries.getOrNull(selection) ?: return
            }
        if (stat(stats.construction) < tier.level) {
            mes("You need level ${tier.level} Construction for this contract.")
            return
        }
        val house = houses[random.of(houses.size)]
        val patterns = house.layouts[tier.ordinal]
        val mask = patterns[random.of(patterns.size)]
        var state = 0
        for (fixture in fixtures.values.filter { it.house == house }) {
            if ((mask and (1 shl fixture.slot)) != 0)
                state = state or ((if (fixture.repair) 1 else 3) shl (4 * fixture.slot))
        }
        vars[ContractBits.discussed] = 1
        vars[ContractVars.tier] = tier.ordinal + 1
        vars[ContractVars.furniture] = state
        vars[MahoganyContract.client] = house.id
        refresh(player)
        mes("Visit ${house.name} in ${house.city} and repair the furniture at their house.")
    }

    private suspend fun ProtectedAccess.work(baseId: Int, action: Int) {
        val fixture = fixtures[baseId] ?: return
        val house = fixture.house
        if (!player.members || vars[MahoganyContract.client] != house.id || !house.contains(coords))
            return
        val shift = fixture.slot * 4
        fun state() = (vars[ContractVars.furniture] ushr shift) and 15
        if (state() != action) return
        val tier = ContractTier.entries.getOrNull(vars[ContractVars.tier] - 1) ?: return
        if (
            hammers.none { it in inv || it in worn } ||
                StashObjs.saws.none { it in inv || it in worn }
        ) {
            mes("Bring a hammer and a saw to work on this contract.")
            return
        }
        if (action == 3) {
            anim(seqs.human_hammer_hit)
            delay(2)
            if (state() == 3) updateState(shift, 4)
            return
        }
        val boost = action == 4 && stat(stats.construction) < tier.level
        if (
            boost &&
                (SawItems.saw !in inv ||
                    charges() == 0 ||
                    stat(stats.construction) + 3 < tier.level)
        ) {
            mes("You need level ${tier.level} Construction to build this furniture.")
            return
        }
        val cost = house.costs[fixture.slot]
        val material = if (cost < 0) objs.steel_bar else tier.plank
        val amount = kotlin.math.abs(cost)
        if (invTotal(inv, material) + ContractRewards.sackCount(this, material) < amount) {
            mes(
                if (cost < 0) "This repair requires a steel bar."
                else "You need $amount ${tier.plankName()} for this job."
            )
            return
        }
        anim(seqs.human_hammer_hit)
        delay(3)
        if (
            state() != action ||
                vars[MahoganyContract.client] != house.id ||
                !ContractRewards.spend(this, material, amount)
        )
            return
        if (boost) useCharge()
        updateState(shift, if (action == 1) 2 else 5 + tier.ordinal)
        val xp =
            when {
                cost < 0 -> 120.0
                action == 1 -> tier.repairXp
                else -> tier.plankXp * amount
            }
        statAdvance(stats.construction, xp * ContractRewards.outfitModifier(this))
        mes("You ${if(action==1) "repair" else "build"} the ${fixture.name.lowercase()}.")
    }

    private fun ContractTier.plankName() =
        when (this) {
            ContractTier.Beginner -> "regular planks"
            ContractTier.Novice -> "oak planks"
            ContractTier.Adept -> "teak planks"
            ContractTier.Expert -> "mahogany planks"
        }

    private fun ProtectedAccess.updateState(shift: Int, value: Int) {
        vars[ContractVars.furniture] =
            (vars[ContractVars.furniture] and (15 shl shift).inv()) or (value shl shift)
        refresh(player)
    }

    private fun ProtectedAccess.visit(house: ContractHouse) {
        if (!player.members || vars[MahoganyContract.client] != house.id) {
            mes("I haven't requested any work from you.")
            return
        }
        val state = vars[ContractVars.furniture]
        val outstanding =
            fixtures.values.filter {
                it.house == house && ((state ushr (it.slot * 4)) and 15) in listOf(1, 3, 4)
            }
        if (outstanding.isNotEmpty()) {
            mes(
                "Please finish the following furniture: " +
                    outstanding.sortedBy { it.slot }.joinToString { it.name.lowercase() } +
                    "."
            )
            return
        }
        val tier = ContractTier.entries.getOrNull(vars[ContractVars.tier] - 1) ?: return
        vars[MahoganyContract.client] = 0
        vars[ContractVars.furniture] = 0
        vars[ContractVars.display] = 0
        clearSoftTimer(ContractTimers.display)
        vars[ContractVars.points] += tier.points
        statAdvance(stats.construction, tier.bonus * ContractRewards.outfitModifier(this))
        player.runEnergy = constants.run_max_energy
        mes(
            "Thank you for your work! You earn ${tier.points} carpenter points and drink a refreshing cup of tea."
        )
    }

    private companion object {
        val HOTSPOT = Regex("con_contract_(.+)_hotspot([1-8])")
    }
}
