package org.rsmod.content.skills.prayer.remains

import jakarta.inject.Inject
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.death.NpcDropContext
import org.rsmod.api.death.NpcDropTableRegistry
import org.rsmod.api.npc.events.NpcKillEvents
import org.rsmod.api.player.events.interact.LocUEvents
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

class ForthosCryptTest {
    @Test
    fun GameTestState
        .`the temple coin opens the real crypt and weighted offerings grant faith and the key`() =
        runInjectedGameTest(Dependencies::class, null, ForthosCrypt::class) { d ->
            player.members = true
            player.observerUUID = player.uuid
            val tile = CoordGrid(1798, 9951)
            player.placeAt(tile.translate(1, 0))
            val olbertus = spawnNpc(tile, npcTypes.getValue(8718))
            fun pay() {
                player.opNpc1(olbertus)
                advance()
                player.resumeActiveCoroutine(ResumePauseButtonInput(components.chatmenu_pbutton, 0))
            }
            for (i in 0..3) player.inv[i] = InvObj(objs.dragonstone)
            pay()
            assertEquals(4, player.count(objs.dragonstone))
            assertEquals(0, player.vars[cacheTypes.varbits.getValue(8407)])
            player.inv[4] = InvObj(objs.dragonstone)
            pay()
            assertEquals(1, player.count(objTypes.getValue(23497)))
            assertEquals(100, player.vars[cacheTypes.varbits.getValue(8407)])
            val relief = placeMapLoc(CoordGrid(1809, 9959), locTypes.getValue(34903))
            val burner = placeMapLoc(CoordGrid(1810, 9951), locTypes.getValue(34855))
            player.placeAt(relief.coords.translate(1, 0))
            player.opLoc1(relief)
            advance()
            assertEquals(1, player.vars[cacheTypes.varbits.getValue(8395)])
            assertEquals(0, player.count(objTypes.getValue(23497)))
            fun offer(slot: Int) {
                val obj = player.inv.getValue(slot)
                player.withProtectedAccess {
                    check(
                        eventBus.publish(
                            this,
                            LocUEvents.Op(
                                burner,
                                burner,
                                locTypes.getValue(34855),
                                objTypes[obj],
                                slot,
                            ),
                        )
                    )
                }
            }
            player.placeAt(burner.coords.translate(1, 0))
            player.stats[stats.prayer] = 70
            val xp = player.statMap.getFineXP(stats.prayer)
            player.inv[0] = InvObj(objs.dragon_bones)
            offer(0)
            assertEquals(xp + 2160, player.statMap.getFineXP(stats.prayer))
            assertEquals(101, player.vars[cacheTypes.varbits.getValue(8407)])
            player.setVarBit(cacheTypes.varbits.getValue(8407), 197)
            player.inv[0] = InvObj(objs.superior_dragon_bones)
            offer(0)
            assertEquals(200, player.vars[cacheTypes.varbits.getValue(8407)])
            assertTrue(d.ground.findAll(CoordGrid(1810, 9950)).any { it.type == 23502 })
            player.setVarBit(cacheTypes.varbits.getValue(8407), 98)
            player.inv[0] = InvObj(objs.lava_dragon_bones)
            val before = player.statMap.getFineXP(stats.prayer)
            offer(0)
            assertEquals(before, player.statMap.getFineXP(stats.prayer))
            assertEquals(100, player.vars[cacheTypes.varbits.getValue(8407)])
        }

    @Test
    fun GameTestState
        .`refusing the bribe creates a private fight and only its credited kill restores Olbertus`() =
        runInjectedGameTest(
            Dependencies::class,
            null,
            ForthosCrypt::class,
            ForthosBeastScript::class,
        ) { d ->
            player.members = true
            player.observerUUID = player.uuid
            val outside = CoordGrid(1798, 9953)
            player.placeAt(outside)
            val guide = spawnNpc(CoordGrid(1798, 9951), npcTypes.getValue(8718))
            player.placeAt(CoordGrid(1799, 9951))
            player.opNpc1(guide)
            advance()
            player.resumeActiveCoroutine(ResumePauseButtonInput(components.chatmenu_pbutton, 1))
            assertTrue(player.coords != outside)
            assertEquals(1, player.vars[cacheTypes.varbits.getValue(8407)])
            val boss = d.npcs.findAll(player.coords.translate(-2, -3)).single { it.id == 8709 }
            val stranger = registerPlayer(outside)
            eventBus.publish(NpcKillEvents.Credited(stranger, boss))
            assertEquals(0, player.vars[cacheTypes.varbits.getValue(8394)])
            eventBus.publish(NpcKillEvents.Credited(player, boss))
            assertEquals(1, player.vars[cacheTypes.varbits.getValue(8394)])
            assertEquals(outside, player.coords)
            assertTrue(d.ground.findAll(outside).any { it.type == 23497 })
        }

