package org.rsmod.content.skills.thieving

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.invs
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.hit.plugin.PlayerHitScript
import org.rsmod.api.player.diary.*
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class SpecialTheftsTest {
    @Test
    fun GameTestState
        .`native Phoenix success requires twenty five and grants unique feather and diary`() =
        test { d ->
            prepare(24)
            player.telejump(CoordGrid(3418, 3155))
            val npc = spawnNpc(CoordGrid(3419, 3155), d.npcs[SpecialTheftNpcs.phoenix])
            player.opNpc3(npc)
            advance()
            assertEquals(0, player.count(SpecialTheftObjs.feather))
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.StealDesertPhoenixFeather))
            player.setCurrentLevel(stats.thieving, 25)
            random.next = 0
            player.opNpc3(npc)
            advance()
            assertEquals(1, player.count(SpecialTheftObjs.feather))
            assertEquals(260, player.statMap.getFineXP(stats.thieving))
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.StealDesertPhoenixFeather))
            advance(3)
            player.opNpc3(npc)
            advance()
            assertEquals(1, player.count(SpecialTheftObjs.feather))
        }

    @Test
    fun GameTestState.`banked feather or quill and full inventory block Phoenix acquisition`() =
        test { d ->
            prepare(99)
            player.telejump(CoordGrid(3418, 3155))
            val npc = spawnNpc(CoordGrid(3419, 3155), d.npcs[SpecialTheftNpcs.phoenix])
            player.withProtectedAccess { inv(invs.bank)[0] = InvObj(SpecialTheftObjs.pen) }
            player.opNpc3(npc)
            advance()
            assertEquals(0, player.count(SpecialTheftObjs.feather))
            player.withProtectedAccess { inv(invs.bank)[0] = null }
            for (i in player.inv.indices) player.inv[i] = InvObj(objs.knife)
            player.opNpc3(npc)
            advance()
            assertEquals(0, player.statMap.getFineXP(stats.thieving))
        }

    @Test
    fun GameTestState.`Phoenix failure stuns and damages without granting feather or diary`() =
        test { d ->
            prepare(25)
            player.stats[stats.hitpoints] = 10
            player.telejump(CoordGrid(3418, 3155))
            val npc = spawnNpc(CoordGrid(3419, 3155), d.npcs[SpecialTheftNpcs.phoenix])
            random.next = 99
            player.opNpc3(npc)
            advance(2)
            assertEquals(0, player.count(SpecialTheftObjs.feather))
            assertTrue(d.stun.isMovementBlocked(player))
            assertEquals(9, player.stats[stats.hitpoints])
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.StealDesertPhoenixFeather))
        }

    @Test
    fun GameTestState
        .`native lobby safe requires fifty and successful diamond awards exact XP and diary`() =
        test { d ->
            prepare(49)
            player.telejump(CoordGrid(3055, 4976, 1))
            val safe =
                placeMapLoc(
                    CoordGrid(3055, 4977, 1),
                    SpecialTheftLocs.safe,
                    LocShape.WallDecorStraightNoOffset,
                    LocAngle.South,
                )
            player.opLoc1(safe)
            advance()
            assertEquals(0, player.statMap.getFineXP(stats.thieving))
            player.setCurrentLevel(stats.thieving, 50)
            random.next = 0
            random.then = 127
            player.opLoc1(safe)
            advance(8)
            assertEquals(1, player.count(objs.uncut_diamond))
            assertEquals(700, player.statMap.getFineXP(stats.thieving))
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.CrackRoguesDenSafe))
        }

    @Test
    fun GameTestState.`stethoscope improves first roll and safe trap awards no loot`() = test { d ->
        prepare(50)
        player.stats[stats.hitpoints] = 20
        player.telejump(CoordGrid(3055, 4976, 1))
        val safe =
            placeMapLoc(
                CoordGrid(3055, 4977, 1),
                SpecialTheftLocs.safe,
                LocShape.WallDecorStraightNoOffset,
                LocAngle.South,
            )
        random.next = 37
        random.then = 0
        random.then = 6
        player.opLoc1(safe)
        advance(10)
        assertEquals(0, player.statMap.getFineXP(stats.thieving))
        assertEquals(14, player.stats[stats.hitpoints])
        assertFalse(player.hasCompletedDiaryTask(DiaryTask.CrackRoguesDenSafe))
        player.inv[0] = InvObj(SpecialTheftObjs.stethoscope)
        random.next = 37
        random.then = 25
        player.opLoc1(safe)
        advance(8)
        assertEquals(20, player.count(objs.coins))
        assertEquals(700, player.statMap.getFineXP(stats.thieving))
    }

    @Test
    fun GameTestState
        .`safe full inventory failure preserves XP and diary even when coin stack exists`() =
        test { d ->
            prepare(99)
            player.telejump(CoordGrid(3055, 4976, 1))
            val safe =
                placeMapLoc(
                    CoordGrid(3055, 4977, 1),
                    SpecialTheftLocs.safe,
                    LocShape.WallDecorStraightNoOffset,
                    LocAngle.South,
                )
            for (i in player.inv.indices) player.inv[i] = InvObj(objs.knife)
            player.inv[0] = InvObj(objs.coins, 100)
            random.next = 0
            random.then = 127
            player.opLoc1(safe)
            advance(5)
            assertEquals(0, player.count(objs.uncut_diamond))
            assertEquals(100, player.count(objs.coins))
            assertEquals(0, player.statMap.getFineXP(stats.thieving))
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.CrackRoguesDenSafe))
        }

    @Test
    fun GameTestState.`native trapdoor and passage provide ordinary lobby access`() = test { d ->
        prepare(1)
        player.telejump(CoordGrid(2905, 3536))
        val trap =
            placeMapLoc(CoordGrid(2905, 3537), SpecialTheftLocs.entrance, LocShape.GroundDecor)
        player.opLoc1(trap)
        advance(2)
        assertEquals(CoordGrid(3061, 4985, 1), player.coords)
        val exit =
            placeMapLoc(
                CoordGrid(3061, 4986, 1),
                SpecialTheftLocs.exit,
                LocShape.WallStraight,
                LocAngle.South,
            )
        player.opLoc1(exit)
        advance(2)
        assertEquals(CoordGrid(2906, 3537), player.coords)
    }

    @Test
    fun GameTestState.`Martin shop requires unboosted skills and stocks the safecracking tool`() =
        test { d ->
            prepare(50)
            player.setBaseLevel(stats.agility, 1)
            player.setCurrentLevel(stats.agility, 99)
            player.telejump(CoordGrid(3050, 4974, 1))
            val martin = spawnNpc(CoordGrid(3051, 4974, 1), d.npcs[SpecialTheftNpcs.martin])
            player.opNpc3(martin)
            advance()
            assertNull(player.openedShop)
            player.setBaseLevel(stats.agility, 50)
            player.opNpc3(martin)
            advance()
            assertNotNull(player.openedShop)
            val shop = player.openedShop!!
            assertEquals(SpecialTheftInvs.shop.id, shop.inv.type.id)
            assertEquals(25, shop.inv.count(d.objects[SpecialTheftObjs.stethoscope]))
            assertEquals(60.0, shop.buyPercentage)
            assertEquals(2.0, shop.changePercentage)
        }

    private fun GameTestScope.prepare(level: Int) {
        player.members = true
        player.stats[stats.thieving] = level
        player.statMap.setFineXP(stats.thieving, 0)
    }

    private fun GameTestState.test(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            null,
            SpecialThefts::class,
            PlayerHitScript::class,
            PickpocketScript::class,
        ) { d ->
            for (c in
                listOf(
                    CoordGrid(3419, 3155),
                    CoordGrid(3055, 4977, 1),
                    CoordGrid(3061, 4985, 1),
                    CoordGrid(2905, 3537),
                )) for (x in -8..8 step 8) for (z in -8..8 step 8) allocZoneCollision(
                c.translate(x, z)
            )
            body(d)
        }

    data class Dependencies
    @Inject
    constructor(
        val thefts: SpecialThefts,
        val stun: ThievingStunStateRepository,
        val locs: LocTypeList,
        val npcs: NpcTypeList,
        val objects: ObjTypeList,
    )
}
