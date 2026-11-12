package org.rsmod.content.skills.fletching.toxicblowpipe

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import net.rsprot.protocol.game.outgoing.sound.SynthSound
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.synths
import org.rsmod.api.config.refs.varobjs
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.HeldUInteractions
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjType

@Execution(ExecutionMode.SAME_THREAD)
class ToxicBlowpipeFletchingTest {
    @Test
    fun GameTestState.`cache 233 pins the toxic blowpipe creation contract`() =
        runToxicBlowpipeFletchingTest {
            assertEquals(12922, objs.tanzanite_fang.id)
            assertEquals(12924, objs.toxic_blowpipe_empty.id)
            assertEquals(12926, objs.toxic_blowpipe.id)
            assertEquals(3015, seqs.snakeboss_fletching.id)
            assertEquals(
                listOf("Dismantle"),
                objTypes[objs.toxic_blowpipe_empty].iop.filterNotNull(),
            )
            assertEquals(12934, objs.zulrahs_scales.id)
            assertEquals(2381, synths.destroy_object.id)
            assertEquals(2655, synths.found_gem.id)
            assertEquals(
                listOf("Wield", "Check", "Unload", "Uncharge"),
                objTypes[objs.toxic_blowpipe].iop.filterNotNull(),
            )
            assertEquals(0..3, varobjs.snakeboss_blowpipe_darttype.bits)
            assertEquals(4..17, varobjs.snakeboss_blowpipe_dartcount.bits)
            assertEquals(18..31, varobjs.snakeboss_blowpipe_flakes.bits)

            assertEquals(28687, objs.blazing_blowpipe_empty.id)
            assertEquals(28688, objs.blazing_blowpipe.id)
            assertEquals(28690, objs.trailblazer_reloaded_blowpipe_ornament_kit.id)
            assertEquals(
                listOf("Dismantle"),
                objTypes[objs.blazing_blowpipe_empty].iop.filterNotNull(),
            )
            assertEquals(
                listOf("Wield", "Check", "Unload", "Uncharge"),
                objTypes[objs.blazing_blowpipe].iop.filterNotNull(),
            )
        }

    @Test
    fun `packed state round trips every accepted dart with stable migration codes`() {
        assertEquals(16_383, ToxicBlowpipeState.MAX_CAPACITY)
        assertEquals((1..9).toList(), ToxicBlowpipeDart.entries.map { it.storageCode })
        for (dart in ToxicBlowpipeDart.entries) {
            val state = ToxicBlowpipeState(dart, dart.storageCode * 10, scales = 12_345)
            assertEquals(state, ToxicBlowpipeState.unpack(state.pack()))
        }
        assertEquals(ToxicBlowpipeState(), ToxicBlowpipeState.unpack(0))
    }

    @Test
    fun GameTestState.`fang becomes empty toxic blowpipe immediately for 120 xp`() =
        runToxicBlowpipeFletchingTest { deps ->
            preparePlayer()
            add(objs.chisel)
            add(objs.tanzanite_fang)
            val startXp = player.statMap.getFineXP(stats.fletching)

            player.withProtectedAccess { deps.fletching.fletch(this) }

            assertEquals(0, player.count(objs.tanzanite_fang))
            assertEquals(1, player.count(objs.toxic_blowpipe_empty))
            assertEquals(1200, player.statMap.getFineXP(stats.fletching) - startXp)
            assertEquals(seqs.snakeboss_fletching.id, player.pendingSequence.id)
        }

