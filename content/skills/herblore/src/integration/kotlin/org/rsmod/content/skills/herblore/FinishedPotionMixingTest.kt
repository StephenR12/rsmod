package org.rsmod.content.skills.herblore

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import net.rsprot.protocol.game.outgoing.interfaces.IfOpenSub
import net.rsprot.protocol.game.outgoing.interfaces.IfSetEventsV2
import net.rsprot.protocol.game.outgoing.misc.player.RunClientScript
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.HeldUInteractions
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.comp.ComponentType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.obj.Wearpos
import org.rsmod.game.type.varbit.VarBitTypeList
import org.rsmod.game.type.varp.VarpLifetime
import org.rsmod.game.type.varp.VarpTypeList
import org.rsmod.game.ui.UserInterface

@Execution(ExecutionMode.SAME_THREAD)
class FinishedPotionMixingTest {
    @Test
    fun GameTestState.`cache 233 pins every finished potion contract`() =
        runFinishedPotionTest { deps ->
            assertEquals(363, seqs.human_herbing_vial.id)
            assertEquals(65535, herblore_varps.chemistryAmuletUses.id)
            assertEquals(VarpLifetime.Perm, deps.varps[herblore_varps.chemistryAmuletUses].scope)
            assertEquals(16173, varbits.alchemists_amulet_charges.id)
            assertEquals(
                VarpLifetime.Perm,
                deps.varbits[varbits.alchemists_amulet_charges].baseVar.scope,
            )
            assertEquals(
                listOf(
                    Contract(91, 221, 121, 3, 250),
                    Contract(93, 235, 175, 5, 375),
                    Contract(95, 225, 115, 12, 500),
                    Contract(97, 21622, 6472, 22, 600),
                    Contract(97, 223, 127, 22, 625),
                    Contract(97, 1581, 1582, 25, 800),
                    Contract(97, 1975, 3010, 26, 675),
                    Contract(99, 239, 133, 30, 750),
                    Contract(3002, 2152, 3034, 34, 800),
                    Contract(97, 9736, 9741, 36, 840),
                    Contract(99, 231, 139, 38, 875),
                    Contract(101, 221, 145, 45, 1000),
                    Contract(101, 235, 181, 48, 1063),
                    Contract(103, 231, 151, 50, 1125),
                    Contract(103, 2970, 3018, 52, 1175),
                    Contract(103, 10111, 10000, 53, 1200),
                    Contract(97, 29993, 30140, 54, 1320),
                    Contract(105, 225, 157, 55, 1250),
                    Contract(30100, 29993, 30128, 58, 1320),
                    Contract(105, 241, 187, 60, 1375),
                    Contract(3004, 223, 3026, 63, 1425),
                    Contract(107, 239, 163, 66, 1500),
                    Contract(2483, 241, 2454, 69, 1575),
                    Contract(109, 245, 169, 72, 1625),
                    Contract(2483, 3138, 3042, 76, 1725),
                    Contract(111, 247, 189, 78, 1750),
                    Contract(22443, 245, 22464, 80, 1550),
                    Contract(22443, 3138, 22452, 80, 1550),
                    Contract(3002, 6693, 6687, 81, 1800),
                    Contract(111, 30800, 30878, 81, 1850),
                    Contract(109, 26368, 26342, 85, 1900),
                    Contract(109, 27272, 27205, 88, 2000),
                    Contract(5942, 6049, 5943, 68, 1550),
                    Contract(5951, 6051, 5952, 79, 1775),
                    Contract(5936, 223, 5937, 73, 1900),
                    Contract(5939, 6018, 5940, 82, 1900),
                ),
                FinishedPotionRecipes.all.map {
                    Contract(
                        it.unfinished.id,
                        it.secondary.id,
                        it.output.id,
                        it.level,
                        (it.xp * 10).toInt(),
                    )
                },
            )
            assertEquals(
                listOf(
                    2428,
                    2446,
                    113,
                    6470,
                    2430,
                    3008,
                    2432,
                    3032,
                    9739,
                    2434,
                    2436,
                    2448,
                    2438,
                    3016,
                    9998,
                    30137,
                    2440,
                    30125,
                    3024,
                    2442,
                    2452,
                    2444,
                    3040,
                    2450,
                    22461,
                    22449,
                    6685,
                    30875,
                    26340,
                    27202,
                ),
                FinishedPotionRecipes.all.mapNotNull {
                    FinishedPotionRecipes.extraDoseByOutput[it.output.id]?.id
                },
            )
        }

