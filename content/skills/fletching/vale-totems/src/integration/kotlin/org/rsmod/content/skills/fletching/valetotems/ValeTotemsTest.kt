package org.rsmod.content.skills.fletching.valetotems

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.HeldInteractions
import org.rsmod.api.player.interact.HeldUInteractions
import org.rsmod.api.shops.OmniShopButtons
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.entity.npc.NoopNpcInfo
import org.rsmod.game.entity.npc.NpcInfoProtocol
import org.rsmod.game.interact.HeldOp
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.type.interf.IfButtonOp
import org.rsmod.map.CoordGrid

class ValeTotemsTest {
    @Test
    fun GameTestState
        .`Ranulph teaches a complete first totem and the player claims and rummages the offering`() =
        runInjectedGameTest(HeldInteractions::class, null, ValeTotems::class, ValeRewards::class) {
            held ->
            player.members = true
            player.setBaseLevel(stats.fletching, 20)
            player.setCurrentLevel(stats.fletching, 20)
            val ranulph = spawnNpc(CoordGrid(1366, 3367), npcTypes.getValue(14576))
            val site =
                placeMapLoc(
                    CoordGrid(1370, 3375),
                    locTypes.getValue(57016),
                    LocShape[10],
                    LocAngle[0],
                )
            val pile =
                placeMapLoc(
                    site.coords.translateZ(-1),
                    locTypes.getValue(57020),
                    LocShape[10],
                    LocAngle[2],
                )
            fun ask() {
                player.teleport(ranulph.coords.translateX(1))
                player.opNpc1(ranulph)
                advance(2)
            }
            fun choose(index: Int) {
                player.resumeActiveCoroutine(
                    ResumePauseButtonInput(components.chatmenu_pbutton, index)
                )
                advance(2)
            }
            fun work() {
                player.teleport(site.coords.translateX(-1))
                player.opLoc1(site)
                advance(2)
            }
            fun bit(i: Int) = cacheTypes.varbits.getValue(i)
            ask()
            assertEquals(0, player.vars[cacheTypes.varbits.getValue(17227)])
            player.setVarBit(cacheTypes.varbits.getValue(9632), 24)
            ask()
            choose(1)
            assertEquals(10, player.vars[cacheTypes.varbits.getValue(17227)])
            player.inv[0] = InvObj(objs.knife)
            player.inv[1] = InvObj(objs.oak_logs)
            player.setVarBit(varbits.poh_house_location, 1)
            work()
            assertEquals(1, player.vars[bit(17611)])
            assertEquals(0, player.inv.count(objs.oak_logs))
            ask()
            assertEquals(20, player.vars[cacheTypes.varbits.getValue(17227)])
            for (spirit in (17618..17620).map(::bit)) {
                work()
                choose(player.vars[spirit])
            }
            assertEquals(1, player.vars[bit(17612)])
            ask()
            assertEquals(30, player.vars[cacheTypes.varbits.getValue(17227)])
            for (slot in 1..4) player.inv[slot] = InvObj(objTypes.getValue(56))
            work()
            advance(8)
            assertEquals(4, player.vars[bit(17617)])
            assertEquals(0, player.inv.count(objTypes.getValue(56)))
            ask()
            assertEquals(40, player.vars[cacheTypes.varbits.getValue(17227)])
            assertEquals(100, player.vars[bit(17622)])
            player.teleport(pile.coords.translateX(1))
            player.opLoc1(pile)
            advance(2)
            assertEquals(100, player.inv.count(objTypes.getValue(31054)))
            ask()
            assertEquals(100, player.vars[cacheTypes.varbits.getValue(17227)])
            assertEquals(1, player.inv.count(objTypes.getValue(31016)))
            val slot = player.inv.indices.first { player.inv[it]?.id == 31054 }
            player.ifClose()
            player.withProtectedAccess { held.interact(this, inv, slot, HeldOp.Op1) }
            assertEquals(0, player.inv.count(objTypes.getValue(31054)))
            assertEquals(1, player.vars[cacheTypes.varps.getValue(4764)])
            assertTrue(player.statMap.getFineXP(stats.fletching) > 0)
        }

