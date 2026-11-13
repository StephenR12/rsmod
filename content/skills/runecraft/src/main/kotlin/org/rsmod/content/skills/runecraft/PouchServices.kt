package org.rsmod.content.skills.runecraft

import jakarta.inject.Inject
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object PouchNpcs : NpcReferences() {
    val mage = find("rcu_zammy_mage2")
    val cordelia = find("gotr_cordelia_1op")
    val cordeliaRepair = find("gotr_cordelia_2ops")
}

internal object PouchItems : ObjReferences() {
    val pearls = find("abyssal_pearl")
    val needle = find("abyssal_needle")
    val book = find("rcu_instruction_book")
}

internal object PouchBits : VarBitReferences() {
    val unlocked = find("gotr_cordelia_repair_pouch")
    val templeQuest = find("tote")
}

internal object PouchSpawns : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<PouchServices>("pouch-npcs.toml")
    }
}

public class PouchServices @Inject constructor(private val storage: EssencePouchStorage) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc3(PouchNpcs.mage) { freeRepair() }
        onOpNpc1(PouchNpcs.mage) {
            if (!player.members) return@onOpNpc1
            when (
                menu(
                    "Dark Mage",
                    true,
                    listOf(
                        "Repair my pouches.",
                        "Replace my small pouch.",
                        "Replace my abyssal book.",
                        "Leave.",
                    ),
                )
            ) {
                0 -> freeRepair()
                1 -> {
                    val owned =
                        listOf(
                            RunecraftObjs.smallPouch,
                            RunecraftObjs.colossalPouch,
                            RunecraftObjs.colossalPouchDegraded,
                        )
                    if (owned.none { item -> item in inv || item in bank })
                        invAdd(inv, RunecraftObjs.smallPouch)
                    else mes("You already have a pouch of that kind.")
                }
                2 ->
                    if (PouchItems.book !in inv && PouchItems.book !in bank)
                        invAdd(inv, PouchItems.book)
            }
        }
        onOpNpc1(PouchNpcs.cordelia) {
            if (!player.members || vars[PouchBits.templeQuest] <= 125) return@onOpNpc1
            if (
                menu(
                    "Pouch repairs",
                    true,
                    listOf("Unlock repairs for 25 abyssal pearls.", "Cancel."),
                ) == 0 && vars[PouchBits.unlocked] == 0
            ) {
                if (invDel(inv, PouchItems.pearls, count = 25).success) {
                    vars[PouchBits.unlocked] = 1
                    mes("Cordelia can now repair your pouches for one abyssal pearl.")
                } else mes("You need 25 abyssal pearls to unlock this service.")
            }
        }
        onOpNpc1(PouchNpcs.cordeliaRepair) { paidRepair() }
        onOpNpc3(PouchNpcs.cordeliaRepair) { paidRepair() }
        for (pouch in smaller) {
            onOpHeldU(PouchItems.needle, pouch.normal) { stitch(it.firstSlot) }
        }
    }

    private fun ProtectedAccess.freeRepair() {
        if (!player.members) return
        val repaired = storage.repairAll(this)
        mes(
            if (repaired > 0) "The Dark Mage repairs your pouches."
            else "Your pouches do not need repairing."
        )
    }

    private fun ProtectedAccess.paidRepair() {
        if (!player.members || vars[PouchBits.unlocked] == 0 || vars[PouchBits.templeQuest] <= 125)
            return
        if (!storage.needsRepair(this)) {
            mes("Your pouches do not need repairing.")
            return
        }
        if (invDel(inv, PouchItems.pearls).success) {
            storage.repairAll(this)
            mes("Cordelia repairs your pouches.")
        } else mes("You need one abyssal pearl for a repair.")
    }

    private suspend fun ProtectedAccess.stitch(needleSlot: Int) {
        if (!player.members || statBase(stats.runecrafting) < 25 || statBase(stats.crafting) < 56) {
            mes("You need 25 Runecraft and 56 Crafting to stitch the pouches together.")
            return
        }
        delay(3)
        if (inv[needleSlot]?.id != PouchItems.needle.id || smaller.any { it.normal !in inv }) {
            mes("You need an undamaged pouch of each size.")
            return
        }
        val colossal = EssencePouchDefinitions.colossal
        if (colossal.variants.any { it in bank || it in inv }) return
        val result =
            player.invTransaction(inv) {
                val bag = select(inv)
                delete(bag, PouchItems.needle.id, 1, slot = needleSlot)
                for (pouch in smaller) delete(bag, pouch.normal.id, 1)
                add(bag, colossal.normal.id, 1, slot = needleSlot)
                for (pouch in smaller) {
                    val essence = storage.essenceType(player, pouch) ?: continue
                    val count = storage.count(player, pouch)
                    if (count > 0) add(bag, essence.obj.id, count)
                }
            }
        if (!result.success) {
            mes("Make room for the essence in your pouches.")
            return
        }
        smaller.forEach { storage.clear(player, it) }
        storage.clear(player, colossal)
        statAdvance(stats.crafting, 1000.0)
        mes("You stitch the four pouches together into a colossal pouch.")
    }

    private val smaller =
        listOf(
            EssencePouchDefinitions.small,
            EssencePouchDefinitions.medium,
            EssencePouchDefinitions.large,
            EssencePouchDefinitions.giant,
        )
}
