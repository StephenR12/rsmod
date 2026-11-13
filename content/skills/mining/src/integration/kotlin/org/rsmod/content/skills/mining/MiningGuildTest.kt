package org.rsmod.content.skills.mining

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.diary.DiaryDifficulty
import org.rsmod.api.player.diary.DiaryRegion
import org.rsmod.api.player.hands
import org.rsmod.api.player.righthand
import org.rsmod.api.player.worn.HeldEquipResult
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.mining.configs.MiningLocs
import org.rsmod.content.skills.mining.configs.MiningObjs
import org.rsmod.content.skills.mining.scripts.Mining
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey

@Execution(ExecutionMode.SAME_THREAD)
class MiningGuildTest {
    @Test
    fun GameTestState.guildBoostAndRespawnExcludeFreeAreaAndAmethystUsesSeventyFiveSeconds() =
        game { d ->
            player.members = true
            val c = CoordGrid(3029, 9721)
            assertEquals(7, d.guild.invisibleBoost(player, c))
            assertEquals(0, d.guild.invisibleBoost(player, CoordGrid(3034, 9738)))
            assertEquals(0, d.guild.invisibleBoost(player, c.translateLevel(1)))
            assertEquals(4, d.guild.respawn(c, objs.iron_ore, 9))
            assertEquals(9, d.guild.respawn(CoordGrid(3034, 9738), objs.iron_ore, 9))
            assertEquals(125, d.guild.respawn(c, objs.amethyst, 125))
            player.members = false
            assertEquals(0, d.guild.invisibleBoost(player, c))
        }

    @Test
    fun GameTestState.threeGloveTiersMatchPostRebalanceOreGatherCounts() = game { d ->
        player.members = true
        for ((index, glove) in
            listOf(MiningGuildObjs.basic, MiningGuildObjs.superior, MiningGuildObjs.expert)
                .withIndex()) {
            player.hands = InvObj(glove)
            assertEquals(index + 1, d.guild.extraGathers(player, objs.iron_ore))
            assertEquals(index, d.guild.extraGathers(player, objs.mithril_ore))
            assertEquals(if (index == 2) 1 else 0, d.guild.extraGathers(player, objs.runite_ore))
            assertEquals(if (index == 2) 1 else 0, d.guild.extraGathers(player, objs.amethyst))
            assertEquals(
                if (index == 2) 1 else 0,
                d.guild.extraGathers(player, QuarryObjs.sandstone[0]),
            )
            assertEquals(0, d.guild.extraGathers(player, QuarryObjs.granite[0]))
        }
        player.hands = null
        assertEquals(0, d.guild.extraGathers(player, objs.iron_ore))
    }

    @Test
    fun GameTestState.basicGlovesRetainIronExactlyOnceAndExpertRetainsAmethystBeyondItsBaseLife() =
        game { d ->
            player.members = true
            player.hands = InvObj(MiningGuildObjs.basic)
            val iron = placeMapLoc(CoordGrid(3028, 9720), MiningLocs.iron1)
            assertTrue(d.guild.retain(player, iron, objs.iron_ore))
            assertFalse(d.guild.retain(player, iron, objs.iron_ore))
            assertTrue(d.guild.retain(player, iron, objs.iron_ore))
            val wall =
                placeMapLoc(
                    CoordGrid(3017, 9701),
                    MiningGuildLocs.amethyst[0],
                    LocShape.WallStraight,
                    LocAngle.East,
                )
            player.hands = InvObj(MiningGuildObjs.expert)
            random.next = 2
            assertTrue(d.guild.retain(player, wall, objs.amethyst))
            assertTrue(d.guild.retain(player, wall, objs.amethyst))
            assertFalse(d.guild.retain(player, wall, objs.amethyst))
        }

