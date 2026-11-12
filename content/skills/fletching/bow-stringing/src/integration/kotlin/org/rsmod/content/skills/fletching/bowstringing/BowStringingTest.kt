package org.rsmod.content.skills.fletching.bowstringing

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import net.rsprot.protocol.game.outgoing.interfaces.IfOpenSub
import net.rsprot.protocol.game.outgoing.interfaces.IfSetEventsV2
import net.rsprot.protocol.game.outgoing.misc.player.RunClientScript
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.HeldInteractions
import org.rsmod.api.player.interact.HeldUInteractions
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.interact.HeldOp
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.ui.UserInterface

@Execution(ExecutionMode.SAME_THREAD)
class BowStringingTest {
    @Test
    fun GameTestState.`cache 233 recipe table pins all standard bows levels xp and sequences`() =
        runBowStringingTest { deps ->
            assertEquals(
                listOf(
                    "Shortbow:5:50:841:5.0:6678",
                    "Longbow:10:48:839:10.0:6684",
                    "Oak shortbow:20:54:843:16.5:6679",
                    "Oak longbow:25:56:845:25.0:6685",
                    "Willow shortbow:35:60:849:33.2:6680",
                    "Willow longbow:40:58:847:41.5:6686",
                    "Maple shortbow:50:64:853:50.0:6681",
                    "Maple longbow:55:62:851:58.2:6687",
                    "Yew shortbow:65:68:857:67.5:6682",
                    "Yew longbow:70:66:855:75.0:6688",
                    "Magic shortbow:80:72:861:83.2:6683",
                    "Magic longbow:85:70:859:91.5:6689",
                ),
                deps.recipes.recipes
                    .filterNot { it.requiresCompOgreBowUnlock }
                    .map {
                        "${it.name}:${it.level}:${it.unstrung.id}:${it.output.id}:${it.xp}:" +
                            it.sequence.id
                    },
            )
            assertEquals(1777, objs.bow_string.id)
        }

    @Test
    fun GameTestState.`cache 233 pins composite ogre bow stringing contract`() =
        runBowStringingTest { deps ->
            val recipe = requireNotNull(deps.recipes.find(objs.unstrung_comp_ogre_bow))
            assertEquals("Comp ogre bow", recipe.name)
            assertEquals(30, recipe.level)
            assertEquals(4825, recipe.unstrung.id)
            assertEquals(4827, recipe.output.id)
            assertEquals(45.0, recipe.xp)
            assertEquals(4433, recipe.sequence.id)
            assertTrue(recipe.requiresCompOgreBowUnlock)
            assertEquals(500, varbits.comp_ogre_bow_unlocked.id)
        }

    @Test
    fun GameTestState.`composite ogre bow strings with loose string and spool after two ticks`() =
        runBowStringingTest { deps ->
            val recipe = requireNotNull(deps.recipes.find(objs.unstrung_comp_ogre_bow))

            preparePlayer()
            add(recipe.unstrung)
            add(objs.bow_string)
            val looseStartXp = player.statMap.getFineXP(stats.fletching)
            player.withProtectedAccess { deps.stringing.start(this, recipe, quantity = 1) }
            assertEquals(4433, player.pendingSequence.id)
            advance()
            assertEquals(0, player.count(recipe.output))
            advance()
            assertEquals(1, player.count(recipe.output))
            assertEquals(0, player.count(recipe.unstrung))
            assertEquals(0, player.count(objs.bow_string))
            assertEquals(450, player.statMap.getFineXP(stats.fletching) - looseStartXp)

            preparePlayer()
            add(recipe.unstrung)
            add(objs.bow_string_spool)
            player.setVarBit(varbits.bow_string_spool_charges, 1)
            val spoolStartXp = player.statMap.getFineXP(stats.fletching)
            player.withProtectedAccess {
                deps.stringing.start(this, recipe, quantity = 1, BowStringSource.Spool)
            }
            advance(2)
            assertEquals(1, player.count(recipe.output))
            assertEquals(0, player.vars[varbits.bow_string_spool_charges])
            assertEquals(450, player.statMap.getFineXP(stats.fletching) - spoolStartXp)
        }