    @Test
    fun GameTestState
        .`an ent visibly admires for fourteen ticks and awards each owner's matching carvings`() =
        runGameTest(ValeTotems::class, ValeEnts::class) {
            val standing = CoordGrid(1368, 3375)
            player.members = true
            player.teleport(standing.translateX(-1))
            val neighbour = registerPlayer(standing.translateX(-2)).apply { members = true }
            fun bit(i: Int) = cacheTypes.varbits.getValue(i)
            for (p in listOf(player, neighbour)) {
                p.setVarBit(cacheTypes.varbits.getValue(17227), 100)
                p.setVarBit(bit(17611), 1)
                p.setVarBit(bit(17612), 1)
                p.setVarBit(bit(17617), 4)
                p.setVarBit(bit(17621), 1)
                (17618..17620).map(::bit).forEachIndexed { i, b -> p.setVarBit(b, i + 1) }
                (17614..17616).map(::bit).forEachIndexed { i, b -> p.setVarBit(b, 10 + i) }
            }
            neighbour.setVarBit((17614..17616).map(::bit)[2], 14)
            player.setVarBit(bit(17347), 1)
            val ent = spawnNpc(standing, npcTypes.getValue(14636))
            advance(10)
            assertEquals(0, player.vars[bit(17622)])
            advance(12)
            assertEquals(20, player.vars[bit(17622)])
            assertEquals(7, neighbour.vars[bit(17622)])
            assertEquals(0, player.vars[bit(17611)])
            assertEquals(0, neighbour.vars[bit(17611)])
            assertEquals(0, player.vars[bit(17347)])
            advance(3)
            assertEquals(20, player.vars[bit(17622)])
            assertTrue(ent.isSlotAssigned)
        }

    @Test
    fun GameTestState
        .`Ranulph trade spends points through native shop buttons and returns an empty spool`() =
        runGameTest(ValeTotems::class, ValeRewards::class, OmniShopButtons::class) {
            player.members = true
            player.setVarBit(cacheTypes.varbits.getValue(17227), 100)
            val trader = spawnNpc(CoordGrid(1366, 3367), npcTypes.getValue(14576))
            player.teleport(trader.coords.translateX(-1))
            player.opNpc3(trader)
            advance(2)
            val list =
                cacheTypes.components.values.single { it.internalName == "omnishop_main:list" }
            val bag =
                cacheTypes.components.values.single { it.internalName == "omnishop_side:items" }
            fun buy() {
                player.ifButton(list, comsub = 0, obj = -1, op = IfButtonOp.Op2)
                advance(1)
            }
            player.setVarp(cacheTypes.varps.getValue(4764), 249)
            buy()
            assertEquals(0, player.inv.count(objTypes.getValue(31052)))
            player.setVarp(cacheTypes.varps.getValue(4764), 250)
            buy()
            assertEquals(1, player.inv.count(objTypes.getValue(31052)))
            assertEquals(0, player.vars[cacheTypes.varps.getValue(4764)])
            val slot = player.inv.indices.first { player.inv[it]?.id == 31052 }
            player.ifButton(bag, comsub = slot, obj = 31052, op = IfButtonOp.Op2)
            advance(1)
            assertEquals(0, player.inv.count(objTypes.getValue(31052)))
            assertEquals(125, player.vars[cacheTypes.varps.getValue(4764)])
        }

