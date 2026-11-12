package org.rsmod.content.skills.crafting.hydraleather

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import net.rsprot.protocol.game.outgoing.misc.player.MessageGame
import net.rsprot.protocol.game.outgoing.misc.player.RunClientScript
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.dbrow.DbRowTypeList
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.obj.Wearpos
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class HydraLeatherCraftingTest {
    @Test
    fun GameTestState.`cache 233 exposes the machine materials animations and quest boundary`() =
        runHydraLeatherTest { deps ->
            assertEquals(22983, objs.hydra_leather.id)
            assertEquals(22981, objs.ferocious_gloves.id)
            assertEquals(33609, hydra_leather_locs.machinery.id)
            assertEquals(1755, seqs.human_hammer_hit.id)
            assertEquals(8915, seqs.human_hammer_hit_imcando.id)
            assertEquals(6104, varbits.dragon_slayer_ii_progress.id)

            val leather = deps.objTypes[objs.hydra_leather]
            assertEquals("Hydra leather", leather.name)
            assertTrue(leather.members)
            assertEquals("Inspect", leather.iop[0])
            assertEquals("Ferocious gloves", deps.objTypes[objs.ferocious_gloves].name)

            val machinery = deps.locTypes[hydra_leather_locs.machinery]
            assertEquals("Machinery", machinery.name)
            assertEquals("Investigate", machinery.op[0])
            assertEquals(listOf(215), deps.dbRows.types.getValue(DRAGON_SLAYER_II_ROW).data[19])
        }

    @Test
    fun GameTestState.`hydra leather inspection uses the exact cache-era dialogue`() =
        runHydraLeatherTest { deps ->
            preparePlayer()

            player.withProtectedAccess { deps.crafting.inspect(this) }

            assertDialogueTextSent(
                "This leather looks pretty tough to work with... Maybe the dragonkin had a way."
            )
        }

    @Test
    fun GameTestState.`forward conversion confirms then transforms after exactly seven ticks`() =
        runHydraLeatherTest { deps ->
            preparePlayer()
            val machinery = prepareMachinery()
            add(objs.hammer)
            add(objs.hydra_leather)
            val startXp = player.statMap.getFineXP(stats.crafting)

            player.withProtectedAccess {
                deps.crafting.transform(this, machinery, objs.hydra_leather)
            }
            assertChoiceSent(
                "Are sure you want to make the Ferocious Gloves? " +
                    "<col=ff0000>(This will make them untradeable)</col>"
            )
            confirm()

            assertEquals(seqs.human_hammer_hit.id, player.pendingSequence.id)
            advance(6)
            assertEquals(1, player.count(objs.hydra_leather))
            assertEquals(0, player.count(objs.ferocious_gloves))
            advance()

            assertEquals(0, player.count(objs.hydra_leather))
            assertEquals(1, player.count(objs.ferocious_gloves))
            assertEquals(startXp, player.statMap.getFineXP(stats.crafting))
            assertDialogueTextSent(
                "By feeding the tough to work leather through the machine, you manage to form " +
                    "a pair of gloves."
            )
        }

    @Test
    fun GameTestState.`reverse conversion confirms then returns one hydra leather`() =
        runHydraLeatherTest { deps ->
            preparePlayer()
            val machinery = prepareMachinery()
            add(objs.hammer)
            add(objs.ferocious_gloves)

            player.withProtectedAccess {
                deps.crafting.transform(this, machinery, objs.ferocious_gloves)
            }
            assertChoiceSent("Would you like to revert your Ferocious Gloves into Hydra Leather?")
            confirm()
            advance(7)

            assertEquals(0, player.count(objs.ferocious_gloves))
            assertEquals(1, player.count(objs.hydra_leather))
            assertDialogueTextSent(
                "By feeding the gloves through the machine, you manage to revert them into " +
                    "leather."
            )
        }

    @Test
    fun GameTestState.`declining leaves the input untouched and never starts the animation`() =
        runHydraLeatherTest { deps ->
            preparePlayer()
            val machinery = prepareMachinery()
            add(objs.hammer)
            add(objs.hydra_leather)

            player.withProtectedAccess {
                deps.crafting.transform(this, machinery, objs.hydra_leather)
            }
            decline()
            advance(7)

            assertEquals(1, player.count(objs.hydra_leather))
            assertEquals(0, player.count(objs.ferocious_gloves))
            assertEquals(65535, player.pendingSequence.id)
        }

    @Test
    fun GameTestState.`membership quest and hammer gates do not consume the leather`() =
        runHydraLeatherTest { deps ->
            preparePlayer()
            val machinery = prepareMachinery()
            add(objs.hammer)
            add(objs.hydra_leather)
            player.members = false
            player.withProtectedAccess {
                deps.crafting.transform(this, machinery, objs.hydra_leather)
            }
            assertMessageSent("You need to be on a members' world to use the machine.")
            assertEquals(1, player.count(objs.hydra_leather))

            player.members = true
            player.setVarBit(varbits.dragon_slayer_ii_progress, 214)
            player.withProtectedAccess {
                deps.crafting.transform(this, machinery, objs.hydra_leather)
            }
            assertMessageSent("You need to complete Dragon Slayer II to use the machine.")
            assertEquals(1, player.count(objs.hydra_leather))

            player.setVarBit(varbits.dragon_slayer_ii_progress, 215)
            player.inv[player.inv.indexOfFirst { it?.id == objs.hammer.id }] = null
            player.withProtectedAccess {
                deps.crafting.transform(this, machinery, objs.hydra_leather)
            }
            assertMessageSent("You need a hammer to use the machine.")
            assertEquals(1, player.count(objs.hydra_leather))
        }

    @Test
    fun GameTestState.`both imcando hammer forms use the matching animation and work equipped`() {
        val hammerSetups =
            listOf(
                objs.imcando_hammer to null,
                objs.imcando_hammer to Wearpos.RightHand,
                objs.imcando_hammer_offhand to null,
                objs.imcando_hammer_offhand to Wearpos.LeftHand,
            )
        for ((hammer, wearpos) in hammerSetups) {
            runHydraLeatherTest { deps ->
                preparePlayer()
                val machinery = prepareMachinery()
                if (wearpos == null) {
                    add(hammer)
                } else {
                    player.worn[wearpos.slot] = InvObj(hammer)
                }
                add(objs.hydra_leather)

                player.withProtectedAccess {
                    deps.crafting.transform(this, machinery, objs.hydra_leather)
                }
                confirm()
                assertEquals(seqs.human_hammer_hit_imcando.id, player.pendingSequence.id)
                advance(7)
                assertEquals(1, player.count(objs.ferocious_gloves))
            }
        }
    }

    @Test
    fun GameTestState.`full inventory one-for-one transaction succeeds`() =
        runHydraLeatherTest { deps ->
            preparePlayer()
            val machinery = prepareMachinery()
            add(objs.hammer)
            add(objs.hydra_leather)
            add(objs.leather, 26)
            assertEquals(28, player.inv.count { it != null })

            player.withProtectedAccess {
                deps.crafting.transform(this, machinery, objs.hydra_leather)
            }
            confirm()
            advance(7)

            assertEquals(1, player.count(objs.ferocious_gloves))
            assertEquals(28, player.inv.count { it != null })
        }

    @Test
    fun GameTestState.`movement tool loss and material loss cancel without duplicating items`() =
        runHydraLeatherTest { deps ->
            preparePlayer()
            var machinery = prepareMachinery()
            add(objs.hammer)
            add(objs.hydra_leather)
            player.withProtectedAccess {
                deps.crafting.transform(this, machinery, objs.hydra_leather)
            }
            confirm()
            player.placeAt(machinery.coords.translateX(4))
            advance(7)
            assertEquals(1, player.count(objs.hydra_leather))
            assertEquals(0, player.count(objs.ferocious_gloves))

            preparePlayer()
            machinery = prepareMachinery()
            add(objs.hammer)
            add(objs.hydra_leather)
            player.withProtectedAccess {
                deps.crafting.transform(this, machinery, objs.hydra_leather)
            }
            confirm()
            player.inv[player.inv.indexOfFirst { it?.id == objs.hammer.id }] = null
            advance(7)
            assertEquals(1, player.count(objs.hydra_leather))
            assertEquals(0, player.count(objs.ferocious_gloves))

            preparePlayer()
            machinery = prepareMachinery()
            add(objs.hammer)
            add(objs.hydra_leather)
            player.withProtectedAccess {
                deps.crafting.transform(this, machinery, objs.hydra_leather)
            }
            confirm()
            player.inv[player.inv.indexOfFirst { it?.id == objs.hydra_leather.id }] = null
            advance(7)
            assertEquals(0, player.count(objs.hydra_leather))
            assertEquals(0, player.count(objs.ferocious_gloves))
        }

    @Test
    fun GameTestState.`Investigate prioritizes leather and uses the registered loc script`() =
        runHydraLeatherTest {
            preparePlayer()
            val machinery = prepareMachinery()
            add(objs.hammer)
            add(objs.hydra_leather)
            add(objs.ferocious_gloves)

            player.opLoc1(machinery)
            advance()

            assertChoiceSent(
                "Are sure you want to make the Ferocious Gloves? " +
                    "<col=ff0000>(This will make them untradeable)</col>"
            )
            decline()
            assertEquals(1, player.count(objs.hydra_leather))
            assertEquals(1, player.count(objs.ferocious_gloves))
        }

    private fun GameTestScope.preparePlayer() {
        player.clearAllInvs()
        player.members = true
        player.setVarBit(varbits.dragon_slayer_ii_progress, 215)
    }

    private fun GameTestScope.prepareMachinery(): BoundLocInfo {
        val machinery = placeMapLoc(CoordGrid(0, 50, 50, 34, 31), hydra_leather_locs.machinery)
        player.placeAt(machinery.coords.translateX(-1))
        return machinery
    }

    private fun GameTestScope.add(obj: ObjType, count: Int = 1) {
        if (objTypes[obj].stackable) {
            val slot = player.inv.indexOfFirst { it == null }
            require(slot >= 0)
            player.inv[slot] = InvObj(obj, count)
            return
        }
        repeat(count) {
            val slot = player.inv.indexOfFirst { it == null }
            require(slot >= 0)
            player.inv[slot] = InvObj(obj)
        }
    }

    private fun GameTestScope.confirm() {
        player.resumeActiveCoroutine(
            ResumePauseButtonInput(components.chatmenu_pbutton, subcomponent = 1)
        )
    }

    private fun GameTestScope.decline() {
        player.resumeActiveCoroutine(
            ResumePauseButtonInput(components.chatmenu_pbutton, subcomponent = 2)
        )
    }

    private fun GameTestScope.assertChoiceSent(title: String) {
        assertTrue(
            client.filterIsInstance<RunClientScript>().any {
                it.id == CHATBOX_MULTI_INIT && it.values == listOf(title, "Yes|No")
            }
        )
    }

    private fun GameTestScope.assertDialogueTextSent(text: String) {
        val messages =
            client.filterIsInstance<MessageGame>().map { it.message.replace("<br>", " ") }
        assertTrue(messages.contains(text)) {
            "Dialogue not found. (search=$text) | (messages=$messages)"
        }
    }

    private fun GameTestState.runHydraLeatherTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            HydraLeatherScript::class,
            testBody = body,
        )

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(HydraLeatherModule())
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val crafting: HydraLeatherCrafting,
        val objTypes: ObjTypeList,
        val locTypes: LocTypeList,
        val dbRows: DbRowTypeList,
    )

    private companion object {
        const val CHATBOX_MULTI_INIT = 58
        const val DRAGON_SLAYER_II_ROW = 32
    }
}