    @Test
    fun GameTestState
        .`composite ogre bow enforces unlock and supports reverse full inventory use`() =
        runBowStringingTest { deps ->
            val recipe = requireNotNull(deps.recipes.find(objs.unstrung_comp_ogre_bow))

            preparePlayer()
            player.setVarBit(varbits.comp_ogre_bow_unlocked, 0)
            add(recipe.unstrung)
            add(objs.bow_string)
            player.withProtectedAccess { deps.stringing.start(this, recipe, quantity = 1) }
            assertMessageSent("You're not sure how you would go about making that item.")
            assertEquals(1, player.count(recipe.unstrung))
            assertEquals(1, player.count(objs.bow_string))
            assertEquals(0, player.count(recipe.output))

            preparePlayer()
            player.stats[stats.fletching] = 29
            add(recipe.unstrung)
            add(objs.bow_string)
            player.withProtectedAccess { deps.stringing.start(this, recipe, quantity = 1) }
            assertMessageSent("You need a Fletching level of 30 to string this bow.")
            assertEquals(1, player.count(recipe.unstrung))
            assertEquals(1, player.count(objs.bow_string))

            preparePlayer()
            add(recipe.unstrung, 14)
            add(objs.bow_string, 14)
            val stringSlot = player.inv.indexOfFirst { it?.id == objs.bow_string.id }
            val bowSlot = player.inv.indexOfFirst { it?.id == recipe.unstrung.id }
            assertEquals(0, player.inv.freeSpace())
            player.withProtectedAccess {
                deps.heldU.interact(
                    this,
                    player.inv,
                    objTypes[recipe.unstrung],
                    bowSlot,
                    objTypes[objs.bow_string],
                    stringSlot,
                )
            }
            resumeSkillMulti(quantity = 1)
            advance(2)
            assertEquals(1, player.count(recipe.output))
            assertEquals(13, player.count(recipe.unstrung))
            assertEquals(13, player.count(objs.bow_string))
            assertEquals(1, player.inv.freeSpace())
        }

    @Test
    fun GameTestState.`bow and string open native one-product quantity menu`() =
        runBowStringingTest { deps ->
            preparePlayer()
            add(objs.unstrung_yew_longbow, 2)
            add(objs.bow_string, 3)
            val recipe = requireNotNull(deps.recipes.find(objs.unstrung_yew_longbow))

            player.withProtectedAccess { deps.stringing.select(this, recipe) }

            assertTrue(client.filterIsInstance<IfOpenSub>().any { it.interfaceId == 270 })
            val setup = client.filterIsInstance<RunClientScript>().single { it.id == 2046 }
            assertEquals(
                listOf<Any>(
                    0,
                    2,
                    objs.yew_longbow.id,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    2,
                    "How many would you like to make?",
                ),
                setup.values,
            )
            val events =
                client.filterIsInstance<IfSetEventsV2>().single {
                    it.interfaceId == 270 &&
                        it.componentId == (components.skillmulti_a.id and 0xFFFF)
                }
            assertEquals(1, events.start)
            assertEquals(2, events.end)
        }

    @Test
    fun GameTestState.`every registered bow transforms atomically with exact xp`() =
        runBowStringingTest { deps ->
            for (recipe in deps.recipes.recipes) {
                preparePlayer()
                add(recipe.unstrung)
                add(objs.bow_string)
                val startXp = player.statMap.getFineXP(stats.fletching)

                player.withProtectedAccess {
                    deps.stringing.process(
                        this,
                        BowStringingRequest(recipe.unstrung, remaining = 1),
                    )
                }

                assertEquals(0, player.count(recipe.unstrung), recipe.name)
                assertEquals(0, player.count(objs.bow_string), recipe.name)
                assertEquals(1, player.count(recipe.output), recipe.name)
                assertEquals(
                    (recipe.xp * 10).toInt(),
                    player.statMap.getFineXP(stats.fletching) - startXp,
                    recipe.name,
                )
            }
        }

