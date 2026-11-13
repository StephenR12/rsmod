package org.rsmod.content.skills.runecraft

import jakarta.inject.Inject
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.*
import org.rsmod.api.player.lefthand
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.type.obj.ObjType

internal class RiftImbuing @Inject constructor(private val modifiers: RunecraftOutputModifiers) {
    fun standard(a: ProtectedAccess, temple: RuneTemple): Unit =
        with(a) {
            val amount = invTotal(inv, RiftItems.essence)
            val cell = cellToCharge(this, temple)
            if (amount == 0 && cell == null) {
                mes("You have no guardian essence or uncharged cells.")
                return
            }
            val recipe = temple.recipe
            val runes = amount * recipe.outputMultiplier(stat(stats.runecrafting))
            val bonus = modifiers.raimentsBonus(a, runes) + lanternRunes(a, temple, runes)
            val stone = RiftItems.stones[if (temple.elemental) 0 else 1]
            val talisman = rollTalisman(a, temple, amount)
            val result =
                player.invTransaction(inv) {
                    val bag = select(inv)
                    if (amount > 0) {
                        delete(bag, RiftItems.essence.id, amount)
                        add(bag, recipe.output.id, runes + bonus)
                        add(bag, stone.id, amount)
                    }
                    if (cell != null) {
                        delete(bag, cell.id, 1)
                        add(bag, RiftItems.cells[temple.cellTier].id, 1)
                    }
                }
            if (!result.success) {
                mes("You need more inventory space to imbue your essence.")
                return
            }
            if (talisman != null) invAdd(inv, talisman)
            statAdvance(stats.runecrafting, recipe.xp * amount)
            anim(RunecraftSeqs.craft)
            spotanim(RunecraftSpotanims.craft)
        }

    fun cellToCharge(a: ProtectedAccess, temple: RuneTemple): ObjType? {
        val held = RiftItems.cells.indexOfLast { it in a.inv }
        return RiftItems.cells.getOrNull(held)?.takeIf { held < temple.cellTier }
    }

    fun rollTalisman(a: ProtectedAccess, temple: RuneTemple, amount: Int): ObjType? {
        val talisman = RiftItems.talismans.getValue(temple)
        if (a.vars[RiftBits.tallies.getValue(temple)] != 0 || talisman in a.inv) return null
        val chance =
            when (a.player.lefthand?.id) {
                in 26824..26834 -> 225
                26840 -> 200
                else -> 250
            }
        return if ((0 until amount).any { a.random.of(chance) == 0 }) talisman else null
    }

    fun lanternRunes(a: ProtectedAccess, temple: RuneTemple, amount: Int): Int {
        val pct =
            when (a.player.lefthand?.id) {
                26838,
                26848 -> 5
                26846 -> 10
                26844 -> if (temple == RuneTemple.Blood) 20 else 0
                else -> 0
            }
        return amount * pct / 100 + if (pct > 0 && a.random.of(100) < amount * pct % 100) 1 else 0
    }
}
