package org.rsmod.content.skills.thieving

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.events.interact.HeldObjEvents
import org.rsmod.api.player.events.interact.WornObjEvents
import org.rsmod.api.player.hands
import org.rsmod.api.player.interact.HeldUInteractions
import org.rsmod.api.player.interact.WornInteractions
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.events.EventBus
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.interf.IfButtonOp
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.varbit.VarBitTypeList
import org.rsmod.game.type.varp.VarpLifetime

class SilentGlovesTest {
    @Test
    fun GameTestState.`native worn Check keeps gloves equipped and Remove unequips them`() =
        runGlovesTest { d ->
            player.members = true
            player.worn[9] = InvObj(objs.gloves_of_silence)
            player.withProtectedAccess { d.worn.interact(this, player.worn, 9, IfButtonOp.Op2) }
            assertMessageSent("Your gloves are new.")
            assertEquals(objs.gloves_of_silence.id, player.worn[9]?.id)
            player.withProtectedAccess { d.worn.interact(this, player.worn, 9, IfButtonOp.Op1) }
            assertEquals(null, player.worn[9])
            assertEquals(1, player.count(objs.gloves_of_silence))
        }

    @Test
    fun GameTestState.`cache pins gloves operations and persistent damage state`() =
        runGlovesTest { deps ->
            val gloves = deps.objs[objs.gloves_of_silence]
            assertEquals(10075, gloves.id)
            assertEquals("Check", gloves.iop[2])
            assertEquals(3074, ThievingVarbits.silentGlovesDamage.id)
            val damage = deps.varbits[ThievingVarbits.silentGlovesDamage]
            assertEquals(6, damage.msb - damage.lsb + 1)
            assertEquals(VarpLifetime.Perm, damage.baseVar.scope)
            assertTrue(deps.events.contains(HeldObjEvents.Op3::class.java, gloves.id))
            assertEquals("Check", gloves.param(params.wear_op1))
            assertTrue(deps.events.contains(WornObjEvents.Op2::class.java, gloves.id))
            assertEquals(false, deps.events.contains(WornObjEvents.Op1::class.java, gloves.id))
        }

    @Test
    fun GameTestState
        .`sixty second failed pickpocket destroys only worn pair and resets shared state`() =
        runGlovesTest { deps ->
            player.hands = InvObj(objs.gloves_of_silence)
            player.inv[0] = InvObj(objs.gloves_of_silence)
            repeat(61) {
                player.withProtectedAccess {
                    deps.gloves.failedPickpocket(this, StandardPickpockets.man)
                }
            }
            assertEquals(61, player.vars[ThievingVarbits.silentGlovesDamage])
            assertEquals(objs.gloves_of_silence.id, player.hands?.id)
            assertMessageSent("Your gloves of silence are going to fall apart!")
            player.withProtectedAccess {
                deps.gloves.failedPickpocket(this, StandardPickpockets.man)
            }
            assertNull(player.hands)
            assertEquals(1, player.count(objs.gloves_of_silence))
            assertEquals(0, player.vars[ThievingVarbits.silentGlovesDamage])
            assertMessageSent("Your gloves of silence have worn out.")
        }

    @Test
    fun GameTestState.`Ardougne diary override preserves gloves only for eligible targets`() =
        runGlovesTest { deps ->
            player.hands = InvObj(objs.gloves_of_silence)
            player.setVarBit(varbits.ardougne_diary_medium_complete, 1)
            player.withProtectedAccess {
                deps.gloves.failedPickpocket(this, StandardPickpockets.knightOfArdougne)
            }
            assertEquals(0, player.vars[ThievingVarbits.silentGlovesDamage])
            player.withProtectedAccess {
                deps.gloves.failedPickpocket(this, StandardPickpockets.man)
            }
            assertEquals(1, player.vars[ThievingVarbits.silentGlovesDamage])
            player.setVarBit(varbits.ardougne_diary_hard_complete, 1)
            player.withProtectedAccess {
                deps.gloves.failedPickpocket(this, StandardPickpockets.man)
            }
            assertEquals(1, player.vars[ThievingVarbits.silentGlovesDamage])
        }

