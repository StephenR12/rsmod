package org.rsmod.content.travel.diaryroutes

import jakarta.inject.Inject
import java.util.WeakHashMap
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.*
import org.rsmod.api.npc.events.NpcKillEvents
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.opPlayer2
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.*
import org.rsmod.api.shops.Shops
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.editors.inv.InvEditor
import org.rsmod.api.type.refs.inv.InvReferences
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.content.skills.mining.GemRockRewards
import org.rsmod.content.skills.mining.scripts.Mining
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.inv.InvScope
import org.rsmod.game.type.inv.InvStackType
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object JungleLocs : LocReferences() {
    val growth =
        listOf("easy", "med", "hard").map { density ->
            (1..5).map { find("tai_bwo_jungle_vegetation_${density}_$it") }
        }
    val fence =
        listOf("rotten", "fixed1", "fixed2", "fixed3", "fixedcomplete").map {
            find("village_fence_$it")
        }
    val gem = (1..3).map { find("village_gem_rock$it") }
    val tuber = find("village_gout_tuber")
}

internal object JungleSeqs : SeqReferences() {
    val hack = listOf("", "_opal", "_jade", "_redtopaz").map { find("tbw_cleanup_player_hack$it") }
    val repair =
        listOf("", "_opal", "_jade", "_redtopaz").map {
            find("tbw_cleanup_player_fixfence_start$it")
        }
    val skewer =
        listOf("", "_opal", "_jade", "_redtopaz").map { find("tbw_cleanup_player_prepare_wood$it") }
    val success = find("tbw_cleanup_player_hack_success")
    val miss = find("tbw_cleanup_player_hack_fail")
    val dig = find("human_dig")
}

internal object JungleItems : ObjReferences() {
    val machetes =
        listOf("machette", "machette_opal", "machette_jade", "machette_redtopaz").map { find(it) }
    val spars = listOf("light", "med", "dense").map { find("thatching_spar_$it") }
    val gemstones = listOf("opal", "jade", "red_topaz").map { find(it) to find("uncut_$it") }
    val tuber = find("village_rare_tuber")
    val skewer = find("tbw_skewer_stick")
    val sticks = find("village_trade_sticks")
    val gloves = listOf("atjun_gloves_hard", "atjun_gloves_elite").map { find(it) }

    data class Good(val item: ObjType, val count: Int, val restock: Int)

    private fun good(name: String, count: Int, restock: Int = 500) =
        Good(find(name), count, restock)

    val stock =
        listOf(
            good("tinderbox", 2),
            good("unlit_candle", 10),
            good("torch_unlit", 10),
            good("pot_empty", 3),
            good("rope", 10, 200),
            good("leather_armour", 12),
            good("leather_gloves", 10),
            good("leather_boots", 10),
            good("cooked_meat", 2),
            good("bread", 10),
            good("vial_empty", 10),
            good("pack_vial_empty", 100, 50),
            good("vial_water", 50, 200),
            good("pack_vial_water", 50, 50),
            good("pestle_and_mortar", 3),
            good("3doseantipoison", 10, 150),
            good("papyrus", 50, 200),
            good("charcoal", 50, 200),
            good("knife", 2, 100),
            good("hammer", 10),
            good("machette", 50, 200),
            good("chisel", 10),
            good("spade", 10),
            good("bronze_axe", 2),
            good("bronze_pickaxe", 2),
            good("iron_axe", 5, 700),
            good("bronze_bar", 10),
            good("blankrune_high", 0, 1000),
        )
}

internal object CleanupNpcs : NpcReferences() {
    // Chat bits select no NPC (0), a villager holding sticks (1), or an empty-handed villager (2+).
    val villagers =
        listOf(
                "sharimika",
                "mamma_bufetta",
                "layleen",
                "karaday",
                "safta_doc",
                "gabooty",
                "fanellaman",
                "jagbakoba",
                "murcaily",
                "rionasta",
            )
            .map { name -> listOf("_multinpc", "", "_nosticks").map { find("tbwcu_$name$it") } }
    val enemies =
        listOf(
                "jungle_spider",
                "tribesman",
                "village_snake",
                "mosquito_single",
                "mosquito_swarm3",
                "mosquito_swarm5",
                "broodoo_combat",
                "broodoo_poison",
                "broodoo_disease",
            )
            .map { find("tbwcu_$it") }
    val spider = find("jungle_spider")
    val jiminua = find("jiminua")
}