    @Test
    fun GameTestState.`membership level bow and string failures are atomic`() =
        runBowStringingTest { deps ->
            val magic = requireNotNull(deps.recipes.find(objs.unstrung_magic_longbow))

            preparePlayer()
            player.members = false
            add(magic.unstrung)
            add(objs.bow_string)
            player.withProtectedAccess { deps.stringing.start(this, magic, quantity = 1) }
            assertMessageSent("You need to be on a members' world to train Fletching.")
            assertEquals(1, player.count(magic.unstrung))

            preparePlayer()
            player.stats[stats.fletching] = 84
            add(magic.unstrung)
            add(objs.bow_string)
            player.withProtectedAccess { deps.stringing.start(this, magic, quantity = 1) }
            assertMessageSent("You need a Fletching level of 85 to string this bow.")
            assertEquals(1, player.count(magic.unstrung))

            preparePlayer()
            add(objs.bow_string)
            player.withProtectedAccess { deps.stringing.start(this, magic, quantity = 1) }
            assertMessageSent("You do not have an unstrung bow to string.")
            assertEquals(1, player.count(objs.bow_string))

            preparePlayer()
            add(magic.unstrung)
            player.withProtectedAccess { deps.stringing.start(this, magic, quantity = 1) }
            assertMessageSent("You need a bow string to string this bow.")
            assertEquals(1, player.count(magic.unstrung))
        }

    @Test
    fun GameTestState.`bow stringing produces one result every two ticks`() =
        runBowStringingTest { deps ->
            preparePlayer()
            add(objs.unstrung_maple_shortbow, 3)
            add(objs.bow_string, 3)
            val recipe = requireNotNull(deps.recipes.find(objs.unstrung_maple_shortbow))

            player.withProtectedAccess { deps.stringing.start(this, recipe, quantity = 3) }
            assertEquals(recipe.sequence.id, player.pendingSequence.id)
            advance()
            assertEquals(0, player.count(recipe.output))
            advance()
            assertEquals(1, player.count(recipe.output))
            advance()
            assertEquals(1, player.count(recipe.output))
            advance()
            assertEquals(2, player.count(recipe.output))
            advance(2)
            assertEquals(3, player.count(recipe.output))
            assertEquals(0, player.weakQueueList.count(fletching_queues.bowStringing))
        }

    @Test
    fun GameTestState.`material loss interrupts the batch without partial mutation`() =
        runBowStringingTest { deps ->
            preparePlayer()
            add(objs.unstrung_oak_longbow, 2)
            add(objs.bow_string, 2)
            val recipe = requireNotNull(deps.recipes.find(objs.unstrung_oak_longbow))

            player.withProtectedAccess { deps.stringing.start(this, recipe, quantity = 2) }
            advance(2)
            assertEquals(1, player.count(recipe.output))
            player.inv[player.inv.indexOfFirst { it?.id == objs.bow_string.id }] = null
            advance(2)

            assertEquals(1, player.count(recipe.output))
            assertEquals(1, player.count(recipe.unstrung))
            assertEquals(0, player.weakQueueList.count(fletching_queues.bowStringing))
        }

    @Test
    fun GameTestState.`full inventory replacement and reverse held-use event are supported`() =
        runBowStringingTest { deps ->
            preparePlayer()
            add(objs.unstrung_shortbow, 14)
            add(objs.bow_string, 14)
            assertEquals(0, player.inv.freeSpace())
            val recipe = requireNotNull(deps.recipes.find(objs.unstrung_shortbow))
            player.withProtectedAccess { deps.stringing.start(this, recipe, quantity = 1) }
            advance(2)
            assertEquals(1, player.count(objs.shortbow))

            preparePlayer()
            add(objs.bow_string)
            add(objs.unstrung_shortbow)
            val stringSlot = player.inv.indexOfFirst { it?.id == objs.bow_string.id }
            val bowSlot = player.inv.indexOfFirst { it?.id == objs.unstrung_shortbow.id }
            player.withProtectedAccess {
                deps.heldU.interact(
                    this,
                    player.inv,
                    objTypes[objs.unstrung_shortbow],
                    bowSlot,
                    objTypes[objs.bow_string],
                    stringSlot,
                )
            }
            resumeSkillMulti(quantity = 1)
            advance(2)
            assertEquals(1, player.count(objs.shortbow))
        }

    @Test
    fun GameTestState.`cache 233 pins bow string spool object and persistent state`() =
        runBowStringingTest {
            assertEquals(1778, objs.noted_bow_string.id)
            assertEquals(31052, objs.bow_string_spool.id)
            assertEquals(
                listOf("Fill", "Check", "Empty", null, null),
                objTypes[objs.bow_string_spool].iop.toList(),
            )
            assertEquals(17291, varbits.bow_string_spool_charges.id)
            assertEquals(17293, varbits.bow_string_spool_size.id)
            assertEquals(17610, varbits.bow_string_spool_charges_shown.id)
            assertEquals(17773, varbits.bow_string_spool_disabled.id)
        }