    @Test
    fun GameTestState.`each documented condition threshold uses player state`() =
        runGlovesTest { deps ->
            val messages =
                mapOf(
                    0 to "Your gloves are new.",
                    1 to "Your gloves are in good condition.",
                    29 to "Your gloves are starting to look quite shabby.",
                    43 to "Your gloves are starting to need repair.",
                    53 to "Your gloves are in need of repair!",
                    61 to "Your gloves are about to fall apart!",
                )
            for ((damage, message) in messages) {
                player.setVarBit(ThievingVarbits.silentGlovesDamage, damage)
                player.withProtectedAccess { deps.gloves.check(this) }
                assertMessageSent(message)
            }
        }

    @Test
    fun GameTestState
        .`repair consumes one fur and thread with no xp and preserves tools and every pair`() =
        runGlovesTest { deps ->
            prepareRepair()
            player.fillInv()
            player.inv[0] = InvObj(objs.gloves_of_silence)
            player.inv[1] = InvObj(objs.dark_kebbit_fur)
            player.inv[2] = InvObj(objs.thread, 2)
            player.inv[3] = InvObj(objs.needle)
            player.inv[4] = InvObj(objs.knife)
            player.inv[5] = InvObj(objs.gloves_of_silence)
            val xp = player.statMap.getFineXP(stats.crafting)
            player.withProtectedAccess {
                deps.heldU.interact(
                    this,
                    player.inv,
                    deps.objs[objs.dark_kebbit_fur],
                    1,
                    deps.objs[objs.gloves_of_silence],
                    0,
                )
            }
            assertEquals(0, player.vars[ThievingVarbits.silentGlovesDamage])
            assertEquals(0, player.count(objs.dark_kebbit_fur))
            assertEquals(1, player.count(objs.thread))
            assertEquals(1, player.count(objs.needle))
            assertEquals(1, player.count(objs.knife))
            assertEquals(2, player.count(objs.gloves_of_silence))
            assertEquals(xp, player.statMap.getFineXP(stats.crafting))
        }

    @Test
    fun GameTestState
        .`missing repair tools level and membership leave materials and condition intact`() =
        runGlovesTest { deps ->
            prepareRepair()
            player.stats[stats.crafting] = 63
            player.withProtectedAccess { deps.gloves.repair(this) }
            assertEquals(61, player.vars[ThievingVarbits.silentGlovesDamage])
            assertEquals(1, player.count(objs.dark_kebbit_fur))
            player.stats[stats.crafting] = 64
            for (tool in listOf(objs.knife, objs.needle, objs.thread)) {
                val slot = player.inv.indexOfFirst { it?.id == tool.id }
                val item = player.inv[slot]
                player.inv[slot] = null
                player.withProtectedAccess { deps.gloves.repair(this) }
                assertEquals(61, player.vars[ThievingVarbits.silentGlovesDamage])
                assertEquals(1, player.count(objs.dark_kebbit_fur))
                player.inv[slot] = item
            }
            player.members = false
            player.withProtectedAccess { deps.gloves.repair(this) }
            assertEquals(61, player.vars[ThievingVarbits.silentGlovesDamage])
            assertEquals(1, player.count(objs.dark_kebbit_fur))
        }

    private fun GameTestScope.prepareRepair() {
        player.members = true
        player.stats[stats.crafting] = 64
        player.setVarBit(ThievingVarbits.silentGlovesDamage, 61)
        player.inv[0] = InvObj(objs.gloves_of_silence)
        player.inv[1] = InvObj(objs.dark_kebbit_fur)
        player.inv[2] = InvObj(objs.thread)
        player.inv[3] = InvObj(objs.needle)
        player.inv[4] = InvObj(objs.knife)
    }

    private fun GameTestState.runGlovesTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            scripts = arrayOf(PickpocketScript::class),
            testBody = body,
        )

    data class Dependencies
    @Inject
    constructor(
        val gloves: SilentGloves,
        val worn: WornInteractions,
        val objs: ObjTypeList,
        val varbits: VarBitTypeList,
        val heldU: HeldUInteractions,
        val events: EventBus,
    )
}