    @Test
    fun GameTestState.nativeOuterDoorAllowsBoostedSixtyAndInnerDoorRequiresMembership() =
        game { d ->
            player.members = false
            player.stats[stats.mining] = 59
            val door =
                placeMapLoc(
                    CoordGrid(3046, 9756),
                    MiningGuildLocs.outer,
                    LocShape.WallStraight,
                    LocAngle.North,
                )
            player.telejump(CoordGrid(3046, 9757))
            player.opLoc1(door)
            advance()
            assertEquals(CoordGrid(3046, 9757), player.coords)
            player.setCurrentLevel(stats.mining, 60)
            player.opLoc1(door)
            advance()
            assertEquals(CoordGrid(3046, 9755), player.coords)
            val inner =
                placeMapLoc(
                    CoordGrid(3019, 9732),
                    MiningGuildLocs.inner[0],
                    LocShape.WallStraight,
                    LocAngle.North,
                )
            player.telejump(CoordGrid(3019, 9733))
            player.opLoc1(inner)
            advance()
            assertEquals(CoordGrid(3019, 9733), player.coords)
            player.members = true
            player.opLoc1(inner)
            advance()
            assertEquals(CoordGrid(3019, 9731), player.coords)
        }

    @Test
    fun GameTestState.westernAmethystRequiresActualEliteCompletionAndBelonaPermission() =
        game { d ->
            player.members = true
            val west =
                placeMapLoc(
                    CoordGrid(3008, 9711),
                    MiningGuildLocs.amethyst[0],
                    LocShape.WallStraight,
                    LocAngle.East,
                )
            player.withProtectedAccess {
                assertFalse(d.guild.canMine(this, west))
                assertFalse(d.guild.permitWest(this))
            }
            player.setVarBit(DiaryRegion.Falador.tier(DiaryDifficulty.Elite).complete, 1)
            player.withProtectedAccess {
                assertFalse(d.guild.canMine(this, west))
                assertTrue(d.guild.permitWest(this))
                assertTrue(d.guild.canMine(this, west))
            }
        }

    @Test
    fun GameTestState.normalSoutheastAmethystNeedsMembershipAndNinetyTwoMining() = game { d ->
        val wall =
            placeMapLoc(
                CoordGrid(3023, 9701),
                MiningGuildLocs.amethyst[0],
                LocShape.WallStraight,
                LocAngle.South,
            )
        player.telejump(wall.coords.translateZ(-1))
        player.righthand = InvObj(MiningObjs.dragonPickaxe)
        player.stats[stats.mining] = 91
        player.members = true
        player.opLoc1(wall)
        advance()
        assertEquals(0, player.count(objs.amethyst))
        assertMessageSent("You need a Mining level of 92 to mine this rock.")
        player.stats[stats.mining] = 92
        player.members = false
        player.opLoc1(wall)
        advance()
        assertEquals(0, player.count(objs.amethyst))
        assertMessageSent("You need to be on a members' world to mine amethyst.")
    }

    @Test
    fun GameTestState.nativeAmethystMiningGivesTwoResourcesThenDepletesAndRespawns() = game { d ->
        player.members = true
        player.stats[stats.mining] = 99
        player.righthand = InvObj(MiningObjs.dragonPickaxe)
        val wall =
            placeMapLoc(
                CoordGrid(3023, 9701),
                MiningGuildLocs.amethyst[0],
                LocShape.WallStraight,
                LocAngle.South,
            )
        player.telejump(wall.coords.translateZ(-1))
        val before = player.statMap.getFineXP(stats.mining)
        random.next = 1
        player.opLoc1(wall)
        advance()
        random.next = 128
        random.then = 0
        random.then = 1
        random.then = 1
        random.then = 2
        random.then = 128
        random.then = 0
        random.then = 1
        random.then = 1
        advance(8)
        assertEquals(2, player.count(objs.amethyst))
        assertEquals(4800, player.statMap.getFineXP(stats.mining) - before)
        assertTrue(locExists(wall.coords, MiningGuildLocs.empty))
        advance(125)
        assertTrue(locExists(wall.coords, MiningGuildLocs.amethyst[0]))
    }

    @Test
    fun GameTestState.mineralsUseOneMainActionRollAndToggleAndRegionRules() = game { d ->
        player.members = true
        player.telejump(CoordGrid(3029, 9721))
        random.next = 0
        player.withProtectedAccess {
            assertTrue(d.guild.mineral(this, player.coords, objs.iron_ore))
            d.guild.toggle(this)
            assertFalse(d.guild.mineral(this, player.coords, objs.iron_ore))
            d.guild.toggle(this)
            assertFalse(d.guild.mineral(this, CoordGrid(3034, 9738), objs.iron_ore))
            assertFalse(d.guild.mineral(this, player.coords, objs.gold_ore))
        }
        assertEquals(1, player.count(MiningGuildObjs.mineral))
        player.members = false
        player.withProtectedAccess {
            assertFalse(d.guild.mineral(this, player.coords, objs.iron_ore))
        }
    }