    @Test
    fun GameTestState.`membership and boosted level boundaries are atomic`() =
        runToxicBlowpipeFletchingTest { deps ->
            preparePlayer()
            player.members = false
            add(objs.chisel)
            add(objs.tanzanite_fang)
            player.withProtectedAccess { deps.fletching.fletch(this) }
            assertMessageSent("You need to be on a members' world to make a toxic blowpipe.")
            assertEquals(1, player.count(objs.tanzanite_fang))

            preparePlayer()
            player.stats[stats.fletching] = 77
            add(objs.chisel)
            add(objs.tanzanite_fang)
            player.withProtectedAccess { deps.fletching.fletch(this) }
            assertMessageSent("You need a Fletching level of 78 to make a toxic blowpipe.")
            assertEquals(1, player.count(objs.tanzanite_fang))

            preparePlayer()
            player.stats[stats.fletching] = 78
            add(objs.chisel)
            add(objs.tanzanite_fang)
            player.withProtectedAccess { deps.fletching.fletch(this) }
            assertEquals(1, player.count(objs.toxic_blowpipe_empty))
        }

    @Test
    fun GameTestState.`missing chisel and fang failures do not mutate inventory`() =
        runToxicBlowpipeFletchingTest { deps ->
            preparePlayer()
            add(objs.tanzanite_fang)
            player.withProtectedAccess { deps.fletching.fletch(this) }
            assertMessageSent("You need a chisel to make a toxic blowpipe.")
            assertEquals(1, player.count(objs.tanzanite_fang))

            preparePlayer()
            add(objs.chisel)
            player.withProtectedAccess { deps.fletching.fletch(this) }
            assertMessageSent("You need a tanzanite fang to make a toxic blowpipe.")
            assertEquals(1, player.count(objs.chisel))
        }

    @Test
    fun GameTestState.`reverse held use works immediately in a full inventory`() =
        runToxicBlowpipeFletchingTest { deps ->
            preparePlayer()
            add(objs.chisel)
            add(objs.tanzanite_fang, 27)
            assertEquals(0, player.inv.freeSpace())
            val chiselSlot = player.inv.indexOfFirst { it?.id == objs.chisel.id }
            val fangSlot = player.inv.indexOfFirst { it?.id == objs.tanzanite_fang.id }

            player.withProtectedAccess {
                deps.heldU.interact(
                    this,
                    player.inv,
                    objTypes[objs.tanzanite_fang],
                    fangSlot,
                    objTypes[objs.chisel],
                    chiselSlot,
                )
            }

            assertEquals(1, player.count(objs.toxic_blowpipe_empty))
            assertEquals(26, player.count(objs.tanzanite_fang))
            assertEquals(0, player.inv.freeSpace())
        }

    @Test
    fun GameTestState.`creation cancels queued Fletching without scheduling a batch`() =
        runToxicBlowpipeFletchingTest { deps ->
            preparePlayer()
            add(objs.chisel)
            add(objs.tanzanite_fang)
            player.withProtectedAccess {
                weakQueue(fletching_queues.battlestaffFletching, 20)
                deps.fletching.fletch(this)
            }

            assertEquals(0, player.weakQueueList.count(fletching_queues.battlestaffFletching))
            assertEquals(0, player.weakQueueList.count(fletching_queues.ammunition))
            assertEquals(1, player.count(objs.toxic_blowpipe_empty))
        }

    @Test
    fun GameTestState.`scales and every unpoisoned dart load through either held-use order`() =
        runToxicBlowpipeFletchingTest { deps ->
            for (dart in ToxicBlowpipeDart.entries) {
                preparePlayer()
                add(objs.toxic_blowpipe_empty)
                add(objs.zulrahs_scales, 25)
                val emptySlot = slotOf(objs.toxic_blowpipe_empty)
                val scaleSlot = slotOf(objs.zulrahs_scales)
                player.withProtectedAccess {
                    deps.heldU.interact(
                        this,
                        player.inv,
                        objTypes[objs.zulrahs_scales],
                        scaleSlot,
                        objTypes[objs.toxic_blowpipe_empty],
                        emptySlot,
                    )
                }
                add(dart.obj, 40)
                val loadedSlot = slotOf(objs.toxic_blowpipe)
                val dartSlot = slotOf(dart.obj)
                player.withProtectedAccess {
                    deps.heldU.interact(
                        this,
                        player.inv,
                        objTypes[objs.toxic_blowpipe],
                        loadedSlot,
                        objTypes[dart.obj],
                        dartSlot,
                    )
                }

                assertEquals(ToxicBlowpipeState(dart, dartCount = 40, scales = 25), loadedState())
                assertEquals(0, player.count(dart.obj))
                assertEquals(0, player.count(objs.zulrahs_scales))
            }
        }