    @Test
    fun GameTestState
        .`local dragon deaths supply bones and favour the least collected temple page`() =
        runInjectedGameTest(Dependencies::class, null, ForthosMonsters::class) { d ->
            player.members = true
            player.observerUUID = player.uuid
            val tile = CoordGrid(1812, 9933)
            val dragon = spawnNpc(tile, npcTypes.getValue(247))
            val table = requireNotNull(d.tables[dragon.type])
            player.setVarBit(cacheTypes.varbits.getValue(8399), 2)
            player.setVarBit(cacheTypes.varbits.getValue(8400), 1)
            random.next = 1
            random.then = 0
            random.then = 0
            val drops = table.roll(NpcDropContext(dragon, player, tile, d.random)).map { it.obj.id }
            assertEquals(listOf(objs.dragon_bones.id, objs.red_dragonhide.id, 23514), drops)
            assertTrue(
                table
                    .roll(NpcDropContext(dragon, player, CoordGrid(3200, 3200), d.random))
                    .isEmpty()
            )
        }

    @Test
    fun GameTestState
        .`grubby chest consumes one key and rolls independent supplies and materials`() =
        runGameTest(ForthosChest::class) {
            player.members = true
            val chest = placeMapLoc(player.coords.translate(1, 0), locTypes.getValue(34901))
            player.opLoc1(chest)
            advance()
            assertEquals(0, player.count(objs.shark))
            player.inv[0] = InvObj(objTypes.getValue(23499))
            random.next = 0
            random.then = 7
            random.then = 0
            random.then = 0
            random.then = 1
            random.then = 1
            player.opLoc1(chest)
            advance(3)
            assertEquals(0, player.count(objTypes.getValue(23499)))
            assertEquals(4, player.count(objs.shark))
            assertEquals(4, player.count(objs.egg_potato))
            assertEquals(1, player.count(objs.super_attack_2))
            assertEquals(200, player.count(objs.law_rune))
        }

    @Test
    fun GameTestState.`three complete tomes earn one lamp with a real level forty skill gate`() =
        runGameTest(ForthosKnowledge::class) {
            player.members = true
            player.setVarBit(cacheTypes.varbits.getValue(8403), 1)
            val librarian = spawnNpc(player.coords.translate(1, 0), npcTypes.getValue(7044))
            for ((i, id) in listOf(23506, 23504, 23508).withIndex()) {
                player.inv[0] = InvObj(objTypes.getValue(id))
                val pages = cacheTypes.varbits.getValue(listOf(8399, 8400, 8401)[i])
                player.setVarBit(pages, 3)
                player.opNpc1(librarian)
                advance()
                assertEquals(id, player.inv[0]?.id)
                player.setVarBit(pages, 4)
                player.opNpc1(librarian)
                advance()
            }
            assertEquals(23516, player.inv[0]?.id)
            player.withProtectedAccess { opHeld1(0) }
            assertEquals(23516, player.inv[0]?.id)
            player.stats[stats.prayer] = 40
            val before = player.statMap.getFineXP(stats.prayer)
            player.withProtectedAccess { opHeld1(0) }
            player.resumeActiveCoroutine(ResumePauseButtonInput(components.chatmenu_pbutton, 0))
            assertNull(player.inv[0])
            assertEquals(before + 100000, player.statMap.getFineXP(stats.prayer))
        }

    private class Dependencies
    @Inject
    constructor(
        val beast: ForthosBeast,
        val npcs: NpcRepository,
        val ground: ObjRepository,
        val random: GameRandom,
        val tables: NpcDropTableRegistry,
    )
}
