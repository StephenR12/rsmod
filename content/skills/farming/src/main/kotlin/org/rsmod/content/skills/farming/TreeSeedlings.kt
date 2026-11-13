package org.rsmod.content.skills.farming

import jakarta.inject.Inject
import org.rsmod.api.config.refs.invs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object SeedlingVarps : VarpReferences() {
    val readyAt = find("farming_seedlings_ready_at")
}

internal object SeedlingVarpBuilder : VarpBuilder() {
    init {
        build("farming_seedlings_ready_at") { transmitNever = true }
    }
}

internal object SeedlingObjs : ObjReferences() {
    val trowel = find("gardening_trowel")
    val filledPot = find("plantpot_compost")

    data class Tree(val seed: ObjType, val dry: ObjType, val wet: ObjType, val sapling: ObjType)

    private fun tree(seed: String, seedling: String, sapling: String): Tree =
        Tree(
            find(seed),
            find("plantpot_$seedling"),
            find("plantpot_${seedling}_watered"),
            find("plantpot_${sapling}_sapling"),
        )

    val trees =
        listOf(
            tree("acorn", "acorn", "oak"),
            tree("willow_seed", "willow_seed", "willow"),
            tree("maple_seed", "maple_seed", "maple"),
            tree("yew_seed", "yew_seed", "yew"),
            tree("magic_tree_seed", "magic_tree_seed", "magic_tree"),
            tree("spirit_tree_seed", "spirit_tree_seed", "spirit_tree"),
            tree("apple_tree_seed", "apple_seed", "apple"),
            tree("banana_tree_seed", "banana_seed", "banana"),
            tree("orange_tree_seed", "orange_seed", "orange"),
            tree("curry_tree_seed", "curry_seed", "curry"),
            tree("pineapple_tree_seed", "pineapple_seed", "pineapple"),
            tree("papaya_tree_seed", "papaya_seed", "papaya"),
            tree("palm_tree_seed", "palm_seed", "palm"),
            tree("calquat_tree_seed", "calquat_seed", "calquat"),
            tree("teak_seed", "teak_seed", "teak"),
            tree("mahogany_seed", "mahogany_seed", "mahogany"),
            tree("dragonfruit_tree_seed", "dragonfruit_seed", "dragonfruit"),
            tree("celastrus_tree_seed", "celastrus_tree_seed", "celastrus_tree"),
            tree("redwood_tree_seed", "redwood_tree_seed", "redwood_tree"),
            tree("crystal_tree_seed", "crystal_tree_seed", "crystal_tree"),
        )
}

public class TreeSeedlings
@Inject
constructor(
    private val crops: FarmingCropCatalogue,
    private val patches: FarmingPatchRepository,
    private val objTypes: ObjTypeList,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (tree in SeedlingObjs.trees) {
            onOpHeldU(tree.seed, SeedlingObjs.filledPot) { event ->
                val crop = requireNotNull(crops.forSeed(tree.sapling.id))
                if (stat(stats.farming) < crop.level) {
                    mes("You need a Farming level of ${crop.level} to plant this seed.")
                    return@onOpHeldU
                }
                if (SeedlingObjs.trowel !in inv) {
                    mes("You need a gardening trowel to plant a seed in this pot.")
                    return@onOpHeldU
                }
                val result =
                    player.invTransaction(inv) {
                        val bag = select(inv)
                        delete(bag, tree.seed.id, 1, event.firstSlot)
                        delete(bag, SeedlingObjs.filledPot.id, 1, event.secondSlot)
                        add(bag, tree.dry.id, 1, slot = event.secondSlot)
                    }
                if (result.success) mes("You plant the seed in the plant pot.")
            }
            for (can in
                FarmingObjs.wateringCans.drop(1) + org.rsmod.api.config.refs.objs.gricollers_can) {
                onOpHeldU(can, tree.dry) { event ->
                    if (!hasWater(event.firstSlot)) {
                        mes("Your watering can is empty.")
                        return@onOpHeldU
                    }
                    val now = System.currentTimeMillis()
                    mature(player, now)
                    if (invReplaceSlot(inv, event.secondSlot, 1, tree.wet).failure) return@onOpHeldU
                    check(useWater(event.firstSlot))
                    val next = patches.firstGrowthTick(player, 5, now)
                    VarPlayerIntMapSetter.set(
                        player,
                        SeedlingVarps.readyAt,
                        ((next + 999) / 1000L).toInt(),
                    )
                    mes("You water the seedling.")
                }
            }
        }
    }

    public fun mature(player: Player, now: Long = System.currentTimeMillis()) {
        val readyAt = player.vars[SeedlingVarps.readyAt]
        if (readyAt == 0 || now / 1000L < readyAt) return
        val saplings = SeedlingObjs.trees.associate { it.wet.id to objTypes[it.sapling] }
        for ((slot, obj) in player.inv.objs.withIndex()) {
            val grown = saplings[obj?.id] ?: continue
            player.inv[slot] = InvObj(grown)
        }
        var pending = false
        val bank = player.invMap[invs.bank]
        if (bank != null) {
            for ((slot, obj) in bank.objs.withIndex()) {
                if (obj == null) continue
                val grown = saplings[obj.id] ?: continue
                val dest =
                    bank.indexOfFirst { it?.id == grown.id || it?.id == grown.placeholderlink }
                if (dest < 0) {
                    bank[slot] = InvObj(grown, obj.count)
                } else {
                    val existing = bank.getValue(dest)
                    val combined = existing.count.toLong() + obj.count
                    if (combined > Int.MAX_VALUE) {
                        pending = true
                        continue
                    }
                    bank[dest] = InvObj(grown, combined.toInt())
                    // A placeholder keeps the original tab boundary intact.
                    bank[slot] = InvObj(objTypes.placeholder(objTypes[obj]), 0)
                }
            }
        }
        if (!pending) VarPlayerIntMapSetter.set(player, SeedlingVarps.readyAt, 0)
    }
}
