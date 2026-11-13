package org.rsmod.content.skills.combattraining

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.hit.plugin.NpcHitScript
import org.rsmod.api.npc.EntTrunkOwnership
import org.rsmod.api.npc.events.NpcKillEvents
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.hasCompletedDiaryTask
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.shops.ShopScript
import org.rsmod.api.shops.config.ShopComponents
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.hit.HitType
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.interf.IfButtonOp
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey

@Execution(ExecutionMode.SAME_THREAD)
class KnightAndRiftTest {
    @Test
    fun GameTestState
        .`rank tracks credited knights and only a successful Master purchase awards Falador Elite`() =
        runGameTest(WhiteKnightArmoury::class, ShopScript::class, NpcHitScript::class) {
            val castle = CoordGrid(2984, 3339, 2)
            player.placeAt(castle)
            player.members = true
            val vyvin = spawnNpc(castle.translate(1, 0), npcTypes.getValue(4736))
            val quest = cacheTypes.varbits.getValue(1051)
            val score = cacheTypes.varps.getValue(65620)
            fun talk() {
                player.placeAt(castle)
                player.opNpc1(vyvin)
                advance()
            }
            fun kill(id: Int) {
                val n = spawnNpc(castle.translate(0, 1), npcTypes.getValue(id))
                eventBus.publish(NpcKillEvents.Credited(player, n))
            }
            talk()
            assertNull(player.openedShop)
            kill(516)
            assertEquals(0, player.vars[score])
            player.setVarBit(quest, 6)
            val black = spawnNpc(castle.translate(-1, 0), npcTypes.getValue(516))
            assertTrue(black.hitpoints > 1)
            black.queueHit(player, 1, HitType.Melee, 200)
            advance()
            assertEquals(1, player.vars[score])
            kill(1545)
            assertEquals(1, player.vars[score])
            player.setVarBit(quest, 11)
            talk()
            assertEquals(101, player.vars[score])
            assertEquals(328, player.openedShop?.inv?.type?.id)
            assertTrue(player.openedShop!!.inv.none { it?.id == 6609 })
            kill(1798)
            assertEquals(100, player.vars[score])
            talk()
            assertEquals(100, player.vars[score])
            player.setVarp(score, 1282)
            kill(13465)
            assertEquals(1288, player.vars[score])
            kill(13463)
            assertEquals(1300, player.vars[score])
            kill(1829)
            assertEquals(1300, player.vars[score])
            talk()
            val shop = player.openedShop!!.inv
            assertEquals(333, shop.type.id)
            val slot = shop.indexOfFirst { it?.id == 6609 }
            assertTrue(slot >= 0)
            fun buy() {
                player.ifButton(
                    ShopComponents.shop_inv,
                    comsub = slot + 1,
                    obj = 6609,
                    op = IfButtonOp.Op2,
                )
                advance()
            }
            buy()
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.BuyWhiteTwoHandedSword))
            val cost = objTypes.getValue(6609).cost
            player.inv[0] = InvObj(objs.coins, cost)
            buy()
            assertEquals(1, player.inv.count(objTypes.getValue(6609)))
            assertEquals(0, player.inv.count(objs.coins))
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.BuyWhiteTwoHandedSword))
        }

    @Test
    fun GameTestState
        .`postquest rift admits the player to the repeatable dungeon and only the entrance rat drops bones`() =
        runInjectedGameTest(Drops::class, null, TolnasRift::class, NpcHitScript::class) { deps ->
            player.members = true
            player.observerUUID = player.uuid
            val edge = CoordGrid(3310, 3453)
            player.placeAt(edge)
            val rift = placeMapLoc(CoordGrid(3310, 3452), locTypes.getValue(13970))
            fun enter() {
                player.opLoc1(rift)
                advance()
            }
            enter()
            assertEquals(edge, player.coords)
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.EnterTolnasRift))
            player.setVarBit(cacheTypes.varbits.getValue(2011), 13)
            enter()
            assertEquals(CoordGrid(3105, 5278), player.coords)
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.EnterTolnasRift))
            fun defeat(id: Int, c: CoordGrid) {
                val npc = spawnNpc(c, npcTypes.getValue(id))
                assertEquals(50, npc.hitpoints)
                npc.queueHit(player, 1, HitType.Melee, 100)
                advance()
            }
            val near = CoordGrid(3094, 5286)
            val far = CoordGrid(3088, 5304)
            defeat(4689, near)
            defeat(4690, far)
            assertEquals(1, deps.ground.findAll(near).count { it.type == 526 })
            assertEquals(0, deps.ground.findAll(far).count { it.type == 526 })
            val rope = placeMapLoc(CoordGrid(3105, 5279), locTypes.getValue(13999))
            player.opLoc1(rope)
            advance()
            assertEquals(edge, player.coords)
        }

    @Test
    fun GameTestState
        .`ents yield owner-only trunks for one minute and ordinary Karamjan spiders drop carcasses`() =
        runInjectedGameTest(
            Drops::class,
            null,
            EntDeaths::class,
            JungleSpiderDrops::class,
            NpcHitScript::class,
        ) { deps ->
            player.observerUUID = player.uuid
            val tile = CoordGrid(3202, 3669)
            player.placeAt(tile)
            val other = registerPlayer(tile.translate(-1, 0))
            val ent = spawnNpc(tile, npcTypes.getValue(6594))
            assertEquals(105, ent.hitpoints)
            ent.queueHit(player, 1, HitType.Melee, 200)
            advance()
            val trunk = npcRepo.findAll(ZoneKey.from(tile), 1).first { it.id == 9474 }
            assertTrue(deps.ownership.canChop(player, trunk))
            assertFalse(deps.ownership.canChop(other, trunk))
            advance(101)
            assertFalse(deps.ownership.canChop(player, trunk))
            val spider = spawnNpc(tile.translate(1, 0), npcTypes.getValue(3020))
            eventBus.publish(NpcKillEvents.Credited(player, spider))
            assertEquals(1, deps.ground.findAll(spider.coords).single { it.type == 6291 }.count)
        }

    private class Drops
    @Inject
    constructor(val ground: ObjRepository, val ownership: EntTrunkOwnership)
}
