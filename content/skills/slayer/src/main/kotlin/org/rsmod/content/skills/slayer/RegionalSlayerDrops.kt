package org.rsmod.content.skills.slayer

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.npc.inWildernessSlayerCave
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.params
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.type.obj.ObjType

internal object SlayerKeys : ObjReferences() {
    val brimstone = find("konar_key")
    val larran = find("slayer_wilderness_key")
    val enchantment = find("slayer_enchantment")
}

internal class RegionalSlayerDrops
@Inject
constructor(private val random: GameRandom, private val objs: ObjRepository) {
    fun roll(player: Player, npc: Npc, master: Int, superior: Boolean) {
        fun give(obj: ObjType) {
            objs.add(obj, npc.coords, constants.lootdrop_duration, player)
        }
        val level = npc.visType.vislevel.coerceIn(1, 350)
        when (master) {
            Master.Konar.id -> {
                val rate =
                    if (level < 100) 100 + (100 - level) * (100 - level) / 5 else 120 - level / 5
                if (superior || random.of(rate) == 0) give(SlayerKeys.brimstone)
            }
            Master.Krystilia.id -> {
                val rate =
                    if (level <= 80) 100 + 3 * (80 - level) * (80 - level) / 10
                    else 115 + Math.floorDiv(-5 * level, 27)
                val slayerBonus =
                    if ((npc.visType.paramOrNull(params.slayer_levelrequire) ?: 0) >= 2) 120
                    else 100
                val caveBonus = if (npc.coords.inWildernessSlayerCave()) 115 else 100
                if (
                    npc.id != 6594 &&
                        (superior || random.of(rate * 10_000) < slayerBonus * caveBonus)
                )
                    give(SlayerKeys.larran)
                val enchantRate =
                    if (npc.visType.id in bosses) 30
                    else 320 - npc.baseHitpointsLvl.coerceIn(0, 300) * 8 / 10
                if (random.of(enchantRate) == 0) give(SlayerKeys.enchantment)
            }
        }
    }

    private companion object {
        val bosses =
            setOf(2054, 6609, 6610, 6611, 6612, 6615, 6618, 6619, 11992, 11993, 11994, 11998)
    }
}
