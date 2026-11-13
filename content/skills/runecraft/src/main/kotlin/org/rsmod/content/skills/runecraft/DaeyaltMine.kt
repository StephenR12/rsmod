package org.rsmod.content.skills.runecraft

import jakarta.inject.Inject
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.feet
import org.rsmod.api.player.legs
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.torso
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.controller.ControllerRepository
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.*
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.api.type.builders.controller.ControllerBuilder
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.content.interfaces.bank.deliverToBank
import org.rsmod.content.other.gembag.GemBagStorage
import org.rsmod.content.skills.mining.MiningGemPreTable
import org.rsmod.content.skills.mining.scripts.Mining
import org.rsmod.content.skills.mining.scripts.Mining.Companion.pickaxeAnim
import org.rsmod.content.skills.mining.scripts.Mining.Companion.pickaxeFastIntervalChance
import org.rsmod.content.skills.mining.scripts.Mining.Companion.pickaxeInterval
import org.rsmod.game.entity.Controller
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object DaeyaltLocs : LocReferences() {
    val entrance = find("darkm_mine_entrance")
    val exit = find("darkm_mine_exit")
    val rock = find("daeyalt_stone_top_active")
    val inactive = find("daeyalt_stone_top")
}

internal object DaeyaltNpcs : NpcReferences() {
    val newVisitor = find("dmine_daeyalt_converter_1op")
    val introduced = find("dmine_daeyalt_converter_2op")
}

internal object DaeyaltItems : ObjReferences() {
    val shard = find("daeyalt_shard")
    val shoes = find("vyrelord_shoes")
    val shirts = (0..12).map { find(if (it == 0) "vyrelord_torso" else "vyrelord_torso_$it") }
    val legs = (0..12).map { find(if (it == 0) "vyrelord_legs" else "vyrelord_legs_$it") }
}

internal object DaeyaltBits : VarBitReferences() {
    val introduction = find("dmine_daeyalt_converter_intro")
    val ironman = find("ironman")
}

internal object DaeyaltRotation : ControllerBuilder() {
    val type = build("daeyalt_mine_rotation")
}

internal object EssenceMineSpawns : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<DaeyaltMine>("essence-miners.toml")
    }
}

