package org.rsmod.content.skills.mining.motherlode

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.invtx.*
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public object MlmPacks : ObjReferences() {
    val clay = listOf("pack_softclay", "pack_softclay_guild", "star_pack_softclay").map(::find)
    val gems = listOf("reward_gem_bag", "reward_gem_bag_guild", "star_reward_gem_bag").map(::find)
}

/** Forty noted gems; non-onyx weights are the dated sample estimates, not claimed exact odds. */
public class MotherlodePacks @Inject constructor(private val types: ObjTypeList) : PluginScript() {
    override fun ScriptContext.startup() {
        for (pack in MlmPacks.clay) onOpHeld1(pack) { openClay(this, it.slot) }
        for (pack in MlmPacks.gems) onOpHeld1(pack) { openGems(this, it.slot) }
    }

    public fun openClay(access: ProtectedAccess, slot: Int): Boolean =
        with(access) {
            val item = inv[slot] ?: return false
            if (MlmPacks.clay.none { it.id == item.id }) return false
            // Dated changes explicitly permit opening the soft-clay pack in free worlds.
            val note = types.getValue(types[objs.soft_clay].certlink)
            player
                .invTransaction(inv) {
                    val i = select(inv)
                    delete(i, item.id, 1, slot = slot)
                    add(i, note.id, 100)
                }
                .success
        }

    public fun openGems(access: ProtectedAccess, slot: Int): Boolean =
        with(access) {
            val item = inv[slot] ?: return false
            if (!player.members || MlmPacks.gems.none { it.id == item.id }) return false
            val gems =
                listOf(
                    objs.uncut_sapphire,
                    objs.uncut_emerald,
                    objs.uncut_ruby,
                    objs.uncut_diamond,
                    objs.uncut_dragonstone,
                    objs.uncut_onyx,
                )
            val notes = gems.map { types.getValue(types[it].certlink) }
            // Reserve room for all possible outputs before rolling; exact native preflight wording
            // is unverified.
            if (inv.freeSpace() + 1 < notes.count { it !in inv }) {
                mes("You need more room for the noted gems.")
                return false
            }
            val counts = IntArray(6)
            repeat(40) {
                val index =
                    if (random.of(100_000_000) == 0) 5
                    else
                        when (random.of(10_000)) {
                            in 0..4989 -> 0
                            in 4990..8448 -> 1
                            in 8449..9650 -> 2
                            in 9651..9949 -> 3
                            else -> 4
                        }
                counts[index]++
            }
            player
                .invTransaction(inv) {
                    val i = select(inv)
                    delete(i, item.id, 1, slot = slot)
                    for (index in counts.indices) if (counts[index] > 0)
                        add(i, notes[index].id, counts[index])
                }
                .success
        }
}
