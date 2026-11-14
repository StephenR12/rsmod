package org.rsmod.content.skills.runecraft

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.invs
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.back
import org.rsmod.api.player.events.interact.HeldBanksideEvents
import org.rsmod.api.player.events.interact.HeldDropEvents
import org.rsmod.api.player.interact.HeldInteractions
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.interact.HeldOp
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjType

@Execution(ExecutionMode.SAME_THREAD)
class EssencePouchTest {
    @Test
    fun GameTestState.`cache 233 pins pouch objects native state and bankside conditions`() =
        runPouchTest {
            val expectedIds = listOf(5509, 5510, 5511, 5512, 5513, 5514, 5515, 26784, 26786)
            val variants = EssencePouchDefinitions.all.flatMap(EssencePouchDefinition::variants)
            assertEquals(expectedIds, variants.map(ObjType::id))
            for (variant in variants) {
                val type = objTypes[variant]
                assertTrue(type.members)
                assertEquals(listOf("Fill", "Check", "Empty", null, null), type.iop.toList())
            }

            val amountVars =
                listOf(
                    RunecraftVarbits.smallPouchAmount,
                    RunecraftVarbits.mediumPouchAmount,
                    RunecraftVarbits.largePouchAmount,
                    RunecraftVarbits.giantPouchAmount,
                    RunecraftVarbits.colossalPouchAmount,
                )
            assertEquals(
                listOf(0..2, 3..8, 9..17, 18..29, 0..5),
                amountVars.map { cacheTypes.varbits[it].bits },
            )
            assertEquals(
                listOf(486, 486, 486, 486, 3393),
                amountVars.map { cacheTypes.varbits[it].baseVar.id },
            )
            val typeVars =
                listOf(
                    RunecraftVarbits.smallPouchType,
                    RunecraftVarbits.mediumPouchType,
                    RunecraftVarbits.largePouchType,
                    RunecraftVarbits.giantPouchType,
                    RunecraftVarbits.colossalPouchType,
                )
            assertEquals(
                listOf(0..2, 3..5, 6..8, 9..11, 12..14),
                typeVars.map { cacheTypes.varbits[it].bits },
            )
            assertEquals(List(5) { 720 }, typeVars.map { cacheTypes.varbits[it].baseVar.id })
            assertEquals(0..9, cacheTypes.varbits[RunecraftVarbits.colossalPouchDegradation].bits)

            for ((bit, pouch) in EssencePouchDefinitions.all.withIndex()) {
                for (variant in pouch.variants) {
                    val type = objTypes[variant]
                    assertEquals(bit, type.param(params.bankside_extraop_bit))
                    assertEquals(amountVars[bit].id, type.param(params.bankside_extraop_varbit).id)
                }
            }
        }

    @Test
    fun GameTestState.`cache scripts capacity boundaries are encoded exactly`() = runPouchTest {
        val medium = EssencePouchDefinitions.medium
        assertEquals(
            listOf(6, 6, 3, 3, 0),
            listOf(0, 399, 400, 799, 800).map { medium.capacity(99, it) },
        )
        val large = EssencePouchDefinitions.large
        assertEquals(
            listOf(9, 7, 5, 3, 0),
            listOf(399, 400, 600, 800, 1000).map { large.capacity(99, it) },
        )
        val giant = EssencePouchDefinitions.giant
        assertEquals(
            listOf(12, 9, 8, 7, 6, 5, 3, 0),
            listOf(199, 200, 300, 400, 600, 800, 1000, 1200).map { giant.capacity(99, it) },
        )
        val colossal = EssencePouchDefinitions.colossal
        assertEquals(
            listOf(40, 35, 30, 25, 20, 15, 10, 5, 0),
            listOf(319, 320, 565, 745, 870, 950, 995, 1015, 1020).map { colossal.capacity(85, it) },
        )
        assertEquals(listOf(8, 16, 27, 40), listOf(25, 50, 75, 85).map { colossal.capacity(it, 0) })
        assertEquals(
            listOf(7, 14, 23, 35),
            listOf(25, 50, 75, 85).map { colossal.capacity(it, 320) },
        )
    }

