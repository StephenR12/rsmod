package org.rsmod.content.skills.hunter.experts

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.events.HunterCapture
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.HeldInteractions
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.interact.HeldOp
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid

class GuildRumoursTest {
    @Test
    fun GameTestState.`Verity and Gilman assign the caught species and its sack can be opened`() =
        runInjectedGameTest(
            HeldInteractions::class,
            null,
            GuildRumours::class,
            RumourSacks::class,
        ) { held ->
            player.members = true
            player.setBaseLevel(stats.hunter, 46)
            player.setCurrentLevel(stats.hunter, 46)
            val tile = CoordGrid(1558, 9464)
            val verity = spawnNpc(tile, npcTypes.getValue(13127))
            val gilman = spawnNpc(tile.translateX(-2), npcTypes.getValue(13121))
            player.teleport(tile.translateX(-1))
            fun ask() {
                player.opNpc3(gilman)
                advance(2)
            }
            ask()
            assertEquals(0, player.vars[cacheTypes.varps.getValue(65756)])
            player.opNpc1(verity)
            advance(2)
            random.next = 0
            ask()
            assertEquals(1, player.vars[cacheTypes.varps.getValue(65756)] and 31)
            repeat(40) { eventBus.publish(HunterCapture(player, "butterfly_snowy")) }
            assertEquals(0, player.inv.count(objTypes.getValue(29222)))
            // All 40 rolls miss until the guaranteed bird-snare threshold.
            repeat(39) {
                random.next = 255
                eventBus.publish(HunterCapture(player, "multicoloured_bird"))
            }
            assertEquals(0, player.inv.count(objTypes.getValue(29222)))
            for (i in player.inv.indices) player.inv[i] = InvObj(objs.logs)
            eventBus.publish(HunterCapture(player, "multicoloured_bird"))
            assertEquals(0, player.inv.count(objTypes.getValue(29222)))
            player.inv[7] = null
            eventBus.publish(HunterCapture(player, "multicoloured_bird"))
            assertEquals(1, player.inv.count(objTypes.getValue(29222)))
            val xp = player.statMap.getFineXP(stats.hunter)
            ask()
            assertEquals(1, player.vars[cacheTypes.varps.getValue(65758)])
            assertEquals(25500, player.statMap.getFineXP(stats.hunter) - xp)
            assertEquals(1, player.inv.count(objTypes.getValue(29242)))
            for (i in player.inv.indices) if (player.inv[i]?.id == objs.logs.id)
                player.inv[i] = null
            val slot = player.inv.indices.first { player.inv[it]?.id == 29242 }
            player.withProtectedAccess { held.interact(this, inv, slot, HeldOp.Op1) }
            advance(1)
            assertEquals(0, player.inv.count(objTypes.getValue(29242)))
            assertTrue(player.inv.any { it != null })
        }

    @Test
    fun GameTestState
        .`the tenth rumour unlocks a crafted whistle which spends only actual recharged meat`() =
        runInjectedGameTest(HeldInteractions::class, null, QuetzalWhistles::class) { held ->
            player.members = true
            val pitri = spawnNpc(CoordGrid(1558, 3047, 2), npcTypes.getValue(13135))
            player.teleport(pitri.coords.translateX(1))
            fun blueprint() {
                player.opNpc1(pitri)
                advance(2)
                player.resumeActiveCoroutine(ResumePauseButtonInput(components.chatmenu_pbutton, 1))
                advance(2)
            }
            blueprint()
            assertEquals(0, player.inv.count(objTypes.getValue(29251)))
            player.setVarp(cacheTypes.varps.getValue(65758), 10)
            blueprint()
            assertEquals(1, player.inv.count(objTypes.getValue(29251)))
            player.inv[10] = InvObj(objs.knife)
            player.inv[11] = InvObj(objs.willow_logs)
            advance(2)
            fun op(id: Int, action: HeldOp) {
                val slot = player.inv.indices.first { player.inv[it]?.id == id }
                player.ifClose()
                player.withProtectedAccess { held.interact(this, inv, slot, action) }
                advance(1)
            }
            op(29251, HeldOp.Op2)
            assertEquals(
                1,
                player.inv.count(objTypes.getValue(29271)),
                player.inv.filterNotNull().toString(),
            )
            player.inv[2] = InvObj(objTypes.getValue(29125))
            player.opNpc3(pitri)
            advance(2)
            assertEquals(2, player.inv.first { it?.id == 29271 }?.vars)
            op(29271, HeldOp.Op1)
            advance(4)
            assertEquals(CoordGrid(1585, 3053), player.coords)
            assertEquals(1, player.inv.first { it?.id == 29271 }?.vars)
        }

    @Test
    fun GameTestState.`Apatura awards the three milestones and her chest consumes only her key`() =
        runGameTest(HunterExpertScript::class, GuildLore::class) {
            player.members = true
            val base = CoordGrid(1556, 3032, 1)
            val apatura = spawnNpc(base.translateX(4), npcTypes.getValue(13120))
            val chest = placeMapLoc(base, locTypes.getValue(51616), LocShape[10], LocAngle[2])
            val shelf =
                placeMapLoc(
                    CoordGrid(1553, 3035, 1),
                    locTypes.getValue(51618),
                    LocShape[5],
                    LocAngle[1],
                )
            fun ask(count: Int) {
                player.setVarp(cacheTypes.varps.getValue(65758), count)
                player.teleport(apatura.coords.translateZ(-1))
                player.opNpc1(apatura)
                advance(2)
            }
            fun examine(target: org.rsmod.game.loc.BoundLocInfo) {
                player.teleport(target.coords.translateZ(1))
                player.opLoc1(target)
                advance(3)
            }
            ask(50)
            assertEquals(1, player.inv.count(objTypes.getValue(29317)))
            ask(100)
            assertEquals(1, player.inv.count(objTypes.getValue(29319)))
            examine(chest)
            assertEquals(0, player.inv.count(objTypes.getValue(29321)))
            ask(150)
            assertEquals(1, player.inv.count(objTypes.getValue(29323)))
            examine(chest)
            assertEquals(0, player.inv.count(objTypes.getValue(29323)))
            assertEquals(1, player.inv.count(objTypes.getValue(29321)))
            val slot = player.inv.indices.first { player.inv[it]?.id == 29317 }
            player.inv[slot] = null
            examine(shelf)
            assertEquals(1, player.inv.count(objTypes.getValue(29317)))
            assertEquals(0, player.inv.count(objTypes.getValue(29323)))
        }

    @Test
    fun GameTestState
        .`the Huntsman's kit stores only native Hunter tools and empties from the last occupied slot`() =
        runGameTest(HuntsmanKit::class) {
            player.members = true
            val kit = objTypes.getValue(29309)
            val snare = objTypes.getValue(10006)
            val trap = objTypes.getValue(10008)
            player.inv[0] = InvObj(kit)
            player.inv[1] = InvObj(snare)
            player.inv[2] = InvObj(trap)
            player.inv[3] = InvObj(objs.knife)
            player.withProtectedAccess { opHeld2(0) }
            val storage = player.invMap[cacheTypes.invs.getValue(855)]!!
            assertEquals(1, storage.count(snare))
            assertEquals(1, storage.count(trap))
            assertEquals(1, player.inv.count(objTypes[objs.knife]))
            for (slot in 1 until player.inv.size) player.inv[slot] = InvObj(objs.logs)
            player.inv[10] = null
            player.withProtectedAccess { opHeld3(0) }
            assertEquals(1, player.inv.count(trap))
            assertEquals(1, storage.count(snare))
        }
}
