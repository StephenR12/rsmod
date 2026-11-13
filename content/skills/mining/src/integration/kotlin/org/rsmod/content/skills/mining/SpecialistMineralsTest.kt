package org.rsmod.content.skills.mining

import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.events.interact.LocUEvents
import org.rsmod.api.player.righthand
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.skills.mining.configs.MiningLocs
import org.rsmod.content.skills.mining.configs.MiningObjs
import org.rsmod.content.skills.mining.scripts.Mining
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

class SpecialistMineralsTest {
    @Test
    fun GameTestState.ashUsesTheBoostedYieldAndDoesNotSpendAnEnhancerCharge() =
        runGameTest(Mining::class) {
            player.members = true
            player.stats[stats.mining] = 97
            player.righthand = InvObj(MiningObjs.runePickaxe)
            player.worn[12] = InvObj(objTypes.getValue(25541), vars = 3)
            val pile = placeMapLoc(CoordGrid(3802, 3813), MiningLocs.fossilAsh)
            player.telejump(pile.coords.translateX(-1))
            player.opLoc1(pile)
            advance(5)
            assertEquals(0, player.count(MiningObjs.volcanicAsh))
            player.setVarBit(cacheTypes.varbits.getValue(5795), 50)
            player.opLoc1(pile)
            advance()
            random.next = 128
            random.then = 0
            random.then = 1
            random.then = 0
            advance(4)
            assertEquals(6, player.count(MiningObjs.volcanicAsh))
            assertEquals(3, player.worn[12]?.vars)
            assertNotNull(findLoc(pile.coords, MiningLocs.fossilEmpty))
            advance(51)
            assertNotNull(findLoc(pile.coords, MiningLocs.fossilAsh))
        }

    @Test
    fun GameTestState.calcifiedDepositPersistsUntilItsTimerAndCrushesOnAnAnvil() =
        runGameTest(Mining::class, CalcifiedDeposits::class) {
            player.members = true
            player.stats[stats.mining] = 70
            player.setVarBit(cacheTypes.varbits.getValue(9819), 5)
            player.righthand = InvObj(MiningObjs.runePickaxe)
            player.statMap.setFineXP(stats.mining, 0)
            val rock =
                placeMapLoc(
                    CoordGrid(1514, 9546, 1),
                    MiningLocs.calcified.first(),
                    angle = org.rsmod.game.loc.LocAngle.South,
                )
            player.telejump(rock.coords.translate(0, -1))
            player.opLoc1(rock)
            advance()
            random.next = 128
            random.then = 0
            random.then = 0
            random.then = 200
            random.then = 190
            advance(4)
            assertEquals(1, player.count(MiningObjs.deposit))
            assertEquals(360, player.statMap.getFineXP(stats.mining))
            player.moveGameClick(player.coords.translate(0, -1))
            advance(118)
            assertNotNull(findLoc(rock.coords, MiningLocs.calcifiedEmpty.first()))
            advance(51)
            assertNotNull(findLoc(rock.coords, MiningLocs.calcified.first()))
            val anvil = placeMapLoc(player.coords.translateX(1), locTypes.getValue(2097))
            val slot = player.inv.indexOfFirst { it?.id == MiningObjs.deposit.id }
            fun crush() {
                player.withProtectedAccess {
                    publish(
                        LocUEvents.Op(
                            anvil,
                            anvil,
                            locTypes[anvil],
                            objTypes[MiningObjs.deposit],
                            slot,
                        )
                    )
                }
                advance(3)
            }
            crush()
            assertEquals(1, player.count(MiningObjs.deposit))
            player.inv[10] = InvObj(objs.hammer)
            random.next = 0
            crush()
            assertEquals(0, player.count(MiningObjs.deposit))
            assertEquals(1, player.count(MiningObjs.moth))
            assertEquals(10, player.statMap.getFineXP(stats.smithing))
        }
}
