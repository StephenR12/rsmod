package org.rsmod.content.skills.construction

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.type.builders.inv.InvBuilder
import org.rsmod.api.type.refs.inv.InvReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.content.skills.construction.CrystalSaw.Companion.charges
import org.rsmod.game.inv.Inventory
import org.rsmod.game.type.inv.InvScope
import org.rsmod.game.type.inv.InvStackType
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.varbit.VarBitType
import org.rsmod.game.type.varbit.VarBitTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object StashObjs : ObjReferences() {
    val saw = find("poh_saw")
    val saws =
        listOf(saw, find("eyeglo_crystal_saw"), find("wearable_saw"), find("wearable_saw_offhand"))
    val goldLeaf = find("gold_leaf")
}

internal object StashInvs : InvReferences() {
    val storage = find("clue_stash_storage")
}

private const val OUTFIT_SIZE = 6

internal object StashInvBuilder : InvBuilder() {
    init {
        build("clue_stash_storage") {
            scope = InvScope.Perm
            stack = InvStackType.Never
            size = 6 * 32 * OUTFIT_SIZE
        }
    }
}

public class StashUnits
@Inject
constructor(
    private val locTypes: LocTypeList,
    private val objTypes: ObjTypeList,
    private val varbits: VarBitTypeList,
) : PluginScript() {
    override fun ScriptContext.startup() {
        val objNames = objTypes.values.associateBy { it.internalName }
        val outfits =
            checkNotNull(javaClass.getResourceAsStream("/stash-items.tsv"))
                .bufferedReader()
                .useLines { lines ->
                    lines
                        .map { row ->
                            val (name, outfit) = row.split('\t')
                            val groups =
                                outfit.split(' ').map { alternatives ->
                                    alternatives
                                        .split('/')
                                        .map { checkNotNull(objNames[it]).id }
                                        .toSet()
                                }
                            require(groups.size <= OUTFIT_SIZE)
                            name to groups
                        }
                        .toList()
                        .groupBy({ it.first }, { it.second })
                }
        for (base in locTypes.values) {
            val choices = outfits[base.internalName] ?: continue
            val tier =
                Tier.entries.first { base.internalName!!.startsWith("hh_" + it.name.lowercase()) }
            val constructed = checkNotNull(varbits[base.multiVarBit])
            onOpLoc1(base) {
                if (player.members && player.vars[constructed] == 0) buildUnit(tier, constructed)
            }
            onOpLoc2(base) {
                if (player.members && player.vars[constructed] != 0) {
                    searchUnit((tier.ordinal * 32 + constructed.lsb) * OUTFIT_SIZE, choices)
                }
            }
        }
    }

    private suspend fun ProtectedAccess.buildUnit(tier: Tier, constructed: VarBitType) {
        val boost = if (SawItems.saw in inv && charges() > 0) 3 else 0
        if (stat(stats.construction) + boost < tier.level) {
            mes("You need a Construction level of ${tier.level} to build this STASH unit.")
            return
        }
        val hammers = listOf(objs.hammer, objs.imcando_hammer, objs.imcando_hammer_offhand)
        if (
            hammers.none { it in inv || it in worn } ||
                StashObjs.saws.none { it in inv || it in worn }
        ) {
            mes("You need a hammer and a saw to build this STASH unit.")
            return
        }
        val nail = nails.firstOrNull { invTotal(inv, it) >= 10 }
        if (
            nail == null ||
                invTotal(inv, tier.plank) + ContractRewards.sackCount(this, tier.plank) < 2 ||
                (tier == Tier.Master && StashObjs.goldLeaf !in inv)
        ) {
            val extra = if (tier == Tier.Master) " and a gold leaf" else ""
            mes(
                "You need two ${objTypes[tier.plank].name.lowercase()}s, ten nails of the same metal$extra."
            )
            return
        }
        anim(seqs.human_hammer_hit)
        delay(3)
        if (player.vars[constructed] != 0) return
        val loose = minOf(2, invTotal(inv, tier.plank))
        val fromSack = 2 - loose
        if (fromSack > ContractRewards.sackCount(this, tier.plank)) return
        val result =
            player.invTransaction(inv) {
                val bag = select(inv)
                if (loose > 0) delete(bag, tier.plank.id, loose)
                delete(bag, nail.id, 10)
                if (tier == Tier.Master) delete(bag, StashObjs.goldLeaf.id, 1)
            }
        if (result.success) {
            if (fromSack > 0) ContractRewards.adjustSack(this, tier.plank, -fromSack)
            VarPlayerIntMapSetter.set(player, constructed, 1)
            statAdvance(stats.construction, tier.xp * ContractRewards.outfitModifier(this))
            mes("You build a STASH unit.")
        }
    }

    private fun ProtectedAccess.searchUnit(first: Int, choices: List<List<Set<Int>>>) {
        val storage = inv(StashInvs.storage)
        val occupied = (first until first + OUTFIT_SIZE).filter { storage[it] != null }
        if (occupied.isNotEmpty()) {
            val result =
                player.invTransaction(storage, inv) {
                    val from = select(storage)
                    val into = select(inv)
                    for (slot in occupied) {
                        val obj = storage.getValue(slot)
                        delete(from, obj.id, obj.count, slot)
                        add(into, obj.id, obj.count, vars = obj.vars)
                    }
                }
            mes(
                if (result.success) "You take the items from the STASH unit."
                else "You need more inventory space."
            )
            return
        }

        val slots = choices.firstNotNullOfOrNull { chooseSlots(inv, it) }
        if (slots == null) {
            val names = choices.first().map { group -> objTypes[group.first()]!!.name }
            mes("To fill this STASH unit you need: " + names.joinToString() + ".")
            return
        }
        val result =
            player.invTransaction(inv, storage) {
                val from = select(inv)
                val into = select(storage)
                for ((offset, slot) in slots.withIndex()) {
                    val obj = inv.getValue(slot)
                    delete(from, obj.id, 1, slot)
                    add(into, obj.id, 1, vars = obj.vars, slot = first + offset)
                }
            }
        if (result.success) mes("You store the items in the STASH unit.")
    }

    private fun chooseSlots(inventory: Inventory, groups: List<Set<Int>>): List<Int>? {
        val slots = mutableListOf<Int>()
        for (group in groups) {
            val slot =
                inventory.objs.indices.firstOrNull { it !in slots && inventory[it]?.id in group }
                    ?: return null
            slots.add(slot)
        }
        return slots
    }

    private enum class Tier(val level: Int, val xp: Double, val plank: ObjType) {
        Beginner(12, 150.0, objs.plank),
        Easy(27, 150.0, objs.plank),
        Medium(42, 250.0, objs.oak_plank),
        Hard(55, 400.0, objs.teak_plank),
        Elite(77, 600.0, objs.mahogany_plank),
        Master(88, 1500.0, objs.mahogany_plank),
    }

    private val nails =
        listOf(
            objs.bronze_nails,
            objs.iron_nails,
            objs.steel_nails,
            objs.black_nails,
            objs.mithril_nails,
            objs.adamant_nails,
            objs.rune_nails,
        )
}