    @Test
    fun GameTestState.`scale and dart capacity consume only what fits`() =
        runToxicBlowpipeFletchingTest { deps ->
            preparePlayer()
            add(objs.toxic_blowpipe_empty)
            add(objs.zulrahs_scales, 20_000)
            loadScales(deps)
            assertEquals(16_383, loadedState().scales)
            assertEquals(3_617, player.count(objs.zulrahs_scales))

            add(objs.rune_dart, 20_000)
            loadDarts(deps, ToxicBlowpipeDart.Rune)
            assertEquals(16_383, loadedState().dartCount)
            assertEquals(3_617, player.count(objs.rune_dart))

            loadScales(deps)
            loadDarts(deps, ToxicBlowpipeDart.Rune)
            assertEquals(3_617, player.count(objs.zulrahs_scales))
            assertEquals(3_617, player.count(objs.rune_dart))
        }

    @Test
    fun GameTestState.`a different dart type atomically returns the old darts`() =
        runToxicBlowpipeFletchingTest { deps ->
            preparePlayer()
            addBlowpipe(ToxicBlowpipeState(ToxicBlowpipeDart.Adamant, 500, scales = 900))
            add(objs.dragon_dart, 750)

            loadDarts(deps, ToxicBlowpipeDart.Dragon)

            assertEquals(
                ToxicBlowpipeState(ToxicBlowpipeDart.Dragon, 750, scales = 900),
                loadedState(),
            )
            assertEquals(500, player.count(objs.adamant_dart))
            assertEquals(0, player.count(objs.dragon_dart))
        }

    @Test
    fun GameTestState.`a full-inventory dart swap fails without losing either stack`() =
        runToxicBlowpipeFletchingTest { deps ->
            preparePlayer()
            val original = ToxicBlowpipeState(ToxicBlowpipeDart.Bronze, 500, scales = 900)
            addBlowpipe(original)
            add(objs.dragon_dart, 20_000)
            add(objs.chisel, 26)
            assertEquals(0, player.inv.freeSpace())

            loadDarts(deps, ToxicBlowpipeDart.Dragon)

            assertEquals(original, loadedState())
            assertEquals(20_000, player.count(objs.dragon_dart))
            assertEquals(0, player.count(objs.bronze_dart))
            assertMessageSent(
                "You don't have enough inventory space to swap the darts in the blowpipe."
            )
        }

    @Test
    fun GameTestState.`unload returns only darts and preserves scales`() =
        runToxicBlowpipeFletchingTest { deps ->
            preparePlayer()
            addBlowpipe(ToxicBlowpipeState(ToxicBlowpipeDart.Amethyst, 321, scales = 654))

            unloadDarts(deps)

            assertEquals(ToxicBlowpipeState(scales = 654), loadedState())
            assertEquals(321, player.count(objs.amethyst_dart))
        }

    @Test
    fun GameTestState.`unload converts a dart-only blowpipe back to empty`() =
        runToxicBlowpipeFletchingTest { deps ->
            preparePlayer()
            addBlowpipe(ToxicBlowpipeState(ToxicBlowpipeDart.Black, 321))

            unloadDarts(deps)

            assertEquals(1, player.count(objs.toxic_blowpipe_empty))
            assertEquals(321, player.count(objs.black_dart))
        }

