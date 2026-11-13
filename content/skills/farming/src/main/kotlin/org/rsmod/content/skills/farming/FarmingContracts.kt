package org.rsmod.content.skills.farming

import jakarta.inject.Inject
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpcU
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object GuildmasterNpcs : NpcReferences() {
    val jane =
        listOf("farming_guild_master", "farming_guild_master_1op", "farming_guild_master_2op").map {
            find(it)
        }
}

internal object ContractItems : ObjReferences() {
    val pack = find("seedbox")
    val spirit =
        listOf(
                "spirit_tree_seed",
                "plantpot_spirit_tree_seed",
                "plantpot_spirit_tree_seed_watered",
                "plantpot_spirit_tree_sapling",
            )
            .map { find(it) }
}

internal object ContractBits : VarBitReferences() {
    val discussed = find("farmguild_contract_discussed")
}

internal object ContractVars : VarpReferences() {
    val assignment = find("farming_guild_contract")
    val previous = find("farming_guild_last_contract")
    val completed = find("farming_guild_contracts_completed")
}

internal object ContractVarBuilder : VarpBuilder() {
    init {
        for (name in
            listOf(
                "farming_guild_contract",
                "farming_guild_last_contract",
                "farming_guild_contracts_completed",
            )) build(name) { transmitNever = true }
    }
}

internal fun ProtectedAccess.finishContract(crop: FarmingCrop, at: CoordGrid) {
    if (at.level != 0 || at.x !in 1216..1263 || at.z !in 3719..3760) return
    val id = vars[ContractVars.assignment]
    if (id <= 0 || farmingContracts.getOrNull(id - 1)?.crop != crop.key) return
    vars[ContractVars.assignment] = -id
    mes("Your Farming contract is complete. Return to Guildmaster Jane for your seed pack.")
}

public class FarmingContracts @Inject constructor(private val crops: FarmingCropCatalogue) :
    PluginScript() {
    override fun ScriptContext.startup() {
        for (npc in GuildmasterNpcs.jane) {
            onOpNpc1(npc) { speak() }
            if (npc != GuildmasterNpcs.jane[1]) onOpNpc3(npc) { speak() }
            for (item in ContractItems.spirit) onOpNpcU(npc, item) {
                if (!player.members || inv[it.invSlot]?.id != item.id) return@onOpNpcU
                if (choice2("Exchange this for a tier five seed pack?", true, "Keep it.", false))
                    invReplaceSlot(inv, it.invSlot, 1, ContractItems.pack, seedPack(5))
            }
        }
    }

    private suspend fun ProtectedAccess.speak() {
        if (!player.members) return
        vars[ContractBits.discussed] = 1
        val id = vars[ContractVars.assignment]
        if (id < 0) {
            val done = farmingContracts.getOrNull(-id - 1) ?: return
            if (invAdd(inv, ContractItems.pack, vars = seedPack(done.tier)).failure) {
                mes("Make room for a seed pack.")
                return
            }
            vars[ContractVars.previous] = -id
            vars[ContractVars.assignment] = 0
            vars[ContractVars.completed]++
            mes("Jane gives you a seed pack for growing ${crops[done.crop]!!.displayName}.")
            return
        }
        val active = farmingContracts.getOrNull(id - 1)
        if (active != null) {
            mes(
                "Your contract is to grow ${crops[active.crop]!!.displayName} in the Farming Guild."
            )
            if (
                active.difficulty > 1 &&
                    choice2("Do you have anything easier?", true, "I'll keep this contract.", false)
            )
                assign(active.difficulty - 1, id)
            return
        }
        val i =
            menu(
                "Farming contracts — ${vars[ContractVars.completed]} completed",
                true,
                listOf("Easy (45 Farming)", "Medium (65 Farming)", "Hard (85 Farming)"),
            )
        if (i in 0..2) assign(i + 1, vars[ContractVars.previous])
    }

    private fun ProtectedAccess.assign(difficulty: Int, previous: Int) {
        val level = stat(stats.farming)
        val minimum = 25 + 20 * difficulty
        if (level < minimum) {
            mes("You need $minimum Farming to take that contract.")
            return
        }
        val oldCrop = farmingContracts.getOrNull(previous - 1)?.crop
        val options =
            farmingContracts.withIndex().filter { (_, contract) ->
                contract.difficulty == difficulty &&
                    contract.level <= level &&
                    contract.crop != oldCrop
            }
        val chosen = options[random.of(options.size)]
        vars[ContractVars.assignment] = chosen.index + 1
        mes("Jane asks you to grow ${crops[chosen.value.crop]!!.displayName} in the Farming Guild.")
    }
}