public class DaeyaltMine
@Inject
constructor(
    private val locs: LocRepository,
    private val controllers: ControllerRepository,
    private val random: GameRandom,
    private val types: ObjTypeList,
    private val xp: XpModifiers,
    private val gems: GemBagStorage,
    private val crystal: org.rsmod.content.skills.mining.CrystalPickaxeLifecycle,
) : PluginScript() {
    private val deposits =
        listOf(CoordGrid(3671, 9750, 2), CoordGrid(3674, 9765, 2), CoordGrid(3687, 9755, 2))
    private var started = false

    override fun ScriptContext.startup() {
        onGameStartup { activate() }
        onAiConTimer(DaeyaltRotation.type) {
            val active = deposits.firstNotNullOfOrNull { locs.findExact(it, DaeyaltLocs.rock) }
            val others = deposits.mapNotNull { locs.findExact(it, DaeyaltLocs.inactive) }
            if (active != null && others.isNotEmpty()) {
                locs.change(active, DaeyaltLocs.inactive, Int.MAX_VALUE)
                locs.change(random.pick(others), DaeyaltLocs.rock, Int.MAX_VALUE)
            }
            controller.aiTimer(random.of(92..110))
        }
        onOpLoc1(DaeyaltLocs.entrance) {
            if (canEnter()) {
                activate()
                telejump(CoordGrid(3696, 9764, 2))
            }
        }
        onOpLoc1(DaeyaltLocs.exit) { telejump(CoordGrid(3634, 3340)) }
        onOpLoc1(DaeyaltLocs.rock) { mine(it.loc, true) }
        onOpLoc3(DaeyaltLocs.rock) { mine(it.loc, false) }
        onOpNpc1(DaeyaltNpcs.newVisitor) { introduce() }
        onOpNpc1(DaeyaltNpcs.introduced) { exchange() }
        onOpNpc3(DaeyaltNpcs.introduced) { exchange() }
        onOpNpcU(DaeyaltNpcs.introduced, DaeyaltItems.shard) { exchange() }
    }

    private fun activate() {
        if (started) return
        val controller = Controller(DaeyaltRotation.type, deposits.first())
        controllers.add(controller, Int.MAX_VALUE)
        controller.aiTimer(random.of(92..110))
        started = true
    }

    private fun ProtectedAccess.canEnter(): Boolean {
        // TODO(quests): Sins of the Father supplies its native completion state.
        if (player.members && vars[RunecraftVarbits.sinsOfTheFather] >= 138) return true
        mes("You must complete Sins of the Father to enter the daeyalt essence mine.")
        return false
    }

    private fun ProtectedAccess.mine(rock: BoundLocInfo, start: Boolean) {
        if (
            !canEnter() ||
                rock.coords !in deposits ||
                locs.findExact(rock.coords, DaeyaltLocs.rock) == null
        )
            return
        if (stat(stats.mining) < 60) {
            mes("You need level 60 Mining to mine daeyalt essence.")
            return
        }
        val pick = Mining.findPickaxe(player, types)
        if (pick == null) {
            mes("You need a pickaxe you can use.")
            return
        }
        val tool = types[pick]
        if (start || skillAnimDelay <= mapClock) {
            anim(tool.pickaxeAnim)
            skillAnimDelay = mapClock + 4
        }
        if (start || actionDelay <= mapClock) {
            if (!start) {
                if (
                    random.of(Mining.gemPreRollDenominator(player)) < MiningGemPreTable.TOTAL_WEIGHT
                ) {
                    val gem = MiningGemPreTable.at(random.of(MiningGemPreTable.TOTAL_WEIGHT)).obj
                    if (
                        gem != null &&
                            gems.storeAcquired(player, gem) == 0 &&
                            !invAdd(inv, gem).success
                    ) {
                        mes("Your inventory is full.")
                        resetAnim()
                        return
                    }
                } else {
                    if (!invAdd(inv, DaeyaltItems.shard, random.of(2..3)).success) {
                        mes("You don't have room for the daeyalt shards.")
                        resetAnim()
                        return
                    }
                    crystal.consumeCharge(this, pick)
                    statAdvance(stats.mining, 5.0 * xp.get(player, stats.mining))
                }
            }
            val fast = tool.pickaxeFastIntervalChance
            actionDelay =
                mapClock + tool.pickaxeInterval -
                    if (fast > 0 && random.randomBoolean(fast)) 1 else 0
        }
        opLoc3(rock)
    }

    private fun ProtectedAccess.disguised(): Boolean {
        if (!canEnter()) return false
        if (
            player.feet?.id == DaeyaltItems.shoes.id &&
                DaeyaltItems.shirts.any { it.id == player.torso?.id } &&
                DaeyaltItems.legs.any { it.id == player.legs?.id }
        )
            return true
        mes("Noranna refuses to deal with you. You need your full vyre noble outfit.")
        return false
    }

    private fun ProtectedAccess.introduce() {
        if (!disguised()) return
        vars[DaeyaltBits.introduction] = 1
        mes("Noranna offers to convert shards from the active deposit into daeyalt essence.")
    }

    private suspend fun ProtectedAccess.exchange() {
        if (!disguised() || vars[DaeyaltBits.introduction] == 0) return
        val available = invTotal(inv, DaeyaltItems.shard)
        if (available == 0) {
            mes("You don't have any daeyalt shards.")
            return
        }
        val requested =
            if (available == 1) 1
            else countDialog("How many shards would you like to convert?").coerceAtMost(available)
        if (requested <= 0 || !disguised()) return
        val useInventory = vars[DaeyaltBits.ironman] == 2
        val delivered =
            if (useInventory) {
                player
                    .invTransaction(inv) {
                        val bag = select(inv)
                        delete(bag, DaeyaltItems.shard.id, requested)
                        add(bag, RunecraftObjs.daeyaltEssence.id, requested)
                    }
                    .success
            } else {
                player.deliverToBank(
                    bank,
                    RunecraftObjs.daeyaltEssence,
                    requested,
                    DaeyaltItems.shard,
                    requested,
                )
            }
        if (!delivered) mes("There is not enough room for the essence.")
        else if (useInventory) mes("Noranna hands you $requested daeyalt essence.")
        else mes("Noranna sends $requested daeyalt essence to your bank.")
    }
}