    @Test
    fun GameTestState.`uncharge confirmation returns all resources`() =
        runToxicBlowpipeFletchingTest { deps ->
            preparePlayer()
            val state = ToxicBlowpipeState(ToxicBlowpipeDart.Rune, 321, scales = 654)
            addBlowpipe(state)
            player.withProtectedAccess {
                deps.lifecycle.uncharge(this, slotOf(objs.toxic_blowpipe))
            }
            confirmChoice()
            assertEquals(1, player.count(objs.toxic_blowpipe_empty))
            assertEquals(321, player.count(objs.rune_dart))
            assertEquals(654, player.count(objs.zulrahs_scales))
            assertEquals(2381, client.filterIsInstance<SynthSound>().single().id)
        }

    @Test
    fun GameTestState.`declining uncharge is inert`() = runToxicBlowpipeFletchingTest { deps ->
        preparePlayer()
        val state = ToxicBlowpipeState(ToxicBlowpipeDart.Rune, 321, scales = 654)
        addBlowpipe(state)
        player.withProtectedAccess { deps.lifecycle.uncharge(this, slotOf(objs.toxic_blowpipe)) }
        declineChoice()

        assertEquals(state, loadedState())
        assertEquals(0, player.count(objs.rune_dart))
        assertEquals(0, player.count(objs.zulrahs_scales))
    }

    @Test
    fun GameTestState.`uncharge capacity failure is atomic after confirmation`() =
        runToxicBlowpipeFletchingTest { deps ->
            preparePlayer()
            val state = ToxicBlowpipeState(ToxicBlowpipeDart.Rune, 321, scales = 654)
            addBlowpipe(state)
            add(objs.chisel, 27)
            player.withProtectedAccess {
                deps.lifecycle.uncharge(this, slotOf(objs.toxic_blowpipe))
            }
            confirmChoice()

            assertEquals(state, loadedState())
            assertEquals(0, player.count(objs.rune_dart))
            assertEquals(0, player.count(objs.zulrahs_scales))
            assertMessageSent(
                "You don't have enough inventory space to uncharge the toxic blowpipe."
            )
        }

    @Test
    fun GameTestState.`dismantle confirmation yields twenty thousand scales in a full inventory`() =
        runToxicBlowpipeFletchingTest { deps ->
            preparePlayer()
            add(objs.toxic_blowpipe_empty)
            add(objs.chisel, 27)
            player.withProtectedAccess {
                deps.lifecycle.dismantle(this, slotOf(objs.toxic_blowpipe_empty))
            }
            confirmChoice()

            assertEquals(0, player.count(objs.toxic_blowpipe_empty))
            assertEquals(20_000, player.count(objs.zulrahs_scales))
            assertEquals(0, player.inv.freeSpace())
        }

    @Test
    fun GameTestState.`ornament kit applies to an empty blowpipe in either held-use order`() =
        runToxicBlowpipeFletchingTest { deps ->
            for (reverse in listOf(false, true)) {
                preparePlayer()
                add(objs.toxic_blowpipe_empty)
                add(objs.trailblazer_reloaded_blowpipe_ornament_kit)
                if (reverse) {
                    add(objs.chisel, 26)
                    assertEquals(0, player.inv.freeSpace())
                }
                client.clearOutgoing()
                val blowpipeSlot = slotOf(objs.toxic_blowpipe_empty)
                val kitSlot = slotOf(objs.trailblazer_reloaded_blowpipe_ornament_kit)
                player.withProtectedAccess {
                    if (reverse) {
                        deps.heldU.interact(
                            this,
                            player.inv,
                            objTypes[objs.trailblazer_reloaded_blowpipe_ornament_kit],
                            kitSlot,
                            objTypes[objs.toxic_blowpipe_empty],
                            blowpipeSlot,
                        )
                    } else {
                        deps.heldU.interact(
                            this,
                            player.inv,
                            objTypes[objs.toxic_blowpipe_empty],
                            blowpipeSlot,
                            objTypes[objs.trailblazer_reloaded_blowpipe_ornament_kit],
                            kitSlot,
                        )
                    }
                }

                assertEquals(1, player.count(objs.blazing_blowpipe_empty))
                assertEquals(0, player.count(objs.toxic_blowpipe_empty))
                assertEquals(0, player.count(objs.trailblazer_reloaded_blowpipe_ornament_kit))
                assertEquals(2655, client.filterIsInstance<SynthSound>().single().id)
            }
        }

