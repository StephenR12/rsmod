package org.rsmod.content.interfaces.journal.tab

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.diary.DiaryDifficulty
import org.rsmod.api.player.diary.DiaryRegion
import org.rsmod.api.player.interact.HeldInteractions
import org.rsmod.api.spells.MagicSpellModule
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.interact.HeldOp
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class DiaryEquipmentTest {
    @Test
    fun GameTestState
        .`two copies of Morytania legs share two daily teleports and a replacement does not grant a lamp`() =
        runInjectedGameTest(HeldInteractions::class, null, DiaryEquipment::class) { held ->
            player.members = true
            val region = DiaryRegion.Morytania
            val item = DiaryRewardItems.equipment.getValue(region).first()
            player.withProtectedAccess { vars[region.tier(DiaryDifficulty.Easy).reward] = 1 }
            player.withProtectedAccess { assertTrue(DiaryRewards.claim(this, region)) }
            assertEquals(1, player.count(item))
            assertEquals(0, player.count(DiaryRewardItems.ordinary.first().item))
            player.withProtectedAccess { assertFalse(DiaryRewards.claim(this, region)) }
            player.inv[1] = InvObj(item)
            repeat(2) { slot ->
                player.withProtectedAccess { held.interact(this, inv, slot, HeldOp.Op3) }
                advance(5)
                assertEquals(CoordGrid(3683, 9888), player.coords)
                player.teleport(CoordGrid(3221, 3219))
            }
            player.withProtectedAccess { held.interact(this, inv, 0, HeldOp.Op3) }
            advance(5)
            assertEquals(CoordGrid(3221, 3219), player.coords)
        }

    @Test
    fun GameTestState
        .`Falador shield caps at base Prayer and an already full bar spends no charge`() =
        runInjectedGameTest(HeldInteractions::class, null, DiaryEquipment::class) { held ->
            player.members = true
            player.stats[stats.prayer] = 80
            val region = DiaryRegion.Falador
            player.withProtectedAccess { vars[region.tier(DiaryDifficulty.Easy).reward] = 1 }
            player.inv[0] = InvObj(DiaryRewardItems.equipment.getValue(region).first())
            fun recharge() = player.withProtectedAccess { held.interact(this, inv, 0, HeldOp.Op3) }
            recharge()
            player.setCurrentLevel(stats.prayer, 73)
            recharge()
            assertEquals(80, player.statMap.getCurrentLevel(stats.prayer).toInt())
            player.setCurrentLevel(stats.prayer, 10)
            recharge()
            assertEquals(10, player.statMap.getCurrentLevel(stats.prayer).toInt())
        }

    @Test
    fun GameTestState
        .`Explorer's ring restores energy and its native alchemy inventory spends one charge without runes or XP`() =
        runInjectedGameTest(
            HeldInteractions::class,
            MagicSpellModule(),
            DiaryEquipment::class,
            ExplorersAlchemy::class,
        ) { held ->
            player.members = true
            val region = DiaryRegion.Lumbridge
            player.withProtectedAccess { vars[region.tier(DiaryDifficulty.Easy).reward] = 1 }
            player.inv[0] = InvObj(DiaryRewardItems.equipment.getValue(region).first())
            player.runEnergy = 0
            player.withProtectedAccess { held.interact(this, inv, 0, HeldOp.Op3) }
            assertEquals(constants.run_max_energy / 2, player.runEnergy)
            player.inv[1] = InvObj(objs.bronze_axe)
            player.stats[stats.magic] = 21
            val xp = player.statMap.getFineXP(stats.magic)
            player.withProtectedAccess { held.interact(this, inv, 0, HeldOp.Op4) }
            player.ifButton(cacheTypes.components.getValue((483 shl 16) or 7), 1)
            advance()
            assertEquals(0, player.count(objs.bronze_axe))
            assertEquals(objTypes[objs.bronze_axe].cost * 40 / 100, player.count(objs.coins))
            assertEquals(1, player.vars[cacheTypes.varbits.getValue(4554)])
            assertEquals(xp, player.statMap.getFineXP(stats.magic))
        }
}
