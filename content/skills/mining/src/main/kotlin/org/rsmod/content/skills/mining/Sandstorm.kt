package org.rsmod.content.skills.mining

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.*
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object QuarryGrinder : LocReferences() {
    val machine = find("grinder_machine")
}

internal object QuarryWorkers : NpcReferences() {
    val drew = find("grinder_drew")
}

internal object QuarryStock : VarpReferences() {
    val buckets = find("sandstorm_buckets")
    val sand = find("sandstorm_sand")
}

internal object QuarrySaves : VarpBuilder() {
    init {
        build("sandstorm_buckets") { permanent = true }
        build("sandstorm_sand") { permanent = true }
    }
}

internal object QuarryCamp : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<QuarryCamp>("drew.toml")
    }
}

public class Sandstorm @Inject constructor(private val objects: ObjTypeList) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(QuarryGrinder.machine) { grind() }
        for (stone in QuarryObjs.sandstone) onOpLocU(QuarryGrinder.machine, stone) { grind() }
        for (bucket in
            listOf(objs.bucket_empty, objects.cert(objects[objs.bucket_empty]))) onOpNpcU(
            QuarryWorkers.drew,
            bucket,
        ) {
            depositBuckets()
        }
        onOpNpc5(QuarryWorkers.drew) { depositBuckets() }
        onOpNpc4(QuarryWorkers.drew) { report() }
        onOpNpc3(QuarryWorkers.drew) { claim() }
        onOpNpc1(QuarryWorkers.drew) {
            when (
                menu(
                    "How can I help?",
                    false,
                    listOf("Deposit buckets", "Claim sand", "Check buckets and sand", "Leave"),
                )
            ) {
                0 -> depositBuckets()
                1 -> claim()
                2 -> report()
            }
        }
    }

    private fun ProtectedAccess.report() {
        mes(
            "Drew is holding ${vars[QuarryStock.buckets]} buckets. The grinder holds ${vars[QuarryStock.sand]} buckets' worth of sand."
        )
    }

    private fun ProtectedAccess.grind() {
        if (!player.members) return
        var added = 0
        val room = LIMIT - vars[QuarryStock.sand]
        for (slot in inv.indices) {
            val item = inv[slot] ?: continue
            val i = QuarryObjs.sandstone.indexOfFirst { it.id == item.id }
            if (i < 0) continue
            val fill = YIELD[i]
            if (fill <= room - added && invDel(inv, objects[item], 1, slot = slot).success)
                added += fill
        }
        vars[QuarryStock.sand] += added
        report()
    }

    private fun ProtectedAccess.depositBuckets() {
        if (!player.members) return
        val note = objects.cert(objects[objs.bucket_empty])
        var room = LIMIT - vars[QuarryStock.buckets]
        for (item in listOf(objs.bucket_empty, note)) {
            val n = minOf(room, invTotal(inv, item))
            if (n > 0 && invDel(inv, item, n).success) {
                room -= n
                vars[QuarryStock.buckets] += n
            }
        }
        report()
    }

    private fun ProtectedAccess.claim() {
        if (!player.members) return
        val count =
            minOf(vars[QuarryStock.buckets], vars[QuarryStock.sand], invTotal(inv, objs.coins) / 50)
        if (count <= 0) {
            mes("You need stored buckets, ground sand and 50 coins for each bucket.")
            return
        }
        val sand = objects.cert(objects[objs.bucket_of_sand])
        val tx =
            player.invTransaction(inv) {
                val bag = select(inv)
                delete(bag, objs.coins.id, count * 50)
                add(bag, sand.id, count)
            }
        if (!tx.success) {
            mes("You need room for your buckets of sand.")
            return
        }
        vars[QuarryStock.buckets] -= count
        vars[QuarryStock.sand] -= count
        mes("Drew gives you $count noted buckets of sand.")
    }

    private companion object {
        const val LIMIT = 25000
        val YIELD = intArrayOf(1, 2, 4, 8)
    }
}