    @Test
    fun GameTestState.fullInventoryMineralFallsToThePlayersGround() = game { d ->
        player.observerUUID = 1L
        player.members = true
        player.telejump(CoordGrid(3029, 9721))
        for (slot in player.inv.indices) player.inv[slot] = InvObj(objs.logs)
        random.next = 0
        player.withProtectedAccess {
            assertTrue(d.guild.mineral(this, player.coords, objs.iron_ore))
        }
        assertEquals(0, player.count(MiningGuildObjs.mineral))
        assertTrue(
            d.ground.findAll(ZoneKey.from(player.coords)).any {
                it.type == MiningGuildObjs.mineral.id && it.ownerId == player.observerUUID
            }
        )
    }

    @Test
    fun GameTestState.mineralShopAndExpertCombinationConsumeExactOrdinaryInputsAtomically() =
        game { d ->
            player.members = true
            player.inv[0] = InvObj(MiningGuildObjs.mineral, 180)
            player.withProtectedAccess {
                assertTrue(d.guild.buy(this, 0))
                assertTrue(d.guild.buy(this, 1))
                assertTrue(d.guild.combine(this))
            }
            assertEquals(1, player.count(MiningGuildObjs.expert))
            assertEquals(0, player.count(MiningGuildObjs.mineral))
            assertEquals(0, player.count(MiningGuildObjs.basic))
            assertEquals(0, player.count(MiningGuildObjs.superior))
            player.withProtectedAccess {
                assertFalse(d.guild.combine(this))
                assertFalse(d.guild.buy(this, 0))
            }
        }

    @Test
    fun GameTestState.shopResaleAcceptsOnlyNativeGuildPacksAndUsesEightyPercentRefund() =
        game { d ->
            player.members = true
            player.inv[0] = InvObj(objs.coins, 10000)
            player.withProtectedAccess { assertFalse(d.guild.buy(this, 2)) }
            player.inv[1] = InvObj(MiningGuildObjs.mineral, 10)
            player.withProtectedAccess {
                assertTrue(d.guild.buy(this, 2))
                assertTrue(d.guild.sell(this, 2))
            }
            assertEquals(8, player.count(MiningGuildObjs.mineral))
            player.inv[2] = InvObj(org.rsmod.content.skills.mining.motherlode.MlmObjs.clayPack)
            player.withProtectedAccess { assertFalse(d.guild.sell(this, 2)) }
        }

    @Test
    fun GameTestState.glovesEnforceTheirUnboostedNativeEquipmentLevels() = game { d ->
        player.members = true
        for ((item, level) in
            listOf(
                MiningGuildObjs.basic to 20,
                MiningGuildObjs.superior to 55,
                MiningGuildObjs.expert to 70,
            )) {
            player.hands = null
            player.inv[0] = InvObj(item)
            player.stats[stats.mining] = level - 1
            player.setCurrentLevel(stats.mining, 99)
            player.withProtectedAccess {
                assertTrue(invEquip(0, player.inv) is HeldEquipResult.Fail.StatRequirements)
            }
            assertEquals(item.id, player.inv[0]?.id)
            assertNull(player.hands)
            player.stats[stats.mining] = level
            player.withProtectedAccess {
                assertTrue(invEquip(0, player.inv) is HeldEquipResult.Success)
            }
            assertEquals(item.id, player.hands?.id)
            assertNull(player.inv[0])
        }
    }

    private fun GameTestState.game(body: GameTestScope.(Deps) -> Unit) =
        runInjectedGameTest(Deps::class, null, Mining::class, MiningGuild::class) { d ->
            for (x in 2992..3064 step 8) for (z in 9696..9760 step 8) allocZoneCollision(
                CoordGrid(x, z)
            )
            body(d)
        }

    data class Deps @Inject constructor(val guild: MiningGuild, val ground: ObjRepository)
}
