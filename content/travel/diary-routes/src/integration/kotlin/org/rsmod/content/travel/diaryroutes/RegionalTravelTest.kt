package org.rsmod.content.travel.diaryroutes

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.player.diary.DiaryDifficulty
import org.rsmod.api.player.diary.DiaryRegion
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.hasCompletedDiaryTask
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class RegionalTravelTest {
    @Test
    fun GameTestState
        .`the customs officer collects the exact fare and confiscates rum only on departure`() =
        runGameTest(CoastalTravel::class) {
            val start = CoordGrid(2950, 3150)
            val landing = CoordGrid(3027, 3218)
            player.members = true
            player.teleport(start)
            allocZoneCollision(landing)
            val officer = spawnNpc(start.translate(0, 1), npcTypes.getValue(3648))
            player.inv[0] = InvObj(objs.coins, 29)
            player.inv[1] = InvObj(objTypes.getValue(431))
            player.opNpc3(officer)
            advance(2)
            assertEquals(29, player.count(objs.coins))
            assertEquals(1, player.count(objTypes.getValue(431)))
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.SailMusaToSarim))
            player.inv[0] = InvObj(objs.coins, 30)
            player.opNpc3(officer)
            advance(2)
            assertEquals(landing, player.coords)
            assertEquals(0, player.count(objs.coins))
            assertEquals(0, player.count(objTypes.getValue(431)))
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.SailMusaToSarim))
        }

    @Test
    fun GameTestState
        .`the carpet merchant charges the fare only when the player can depart`() =
        runGameTest(DesertCarpets::class) {
            val start = CoordGrid(3311, 3108)
            val landing = CoordGrid(3350, 3000)
            player.members = true
            player.teleport(start)
            allocZoneCollision(landing)
            val seller = spawnNpc(start.translate(0, 1), npcTypes.getValue(17))
            fun fly() {
                player.opNpc3(seller)
                advance(2)
                player.resumeActiveCoroutine(ResumePauseButtonInput(components.menu_list, 1))
                advance(5)
            }
            player.inv[0] = InvObj(objs.coins, 199)
            fly()
            assertEquals(start, player.coords)
            assertEquals(199, player.count(objs.coins))
            player.inv[0] = InvObj(objs.coins, 205)
            fly()
            assertEquals(landing, player.coords)
            assertEquals(5, player.count(objs.coins))
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.FlyPollnivneachCarpet))
        }

    @Test
    fun GameTestState.`a player who claimed Desert Hard travels without coins`() =
        runGameTest(DesertCarpets::class) {
            val start = CoordGrid(3311,3108)
            val landing = CoordGrid(3350,3000)
            player.members = true
            player.withProtectedAccess { vars[DiaryRegion.Desert.tier(DiaryDifficulty.Hard).reward] = 1 }
            player.teleport(start)
            allocZoneCollision(landing)
            val merchant = spawnNpc(start.translate(0,1), npcTypes.getValue(17))
            player.opNpc3(merchant)
            advance(2)
            player.resumeActiveCoroutine(ResumePauseButtonInput(components.menu_list,1))
            advance(5)
            assertEquals(landing,player.coords)
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.FlyPollnivneachCarpet))
        }

    @Test
    fun GameTestState.`the Al Kharid pilot opens the native map only after the Grand Tree`() =
        runGameTest(GliderTravel::class) {
            player.members = true
            val start = CoordGrid(3279, 3211)
            player.teleport(start)
            allocZoneCollision(CoordGrid(2970, 2973))
            val pilot =
                spawnNpc(
                    start.translate(-1, 0),
                    npcTypes.values.single { it.internalName == "pilot_al_kharid" },
                )
            fun selectKaramja() {
                player.opNpc1(pilot)
                advance(2)
                player.ifButton(cacheTypes.components.getValue((138 shl 16) or 16))
                advance(5)
            }
            selectKaramja()
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.FlyKaramjaGlider))
            player.withProtectedAccess { vars[cacheTypes.varps.getValue(150)] = 160 }
            selectKaramja()
            assertEquals(CoordGrid(2970, 2973), player.coords)
            assertEquals(1, player.vars[cacheTypes.varbits.getValue(9584)])
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.FlyKaramjaGlider))
        }

    @Test
    fun GameTestState.`a spirit tree journey credits the departure and arrival regions`() =
        runGameTest(SpiritTreeTravel::class) {
            val source = CoordGrid(3184, 3509)
            val landing = CoordGrid(2461, 3444)
            player.members = true
            player.teleport(source.translate(0, -1))
            allocZoneCollision(landing)
            val tree =
                placeMapLoc(
                    source,
                    locTypes.values.single { it.internalName == "spirittree_big_2ops" },
                )
            player.withProtectedAccess {
                vars[cacheTypes.varps.getValue(111)] = 9
                vars[cacheTypes.varps.getValue(150)] = 160
            }
            player.opLoc1(tree)
            advance(2)
            player.resumeActiveCoroutine(ResumePauseButtonInput(components.menu_list, 1))
            advance(3)
            assertEquals(landing, player.coords)
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.UseExchangeSpiritTree))
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.TravelStrongholdSpiritTree))
        }
}