    @Test
    fun GameTestState.`held fill and empty preserve every cache essence type`() =
        runPouchTest { deps ->
            for (essence in EssenceType.entries) {
                preparePlayer(level = 99)
                add(RunecraftObjs.smallPouch)
                repeat(3) { add(essence.obj) }
                val pouchSlot = player.inv.indexOfFirst { it?.id == RunecraftObjs.smallPouch.id }

                player.withProtectedAccess {
                    deps.held.interact(this, player.inv, pouchSlot, HeldOp.Op1)
                }
                assertEquals(3, deps.storage.count(player, EssencePouchDefinitions.small))
                assertEquals(
                    essence,
                    deps.storage.essenceType(player, EssencePouchDefinitions.small),
                )
                assertEquals(0, player.count(essence.obj))

                player.withProtectedAccess {
                    deps.held.interact(this, player.inv, pouchSlot, HeldOp.Op3)
                }
                assertEquals(0, deps.storage.count(player, EssencePouchDefinitions.small))
                assertNull(deps.storage.essenceType(player, EssencePouchDefinitions.small))
                assertEquals(3, player.count(essence.obj))
            }
        }

    @Test
    fun GameTestState.`membership base level capacity and mixed type gates are atomic`() =
        runPouchTest { deps ->
            preparePlayer(level = 99)
            add(RunecraftObjs.mediumPouch)
            add(RunecraftObjs.runeEssence)
            val slot = player.inv.indexOfFirst { it?.id == RunecraftObjs.mediumPouch.id }

            player.members = false
            player.withProtectedAccess { deps.held.interact(this, player.inv, slot, HeldOp.Op1) }
            assertEquals(0, deps.storage.count(player, EssencePouchDefinitions.medium))
            assertEquals(1, player.count(RunecraftObjs.runeEssence))
            assertMessageSent(EssencePouchStorage.MEMBERS_MESSAGE)

            player.members = true
            player.stats[stats.runecrafting] = 24
            player.withProtectedAccess { deps.held.interact(this, player.inv, slot, HeldOp.Op1) }
            assertEquals(0, deps.storage.count(player, EssencePouchDefinitions.medium))
            assertMessageSent("You need a Runecraft level of 25 to use this pouch.")

            player.stats[stats.runecrafting] = 25
            deps.storage.setState(
                player,
                EssencePouchDefinitions.medium,
                count = 1,
                essence = EssenceType.Pure,
            )
            player.withProtectedAccess { deps.held.interact(this, player.inv, slot, HeldOp.Op1) }
            assertEquals(1, deps.storage.count(player, EssencePouchDefinitions.medium))
            assertEquals(1, player.count(RunecraftObjs.runeEssence))
            assertMessageSent(EssencePouchStorage.NO_ESSENCE_MESSAGE)
        }

    @Test
    fun GameTestState
        .`ordinary degradation is random per stored essence and changes object at threshold`() =
        runPouchTest { deps ->
            preparePlayer(level = 99)
            add(RunecraftObjs.mediumPouch)
            repeat(3) { add(RunecraftObjs.runeEssence) }
            val slot = player.inv.indexOfFirst { it?.id == RunecraftObjs.mediumPouch.id }
            deps.storage.setState(
                player,
                EssencePouchDefinitions.medium,
                count = 0,
                essence = null,
                degradation = 399,
            )
            random.next = 1
            random.then = 0
            random.then = 0

            player.withProtectedAccess { deps.held.interact(this, player.inv, slot, HeldOp.Op1) }

            assertEquals(3, deps.storage.count(player, EssencePouchDefinitions.medium))
            assertEquals(400, deps.storage.degradation(player, EssencePouchDefinitions.medium))
            assertEquals(RunecraftObjs.mediumPouchDegraded.id, player.inv[slot]?.id)
            assertEquals(0 until 4, random.lastInclusiveRange)
        }

