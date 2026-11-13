package org.rsmod.content.skills.mining.camdozaal

import jakarta.inject.Inject
import org.rsmod.api.clue.ClueVessels
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.cookingLvl
import org.rsmod.api.player.stat.fishingLvl
import org.rsmod.api.script.*
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.skills.pets.SkillPetRates
import org.rsmod.content.skills.prayer.remains.zealotPreservesOffering
import org.rsmod.game.entity.Npc
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private enum class CaveFish(
    val raw: Int,
    val level: Int,
    val fishXp: Int,
    val cookXp: Int,
    val prayXp: Int,
    val noBurn: Int,
    val petRate: Int,
) {
    Guppy(25652, 7, 8, 12, 4, 42, 820330),
    Cavefish(25658, 20, 16, 23, 7, 53, 300792),
    Tetra(25664, 33, 24, 31, 10, 61, 257770),
    Catfish(25670, 46, 33, 43, 16, 78, 152120),
}

private data class NetCast(val spot: Npc, val big: Boolean)

/** The two net operations share the ruins' rare component and use different fish pools. */
public class CamdozaalFishing
@Inject
constructor(
    private val npcs: NpcTypeList,
    private val locs: LocTypeList,
    private val items: ObjTypeList,
    private val pets: SkillPetRates,
    private val clues: ClueVessels,
    private val xp: XpModifiers,
) : PluginScript() {
    override fun ScriptContext.startup() {
        val spot = npcs.getValue(10686)
        onOpNpc1(spot) { cast(NetCast(it.npc, false), false) }
        onOpNpc3(spot) { cast(NetCast(it.npc, true), false) }
        onOpNpcU(spot, objs.small_fishing_net) { cast(NetCast(it.npc, false), false) }
        onOpNpcU(spot, objs.big_fishing_net) { cast(NetCast(it.npc, true), false) }
        onPlayerQueueWithArgs<NetCast>(CamQueues.fish) { cast(it.args, true) }
        for (prepare in listOf(true, false)) {
            val loc = locs.getValue(if (prepare) 41545 else 41546)
            onOpLoc1(loc) { selectFish(it.loc, prepare) }
            for (fish in CaveFish.entries) onOpLocU(
                loc,
                items.getValue(if (prepare) fish.raw else fish.raw + 2),
            ) {
                process(it.loc, fish, prepare, 1)
            }
        }
    }

    private fun ProtectedAccess.cast(req: NetCast, roll: Boolean) {
        val level = player.fishingLvl
        if (
            !Camdozaal.allowed(player) || !req.spot.isSlotAssigned || !isWithinDistance(req.spot, 1)
        )
            return
        val tool = if (req.big) objs.big_fishing_net else objs.small_fishing_net
        if (tool !in inv || level < (if (req.big) 33 else 7)) {
            mes("You need the correct net and Fishing level.")
            resetAnim()
            return
        }
        if (inv.isFull()) {
            mes("Your inventory is too full to hold another catch.")
            resetAnim()
            return
        }
        clearWeakQueue(CamQueues.fish)
        anim(if (req.big) seqs.human_bignet else seqs.human_smallnet)
        val boost = Camdozaal.boost(player, 0)
        if (roll && statRandom(stats.fishing, 65 * boost / 100, 185 * boost / 100, 0)) {
            val pool =
                CaveFish.entries.filter {
                    level >= it.level &&
                        (if (req.big) it.ordinal >= 2 else it.ordinal <= 2) &&
                        (it != CaveFish.Catfish || player.members)
                }
            val fish = pool[random.of(pool.size)]
            val rollId = random.of(100000)
            val handle = rollId < 100 * Camdozaal.boost(player, 4) / 100
            val shards = !handle && rollId < 15000
            val id = if (handle) 25637 else if (shards) 25676 else fish.raw
            val count = if (shards) 3 else 1
            val slot = inv.indexOfFirst { it == null }
            if (invAdd(inv, items.getValue(id), count).success) {
                val xpBase = if (handle) 33 else if (shards) 8 else fish.fishXp
                statAdvance(stats.fishing, xpBase * xp.get(player, stats.fishing))
                if (!handle && !shards && player.members)
                    pets.roll(player, stats.fishing, 13320, fish.petRate, slot)
                clues.bottles(player, level, fish.petRate)
            }
        }
        weakQueue(CamQueues.fish, 5, req)
    }

    private suspend fun ProtectedAccess.selectFish(loc: BoundLocInfo, prepare: Boolean) {
        val options =
            CaveFish.entries.filter { items.getValue(it.raw + (if (prepare) 0 else 2)) in inv }
        if (options.isEmpty()) {
            mes(
                if (prepare) "You need some raw fish to prepare."
                else "You need some prepared fish to offer."
            )
            return
        }
        if (options.size == 1) {
            process(loc, options.first(), prepare, 28)
            return
        }
        val buttons =
            listOf(
                components.skillmulti_a,
                components.skillmulti_b,
                components.skillmulti_c,
                components.skillmulti_d,
            )
        val items = options.map { items.getValue(it.raw + 2) }
        val selection =
            skillMulti(
                if (prepare) "Which fish would you like to prepare?"
                else "Which fish would you like to offer?",
                items,
                28,
                28,
                0,
            )
        val fish =
            options.getOrNull(buttons.indexOfFirst { it.isType(selection.component) }) ?: return
        process(loc, fish, prepare, selection.subcomponent.coerceIn(1, 28))
    }

    private suspend fun ProtectedAccess.process(
        loc: BoundLocInfo,
        fish: CaveFish,
        prepare: Boolean,
        qty: Int,
    ) {
        val input = items.getValue(fish.raw + if (prepare) 0 else 2)
        repeat(qty) {
            if (
                !Camdozaal.allowed(player) ||
                    fish == CaveFish.Catfish && !player.members ||
                    !isWithinDistance(loc, 1)
            )
                return
            val slot = inv.indexOfFirst { it?.id == input.id }
            if (slot < 0) return
            if (prepare) {
                if (objs.knife !in inv || player.cookingLvl < fish.level) {
                    mes("You need a knife and level ${fish.level} Cooking.")
                    return
                }
                val boost = Camdozaal.boost(player, 1)
                val chance =
                    (100 - (fish.noBurn - player.cookingLvl).coerceAtLeast(0)) * boost / 100
                val ok = chance >= 100 || random.of(100) < chance
                if (
                    !invReplaceSlot(inv, slot, 1, items.getValue(fish.raw + if (ok) 2 else 4))
                        .success
                )
                    return
                if (ok) statAdvance(stats.cooking, fish.cookXp * xp.get(player, stats.cooking))
                mes(if (ok) "You prepare the fish for the altar." else "You ruin the fish.")
            } else {
                if (zealotPreservesOffering())
                    mes("Your Zealot's robes save the fish but still grant you experience.")
                else if (!invDel(inv, input, 1, slot).success) return
                statAdvance(stats.prayer, fish.prayXp * xp.get(player, stats.prayer))
            }
            delay(if (prepare) 4 else 3)
        }
    }
}