    @Test
    fun GameTestState.`bales unpack atomically and dirty tips preserve all products and space`() =
        runGameTest(ValeRewardItems::class) {
            player.members = true
            val bale = objTypes.getValue(31045)
            player.inv[0] = InvObj(bale, 2)
            for (i in 1..4) player.inv[i] = InvObj(objs.knife)
            player.withProtectedAccess { opHeld1(0) }
            assertEquals(2, player.inv.count(bale))
            assertEquals(0, player.inv.count(objTypes[objs.flax]))
            for (i in 1..4) player.inv[i] = null
            player.withProtectedAccess { opHeld1(0) }
            assertEquals(1, player.inv.count(bale))
            assertEquals(25, player.inv.count(objTypes[objs.flax]))
            eventBus.publish(
                org.rsmod.api.player.events.interact.HeldBanksideEvents.Type(player, 0, bale)
            )
            val bank = player.invMap[cacheTypes.invs.getValue(95)]!!
            assertEquals(25, bank.count(objTypes[objs.flax]))
            assertEquals(0, player.inv.count(bale))
            player.inv[0] = InvObj(objTypes.getValue(31047), 5)
            player.withProtectedAccess { opHeld1(0) }
            assertEquals(0, player.inv.count(objTypes.getValue(31047)))
            assertEquals(
                5,
                listOf(41, 42, 43, 44).sumOf { player.inv.count(objTypes.getValue(it)) },
            )
        }

    @Test
    fun GameTestState
        .`native leaf use unlocks a permanent mask style and produces a portable statue`() =
        runInjectedGameTest(HeldUInteractions::class, null, ValeRewardItems::class) { held ->
            player.members = true
            player.setBaseLevel(stats.fletching, 79)
            player.setCurrentLevel(stats.fletching, 79)
            player.inv[0] = InvObj(objTypes.getValue(31034))
            player.inv[1] = InvObj(objTypes.getValue(6022), 300)
            fun use(a: Int, b: Int) {
                player.ifClose()
                player.withProtectedAccess {
                    held.interact(
                        this,
                        inv,
                        objTypes[player.inv[a]!!],
                        a,
                        objTypes[player.inv[b]!!],
                        b,
                    )
                }
            }
            use(1, 0)
            assertEquals(1, player.inv.count(objTypes.getValue(31038)))
            assertEquals(0, player.inv.count(objTypes.getValue(6022)))
            assertEquals(2, player.vars[cacheTypes.varps.getValue(4759)])
            player.inv[1] = InvObj(objs.maple_logs)
            player.inv[2] = InvObj(objs.ent_branch, 5)
            player.inv[3] = InvObj(objs.knife)
            use(0, 1)
            advance(4)
            assertEquals(1, player.inv.count(objTypes.getValue(31027)))
            assertEquals(0, player.inv.count(objTypes.getValue(31038)))
            assertEquals(0, player.inv.count(objTypes[objs.ent_branch]))
            player.inv[0] = InvObj(objs.chisel)
            player.inv[1] = InvObj(objTypes.getValue(31086))
            use(0, 1)
            advance(3)
            assertEquals(1, player.inv.count(objTypes.getValue(31086)))
            advance()
            assertEquals(100, player.inv.count(objTypes[objs.atlatl_dart_tips]))
        }

    @Test
    fun GameTestState
        .`ent physically walks the Auburn Valley route and visits all eight built totems`() =
        runGameTest(ValeTotems::class, ValeEnts::class) {
            val start = CoordGrid(1368, 3375)
            player.members = true
            player.teleport(start.translateX(-3))
            fun bit(site: Int, suffix: String) =
                cacheTypes.varbits.values.single {
                    it.internalName == "ent_totems_site_${site}_" + suffix
                }
            player.setVarBit(cacheTypes.varbits.getValue(17227), 100)
            for (i in 1..8) {
                player.setVarBit(bit(i, "base"), 1)
                player.setVarBit(bit(i, "base_carved"), 1)
                player.setVarBit(bit(i, "decay"), 2)
                player.setVarBit(bit(i, "decorations"), 4)
            }
            val ent = spawnNpc(start, npcTypes.getValue(14636))
            ent.infoProtocol =
                object : NpcInfoProtocol by NoopNpcInfo {
                    override fun isActive() = true
                }
            advanceUntil({ player.vars[bit(8, "points")] > 0 }, 1400) {
                "ent at ${ent.coords}, site offerings=" +
                    (1..8).map { player.vars[bit(it, "points")] }
            }
            for (i in 1..8) {
                assertTrue(player.vars[bit(i, "points")] > 0, "site $i")
                assertEquals(1, player.vars[bit(i, "decay")], "site $i")
            }
        }