    @Test
    fun GameTestState
        .`colossal degradation is deterministic and runecraft capes prevent all decay`() =
        runPouchTest { deps ->
            preparePlayer(level = 85)
            add(RunecraftObjs.colossalPouch)
            add(RunecraftObjs.guardianEssence)
            val colossalSlot = player.inv.indexOfFirst { it?.id == RunecraftObjs.colossalPouch.id }
            deps.storage.setState(
                player,
                EssencePouchDefinitions.colossal,
                count = 0,
                essence = null,
                degradation = 319,
            )

            player.withProtectedAccess {
                deps.held.interact(this, player.inv, colossalSlot, HeldOp.Op1)
            }
            assertEquals(320, deps.storage.degradation(player, EssencePouchDefinitions.colossal))
            assertEquals(RunecraftObjs.colossalPouchDegraded.id, player.inv[colossalSlot]?.id)
            assertNull(random.lastInclusiveRange)

            val maxCapeVariant =
                objTypes.values.first { it.internalName == "skillcape_max_firecape" }
            val protectiveCapes =
                listOf(
                    objTypes[objs.runecrafting_skillcape],
                    objTypes[objs.runecrafting_skillcape_t],
                    objTypes[objs.max_cape_worn],
                    maxCapeVariant,
                )
            for (cape in protectiveCapes) {
                preparePlayer(level = 99)
                add(RunecraftObjs.mediumPouch)
                repeat(6) { add(RunecraftObjs.pureEssence) }
                player.back = InvObj(cape)
                val mediumSlot = player.inv.indexOfFirst { it?.id == RunecraftObjs.mediumPouch.id }
                deps.storage.setState(
                    player,
                    EssencePouchDefinitions.medium,
                    count = 0,
                    essence = null,
                    degradation = 399,
                )
                player.withProtectedAccess {
                    deps.held.interact(this, player.inv, mediumSlot, HeldOp.Op1)
                }
                assertEquals(6, deps.storage.count(player, EssencePouchDefinitions.medium))
                assertEquals(399, deps.storage.degradation(player, EssencePouchDefinitions.medium))
                assertEquals(RunecraftObjs.mediumPouch.id, player.inv[mediumSlot]?.id)
            }
        }

    @Test
    fun GameTestState.`emptying is partial when inventory fills and only clears type at zero`() =
        runPouchTest { deps ->
            preparePlayer(level = 99)
            add(RunecraftObjs.largePouch)
            repeat(26) { add(objs.bones) }
            deps.storage.setState(
                player,
                EssencePouchDefinitions.large,
                count = 5,
                essence = EssenceType.Pure,
            )
            val slot = player.inv.indexOfFirst { it?.id == RunecraftObjs.largePouch.id }

            player.withProtectedAccess { deps.held.interact(this, player.inv, slot, HeldOp.Op3) }
            assertEquals(1, player.count(RunecraftObjs.pureEssence))
            assertEquals(4, deps.storage.count(player, EssencePouchDefinitions.large))
            assertEquals(
                EssenceType.Pure,
                deps.storage.essenceType(player, EssencePouchDefinitions.large),
            )

            for (index in player.inv.indices) {
                if (player.inv[index]?.id == objs.bones.id) {
                    player.inv[index] = null
                }
            }
            player.withProtectedAccess { deps.held.interact(this, player.inv, slot, HeldOp.Op3) }
            assertEquals(5, player.count(RunecraftObjs.pureEssence))
            assertEquals(0, deps.storage.count(player, EssencePouchDefinitions.large))
            assertNull(deps.storage.essenceType(player, EssencePouchDefinitions.large))
        }

    @Test
    fun GameTestState.`repair restores carried and banked variants without deleting contents`() =
        runPouchTest { deps ->
            preparePlayer(level = 99)
            add(RunecraftObjs.mediumPouchDegraded)
            val bank = player.invMap.getOrPut(cacheTypes.invs[invs.bank])
            bank[0] = InvObj(RunecraftObjs.largePouchDegraded)
            deps.storage.setState(player, EssencePouchDefinitions.medium, 3, EssenceType.Rune, 500)
            deps.storage.setState(player, EssencePouchDefinitions.large, 5, EssenceType.Pure, 700)

            player.withProtectedAccess { assertEquals(4, deps.storage.repairAll(this)) }

            assertTrue(player.inv.contains(RunecraftObjs.mediumPouch))
            assertTrue(bank.contains(RunecraftObjs.largePouch))
            assertEquals(0, deps.storage.degradation(player, EssencePouchDefinitions.medium))
            assertEquals(0, deps.storage.degradation(player, EssencePouchDefinitions.large))
            assertEquals(3, deps.storage.count(player, EssencePouchDefinitions.medium))
            assertEquals(
                EssenceType.Rune,
                deps.storage.essenceType(player, EssencePouchDefinitions.medium),
            )
            assertEquals(5, deps.storage.count(player, EssencePouchDefinitions.large))
            assertEquals(
                EssenceType.Pure,
                deps.storage.essenceType(player, EssencePouchDefinitions.large),
            )
        }