    @Test
    fun GameTestState.`spool fill respects capacity and charges persist when item is lost`() =
        runBowStringingTest { deps ->
            preparePlayer()
            add(objs.bow_string_spool)
            add(objs.bow_string, 3)

            player.withProtectedAccess { assertEquals(3, deps.spool.fill(this)) }
            assertEquals(0, player.count(objs.bow_string))
            player.clearAllInvs()
            player.withProtectedAccess { assertEquals(3, deps.spool.charges(this)) }

            add(objs.bow_string_spool)
            add(objs.bow_string, 3)
            player.setVarBit(varbits.bow_string_spool_charges, 249)
            player.withProtectedAccess { assertEquals(1, deps.spool.fill(this)) }
            assertEquals(2, player.count(objs.bow_string))
            player.withProtectedAccess {
                assertEquals(250, deps.spool.charges(this))
                assertEquals(250, deps.spool.capacity(this))
                deps.held.interact(
                    this,
                    player.inv,
                    player.inv.indexOfFirst { it?.id == objs.bow_string_spool.id },
                    HeldOp.Op2,
                )
            }
            assertMessageSent("Your bow string spool contains 250 bow strings and can hold 250.")
        }

    @Test
    fun GameTestState.`combining spools unlocks every historical capacity tier`() =
        runBowStringingTest { deps ->
            preparePlayer()
            add(objs.bow_string_spool)
            for ((expectedSize, expectedCapacity) in
                listOf(1 to 500, 2 to 2_000, 3 to 5_000, 4 to 10_000, 5 to 100_000)) {
                add(objs.bow_string_spool)
                player.withProtectedAccess {
                    if (expectedSize == 1) {
                        deps.heldU.interact(
                            this,
                            player.inv,
                            objTypes[objs.bow_string_spool],
                            player.inv.indexOfFirst { it?.id == objs.bow_string_spool.id },
                            objTypes[objs.bow_string_spool],
                            player.inv.indexOfLast { it?.id == objs.bow_string_spool.id },
                        )
                    } else {
                        assertTrue(deps.spool.combine(this))
                    }
                }
                assertEquals(1, player.count(objs.bow_string_spool))
                player.withProtectedAccess {
                    assertEquals(expectedSize, deps.spool.size(this))
                    assertEquals(expectedCapacity, deps.spool.capacity(this))
                }
            }

            add(objs.bow_string_spool)
            player.withProtectedAccess { assertFalse(deps.spool.combine(this)) }
            assertEquals(2, player.count(objs.bow_string_spool))
            assertMessageSent("Your bow string spool is already at its maximum capacity.")
        }

    @Test
    fun GameTestState.`loose and noted string use-on-spool deposits exact quantities`() =
        runBowStringingTest { deps ->
            preparePlayer()
            add(objs.bow_string_spool)
            add(objs.bow_string, 2)
            add(objs.noted_bow_string, 4)

            val spoolSlot = player.inv.indexOfFirst { it?.id == objs.bow_string_spool.id }
            val looseSlot = player.inv.indexOfFirst { it?.id == objs.bow_string.id }
            player.withProtectedAccess {
                deps.heldU.interact(
                    this,
                    player.inv,
                    objTypes[objs.bow_string_spool],
                    spoolSlot,
                    objTypes[objs.bow_string],
                    looseSlot,
                )
            }
            assertEquals(0, player.count(objs.bow_string))
            val notedSlot = player.inv.indexOfFirst { it?.id == objs.noted_bow_string.id }
            player.withProtectedAccess {
                deps.heldU.interact(
                    this,
                    player.inv,
                    objTypes[objs.noted_bow_string],
                    notedSlot,
                    objTypes[objs.bow_string_spool],
                    spoolSlot,
                )
                assertEquals(6, deps.spool.charges(this))
            }
            assertEquals(0, player.count(objs.noted_bow_string))
        }