    @Test
    fun GameTestState
        .`Stalker Den shortcut respects quest and Agility then provides physical access to the antler source`() =
        runInjectedGameTest(
            org.rsmod.api.death.NpcDropTableRegistry::class,
            null,
            StalkerDen::class,
        ) { registry ->
            val entrance =
                placeMapLoc(
                    CoordGrid(1324, 3364),
                    locTypes.getValue(57279),
                    LocShape[10],
                    LocAngle[0],
                )
            val outside = CoordGrid(1324, 3363)
            player.members = true
            player.teleport(outside)
            player.opLoc1(entrance)
            advance(3)
            assertEquals(outside, player.coords)
            player.setVarBit(cacheTypes.varbits.getValue(16632), 24)
            player.opLoc1(entrance)
            advance(3)
            assertEquals(outside, player.coords)
            player.setCurrentLevel(stats.agility, 59)
            player.opLoc1(entrance)
            advance(3)
            assertEquals(CoordGrid(1323, 9767), player.coords)
            for (id in 14702..14704) {
                val npc = spawnNpc(player.coords, npcTypes.getValue(id))
                val table = requireNotNull(registry[npc.type])
                val range =
                    when (id) {
                        14702 -> 480
                        14703 -> 1712
                        else -> 124
                    }
                val drops =
                    (0 until range).count { roll ->
                        val rng =
                            object : org.rsmod.api.random.GameRandom {
                                override fun of(maxExclusive: Int): Int =
                                    if (maxExclusive == range) roll else maxExclusive - 1

                                override fun of(minInclusive: Int, maxInclusive: Int): Int =
                                    minInclusive

                                override fun randomDouble(): Double = 0.99
                            }
                        table
                            .roll(org.rsmod.api.death.NpcDropContext(npc, player, npc.coords, rng))
                            .any { it.obj.id == 31086 }
                    }
                assertEquals(
                    range /
                        when (id) {
                            14702 -> 15
                            14703 -> 16
                            else -> 31
                        },
                    drops,
                    "type $id",
                )
            }
        }

    @Test
    fun GameTestState
        .`offering and native seed on ent deliver separate scroll boxes after X Marks the Spot`() =
        runInjectedGameTest(
            org.rsmod.api.player.interact.NpcUInteractions::class,
            null,
            ValeTotems::class,
            ValeRewards::class,
        ) { interactions ->
            player.members = true
            player.setVarBit(cacheTypes.varbits.getValue(17227), 100)
            player.setVarBit(varbits.x_marks_the_spot, 8)
            player.inv[0] = InvObj(objTypes.getValue(31054), 100)
            random.next = 0
            random.then = 1
            random.then = 0
            player.withProtectedAccess { opHeld1(0) }
            assertEquals(1, player.inv.count(objs.scroll_box_beginner))
            assertEquals(1, player.inv.count(objs.empty_bird_nest))
            val ent = spawnNpc(CoordGrid(1368, 3375), npcTypes.getValue(14636))
            player.teleport(ent.coords.translateX(-1))
            player.inv[8] = InvObj(objTypes.getValue(31018))
            random.next = 1
            player.withProtectedAccess {
                interactions.interactOp(this, ent, inv, 8, ent.type, objTypes.getValue(31018))
            }
            assertEquals(1, player.inv.count(objs.scroll_box_easy))
            assertEquals(2, player.inv.count(objs.empty_bird_nest))
            assertEquals(0, player.inv.count(objTypes.getValue(31018)))
        }
}