    @Test
    fun GameTestState.`ornament kit cannot be applied until the standard blowpipe is empty`() =
        runToxicBlowpipeFletchingTest { deps ->
            preparePlayer()
            val state = ToxicBlowpipeState(ToxicBlowpipeDart.Rune, 1, scales = 1)
            addBlowpipe(state)
            add(objs.trailblazer_reloaded_blowpipe_ornament_kit)

            player.withProtectedAccess {
                deps.lifecycle.applyOrnament(
                    this,
                    slotOf(objs.toxic_blowpipe),
                    slotOf(objs.trailblazer_reloaded_blowpipe_ornament_kit),
                )
            }

            assertEquals(state, loadedState())
            assertEquals(1, player.count(objs.trailblazer_reloaded_blowpipe_ornament_kit))
            assertEquals(0, player.count(objs.blazing_blowpipe_empty))
        }

    @Test
    fun GameTestState.`blazing blowpipe preserves its style through charge unload and uncharge`() =
        runToxicBlowpipeFletchingTest { deps ->
            preparePlayer()
            add(objs.blazing_blowpipe_empty)
            add(objs.zulrahs_scales, 654)
            loadScales(deps)
            assertEquals(ToxicBlowpipeState(scales = 654), loadedState(objs.blazing_blowpipe))

            add(objs.amethyst_dart, 321)
            loadDarts(deps, ToxicBlowpipeDart.Amethyst, objs.blazing_blowpipe)
            assertEquals(
                ToxicBlowpipeState(ToxicBlowpipeDart.Amethyst, 321, scales = 654),
                loadedState(objs.blazing_blowpipe),
            )

            unloadDarts(deps, objs.blazing_blowpipe)
            assertEquals(ToxicBlowpipeState(scales = 654), loadedState(objs.blazing_blowpipe))
            assertEquals(321, player.count(objs.amethyst_dart))

            player.withProtectedAccess {
                deps.lifecycle.uncharge(this, slotOf(objs.blazing_blowpipe))
            }
            confirmChoice()
            assertEquals(1, player.count(objs.blazing_blowpipe_empty))
            assertEquals(654, player.count(objs.zulrahs_scales))
            assertEquals(0, player.count(objs.toxic_blowpipe_empty))
        }

    @Test
    fun GameTestState.`dismantling an empty blazing blowpipe returns both components`() =
        runToxicBlowpipeFletchingTest { deps ->
            preparePlayer()
            add(objs.blazing_blowpipe_empty)
            player.withProtectedAccess {
                deps.lifecycle.dismantle(this, slotOf(objs.blazing_blowpipe_empty))
            }
            confirmChoice()

            assertEquals(1, player.count(objs.toxic_blowpipe_empty))
            assertEquals(1, player.count(objs.trailblazer_reloaded_blowpipe_ornament_kit))
            assertEquals(0, player.count(objs.blazing_blowpipe_empty))
            assertEquals(0, player.count(objs.zulrahs_scales))
        }

    @Test
    fun GameTestState.`full inventory blocks blazing dismantle without losing the item`() =
        runToxicBlowpipeFletchingTest { deps ->
            preparePlayer()
            add(objs.blazing_blowpipe_empty)
            add(objs.chisel, 27)
            player.withProtectedAccess {
                deps.lifecycle.dismantle(this, slotOf(objs.blazing_blowpipe_empty))
            }
            confirmChoice()

            assertEquals(1, player.count(objs.blazing_blowpipe_empty))
            assertEquals(0, player.count(objs.toxic_blowpipe_empty))
            assertEquals(0, player.count(objs.trailblazer_reloaded_blowpipe_ornament_kit))
            assertMessageSent(
                "You don't have enough inventory space to dismantle the blazing blowpipe."
            )
        }

