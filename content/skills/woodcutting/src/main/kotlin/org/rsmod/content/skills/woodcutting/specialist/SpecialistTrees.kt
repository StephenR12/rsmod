package org.rsmod.content.skills.woodcutting.specialist

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.*
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.api.script.*
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.api.type.editors.loc.LocEditor
import org.rsmod.api.type.editors.varp.VarpEditor
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.content.skills.woodcutting.WoodcuttingAxeEffects
import org.rsmod.content.skills.woodcutting.WoodcuttingBirdNests
import org.rsmod.content.skills.woodcutting.WoodcuttingClueNests
import org.rsmod.content.skills.woodcutting.configs.WoodcuttingParams
import org.rsmod.api.config.FellingAxes
import org.rsmod.content.skills.woodcutting.forestry.ForestryHarvest
import org.rsmod.content.skills.woodcutting.scripts.Woodcutting
import org.rsmod.content.skills.woodcutting.scripts.Woodcutting.Companion.axeWoodcuttingAnim
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public object SpecialistLocs : LocReferences() {
    val cep = (1..6).map { find("fossil_cep_multi$it") }
    val grown = find("fossil_cep_grown")
    val sprout = find("fossil_cep_sprout")
    val blisterwood = find("blisterwood_tree")
    val doors = listOf(find("darkm_arboretum_doubledoorl"), find("darkm_arboretum_doubledoorr"))
}

public object SpecialistObjs : ObjReferences() {
    val mushroom = find("bittercap_mushroom")
    val fungus = find("mortmyremushroom")
    val cap = find("fossil_sulliuscep_cap")
    val numulite = find("fossil_numulite")
    val fossils = listOf("small", "medium", "large", "rare").map { find("fossil_${it}_unid") }
    val proboscis = find("mosquito_proboscis")
    val sprays = (10 downTo 0).map { find("slayer_spray_pump_$it") }
}

public object SpecialistBits : VarBitReferences() {
    val cep = find("fossil_cep")
    val quest = find("fossilquest_progress")
    val chopped = find("fossil_activity_sulliuscepchop")
    val injected = find("fossil_activity_sulliusceppoison")
    val fossilsBlocked = find("fossil_drop_blocked")
    val rareBlocked = find("fossil_drop_rare_blocked")
    val sins = find("myq5")
}

internal object SpecialistVars : VarpReferences() {
    val native =
        listOf(
            find("fossil_perm_transmit"),
            find("fossil_perm_transmit_noprotect"),
            find("fossil_storage11"),
        )
}

internal object SpecialistSaves : VarpEditor() {
    init {
        for (save in SpecialistVars.native) edit(save) { permanent = true }
    }
}

internal object SpecialistClueRate : LocEditor() {
    init {
        edit(SpecialistLocs.grown) { param[WoodcuttingParams.clue_nest_base_chance] = 343_000 }
    }
}

public object Sulliusceps {
    /** Native multiloc order is not the physical north-to-south route. */
    val sites =
        listOf(
            CoordGrid(3683, 3758),
            CoordGrid(3678, 3733),
            CoordGrid(3683, 3775),
            CoordGrid(3663, 3781),
            CoordGrid(3663, 3802),
            CoordGrid(3678, 3806),
        )
    val next = listOf(1, 5, 0, 2, 3, 4)
    val rates =
        listOf(
            11 to 55,
            17 to 85,
            23 to 115,
            26 to 131,
            29 to 147,
            35 to 177,
            41 to 208,
            45 to 228,
            48 to 240,
        )

    /** A single 3500-cell pre-roll preserves the dated fossil/numulite marginal probabilities. */
    fun preRoll(player: Player, roll: Int): Pair<ObjType, Int>? {
        require(roll in 0 until 3500)
        return when (roll) {
            in 0..899 -> SpecialistObjs.numulite to 0
            in 900..949 ->
                if (player.vars[SpecialistBits.fossilsBlocked] == 0) SpecialistObjs.fossils[0] to 1
                else null
            in 950..974 ->
                if (player.vars[SpecialistBits.fossilsBlocked] == 0) SpecialistObjs.fossils[1] to 1
                else null
            in 975..994 ->
                if (player.vars[SpecialistBits.fossilsBlocked] == 0) SpecialistObjs.fossils[2] to 1
                else null
            in 995..999 ->
                if (player.vars[SpecialistBits.rareBlocked] == 0) SpecialistObjs.fossils[3] to 1
                else null
            else -> null
        }
    }
}

