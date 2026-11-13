package org.rsmod.content.other.special.weapons.ranged

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.isAnyType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ElvenBowServices @Inject constructor(private val types: ObjTypeList) : PluginScript() {
    override fun ScriptContext.startup() {
        for (npc in BowSingers.ilfeen) {
            onOpNpc1(npc) { offer(this, false) }
            onOpNpc3(npc) { offer(this, false) }
        }
        for (npc in BowSingers.islwyn) {
            onOpNpc1(npc) { offer(this, true) }
            onOpNpc3(npc) { offer(this, true) }
        }
    }

    private suspend fun offer(access: ProtectedAccess, new: Boolean) =
        with(access) {
            if (!eligible(this)) return
            val cost = price(player, new)
            val accepted =
                choice2(
                    "Yes, please.",
                    true,
                    "No, thanks.",
                    false,
                    title =
                        if (new) "Buy a new crystal bow for $cost coins?"
                        else "Enchant a seed or recharge an inactive bow for $cost coins?",
                )
            if (accepted) buy(this, new)
        }

    fun buy(access: ProtectedAccess, new: Boolean): Boolean =
        with(access) {
            if (!eligible(this)) return false
            val cost = price(player, new)
            val slot =
                inv.indexOfFirst { it.isAnyType(CrystalBowItems.seed, CrystalBowItems.inactive) }
            if (!new && slot < 0) {
                mes("Bring a crystal weapon seed or an inactive crystal bow.")
                return false
            }
            if (inv.count(types[objs.coins]) < cost) {
                mes("You need $cost coins.")
                return false
            }
            val material = if (new) null else inv[slot]
            val result =
                player.invTransaction(inv) {
                    val stock = select(inv)
                    if (material != null) delete(stock, material.id, 1, slot)
                    delete(stock, objs.coins.id, cost)
                    if (new) add(stock, CrystalBowItems.newBow.id, 1)
                    else add(stock, objs.crystal_bow.id, 1, vars = 2500)
                }
            if (!result.success) {
                mes("You need room to receive the crystal bow.")
                return false
            }
            if (!new) player.sungSeeds = (player.sungSeeds + 1).coerceAtMost(4)
            mes("You receive a crystal bow.")
            true
        }

    private fun eligible(access: ProtectedAccess): Boolean =
        with(access) {
            if (player.members && player.vars[BowQuest.roving] >= 6) return true
            mes("You must complete Roving Elves before obtaining a crystal bow.")
            false
        }

    private fun price(player: Player, new: Boolean): Int =
        if (new) 900000 else 900000 - player.sungSeeds.coerceIn(0, 4) * 180000
}

internal object BowQuest : VarpReferences() {
    val roving = find("roving_elves_quest")
}

private var Player.sungSeeds by intVarBit(CrystalBowBits.chants)

internal object BowSingers : NpcReferences() {
    val ilfeen =
        listOf("roving_ilfeen_1op", "roving_ilfeen_2ops", "sote_ilfeen_vis", "ilfeen_prif").map {
            find(it)
        }
    val islwyn = listOf("roving_islwyn_1op", "roving_islwyn_2ops", "islwyn_prif").map { find(it) }
}

internal object BowSingerSpawns : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<ElvenBowServices>("crystal-bow-singers.toml")
    }
}