    @Test
    fun GameTestState.`chemistry amulet rolls make four doses and consume shared charges`() =
        runFinishedPotionTest { deps ->
            val recipe = FinishedPotionRecipes.all.first()
            preparePlayer()
            player.worn[Wearpos.Front.slot] = InvObj(objs.amulet_of_chemistry)

            repeat(ChemistryAmuletRepository.MAX_CHARGES) { made ->
                add(recipe.unfinished)
                add(recipe.secondary)
                random.next = 0
                player.withProtectedAccess { deps.mixing.start(this, recipe, quantity = 1) }
                advance(2)
                assertEquals(made + 1, player.count(objs.attack_potion_4))
                if (made + 1 < ChemistryAmuletRepository.MAX_CHARGES) {
                    val remaining = ChemistryAmuletRepository.MAX_CHARGES - made - 1
                    assertEquals(remaining, deps.chemistryAmulet.remaining(player))
                    assertEquals(objs.amulet_of_chemistry.id, player.worn[Wearpos.Front.slot]?.id)
                    val count = if (remaining == 1) "one" else remaining.toString()
                    val noun = if (remaining == 1) "charge" else "charges"
                    assertMessageSent(
                        "Your amulet of chemistry helps you create a 4-dose potion. " +
                            "It has $count $noun left."
                    )
                } else {
                    assertMessageSent(
                        "Your amulet of chemistry helps you create a 4-dose potion. " +
                            "It then crumbles to dust."
                    )
                }
                client.clear()
            }
            assertEquals(null, player.worn[Wearpos.Front.slot])
            assertEquals(
                ChemistryAmuletRepository.MAX_CHARGES,
                deps.chemistryAmulet.remaining(player),
            )
            assertEquals((recipe.xp * 50).toInt(), player.statMap.getFineXP(stats.herblore))

            preparePlayer()
            player.worn[Wearpos.Front.slot] = InvObj(objs.amulet_of_chemistry)
            add(recipe.unfinished)
            add(recipe.secondary)
            random.next = 99
            player.withProtectedAccess { deps.mixing.start(this, recipe, quantity = 1) }
            advance(2)
            assertEquals(1, player.count(recipe.output))
            assertEquals(
                ChemistryAmuletRepository.MAX_CHARGES,
                deps.chemistryAmulet.remaining(player),
            )
        }

    @Test
    fun GameTestState.`alchemists amulet and goggles rolls compose without extra xp`() =
        runFinishedPotionTest { deps ->
            val recipe = FinishedPotionRecipes.all.first { it.output == objs.bastion_potion_3 }
            preparePlayer()
            player.worn[Wearpos.Front.slot] = InvObj(objs.alchemists_amulet_charged)
            player.worn[Wearpos.Hat.slot] = InvObj(objs.prescription_goggles_focused)
            player.withProtectedAccess { deps.modifiers.setAlchemistsAmuletCharges(this, 1) }
            add(recipe.unfinished)
            add(recipe.secondary)
            random.next = 0
            random.then = 0

            player.withProtectedAccess { deps.mixing.start(this, recipe, quantity = 1) }
            advance(2)

            assertEquals(1, player.count(objs.bastion_potion_4))
            assertEquals(1, player.count(recipe.secondary))
            assertEquals(0, player.count(recipe.output))
            assertEquals(objs.alchemists_amulet_uncharged.id, player.worn[Wearpos.Front.slot]?.id)
            player.withProtectedAccess {
                assertEquals(0, deps.modifiers.alchemistsAmuletCharges(this))
            }
            assertEquals((recipe.xp * 10).toInt(), player.statMap.getFineXP(stats.herblore))
        }

