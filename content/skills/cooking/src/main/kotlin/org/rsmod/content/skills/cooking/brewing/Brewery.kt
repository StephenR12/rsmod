package org.rsmod.content.skills.cooking.brewing

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.*
import org.rsmod.api.player.front
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.*
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.obj.ObjType
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Three native personal vats, real640-minute growth phases and persistent barrels. */
@Singleton
public class Brewery
@Inject
constructor(private val random: GameRandom, private val locs: LocRepository) : PluginScript() {
    private data class Press(
        var apples: Int = 0,
        var busyUntil: Int = Int.MIN_VALUE,
        var crushed: Boolean = false,
    )

    private val presses = mutableMapOf<CoordGrid, Press>()

    override fun ScriptContext.startup() {
        val ingredients =
            listOf(BreweryObjs.water, BreweryObjs.malt, BreweryObjs.yeast, BreweryObjs.stuff) +
                BreweryObjs.ingredients
        for (vat in
            (BreweryLocs.vats + BreweryLocs.transformedVats).distinctBy { it.id }) for (input in
            ingredients.distinctBy { it.id }) onOpLocU(vat, input) {
            add(this, it.loc, input, it.invSlot)
        }
        for (valve in BreweryLocs.valves) onOpLoc1(valve) { turn(this, it.loc) }
        for (barrel in
            (BreweryLocs.barrels + BreweryLocs.transformedBarrels).distinctBy { it.id }) {
            onOpLoc1(barrel) { drain(this, it.loc) }
            for (container in listOf(BreweryObjs.glass, BreweryObjs.keg)) onOpLocU(
                barrel,
                container,
            ) {
                collect(this, it.loc, container, it.invSlot)
            }
        }
        for (press in listOf(BreweryLocs.press, BreweryLocs.mush)) {
            onOpLocU(press, BreweryObjs.apple) { loadApple(this, it.loc, it.invSlot) }
            onOpLocU(press, BreweryObjs.bucket) { drawMush(this, it.loc, it.invSlot) }
        }
        onOpLocU(BreweryLocs.tap, BreweryObjs.bucket) { drawMush(this, it.loc, it.invSlot) }
        for ((index, npc) in BreweryNpcs.all.withIndex()) onOpNpc1(npc) {
            val price = if (index == 1) "five ecto-tokens" else "25 coins"
            if (
                menu(
                    "Ale yeast costs $price and an empty pot.",
                    "Buy ale yeast.",
                    "No, thank you.",
                ) == 1
            )
                buyYeast(this, index)
        }
        for (knife in listOf(objs.knife, objs.fletching_knife)) onOpHeldU(
            knife,
            BreweryObjs.fruit,
        ) {
            hollow(this, it.secondSlot)
        }
        onOpLoc1(BreweryLocs.trapdoor) {
            if (player.members) locs.change(it.loc, BreweryLocs.trapdoorOpen, Int.MAX_VALUE)
        }
        onOpLoc1(BreweryLocs.trapdoorOpen) { if (player.members) telejump(CoordGrid(3682, 9961)) }
        onOpLoc2(BreweryLocs.trapdoorOpen) {
            locs.change(it.loc, BreweryLocs.trapdoor, Int.MAX_VALUE)
        }
        onOpLoc1(BreweryLocs.ladder) { telejump(CoordGrid(3681, 3499)) }
        onPlayerLogin { update(player) }
    }

    private fun control(player: Player, index: Int): Int =
        (player.vars[BreweryVars.control] ushr (index * 8)) and 255

    private fun control(player: Player, index: Int, value: Int) {
        val shift = index * 8
        val old = player.vars[BreweryVars.control]
        VarPlayerIntMapSetter.set(
            player,
            BreweryVars.control,
            (old and (255 shl shift).inv()) or ((value and 255) shl shift),
        )
    }

    public fun growthCycle(player: Player, nowMillis: Long): Int {
        // Same phase model as FarmingPatchRepository; no Cooking-to-Farming dependency.
        val offset = Math.floorMod(player.displayName.hashCode().toLong(), 30L * 60_000)
        return Math.floorDiv(nowMillis + offset, 640L * 60_000).toInt()
    }

    public fun update(player: Player, nowMillis: Long = System.currentTimeMillis()) {
        for (index in Breweries.all.indices) {
            val value = player.vars[BreweryBits.vat[index]]
            val recipe = AleRecipes.fromVat(value) ?: continue
            val relative = value - recipe.preparation
            if (relative !in 1..2) continue
            val started = player.vars[BreweryVars.started[index]]
            if (started <= 0) continue
            val elapsed = growthCycle(player, nowMillis) - started
            val target = (control(player, index) ushr 3).coerceAtLeast(3)
            val next =
                if (elapsed >= target)
                    when (control(player, index) and 3) {
                        2 -> recipe.preparation + 4
                        3 -> if (recipe.index == 9) 65 else 64
                        else -> recipe.preparation + 3
                    }
                else if (elapsed >= 1) recipe.preparation + 2 else recipe.preparation + 1
            VarPlayerIntMapSetter.set(player, BreweryBits.vat[index], next)
        }
    }

    public fun add(
        access: ProtectedAccess,
        source: BoundLocInfo,
        input: ObjType,
        slot: Int,
    ): Boolean =
        with(access) {
            if (!player.members || slot !in inv.indices || inv[slot]?.id != input.id) return false
            val index = Breweries.all.indexOfFirst { it.vat == source.coords }
            if (index < 0 || !isWithinDistance(source, 1)) return false
            update(player)
            val value = vars[BreweryBits.vat[index]]
            var packed = control(player, index)
            fun take(replacement: ObjType? = null): Boolean =
                player
                    .invTransaction(inv) {
                        val i = select(inv)
                        delete(i, input.id, 1, slot = slot)
                        if (replacement != null) add(i, replacement.id, 1, slot = slot)
                    }
                    .success
            fun save(state: Int) {
                vars[BreweryBits.vat[index]] = state
                control(player, index, packed)
            }
            when (input.id) {
                BreweryObjs.water.id -> {
                    if (value !in 0..1 || packed and 3 >= 2) return false
                    if (!take(BreweryObjs.bucket)) return false
                    packed = (packed and 252) or ((packed and 3) + 1)
                    save(1)
                }
                BreweryObjs.malt.id -> {
                    if (value != 1 || packed and 3 != 2 || (packed ushr 2) and 3 >= 2) return false
                    if (!take()) return false
                    val malt = ((packed ushr 2) and 3) + 1
                    packed = (packed and 243) or (malt shl 2)
                    save(if (malt == 2) 2 else 1)
                }
                BreweryObjs.stuff.id -> {
                    if (value != 2 || packed and 128 != 0) return false
                    if (!take()) return false
                    packed = packed or 128
                    save(2)
                }
                BreweryObjs.yeast.id -> {
                    val recipe = AleRecipes.fromVat(value) ?: return false
                    if (
                        value != recipe.preparation ||
                            packed and 15 < recipe.quantity ||
                            player.stat(stats.cooking) < recipe.level
                    )
                        return false
                    if (!take(BreweryObjs.pot)) return false
                    val stuff = packed and 128 != 0
                    val roll = random.of(128)
                    val outcome = if (roll < 16) 3 else if (roll < if (stuff) 98 else 22) 2 else 1
                    val cycles = if (stuff) random.of(3..16) else 3
                    control(player, index, outcome or (if (stuff) 4 else 0) or (cycles shl 3))
                    vars[BreweryVars.started[index]] =
                        growthCycle(player, System.currentTimeMillis())
                    vars[BreweryBits.vat[index]] = recipe.preparation + 1
                }
                BreweryObjs.mush.id -> {
                    if (value != 0 && value != 58) return false
                    if (player.stat(stats.cooking) < 14 || packed and 15 >= 4) return false
                    if (!take(BreweryObjs.bucket)) return false
                    packed = (packed and 128) or ((packed and 15) + 1)
                    save(58)
                }
                else -> {
                    val recipe =
                        AleRecipes.all.firstOrNull { it.ingredient.id == input.id && it.index != 9 }
                            ?: return false
                    if (player.stat(stats.cooking) < recipe.level) return false
                    if (value != 2 && value != recipe.preparation) return false
                    val count = if (value == 2) 0 else packed and 15
                    if (count >= recipe.quantity) return false
                    if (!take()) return false
                    packed = (packed and 128) or (count + 1)
                    save(recipe.preparation)
                }
            }
            mes("You add the ingredient to the vat.")
            true
        }

    public fun turn(access: ProtectedAccess, source: BoundLocInfo): Boolean =
        with(access) {
            if (!player.members) return false
            val index = Breweries.all.indexOfFirst { it.valve == source.coords }
            if (index < 0) return false
            update(player)
            if (vars[BreweryBits.barrel[index]] != 0) {
                mes("Empty the barrel before turning the valve.")
                return false
            }
            val value = vars[BreweryBits.vat[index]]
            if (value == 0) return false
            val recipe = AleRecipes.fromVat(value)
            val ready = recipe != null && value - recipe.preparation in 3..4
            val barrel =
                when {
                    ready ->
                        ((requireNotNull(recipe).index + 1) shl 3) or
                            7 or
                            (if (value - recipe.preparation == 4) 128 else 0)
                    value == 64 -> 1
                    value == 65 -> 2
                    value >= 68 -> return false
                    else -> 4
                }
            vars[BreweryBits.barrel[index]] = barrel
            vars[BreweryBits.vat[index]] = 0
            vars[BreweryVars.started[index]] = 0
            control(player, index, 0)
            if (ready) statAdvance(stats.cooking, requireNotNull(recipe).xp)
            mes(
                if (ready) "You drain the finished brew into the barrel."
                else "You drain the unusable contents into the barrel."
            )
            true
        }

    public fun collect(
        access: ProtectedAccess,
        source: BoundLocInfo,
        container: ObjType,
        slot: Int,
    ): Boolean =
        with(access) {
            if (!player.members || slot !in inv.indices || inv[slot]?.id != container.id)
                return false
            val index = Breweries.all.indexOfFirst { it.barrel == source.coords }
            if (index < 0) return false
            val packed = vars[BreweryBits.barrel[index]]
            val recipeIndex = ((packed and 127) ushr 3) - 1
            val recipe = AleRecipes.all.getOrNull(recipeIndex) ?: return false
            val amount = (packed and 7) + 1
            val take = if (container == BreweryObjs.keg) minOf(4, amount) else 1
            if (container != BreweryObjs.keg && container != BreweryObjs.glass) return false
            val mature = packed and 128 != 0
            val product =
                if (container == BreweryObjs.keg) {
                    if (mature) BreweryObjs.matureKegs[recipeIndex][take - 1]
                    else BreweryObjs.kegs[recipeIndex][take - 1]
                } else if (mature) recipe.mature else recipe.normal
            val result =
                player.invTransaction(inv) {
                    val i = select(inv)
                    delete(i, container.id, 1, slot = slot)
                    add(i, product.id, 1, slot = slot)
                }
            if (!result.success) return false
            val remaining = amount - take
            vars[BreweryBits.barrel[index]] =
                if (remaining == 0) 0 else (packed and 248) or (remaining - 1)
            mes("You fill the container with your brew.")
            true
        }

    public fun drain(access: ProtectedAccess, source: BoundLocInfo): Boolean =
        with(access) {
            val index = Breweries.all.indexOfFirst { it.barrel == source.coords }
            if (!player.members || index < 0 || vars[BreweryBits.barrel[index]] == 0) return false
            vars[BreweryBits.barrel[index]] = 0
            mes("You drain the barrel.")
            true
        }

    public fun buyYeast(access: ProtectedAccess, index: Int): Boolean =
        with(access) {
            if (!player.members || index !in 0..2) return false
            if (index == 1 && BreweryObjs.ghostAmulets.none { it.id == player.front?.id }) {
                mes("You need to wear a ghostspeak amulet to understand him.")
                return false
            }
            val currency = if (index == 1) BreweryObjs.tokens else objs.coins
            val cost = if (index == 1) 5 else 25
            val result =
                player.invTransaction(inv) {
                    val i = select(inv)
                    delete(i, BreweryObjs.pot.id, 1)
                    delete(i, currency.id, cost)
                    add(i, BreweryObjs.yeast.id, 1)
                }
            if (!result.success)
                mes("You need an empty pot and ${if(index==1)"five ecto-tokens"else"25 coins"}.")
            result.success
        }

    public fun hollow(access: ProtectedAccess, slot: Int): Boolean =
        with(access) {
            if (!player.members || slot !in inv.indices || inv[slot]?.id != BreweryObjs.fruit.id)
                return false
            player
                .invTransaction(inv) {
                    val i = select(inv)
                    delete(i, BreweryObjs.fruit.id, 1, slot = slot)
                    add(i, BreweryObjs.keg.id, 1, slot = slot)
                }
                .success
        }

    public suspend fun loadApple(
        access: ProtectedAccess,
        source: BoundLocInfo,
        slot: Int,
    ): Boolean =
        with(access) {
            if (
                !player.members ||
                    player.stat(stats.cooking) < 14 ||
                    slot !in inv.indices ||
                    inv[slot]?.id != BreweryObjs.apple.id
            )
                return false
            val site = Breweries.all.firstOrNull { it.press == source.coords } ?: return false
            val press = presses.getOrPut(site.press) { Press() }
            if (press.apples >= 4 || press.crushed) return false
            if (!player.invDel(inv, BreweryObjs.apple, 1, slot = slot).success) return false
            press.apples++
            mes("You add an apple to the press.")
            if (press.apples == 4) crush(this, site, source, press)
            true
        }

    private suspend fun crush(
        access: ProtectedAccess,
        site: BrewerySite,
        source: BoundLocInfo,
        press: Press,
    ): Boolean =
        with(access) {
            if (press.crushed) return true
            if (press.apples < 4 || press.busyUntil > mapClock || !isWithinDistance(source, 2))
                return false
            press.busyUntil = mapClock + 15
            anim(seqs.human_cooking)
            gatheringDelay(15)
            if (
                presses[site.press] !== press ||
                    press.apples != 4 ||
                    press.crushed ||
                    !isWithinDistance(source, 2)
            )
                return false
            press.crushed = true
            press.busyUntil = Int.MIN_VALUE
            locs.findExact(site.press, BreweryLocs.press)?.let {
                locs.change(it, BreweryLocs.mush, Int.MAX_VALUE)
            }
            resetAnim()
            mes("You crush the apples into mush.")
            true
        }

    public suspend fun drawMush(access: ProtectedAccess, source: BoundLocInfo, slot: Int): Boolean =
        with(access) {
            if (
                !player.members ||
                    player.stat(stats.cooking) < 14 ||
                    slot !in inv.indices ||
                    inv[slot]?.id != BreweryObjs.bucket.id
            )
                return false
            val site =
                Breweries.all.firstOrNull { it.press == source.coords || it.tap == source.coords }
                    ?: return false
            val press = presses[site.press] ?: return false
            if (!press.crushed && !crush(this, site, source, press)) return false
            if (inv[slot]?.id != BreweryObjs.bucket.id || !press.crushed) return false
            val result =
                player.invTransaction(inv) {
                    val i = select(inv)
                    delete(i, BreweryObjs.bucket.id, 1, slot = slot)
                    add(i, BreweryObjs.mush.id, 1, slot = slot)
                }
            if (!result.success) return false
            presses.remove(site.press)
            locs.findExact(site.press, BreweryLocs.mush)?.let {
                locs.change(it, BreweryLocs.press, Int.MAX_VALUE)
            }
            true
        }
}

internal object BreweryPeople : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<Brewery>("breweries.toml")
    }
}