internal object CleanupBits : VarBitReferences() {
    val favour = find("vill_comm_points")
    val best = find("vill_comm_max")
    val visible = find("villagers_visible")
    val gabootyShop = find("gabootyshopchat")
    val stage = find("cleanup_progress")
    val chat =
        listOf(
                "sharimika",
                "mamma",
                "layleen",
                "karaday",
                "safta",
                "gabooty",
                "fanellaman",
                "jag",
                "murc",
                "rion",
            )
            .map { find("chat_$it") }
}

internal object CleanupVars : VarpReferences() {
    val potion = find("junglepotion")
    val lastMinute = find("tai_bwo_last_villager_minute")
}

internal object CleanupSaves : VarpBuilder() {
    init {
        build("tai_bwo_last_villager_minute") { transmitNever = true }
    }
}

internal object JungleStoreInv : InvReferences() {
    val store = find("junglestore")
}

internal object JungleStoreStock : InvEditor() {
    init {
        edit(JungleStoreInv.store) {
            scope = InvScope.Shared
            stack = InvStackType.Always
            restock = true
            for (g in JungleItems.stock) stock +=
                stock(g.item, count = g.count, restockCycles = g.restock)
        }
    }
}

public class TaiBwoCleanup
@Inject
constructor(
    private val locs: LocRepository,
    private val npcs: NpcRepository,
    private val npcTypes: NpcTypeList,
    private val objects: ObjTypeList,
    private val rng: GameRandom,
    private val ai: AiPlayerInteractions,
    private val shops: Shops,
) : PluginScript() {
    private val eventChance = WeakHashMap<Player, Int>()

    private data class Find(val owner: Player, val expires: Int)

    private val tubers = mutableMapOf<CoordGrid, Find>()

    override fun ScriptContext.startup() {
        onPlayerLogin { refreshVillagers(player) }
        onPlayerLogout {
            eventChance.remove(player)
            tubers.values.removeIf { it.owner === player }
        }
        onOpNpc1(CleanupNpcs.jiminua) {
            if (choice2("Trade with Jiminua.", true, "Leave.", false)) shop()
        }
        onOpNpc3(CleanupNpcs.jiminua) { shop() }
        for ((index, variants) in CleanupNpcs.villagers.withIndex()) for (v in variants) onOpNpc1(
            v
        ) {
            talk(index)
        }
        for ((density, stages) in JungleLocs.growth.withIndex()) for (stage in 0..3) {
            onOpLoc4(stages[stage]) { hack(it.loc, density, stage) }
            for (tool in JungleItems.machetes) onOpLocU(stages[stage], tool) {
                hack(it.loc, density, stage)
            }
        }
        for ((stage, fence) in JungleLocs.fence.withIndex()) {
            onOpLoc1(fence) { repair(it.loc, stage, null) }
            for ((density, spar) in JungleItems.spars.withIndex()) onOpLocU(fence, spar) {
                repair(it.loc, stage, density)
            }
        }
        for ((tool, machete) in JungleItems.machetes.withIndex()) for (spar in
            JungleItems.spars) onOpHeldU(machete, spar) {
            if (!player.members) return@onOpHeldU
            val n = rng.of(3, 6)
            if (
                player
                    .invTransaction(inv) {
                        val bag = select(inv)
                        delete(bag, spar.id, 1)
                        add(bag, JungleItems.skewer.id, n)
                    }
                    .success
            ) {
                anim(JungleSeqs.skewer[tool])
                mes("You carve the thatch spar into $n skewer sticks.")
            } else mes("You need more room to make the skewer sticks.")
        }
        onOpLocU(JungleLocs.tuber, objs.spade) {
            if (!ready() || locs.findExact(it.loc.coords, JungleLocs.tuber) == null) return@onOpLocU
            val owner = tubers[it.loc.coords]
            if (owner != null && mapClock < owner.expires && owner.owner !== player) {
                mes("It's not yours to dig.")
                return@onOpLocU
            }
            if (invAdd(inv, JungleItems.tuber).success) {
                anim(JungleSeqs.dig)
                tubers.remove(it.loc.coords)
                locs.change(it.loc, JungleLocs.growth[0].last(), 150)
                mes("You dig up the gout tuber.")
            }
        }
        for ((i, rock) in JungleLocs.gem.withIndex()) onOpLoc1(rock) {
            if (!ready()) return@onOpLoc1
            if (stat(stats.mining) < 40 || Mining.findPickaxe(player, objects) == null) {
                mes("You need level 40 Mining and a usable pickaxe to mine the rock.")
                return@onOpLoc1
            }
            delay(3)
            if (locs.findExact(it.loc.coords, rock) == null) return@onOpLoc1
            val reward = GemRockRewards.at(rng.of(GemRockRewards.totalWeight))
            if (invAdd(inv, reward.obj).success) {
                statAdvance(stats.mining, 65.0)
                locs.change(
                    it.loc,
                    JungleLocs.gem.getOrNull(i + 1) ?: JungleLocs.growth[0].last(),
                    150,
                )
            }
        }
        onEvent<NpcKillEvents.Credited> {
            if (
                !player.members ||
                    player.vars[CleanupVars.potion] < 12 ||
                    player.vars[CleanupBits.stage] < 1 ||
                    npc.coords.level != 0 ||
                    npc.coords.x !in 2730..2845 ||
                    npc.coords.z !in 3020..3150
            )
                return@onEvent
            val kind = CleanupNpcs.enemies.indexOfFirst { it.id == npc.id }
            val reward =
                when {
                    npc.id == CleanupNpcs.spider.id &&
                        player.vars[CleanupBits.stage] in listOf(3, 5) -> rng.of(10, 20)
                    kind in 0..1 -> rng.of(10, 20)
                    kind in 2..5 -> 10
                    kind in 6..8 -> 30
                    else -> return@onEvent
                }
            favour(player, reward)
        }
    }

    private fun ProtectedAccess.shop() {
        val discounted = JungleItems.gloves.any { it in worn }
        shops.open(
            player,
            "Jiminua's Jungle Store.",
            JungleStoreInv.store,
            if (discounted) 55.0 else 50.0,
            if (discounted) 130.0 else 150.0,
            2.0,
        )
    }

    private fun refreshVillagers(p: Player) {
        if (!p.members || p.vars[CleanupVars.potion] < 12) return
        val now = (System.currentTimeMillis() / 60_000L).toInt()
        val last = p.vars[CleanupVars.lastMinute]
        val elapsed = if (last > 0) (now - last).coerceIn(0, 7) else 7
        for (bit in CleanupBits.chat) {
            val n = p.vars[bit]
            if (n != 1 && (n == 0 || elapsed > 0))
                VarPlayerIntMapSetter.set(p, bit, (n - elapsed).coerceAtLeast(1))
        }
        VarPlayerIntMapSetter.set(p, CleanupBits.visible, 1)
        VarPlayerIntMapSetter.set(p, CleanupVars.lastMinute, now)
    }

    private suspend fun ProtectedAccess.talk(index: Int) {
        if (!player.members || vars[CleanupVars.potion] < 12) {
            mes("You must complete Jungle Potion before you can help the village.")
            return
        }
        refreshVillagers(player)
        if (index == 8 && vars[CleanupBits.stage] == 0) {
            vars[CleanupBits.stage] = 1
            mes(
                "Murcaily asks you to cut back the jungle with a machete and repair the village fence with the thatch."
            )
            return
        }
        val opts = mutableListOf("Can I have trading sticks for my work?")
        if (index == 8) opts += "Where can I get a machete?"
        if (index == 4 || index == 5) opts += "What do you do here?"
        opts += "Leave."
        when (menu("Tai Bwo Wannai", true, opts)) {
            0 -> claim(index)
            1 ->
                if (index == 8) {
                    if (vars[CleanupBits.stage] >= 2) {
                        // Stage 4 records a referral before the first payout; stage 5 records both.
                        vars[CleanupBits.stage] =
                            if (vars[CleanupBits.stage] == 2 || vars[CleanupBits.stage] == 4) 4
                            else 5
                        mes(
                            "Safta Doc can fit a gem blade to a machete. You'll find him by the anvil."
                        )
                    } else
                        mes(
                            "Jiminua sells machetes northwest of the village. Repair part of the fence and come back."
                        )
                } else if (index == 4) orderMachete()
                else if (index == 5) {
                    vars[CleanupBits.gabootyShop] = 1
                    mes("Gabooty trades village goods and drinks for trading sticks.")
                }
        }
    }

    private fun ProtectedAccess.claim(index: Int) {
        val stage = vars[CleanupBits.stage]
        if (stage < 2 || index != 8 && stage != 3 && stage != 5) {
            mes("Repair some of the fence and claim your first trading sticks from Murcaily.")
            return
        }
        if (vars[CleanupBits.chat[index]] != 1) {
            mes("I've no more sticks to spare just now.")
            return
        }
        val base = vars[CleanupBits.favour].coerceAtMost(rng.of(30, 100))
        if (base <= 0) {
            mes("Help out around the village first.")
            return
        }
        val dressed =
            worn
                .filterNotNull { objects[it].internalName.orEmpty().startsWith("tbw_villager_") }
                .size
        val amount = base + base * dressed / 10
        if (!invAdd(inv, JungleItems.sticks, amount).success) return
        vars[CleanupBits.favour] -= base
        vars[CleanupBits.chat[index]] = 7
        if (index == 8 && stage == 2) vars[CleanupBits.stage] = 3
        if (index == 8 && stage == 4) vars[CleanupBits.stage] = 5
        mes("The villager gives you $amount trading sticks.")
    }

    private suspend fun ProtectedAccess.orderMachete() {
        if (vars[CleanupBits.stage] < 4) {
            mes("Ask Murcaily about machetes after repairing part of the fence.")
            return
        }
        val tier =
            menu(
                "Choose a gem blade",
                true,
                listOf(
                    "Opal (300 sticks).",
                    "Jade (600 sticks).",
                    "Red topaz (1,200 sticks).",
                    "Leave.",
                ),
            )
        val gems = JungleItems.gemstones.getOrNull(tier) ?: return
        val cut = inv.count(objects[gems.first]).coerceAtMost(3)
        val price = listOf(300, 600, 1200)[tier]
        if (
            !player
                .invTransaction(inv) {
                    val bag = select(inv)
                    if (cut > 0) delete(bag, gems.first.id, cut)
                    if (cut < 3) delete(bag, gems.second.id, 3 - cut)
                    delete(bag, JungleItems.sticks.id, price)
                    delete(bag, JungleItems.tuber.id, 1)
                    add(bag, JungleItems.machetes[tier + 1].id, 1)
                }
                .success
        ) {
            mes("Bring me three matching gems, a gout tuber and $price trading sticks.")
            return
        }
        player.completeDiaryTask(DiaryTask.ExchangeMacheteGems)
        mes("Safta Doc hands you the gem-bladed machete.")
    }

    private fun ProtectedAccess.ready(): Boolean {
        if (!player.members || vars[CleanupVars.potion] < 12 || vars[CleanupBits.stage] < 1) {
            mes("Speak to Murcaily before helping with the cleanup.")
            return false
        }
        return true
    }

    private fun ProtectedAccess.tool(): Int {
        val idx = JungleItems.machetes.indexOfLast { it in inv || it in worn }
        if (idx < 0) mes("You need a machete.")
        return idx
    }

    private suspend fun ProtectedAccess.hack(loc: BoundLocInfo, density: Int, initial: Int) {
        if (!ready()) return
        val req = listOf(10, 20, 35)[density]
        if (stat(stats.woodcutting) < req) {
            mes("You need a Woodcutting level of $req to cut this jungle.")
            return
        }
        val tool = tool()
        if (tool < 0) return
        val low =
            listOf(listOf(49, 54, 70, 85), listOf(34, 39, 54, 70), listOf(16, 21, 36, 52))[density][
                tool]
        val high = low + listOf(120, 51, 28)[density]
        val plants = JungleLocs.growth[density]
        var stage = initial
        while (stage < 4) {
            if (inv.isFull()) {
                mes("You need space for the thatch spar.")
                break
            }
            val interval = 17 - (stat(stats.woodcutting) / 10).coerceIn(1, 9)
            anim(JungleSeqs.hack[tool])
            delay(1)
            repeat(interval - 5) {
                delay(1)
                val current = locs.findExact(loc.coords, plants[stage]) ?: return
                if (rng.of(101) < (eventChance[player] ?: 0)) {
                    eventChance.remove(player)
                    event(current, player)
                    resetAnim()
                    return
                }
            }
            val current = locs.findExact(loc.coords, plants[stage]) ?: return
            eventChance[player] = ((eventChance[player] ?: 0) + 1).coerceAtMost(7)
            if (statRandom(stats.woodcutting, low, high, 0)) {
                if (!invAdd(inv, JungleItems.spars[density]).success) break
                statAdvance(stats.woodcutting, listOf(32.0, 55.0, 80.0)[density])
                favour(player, listOf(3, 5, 10)[density])
                locs.change(current, plants[++stage], 150)
                anim(JungleSeqs.success)
            } else anim(JungleSeqs.miss)
            delay(4)
        }
        resetAnim()
    }

    private fun ProtectedAccess.event(loc: org.rsmod.game.loc.LocInfo, p: Player) {
        when (val roll = rng.of(100)) {
            99 -> {
                tubers.entries.removeIf { it.value.expires <= mapClock }
                tubers[loc.coords] = Find(p, mapClock + 1000)
                locs.change(loc, JungleLocs.tuber, 1000)
                mes("You hack away at the remaining bush to reveal an unusual plant.")
            }
            in 85..89 -> {
                locs.change(loc, JungleLocs.gem.first(), 1000)
                mes("You uncover some gem rocks.")
            }
            else -> {
                val index =
                    when (roll) {
                        in 0..21 -> 0
                        in 22..42 -> 1
                        in 43..63 -> 2
                        in 64..71 -> 3
                        in 72..78 -> 4
                        in 79..84 -> 5
                        in 90..93 -> 6
                        in 94..96 -> 7
                        else -> 8
                    }
                val n = Npc(npcTypes[CleanupNpcs.enemies[index]], p.coords)
                npcs.add(n, 300)
                n.opPlayer2(p, ai)
            }
        }
    }

    private suspend fun ProtectedAccess.repair(loc: BoundLocInfo, stage: Int, chosen: Int?) {
        if (!ready()) return
        val tool = tool()
        if (tool < 0) return
        val density = chosen ?: JungleItems.spars.indexOfLast { it in inv }
        if (density < 0) {
            mes("You need some thatching spars.")
            return
        }
        anim(JungleSeqs.repair[tool])
        delay(3)
        val current = locs.findExact(loc.coords, JungleLocs.fence[stage]) ?: return
        if (!invDel(inv, JungleItems.spars[density]).success) return
        locs.change(current, JungleLocs.fence[(stage + density + 1).coerceAtMost(4)], 200)
        statAdvance(stats.woodcutting, listOf(28.1, 42.1, 65.1)[density])
        favour(player, listOf(1, 3, 5)[density])
        if (vars[CleanupBits.stage] == 1) vars[CleanupBits.stage] = 2
        mes("You reinforce the village fence with the thatch.")
    }

    private fun favour(player: Player, amount: Int) {
        val value = (player.vars[CleanupBits.favour] + amount).coerceAtMost(1000)
        VarPlayerIntMapSetter.set(player, CleanupBits.favour, value)
        if (value > player.vars[CleanupBits.best])
            VarPlayerIntMapSetter.set(player, CleanupBits.best, value)
        if (value == 1000) player.completeDiaryTask(DiaryTask.EarnCleanupFavour)
    }
}