    @Test
    fun GameTestState.`goggles preserve ordinary secondaries without text or output changes`() =
        runFinishedPotionTest { deps ->
            val recipe = FinishedPotionRecipes.all.first { it.output == objs.weapon_poison }
            preparePlayer()
            player.worn[Wearpos.Hat.slot] = InvObj(objs.prescription_goggles)
            player.worn[Wearpos.Front.slot] = InvObj(objs.amulet_of_chemistry)
            add(recipe.unfinished)
            add(recipe.secondary)
            random.next = 0

            player.withProtectedAccess { deps.mixing.start(this, recipe, quantity = 1) }
            advance(2)

            assertEquals(1, player.count(recipe.output))
            assertEquals(1, player.count(recipe.secondary))
            assertEquals(
                ChemistryAmuletRepository.MAX_CHARGES,
                deps.chemistryAmulet.remaining(player),
            )
            assertEquals((recipe.xp * 10).toInt(), player.statMap.getFineXP(stats.herblore))
        }

    @Test
    fun GameTestState.`unfinished potion and secondary open the native one-product menu`() =
        runFinishedPotionTest { deps ->
            val recipe = FinishedPotionRecipes.all.first { it.output == objs.super_restore_3 }
            preparePlayer()
            add(recipe.unfinished, 3)
            add(recipe.secondary, 2)

            player.withProtectedAccess { deps.mixing.select(this, recipe) }

            val open = client.filterIsInstance<IfOpenSub>().single { it.interfaceId == 270 }
            assertEquals(components.chatbox_chatmodal.id, open.destinationCombinedId)
            val setup = client.filterIsInstance<RunClientScript>().single { it.id == 2046 }
            assertEquals(
                listOf<Any>(
                    0,
                    2,
                    recipe.output.id,
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
    fun GameTestState.`every recipe makes its exact output and awards exact xp`() =
        runFinishedPotionTest { deps ->
            for (recipe in FinishedPotionRecipes.all) {
                preparePlayer(level = recipe.level)
                add(recipe.unfinished)
                add(recipe.secondary)

                player.withProtectedAccess { deps.mixing.start(this, recipe, quantity = 1) }

                assertEquals(seqs.human_herbing_vial.id, player.pendingSequence.id)
                advance()
                assertEquals(0, player.count(recipe.output), deps.objs[recipe.output].name)
                advance()
                assertEquals(0, player.count(recipe.unfinished), deps.objs[recipe.output].name)
                assertEquals(0, player.count(recipe.secondary), deps.objs[recipe.output].name)
                assertEquals(1, player.count(recipe.output), deps.objs[recipe.output].name)
                assertEquals(
                    (recipe.xp * 10).toInt(),
                    player.statMap.getFineXP(stats.herblore),
                    deps.objs[recipe.output].name,
                )
            }
        }

    @Test
    fun GameTestState.`strong weapon poisons support goggles but not extra-dose amulets`() =
        runFinishedPotionTest { deps ->
            for ((index, output) in
                listOf(objs.weapon_poison_plus, objs.weapon_poison_plus_plus).withIndex()) {
                val recipe = FinishedPotionRecipes.all.single { it.output == output }
                preparePlayer(level = recipe.level)
                player.worn[Wearpos.Hat.slot] = InvObj(objs.prescription_goggles_focused)
                if (index == 0) {
                    player.worn[Wearpos.Front.slot] = InvObj(objs.amulet_of_chemistry)
                } else {
                    player.worn[Wearpos.Front.slot] = InvObj(objs.alchemists_amulet_charged)
                    player.withProtectedAccess {
                        deps.modifiers.setAlchemistsAmuletCharges(this, 1)
                    }
                }
                add(recipe.unfinished)
                add(recipe.secondary)
                random.next = 0

                player.withProtectedAccess { deps.mixing.start(this, recipe, quantity = 1) }
                advance(2)

                assertEquals(1, player.count(recipe.output))
                assertEquals(1, player.count(recipe.secondary))
                if (index == 0) {
                    assertEquals(
                        ChemistryAmuletRepository.MAX_CHARGES,
                        deps.chemistryAmulet.remaining(player),
                    )
                } else {
                    assertEquals(
                        objs.alchemists_amulet_charged.id,
                        player.worn[Wearpos.Front.slot]?.id,
                    )
                    player.withProtectedAccess {
                        assertEquals(1, deps.modifiers.alchemistsAmuletCharges(this))
                    }
                }
                assertEquals(1900, player.statMap.getFineXP(stats.herblore))
            }
        }

    @Test
    fun GameTestState.`antidotes make four doses and preserve roots without amulet charges`() =
        runFinishedPotionTest { deps ->
            val outputs = listOf(objs.antidote_plus_4, objs.antidote_plus_plus_4)
            for ((index, output) in outputs.withIndex()) {
                val recipe = FinishedPotionRecipes.all.single { it.output == output }
                preparePlayer(level = recipe.level)
                player.worn[Wearpos.Hat.slot] = InvObj(objs.prescription_goggles)
                if (index == 0) {
                    player.worn[Wearpos.Front.slot] = InvObj(objs.amulet_of_chemistry)
                } else {
                    player.worn[Wearpos.Front.slot] = InvObj(objs.alchemists_amulet_charged)
                    player.withProtectedAccess {
                        deps.modifiers.setAlchemistsAmuletCharges(this, 1)
                    }
                }
                add(recipe.unfinished)
                add(recipe.secondary)
                random.next = 0

                player.withProtectedAccess { deps.mixing.start(this, recipe, quantity = 1) }
                advance()
                assertEquals(0, player.count(recipe.output))
                advance()

                assertEquals(1, player.count(recipe.output))
                assertEquals(1, player.count(recipe.secondary))
                assertEquals((recipe.xp * 10).toInt(), player.statMap.getFineXP(stats.herblore))
                if (index == 0) {
                    assertEquals(
                        ChemistryAmuletRepository.MAX_CHARGES,
                        deps.chemistryAmulet.remaining(player),
                    )
                } else {
                    assertEquals(
                        objs.alchemists_amulet_charged.id,
                        player.worn[Wearpos.Front.slot]?.id,
                    )
                    player.withProtectedAccess {
                        assertEquals(1, deps.modifiers.alchemistsAmuletCharges(this))
                    }
                }
            }
        }

    @Test
    fun GameTestState.`all coconut potion final stages dispatch in both held-use orders`() =
        runFinishedPotionTest { deps ->
            for (recipe in
                FinishedPotionRecipes.all.filter {
                    it.output == objs.antidote_plus_4 ||
                        it.output == objs.antidote_plus_plus_4 ||
                        it.output == objs.weapon_poison_plus ||
                        it.output == objs.weapon_poison_plus_plus
                }) {
                repeat(2) { reverse ->
                    preparePlayer(level = recipe.level)
                    add(recipe.unfinished)
                    add(recipe.secondary)
                    val unfinishedSlot = player.inv.indexOfFirst { it?.id == recipe.unfinished.id }
                    val secondarySlot = player.inv.indexOfFirst { it?.id == recipe.secondary.id }
                    player.withProtectedAccess {
                        if (reverse == 0) {
                            deps.heldU.interact(
                                this,
                                player.inv,
                                deps.objs[recipe.unfinished],
                                unfinishedSlot,
                                deps.objs[recipe.secondary],
                                secondarySlot,
                            )
                        } else {
                            deps.heldU.interact(
                                this,
                                player.inv,
                                deps.objs[recipe.secondary],
                                secondarySlot,
                                deps.objs[recipe.unfinished],
                                unfinishedSlot,
                            )
                        }
                    }
                    resumeSkillMulti(components.skillmulti_a, quantity = 1)
                    advance(2)
                    assertEquals(1, player.count(recipe.output), "output=${recipe.output.id}")
                }
            }
        }

    @Test
    fun GameTestState.`blamish oil accepts boosts and requires no Heroes Quest progress`() =
        runFinishedPotionTest { deps ->
            val recipe = FinishedPotionRecipes.all.single { it.output == objs.blamish_oil }
            assertEquals(188, varps.heroes_quest.id)
            preparePlayer(level = 25, baseLevel = 20)
            player.setVarp(varps.heroes_quest, 0)
            add(recipe.unfinished)
            add(recipe.secondary)

            player.withProtectedAccess { deps.mixing.start(this, recipe, quantity = 1) }
            advance()
            assertEquals(0, player.count(objs.blamish_oil))
            advance()
            assertEquals(1, player.count(objs.blamish_oil))
            assertEquals(800, player.statMap.getFineXP(stats.herblore))

            preparePlayer(level = 24)
            player.setVarp(varps.heroes_quest, 0)
            assertAtomicFailure(deps, recipe)
            assertMessageSent("You need a Herblore level of 25 to make this potion.")
        }

    @Test
    fun GameTestState.`Make-All produces one potion every two ticks`() =
        runFinishedPotionTest { deps ->
            val recipe = FinishedPotionRecipes.all.first { it.output == objs.super_attack_3 }
            preparePlayer()
            add(recipe.unfinished, 3)
            add(recipe.secondary, 3)

            player.withProtectedAccess { deps.mixing.start(this, recipe, quantity = 3) }

            repeat(3) { made ->
                advance()
                assertEquals(made, player.count(recipe.output))
                advance()
                assertEquals(made + 1, player.count(recipe.output))
            }
            assertEquals(0, player.weakQueueList.count(herblore_queues.finishedPotions))
            assertEquals((recipe.xp * 30).toInt(), player.statMap.getFineXP(stats.herblore))
        }

    @Test
    fun GameTestState.`membership quest and boosted level gates preserve both inputs`() =
        runFinishedPotionTest { deps ->
            val recipe = FinishedPotionRecipes.all.single { it.output == objs.menaphite_remedy_3 }

            preparePlayer(level = recipe.level, baseLevel = recipe.level - 5)
            add(recipe.unfinished)
            add(recipe.secondary)
            player.withProtectedAccess { deps.mixing.start(this, recipe, quantity = 1) }
            advance(2)
            assertEquals(1, player.count(recipe.output))

            preparePlayer(level = recipe.level - 1)
            assertAtomicFailure(deps, recipe)
            assertMessageSent("You need a Herblore level of 88 to make this potion.")

            preparePlayer(members = false)
            assertAtomicFailure(deps, recipe)
            assertMessageSent("You need to be on a members' world to make potions.")

            preparePlayer(questState = 3)
            assertAtomicFailure(deps, recipe)
            assertMessageSent("You need to complete Druidic Ritual before training Herblore.")
        }

    @Test
    fun GameTestState.`full inventory resource loss and shared queue replacement are safe`() =
        runFinishedPotionTest { deps ->
            val recipe = FinishedPotionRecipes.all.first()
            preparePlayer()
            player.inv[0] = InvObj(recipe.unfinished)
            player.inv[1] = InvObj(recipe.secondary)
            for (slot in 2 until player.inv.size) {
                player.inv[slot] = InvObj(objs.coins)
            }
            player.withProtectedAccess { deps.mixing.start(this, recipe, quantity = 1) }
            advance(2)
            assertEquals(1, player.count(recipe.output))
            assertEquals(27, player.inv.count { it != null })

            preparePlayer()
            add(recipe.unfinished, 2)
            add(recipe.secondary, 2)
            player.withProtectedAccess { deps.mixing.start(this, recipe, quantity = 2) }
            advance(2)
            player.inv[player.inv.indexOfFirst { it?.id == recipe.secondary.id }] = null
            advance(2)
            assertEquals(1, player.count(recipe.output))
            assertEquals(0, player.weakQueueList.count(herblore_queues.finishedPotions))

            preparePlayer()
            add(recipe.unfinished, 2)
            add(recipe.secondary, 2)
            player.withProtectedAccess {
                weakQueue(herblore_queues.unfinishedPotions, 20)
                deps.mixing.start(this, recipe, quantity = 2)
            }
            assertEquals(0, player.weakQueueList.count(herblore_queues.unfinishedPotions))
            advance(2)
            player.clearPendingAction()
            advance(2)
            assertEquals(1, player.count(recipe.output))
        }

    @Test
    fun GameTestState.`both held-use orders dispatch the selected quantity`() =
        runFinishedPotionTest { deps ->
            val recipe = FinishedPotionRecipes.all.first { it.output == objs.bastion_potion_3 }
            repeat(2) { reverse ->
                preparePlayer()
                add(recipe.unfinished, 2)
                add(recipe.secondary, 2)
                val unfinishedSlot = player.inv.indexOfFirst { it?.id == recipe.unfinished.id }
                val secondarySlot = player.inv.indexOfFirst { it?.id == recipe.secondary.id }

                player.withProtectedAccess {
                    if (reverse == 0) {
                        deps.heldU.interact(
                            this,
                            player.inv,
                            deps.objs[recipe.unfinished],
                            unfinishedSlot,
                            deps.objs[recipe.secondary],
                            secondarySlot,
                        )
                    } else {
                        deps.heldU.interact(
                            this,
                            player.inv,
                            deps.objs[recipe.secondary],
                            secondarySlot,
                            deps.objs[recipe.unfinished],
                            unfinishedSlot,
                        )
                    }
                }
                resumeSkillMulti(components.skillmulti_a, quantity = 2)
                advance(4)
                assertEquals(2, player.count(recipe.output))
            }
        }

    private fun GameTestScope.assertAtomicFailure(
        deps: Dependencies,
        recipe: FinishedPotionRecipe,
    ) {
        add(recipe.unfinished)
        add(recipe.secondary)
        player.withProtectedAccess { deps.mixing.start(this, recipe, quantity = 1) }
        assertEquals(1, player.count(recipe.unfinished))
        assertEquals(1, player.count(recipe.secondary))
        assertEquals(0, player.count(recipe.output))
        assertEquals(0, player.statMap.getFineXP(stats.herblore))
        assertEquals(0, player.weakQueueList.count(herblore_queues.finishedPotions))
    }

    private fun GameTestScope.preparePlayer(
        level: Int = 99,
        baseLevel: Int = level,
        members: Boolean = true,
        questState: Int = 4,
    ) {
        player.clearAllInvs()
        player.members = members
        player.setBaseLevel(stats.herblore, baseLevel)
        player.setCurrentLevel(stats.herblore, level)
        player.statMap.setXP(stats.herblore, 0)
        player.setVarp(varps.druidic_ritual, questState)
        player.weakQueueList.clear()
        client.clear()
    }

    private fun GameTestScope.add(obj: ObjType, count: Int = 1) {
        repeat(count) {
            val slot = player.inv.indexOfFirst { it == null }
            require(slot >= 0)
            player.inv[slot] = InvObj(obj)
        }
    }

    private fun GameTestScope.resumeSkillMulti(component: ComponentType, quantity: Int) {
        val target =
            requireNotNull(player.ui.modals.getComponent(UserInterface(interfaces.skillmulti.id)))
        player.ui.queueClose(target)
        player.resumeActiveCoroutine(ResumePauseButtonInput(component, subcomponent = quantity))
    }

    private fun GameTestState.runFinishedPotionTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(Dependencies::class, TestModule, HerbloreScript::class, testBody = body)

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(HerbloreModule())
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val mixing: FinishedPotionMixing,
        val modifiers: PotionProductionModifiers,
        val chemistryAmulet: ChemistryAmuletRepository,
        val heldU: HeldUInteractions,
        val objs: ObjTypeList,
        val varps: VarpTypeList,
        val varbits: VarBitTypeList,
    )

    private data class Contract(
        val unfinished: Int,
        val secondary: Int,
        val output: Int,
        val level: Int,
        val fineXp: Int,
    )
}