/**
 * Native specialist targets share normal axe selection, charges, outfit and felling ration rules.
 */
public class SpecialistTrees
@Inject
constructor(
    private val types: ObjTypeList,
    private val locTypes: LocTypeList,
    private val xp: XpModifiers,
    private val harvest: ForestryHarvest,
    private val effects: WoodcuttingAxeEffects,
    private val nests: WoodcuttingBirdNests,
    private val clues: WoodcuttingClueNests,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (tree in SpecialistLocs.cep) {
            onOpLoc1(tree) { cut(this, it.loc, true) }
            onOpLoc2(tree) { inject(this, it.loc) }
        }
        onOpLoc1(SpecialistLocs.grown) { cut(this, it.loc, true) }
        onOpLoc2(SpecialistLocs.grown) { inject(this, it.loc) }
        onOpLoc1(SpecialistLocs.blisterwood) { cut(this, it.loc, false) }
        for (door in SpecialistLocs.doors) {
            onOpLoc1(door) { crossArboretum(this, it.loc) }
            onApLoc1(door) { if (isWithinApRange(it.loc, 1)) crossArboretum(this, it.loc) }
        }
    }

    public fun crossArboretum(access: ProtectedAccess, door: BoundLocInfo): Boolean =
        with(access) {
            stopAction()
            if (!player.members || vars[SpecialistBits.sins] < 112) {
                mes("The Arboretum is not accessible yet.")
                return false
            }
            if (door.coords.x != 3614 || door.coords.z !in 3361..3362 || !isWithinDistance(door, 1))
                return false
            telejump(
                CoordGrid(
                    if (player.coords.x <= 3614) 3615 else 3613,
                    player.coords.z.coerceIn(3361, 3362),
                )
            )
            true
        }

    private fun eligible(player: Player, tree: BoundLocInfo, cep: Boolean): Boolean {
        if (!player.members) return false
        return if (cep) {
            val index = Sulliusceps.sites.indexOf(tree.coords)
            player.vars[SpecialistBits.quest] >= 50 &&
                index >= 0 &&
                player.vars[SpecialistBits.cep] == index
        } else tree.coords == CoordGrid(3633, 3359) && player.vars[SpecialistBits.sins] >= 112
    }

    public suspend fun cut(access: ProtectedAccess, tree: BoundLocInfo, cep: Boolean): Unit =
        with(access) {
            val level = if (cep) 65 else 62
            if (!eligible(player, tree, cep)) {
                mes(
                    if (cep) "This sulliuscep is not ready to be cut."
                    else "You cannot cut the blisterwood tree yet."
                )
                return
            }
            if (player.stat(stats.woodcutting) < level) {
                mes("You need a Woodcutting level of $level.")
                return
            }
            while (eligible(player, tree, cep) && isWithinDistance(tree, 1)) {
                val axe = Woodcutting.findAxe(player, types)
                if (axe == null) {
                    mes("You need an axe you can use.")
                    break
                }
                if (inv.isFull()) {
                    mes("You don't have enough inventory space.")
                    break
                }
                faceLoc(tree)
                anim(types[axe].axeWoodcuttingAnim)
                gatheringDelay(4)
                if (
                    !eligible(player, tree, cep) ||
                        !isWithinDistance(tree, 1) ||
                        Woodcutting.findAxe(player, types) != axe ||
                        player.stat(stats.woodcutting) < level ||
                        inv.isFull()
                )
                    break
                val tier = axeTier(FellingAxes.ordinaryId(axe.id))
                val (low, high) = if (cep) Sulliusceps.rates[tier] else blisterwoodRate(tier)
                if (!statRandom(stats.woodcutting, low, high, 0)) continue
                val depleted = random.of(if (cep) 16 else 10) == 0
                if (cep) {
                    clues.tryDrops(this, locTypes[SpecialistLocs.grown])
                    if (nests.tryDrop(this, SpecialistObjs.mushroom)) {
                        if (depleted) advanceCep(this, tree)
                        if (depleted) break else continue
                    }
                }
                val felling = harvest.chop(player, axe)
                val product: ObjType
                val count: Int
                if (cep) {
                    val pre = Sulliusceps.preRoll(player, random.of(3500))
                    if (pre != null) {
                        product = pre.first
                        count = if (pre.second == 0) random.of(5..16) else pre.second
                    } else {
                        val roll = random.of(100)
                        product =
                            when (roll) {
                                0 -> SpecialistObjs.cap
                                in 1..9 -> SpecialistObjs.fungus
                                else -> SpecialistObjs.mushroom
                            }
                        count = 1
                    }
                } else {
                    product = objs.blisterwood_logs
                    count = 1
                }
                val effect = effects.apply(this, axe, product)
                if (!felling.skipLog && !effect.logConsumed) {
                    val tx = player.invTransaction(inv) { add(select(inv), product.id, count) }
                    if (!tx.success) break
                }
                statAdvance(
                    stats.woodcutting,
                    (if (cep) 127.0 else 76.0) * xp.get(player, stats.woodcutting) * felling.xp,
                )
                if (cep) vars[SpecialistBits.chopped] = 1
                mes(
                    if (felling.skipLog) "You make a clean cut without collecting any material."
                    else "You get some ${types[product].name.lowercase()}."
                )
                if (depleted) {
                    if (cep) advanceCep(this, tree)
                    else mes("You stop chopping the blisterwood tree.")
                    break
                }
            }
            resetAnim()
        }

    private fun advanceCep(access: ProtectedAccess, tree: BoundLocInfo) {
        with(access) {
            val index = Sulliusceps.sites.indexOf(tree.coords)
            if (index >= 0 && vars[SpecialistBits.cep] == index) {
                vars[SpecialistBits.cep] = Sulliusceps.next[index]
                mes("The sulliuscep dies away and another one grows elsewhere in the swamp.")
            }
        }
    }

    public fun inject(access: ProtectedAccess, tree: BoundLocInfo): Boolean =
        with(access) {
            if (!eligible(player, tree, true) || !isWithinDistance(tree, 1)) return false
            val spray =
                inv.indexOfFirst { obj ->
                    obj != null && SpecialistObjs.sprays.dropLast(1).any { it.id == obj.id }
                }
            if (spray < 0 || SpecialistObjs.proboscis !in inv) {
                mes("You need a proboscis and a charged fungicide spray.")
                return false
            }
            val selected = inv.getValue(spray)
            val replacement =
                SpecialistObjs.sprays[
                        SpecialistObjs.sprays.indexOfFirst { it.id == selected.id } + 1]
            val tx =
                player.invTransaction(inv) {
                    val i = select(inv)
                    delete(i, SpecialistObjs.proboscis.id, 1)
                    delete(i, selected.id, 1, slot = spray)
                    add(i, replacement.id, 1, slot = spray)
                }
            if (!tx.success) return false
            advanceCep(this, tree)
            vars[SpecialistBits.injected] = 1
            mes("You inject the sulliuscep with fungicide.")
            true
        }

    public companion object {
        fun axeTier(id: Int): Int =
            when (id) {
                1351 -> 0
                1349 -> 1
                1353 -> 2
                1361 -> 3
                1355 -> 4
                1357 -> 5
                1359,
                23279 -> 6
                23673 -> 8
                else -> 7
            }

        /**
         * Bronze15/50 and dragon61/186 are verified; intermediate tiers interpolate the Sulliuscep
         * tier ratios.
         */
        fun blisterwoodRate(tier: Int): Pair<Int, Int> {
            if (tier == 0) return 15 to 50
            if (tier == 7) return 61 to 186
            val ratio = (Sulliusceps.rates[tier].second - 55) / 173.0
            return (15 + 46 * ratio).toInt() to (50 + 136 * ratio).toInt()
        }
    }
}