    @Test
    fun GameTestState.`bankside operation dynamically fills then empties through held inventory`() =
        runPouchTest { deps ->
            preparePlayer(level = 99)
            add(RunecraftObjs.smallPouch)
            repeat(3) { add(RunecraftObjs.runeEssence) }
            val slot = player.inv.indexOfFirst { it?.id == RunecraftObjs.smallPouch.id }

            assertTrue(
                eventBus.publish(
                    HeldBanksideEvents.Type(player, slot, objTypes[RunecraftObjs.smallPouch])
                )
            )
            advance()
            assertEquals(3, deps.storage.count(player, EssencePouchDefinitions.small))
            assertEquals(0, player.count(RunecraftObjs.runeEssence))

            assertTrue(
                eventBus.publish(
                    HeldBanksideEvents.Type(player, slot, objTypes[RunecraftObjs.smallPouch])
                )
            )
            advance()
            assertEquals(0, deps.storage.count(player, EssencePouchDefinitions.small))
            assertEquals(3, player.count(RunecraftObjs.runeEssence))
        }

    @Test
    fun GameTestState.`drop clears contents and final decay destroys the pouch`() =
        runPouchTest { deps ->
            preparePlayer(level = 99)
            add(RunecraftObjs.mediumPouch)
            deps.storage.setState(player, EssencePouchDefinitions.medium, 3, EssenceType.Rune, 500)
            assertTrue(
                eventBus.publish(
                    HeldDropEvents.Drop(
                        player,
                        0,
                        InvObj(RunecraftObjs.mediumPouch),
                        objTypes[RunecraftObjs.mediumPouch],
                    )
                )
            )
            assertEquals(0, deps.storage.count(player, EssencePouchDefinitions.medium))
            assertNull(deps.storage.essenceType(player, EssencePouchDefinitions.medium))
            assertEquals(0, deps.storage.degradation(player, EssencePouchDefinitions.medium))

            preparePlayer(level = 99)
            add(RunecraftObjs.mediumPouchDegraded)
            add(RunecraftObjs.runeEssence)
            deps.storage.setState(
                player,
                EssencePouchDefinitions.medium,
                count = 0,
                essence = null,
                degradation = 799,
            )
            random.next = 1
            val slot = player.inv.indexOfFirst { it?.id == RunecraftObjs.mediumPouchDegraded.id }
            player.withProtectedAccess { deps.held.interact(this, player.inv, slot, HeldOp.Op1) }
            assertFalse(player.inv.contains(RunecraftObjs.mediumPouchDegraded))
            assertEquals(1, player.count(RunecraftObjs.runeEssence))
            assertEquals(0, deps.storage.count(player, EssencePouchDefinitions.medium))
            assertEquals(0, deps.storage.degradation(player, EssencePouchDefinitions.medium))
        }

    private fun GameTestScope.preparePlayer(level: Int) {
        player.clearAllInvs()
        player.members = true
        player.back = null
        player.stats[stats.runecrafting] = level
        client.clear()
        for (pouch in EssencePouchDefinitions.all) {
            storageFromScope.clear(player, pouch)
        }
    }

    private lateinit var storageFromScope: EssencePouchStorage

    private fun GameTestScope.add(type: ObjType) {
        val slot = player.inv.indexOfFirst { it == null }
        require(slot >= 0)
        player.inv[slot] = InvObj(type)
    }

    private fun GameTestState.runPouchTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            RunecraftScript::class,
            testBody = {
                storageFromScope = it.storage
                body(it)
            },
        )

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(RunecraftModule())
        }
    }

    data class Dependencies
    @Inject
    constructor(val storage: EssencePouchStorage, val held: HeldInteractions)
}