    @Test
    fun GameTestState.`spool strings bows every two ticks and consumes account charges`() =
        runBowStringingTest { deps ->
            preparePlayer()
            add(objs.bow_string_spool)
            add(objs.unstrung_willow_shortbow, 3)
            player.setVarBit(varbits.bow_string_spool_charges, 2)
            val recipe = requireNotNull(deps.recipes.find(objs.unstrung_willow_shortbow))

            player.withProtectedAccess {
                deps.stringing.start(this, recipe, quantity = 3, BowStringSource.Spool)
            }
            advance()
            assertEquals(0, player.count(recipe.output))
            advance()
            assertEquals(1, player.count(recipe.output))
            advance(2)
            assertEquals(2, player.count(recipe.output))
            assertEquals(1, player.count(recipe.unstrung))
            assertEquals(0, player.vars[varbits.bow_string_spool_charges])
            assertEquals(0, player.weakQueueList.count(fletching_queues.bowStringing))
        }

    @Test
    fun GameTestState.`held spool and bow opens quantity menu and empty spool is inert`() =
        runBowStringingTest { deps ->
            preparePlayer()
            add(objs.bow_string_spool)
            add(objs.unstrung_shortbow, 2)
            player.setVarBit(varbits.bow_string_spool_charges, 1)
            val spoolSlot = player.inv.indexOfFirst { it?.id == objs.bow_string_spool.id }
            val bowSlot = player.inv.indexOfFirst { it?.id == objs.unstrung_shortbow.id }
            player.withProtectedAccess {
                deps.heldU.interact(
                    this,
                    player.inv,
                    objTypes[objs.unstrung_shortbow],
                    bowSlot,
                    objTypes[objs.bow_string_spool],
                    spoolSlot,
                )
            }
            val setup = client.filterIsInstance<RunClientScript>().single { it.id == 2046 }
            assertEquals(1, setup.values[1])
            resumeSkillMulti(quantity = 1)
            advance(2)
            assertEquals(1, player.count(objs.shortbow))
            assertEquals(0, player.vars[varbits.bow_string_spool_charges])

            client.clear()
            val remainingBow = player.inv.indexOfFirst { it?.id == objs.unstrung_shortbow.id }
            player.withProtectedAccess {
                deps.heldU.interact(
                    this,
                    player.inv,
                    objTypes[objs.bow_string_spool],
                    spoolSlot,
                    objTypes[objs.unstrung_shortbow],
                    remainingBow,
                )
            }
            assertTrue(client.filterIsInstance<IfOpenSub>().isEmpty())
            assertEquals(0, player.weakQueueList.count(fletching_queues.bowStringing))
        }

    @Test
    fun GameTestState.`spool fill and combine enforce membership`() = runBowStringingTest { deps ->
        preparePlayer()
        player.members = false
        add(objs.bow_string_spool, 2)
        add(objs.bow_string)
        val spoolSlot = player.inv.indexOfFirst { it?.id == objs.bow_string_spool.id }

        player.withProtectedAccess {
            deps.held.interact(this, player.inv, spoolSlot, HeldOp.Op1)
            assertFalse(deps.spool.combine(this))
        }
        assertEquals(1, player.count(objs.bow_string))
        assertEquals(2, player.count(objs.bow_string_spool))
        assertEquals(0, player.vars[varbits.bow_string_spool_charges])
        assertMessageSent("You need to be on a members' world to use the bow string spool.")
    }

    private fun GameTestScope.preparePlayer() {
        player.clearAllInvs()
        player.members = true
        player.stats[stats.fletching] = 99
        player.setVarBit(varbits.comp_ogre_bow_unlocked, 1)
    }

    private fun GameTestScope.add(obj: ObjType, count: Int = 1) {
        if (objTypes[obj].isStackable) {
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

    private fun GameTestScope.resumeSkillMulti(quantity: Int) {
        val target =
            requireNotNull(player.ui.modals.getComponent(UserInterface(interfaces.skillmulti.id)))
        player.ui.queueClose(target)
        player.resumeActiveCoroutine(
            ResumePauseButtonInput(components.skillmulti_a, subcomponent = quantity)
        )
    }

    private fun GameTestState.runBowStringingTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            BowStringingScript::class,
            testBody = body,
        )

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(BowStringingModule())
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val stringing: BowStringing,
        val spool: BowStringSpool,
        val recipes: BowStringingRecipeRegistry,
        val held: HeldInteractions,
        val heldU: HeldUInteractions,
    )
}