    @Test
    fun GameTestState.`non-members cannot charge or dismantle a blowpipe`() =
        runToxicBlowpipeFletchingTest { deps ->
            preparePlayer()
            player.members = false
            add(objs.toxic_blowpipe_empty)
            add(objs.zulrahs_scales, 100)
            loadScales(deps)
            assertEquals(1, player.count(objs.toxic_blowpipe_empty))
            assertEquals(100, player.count(objs.zulrahs_scales))

            player.withProtectedAccess {
                deps.lifecycle.dismantle(this, slotOf(objs.toxic_blowpipe_empty))
            }
            assertEquals(1, player.count(objs.toxic_blowpipe_empty))
            assertEquals(100, player.count(objs.zulrahs_scales))
            assertMessageSent("You need to be on a members' world to use a toxic blowpipe.")
        }

    private fun GameTestScope.preparePlayer() {
        player.clearAllInvs()
        player.members = true
        player.stats[stats.fletching] = 99
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

    private fun GameTestScope.addBlowpipe(
        state: ToxicBlowpipeState,
        obj: ObjType = objs.toxic_blowpipe,
    ) {
        val slot = player.inv.indexOfFirst { it == null }
        require(slot >= 0)
        player.inv[slot] = InvObj(obj, vars = state.pack())
    }

    private fun GameTestScope.slotOf(obj: ObjType): Int =
        player.inv.indexOfFirst { it?.id == obj.id }.also { require(it >= 0) }

    private fun GameTestScope.loadedState(obj: ObjType = objs.toxic_blowpipe): ToxicBlowpipeState =
        ToxicBlowpipeState.unpack(player.inv[slotOf(obj)])

    private fun GameTestScope.loadScales(deps: Dependencies) {
        val blowpipeSlot =
            player.inv.indexOfFirst {
                it?.id == objs.toxic_blowpipe.id ||
                    it?.id == objs.toxic_blowpipe_empty.id ||
                    it?.id == objs.blazing_blowpipe.id ||
                    it?.id == objs.blazing_blowpipe_empty.id
            }
        val scaleSlot = slotOf(objs.zulrahs_scales)
        player.withProtectedAccess { deps.lifecycle.loadScales(this, blowpipeSlot, scaleSlot) }
    }

    private fun GameTestScope.loadDarts(
        deps: Dependencies,
        dart: ToxicBlowpipeDart,
        blowpipe: ObjType = objs.toxic_blowpipe,
    ) {
        val blowpipeSlot = slotOf(blowpipe)
        val dartSlot = slotOf(dart.obj)
        player.withProtectedAccess { deps.lifecycle.loadDarts(this, blowpipeSlot, dartSlot, dart) }
    }

    private fun GameTestScope.unloadDarts(
        deps: Dependencies,
        blowpipe: ObjType = objs.toxic_blowpipe,
    ) {
        val blowpipeSlot = slotOf(blowpipe)
        player.withProtectedAccess { deps.lifecycle.unloadDarts(this, blowpipeSlot) }
    }

    private fun GameTestScope.confirmChoice() {
        player.resumeActiveCoroutine(
            ResumePauseButtonInput(components.chatmenu_pbutton, subcomponent = 1)
        )
    }

    private fun GameTestScope.declineChoice() {
        player.resumeActiveCoroutine(
            ResumePauseButtonInput(components.chatmenu_pbutton, subcomponent = 2)
        )
    }

    private fun GameTestState.runToxicBlowpipeFletchingTest(
        body: GameTestScope.(Dependencies) -> Unit
    ) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            ToxicBlowpipeFletchingScript::class,
            testBody = body,
        )

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(ToxicBlowpipeFletchingModule())
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val fletching: ToxicBlowpipeFletching,
        val lifecycle: ToxicBlowpipeLifecycle,
        val heldU: HeldUInteractions,
    )
}
