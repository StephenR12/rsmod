package org.rsmod.content.skills.hunter.experts

import jakarta.inject.Inject
import org.rsmod.api.config.refs.invs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varps
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.*
import org.rsmod.api.utils.skills.SkillingSuccessRate
import org.rsmod.content.skills.pets.SkillPets
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal data class SackDrop(val id: Int, val min: Int = 1, val max: Int = min)

public class RumourSacks
@Inject
constructor(
    private val items: ObjTypeList,
    private val objects: ObjRepository,
    private val pets: SkillPets,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for ((tier, id) in listOf(29242, 29244, 29246, 29248, 30644).withIndex()) {
            onOpHeld1(items.getValue(id)) {
                if (!player.members || invDel(inv, items.getValue(id), slot = it.slot).failure)
                    return@onOpHeld1
                val rolls = listOf(5, 7, 9, 11, 11)[tier]
                val table =
                    when (tier) {
                        0 -> basic
                        1 -> adept
                        else -> expert
                    }
                val loot = linkedMapOf<Int, Int>()
                repeat(rolls) {
                    val drop = random.pick(table)
                    val item =
                        when (drop.id) {
                            -1 -> cascade(logs, statBase(stats.woodcutting), 6334)
                            -2 -> cascade(herbs, statBase(stats.herblore), 206)
                            else -> drop.id
                        }
                    loot[item] = loot.getOrDefault(item, 0) + random.of(drop.min..drop.max)
                }
                for ((item, count) in loot) invAddOrDrop(objects, items.getValue(item), count)
                if (tier in 1..3) uniques(tier)
                milestoneBlueprints()
                mes("You open the hunters' loot sack.")
            }
        }
        for (id in listOf(29253, 29256, 29309, 29263, 29265, 29267, 29269)) {
            onOpNpcU(GuildNpcs.verity, items.getValue(id)) {
                if (
                    !choice2("Trade this item for a hunters' supply sack.", true, "Keep it.", false)
                )
                    return@onOpNpcU
                if (id == 29309 && player.invMap[HuntsmanInv.items]?.any { it != null } == true) {
                    mes("Empty your huntsman's kit before exchanging it.")
                    return@onOpNpcU
                }
                val tx =
                    player.invTransaction(inv) {
                        val source = select(inv)
                        delete(source, id, 1, slot = it.invSlot)
                        add(source, 30644, 1)
                    }
                if (tx.success) mes("Verity gives you a hunters' supply sack.")
            }
        }
    }

    private fun ProtectedAccess.cascade(
        table: List<Triple<Int, Int, Int>>,
        level: Int,
        fallback: Int,
    ): Int =
        table
            .firstOrNull { (_, low, high) ->
                random.randomDouble() < SkillingSuccessRate.successRate(low, high, level, 99)
            }
            ?.first ?: fallback

    private fun ProtectedAccess.milestoneBlueprints() {
        var bits = vars[GuildVars.rewards]
        val count = vars[varps.hunter_rumours_completed]
        if (count >= 100 && bits and 5 == 0) {
            invAddOrDrop(objects, items.getValue(29259))
            bits = bits or 4
        }
        if (count >= 250 && bits and 10 == 0) {
            invAddOrDrop(objects, items.getValue(29261))
            bits = bits or 8
        }
        vars[GuildVars.rewards] = bits
    }

    private fun ProtectedAccess.uniques(tier: Int) {
        var bits = vars[GuildVars.rewards]
        fun owned(id: Int): Boolean {
            val type = items.getValue(id)
            return type in inv || type in worn || player.invMap[invs.bank]?.contains(type) == true
        }
        fun give(id: Int) {
            invAddOrDrop(objects, items.getValue(id))
        }
        val enhancedReceived = bits and 5 != 0
        if (random.randomBoolean(50)) {
            give(29253)
            bits = bits or 1
        }
        if (enhancedReceived && tier >= 2 && random.randomBoolean(50)) {
            give(29256)
            bits = bits or 2
        }
        if (!owned(29309) && random.randomBoolean(50)) give(29309)
        if (random.randomBoolean(50)) {
            val index = (bits ushr 4) and 3
            give(29263 + 2 * index)
            bits = (bits and (3 shl 4).inv()) or (((index + 1) and 3) shl 4)
        }
        vars[GuildVars.rewards] = bits
        if (
            tier >= 2 &&
                random.randomBoolean(
                    if (player.statMap.getXP(stats.hunter) >= 200000000) 67 else 1000
                )
        )
            pets.award(player, 28962)
    }

    private companion object {
        val logs =
            listOf(
                Triple(1514, -60, 60),
                Triple(1516, -50, 90),
                Triple(8836, -40, 130),
                Triple(1518, 0, 160),
            )
        val herbs =
            listOf(
                Triple(2486, -30, 60),
                Triple(216, -10, 70),
                Triple(214, 10, 85),
                Triple(212, 20, 100),
                Triple(210, 30, 115),
                Triple(208, 10, 170),
                Triple(204, 70, -20),
            )
        val basic =
            listOf(
                SackDrop(29307),
                SackDrop(995, 750, 1250),
                SackDrop(29311, 15, 30),
                SackDrop(29381, 100, 200),
                SackDrop(29126, 2),
                SackDrop(29111, 3),
                SackDrop(-1, 4),
            )
        val adept =
            basic +
                listOf(
                    SackDrop(20783),
                    SackDrop(20783, 2, 4),
                    SackDrop(29379, 2),
                    SackDrop(29117, 2),
                    SackDrop(-2, 4),
                )
        val expert =
            adept + listOf(SackDrop(29379, 3), SackDrop(29114, 2), SackDrop(995, 2500, 3500))
    }
}
