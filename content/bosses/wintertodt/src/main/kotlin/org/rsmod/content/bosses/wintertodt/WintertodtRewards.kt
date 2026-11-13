package org.rsmod.content.bosses.wintertodt

import jakarta.inject.Inject
import java.util.WeakHashMap
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjType

public class WintertodtRewards @Inject constructor(private val pets: org.rsmod.content.skills.pets.SkillPets, private val loot: WintertodtLoot) {
    // Preserve a rolled item across failed delivery; players cannot reroll a full stack by
    // searching again.
    private val pending = WeakHashMap<Player, InvObj>()

    public suspend fun search(access: ProtectedAccess, big: Boolean = false): Unit =
        with(access) {
            if (!player.members) return
            val max = if (big) 5 else 8000
            repeat(max) {
                if (player.winterRewards <= 0) {
                    mes("You have no rewards waiting in the cart.")
                    return
                }
                if (player.inv.isFull()) {
                    mes("Make some inventory space before collecting your rewards.")
                    return
                }
                delay(3)
                if (player.winterRewards <= 0 || player.inv.isFull()) return
                val obj = pending.getOrPut(player) { loot.roll(player) }
                val delivered = if (obj.id == WinterObjs.phoenix.id) {
                    pets.award(player,obj.id)
                    true
                } else player.invTransaction(player.inv) { add(select(player.inv), obj.id, obj.count) }.success
                if (!delivered) {
                    mes("Make room for the reward you have rolled.")
                    return
                }
                pending.remove(player)
                player.winterRewards--
            }
        }

    public fun openExtra(access: ProtectedAccess, slot: Int): Unit =
        with(access) {
            if (!player.members || player.inv[slot]?.id != WinterObjs.extra.id) return
            val obj = loot.roll(player, extra = true)
            val result =
                player.invTransaction(player.inv) {
                    val inv = select(player.inv)
                    delete(inv, WinterObjs.extra.id, 1, slot)
                    add(inv, obj.id, obj.count)
                }
            if (!result.success) mes("You need room for the contents of the crate.")
        }

    public suspend fun exchange(access: ProtectedAccess): Unit =
        with(access) {
            if (!player.members) return
            val whole =
                menu(
                    "What would you like to exchange?",
                    "A complete Pyromancer outfit.",
                    "One Pyromancer piece, gloves or torch.",
                    "An empty tome of fire.",
                    "Leave.",
                )
            val inputs: List<ObjType>
            val pages: Int
            val crates: Int
            when (whole) {
                1 -> {
                    inputs = WinterObjs.pyro
                    pages = 250
                    crates = 5
                }
                2 -> {
                    val item =
                        (WinterObjs.pyro +
                                WinterObjs.gloves +
                                WinterObjs.torch +
                                WinterObjs.offhand)
                            .firstOrNull { it in player.inv }
                            ?: run {
                                mes("You have no spare Wintertodt clothing or torch to exchange.")
                                return
                            }
                    inputs = listOf(item)
                    pages = 50
                    crates = 1
                }
                3 -> {
                    inputs = listOf(WinterObjs.tome)
                    pages = 250
                    crates = 1
                }
                else -> return
            }
            val output =
                if (
                    menu(
                        "What would you like in return?",
                        "Burnt pages.",
                        "Extra supply crates.",
                    ) == 1
                )
                    WinterObjs.pages
                else WinterObjs.extra
            val count = if (output == WinterObjs.pages) pages else crates
            val result =
                player.invTransaction(player.inv) {
                    val inv = select(player.inv)
                    for (item in inputs) delete(inv, item.id, 1)
                    add(inv, output.id, count)
                }
            if (!result.success)
                mes("You do not have the required items or enough inventory space.")
        }
}
