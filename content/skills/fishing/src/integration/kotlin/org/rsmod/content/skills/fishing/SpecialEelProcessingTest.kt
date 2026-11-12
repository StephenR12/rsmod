package org.rsmod.content.skills.fishing

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.events.interact.HeldObjEvents
import org.rsmod.api.player.events.interact.HeldUEvents
import org.rsmod.api.player.interact.HeldInteractions
import org.rsmod.api.player.interact.HeldUInteractions
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.events.EventBus
import org.rsmod.game.interact.HeldOp
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.seq.SeqTypeList

@Execution(ExecutionMode.SAME_THREAD)
class SpecialEelProcessingTest {
    @Test
    fun GameTestState.`cache 233 special eel processing contracts and dispatch are pinned`() =
        runFishingTest { deps ->
            assertEquals(6529, objs.tokkul.id)
            assertEquals(11994, objs.lava_scale_shard.id)
            assertEquals(9194, objs.onyx_bolt_tips.id)
            assertEquals(12934, objs.zulrahs_scales.id)
            assertEquals(2347, objs.hammer.id)
            assertEquals(25644, objs.imcando_hammer.id)
            assertEquals(29775, objs.imcando_hammer_offhand.id)
            assertEquals(7151, seqs.sacred_eel_dissection.id)
            assertEquals(7553, seqs.infernal_eel_smashing.id)
            assertEquals(8969, seqs.infernal_eel_smashing_imcando.id)
            assertEquals(105, fishing_queues.sacredEelDissection.id)
            assertEquals(
                listOf(null, null, null, "Eat", null),
                deps.objs[objs.infernal_eel].iop.toList(),
            )
            assertEquals(
                listOf(null, null, null, "Eat", null),
                deps.objs[objs.sacred_eel].iop.toList(),
            )
            assertTrue(deps.objs[objs.tokkul].stackable)
            assertTrue(deps.objs[objs.lava_scale_shard].stackable)
            assertTrue(deps.objs[objs.onyx_bolt_tips].stackable)
            assertTrue(deps.objs[objs.zulrahs_scales].stackable)
            assertNotNull(deps.seqs[seqs.sacred_eel_dissection])
            assertNotNull(deps.seqs[seqs.infernal_eel_smashing])
            assertNotNull(deps.seqs[seqs.infernal_eel_smashing_imcando])
            for (hammer in listOf(objs.hammer, objs.imcando_hammer, objs.imcando_hammer_offhand)) {
                assertTrue(
                    deps.events.contains(
                        HeldUEvents.Type::class.java,
                        EventBus.composeLongKey(hammer.id, objs.infernal_eel.id),
                    )
                )
            }
            assertTrue(
                deps.events.contains(
                    HeldUEvents.Type::class.java,
                    EventBus.composeLongKey(objs.knife.id, objs.sacred_eel.id),
                )
            )
            assertTrue(deps.events.contains(HeldObjEvents.Op4::class.java, objs.infernal_eel.id))
            assertTrue(deps.events.contains(HeldObjEvents.Op4::class.java, objs.sacred_eel.id))
        }

    @Test
    fun `infernal reward table and sacred scale level bands have exact boundaries`() {
        assertEquals(InfernalEelReward(objs.tokkul, 14..20), rollInfernalReward(0))
        assertEquals(InfernalEelReward(objs.tokkul, 14..20), rollInfernalReward(28))
        assertEquals(InfernalEelReward(objs.onyx_bolt_tips, 1..1), rollInfernalReward(29))
        assertEquals(InfernalEelReward(objs.onyx_bolt_tips, 1..1), rollInfernalReward(30))
        assertEquals(InfernalEelReward(objs.lava_scale_shard, 1..5), rollInfernalReward(31))
        assertEquals(InfernalEelReward(objs.lava_scale_shard, 1..5), rollInfernalReward(34))

        val scaleCases =
            mapOf(
                1 to 3..5,
                72 to 3..5,
                79 to 3..5,
                80 to 4..6,
                87 to 4..6,
                88 to 5..7,
                95 to 5..7,
                96 to 6..8,
                103 to 6..8,
                104 to 7..9,
                126 to 7..9,
            )
        scaleCases.forEach { (level, expected) ->
            assertEquals(expected, sacredEelScaleRange(level), "Cooking level $level")
        }
    }

    @Test
    fun GameTestState.`infernal eels use exact rewards and process the first eel`() =
        runFishingTest { deps ->
            val cases =
                listOf(
                    InfernalCase(0, 14, objs.tokkul, seqs.infernal_eel_smashing.id),
                    InfernalCase(28, 20, objs.tokkul, seqs.infernal_eel_smashing.id),
                    InfernalCase(29, 1, objs.onyx_bolt_tips, seqs.infernal_eel_smashing.id),
                    InfernalCase(31, 1, objs.lava_scale_shard, seqs.infernal_eel_smashing.id),
                    InfernalCase(34, 5, objs.lava_scale_shard, seqs.infernal_eel_smashing.id),
                )
            for (case in cases) {
                prepare(cookingLevel = 1)
                player.inv[0] = InvObj(objs.hammer)
                player.inv[2] = InvObj(objs.infernal_eel)
                player.inv[9] = InvObj(objs.infernal_eel)
                queueRandom(case.roll, case.count)

                player.withProtectedAccess {
                    deps.heldU.interact(
                        this,
                        player.inv,
                        deps.objs[objs.hammer],
                        0,
                        deps.objs[objs.infernal_eel],
                        9,
                    )
                }

                assertEquals(null, player.inv[2], "The first eel must be processed")
                assertEquals(objs.infernal_eel.id, player.inv[9]?.id)
                assertEquals(case.count, player.count(case.output))
                assertEquals(0, player.statMap.getFineXP(stats.cooking))
                assertEquals(case.animation, player.pendingSequence.id)
            }
        }

    @Test
    fun GameTestState
        .`infernal manual spam and imcando animation process multiple eels in one tick`() =
        runFishingTest { deps ->
            prepare(cookingLevel = 1)
            player.inv[0] = InvObj(objs.imcando_hammer)
            player.inv[4] = InvObj(objs.infernal_eel)
            player.inv[5] = InvObj(objs.infernal_eel)
            queueRandom(0, 14, 31, 5)

            player.withProtectedAccess {
                repeat(2) {
                    deps.heldU.interact(
                        this,
                        player.inv,
                        deps.objs[objs.imcando_hammer],
                        0,
                        deps.objs[objs.infernal_eel],
                        5,
                    )
                }
            }

            assertEquals(0, player.count(objs.infernal_eel))
            assertEquals(14, player.count(objs.tokkul))
            assertEquals(5, player.count(objs.lava_scale_shard))
            assertEquals(seqs.infernal_eel_smashing_imcando.id, player.pendingSequence.id)
        }

    @Test
    fun GameTestState
        .`special eel processing enforces membership visible level and selected tools`() =
        runFishingTest { deps ->
            prepare(cookingLevel = 72)
            player.members = false
            player.inv[0] = InvObj(objs.knife)
            player.inv[1] = InvObj(objs.sacred_eel)
            player.withProtectedAccess { deps.processing.dissectSacred(this, 0, 1) }
            assertEquals(1, player.count(objs.sacred_eel))
            assertMessageSent("You need to be on a members' world to dissect this eel.")

            prepare(cookingLevel = 71)
            player.inv[0] = InvObj(objs.knife)
            player.inv[1] = InvObj(objs.sacred_eel)
            player.withProtectedAccess { deps.processing.dissectSacred(this, 0, 1) }
            assertEquals(1, player.count(objs.sacred_eel))
            assertMessageSent("You need a Cooking level of 72 to dissect this eel.")

            prepare(cookingLevel = 72)
            player.inv[0] = InvObj(objs.hammer)
            player.inv[1] = InvObj(objs.sacred_eel)
            player.withProtectedAccess { deps.processing.dissectSacred(this, 0, 1) }
            assertEquals(1, player.count(objs.sacred_eel))

            prepare(cookingLevel = 1)
            player.members = false
            player.inv[0] = InvObj(objs.hammer)
            player.inv[1] = InvObj(objs.infernal_eel)
            player.withProtectedAccess { deps.processing.smashInfernal(this, 0, 1, objs.hammer) }
            assertEquals(1, player.count(objs.infernal_eel))
            assertMessageSent("You need to be on a members' world to smash this eel.")
        }

    @Test
    fun GameTestState.`sacred eels award boosted-level scales xp and exact automatic cadence`() =
        runFishingTest { deps ->
            prepare(cookingLevel = 71, visibleCookingLevel = 80)
            player.inv[0] = InvObj(objs.knife)
            player.inv[2] = InvObj(objs.sacred_eel)
            player.inv[7] = InvObj(objs.sacred_eel)
            player.inv[9] = InvObj(objs.sacred_eel)
            queueRandom(4, 5, 6)

            player.withProtectedAccess {
                deps.heldU.interact(
                    this,
                    player.inv,
                    deps.objs[objs.knife],
                    0,
                    deps.objs[objs.sacred_eel],
                    9,
                )
            }
            assertEquals(null, player.inv[2], "The first eel must be processed")
            assertEquals(objs.sacred_eel.id, player.inv[9]?.id)
            assertEquals(2, player.count(objs.sacred_eel))
            assertEquals(4, player.count(objs.zulrahs_scales))
            assertEquals(1120, player.statMap.getFineXP(stats.cooking))
            assertEquals(seqs.sacred_eel_dissection.id, player.pendingSequence.id)

            advance(1)
            assertEquals(2, player.count(objs.sacred_eel))
            advance(1)
            assertEquals(1, player.count(objs.sacred_eel))
            assertEquals(9, player.count(objs.zulrahs_scales))
            assertEquals(2270, player.statMap.getFineXP(stats.cooking))

            advance(2)
            assertEquals(1, player.count(objs.sacred_eel))
            advance(1)
            assertEquals(0, player.count(objs.sacred_eel))
            assertEquals(15, player.count(objs.zulrahs_scales))
            assertEquals(3450, player.statMap.getFineXP(stats.cooking))
        }

    @Test
    fun GameTestState
        .`sacred automatic processing revalidates the knife and full inventory is safe`() =
        runFishingTest { deps ->
            prepare(cookingLevel = 104)
            fillInventory()
            player.inv[0] = InvObj(objs.knife)
            player.inv[1] = InvObj(objs.sacred_eel)
            player.inv[2] = InvObj(objs.sacred_eel)
            queueRandom(9)
            player.withProtectedAccess { deps.processing.dissectSacred(this, 0, 2) }
            assertEquals(1, player.count(objs.sacred_eel))
            assertEquals(9, player.count(objs.zulrahs_scales))
            player.inv[0] = InvObj(objs.coins)
            advance(2)
            assertEquals(1, player.count(objs.sacred_eel))
            assertEquals(1270, player.statMap.getFineXP(stats.cooking))
        }

    @Test
    fun GameTestState.`cached Eat options dispatch the exact rejection messages`() =
        runFishingTest { deps ->
            prepare(cookingLevel = 72)
            player.inv[3] = InvObj(objs.infernal_eel)
            player.withProtectedAccess { deps.held.interact(this, player.inv, 3, HeldOp.Op4) }
            assertMessageSent(SpecialEelProcessing.INFERNAL_EAT_MESSAGE)

            client.clear()
            player.inv[4] = InvObj(objs.sacred_eel)
            player.withProtectedAccess { deps.held.interact(this, player.inv, 4, HeldOp.Op4) }
            assertMessageSent(SpecialEelProcessing.SACRED_EAT_MESSAGE)
        }

    private fun GameTestScope.prepare(cookingLevel: Int, visibleCookingLevel: Int = cookingLevel) {
        player.withProtectedAccess { clearSacredEelDissectionQueue() }
        player.clearAllInvs()
        player.members = true
        player.stats[stats.cooking] = cookingLevel
        player.setCurrentLevel(stats.cooking, visibleCookingLevel)
        player.statMap.setFineXP(stats.cooking, 0)
        client.clear()
    }

    private fun GameTestScope.add(obj: ObjType, count: Int = 1) {
        val type = objTypes[obj]
        if (type.stackable) {
            val slot = player.inv.indexOfFirst { it == null }
            player.inv[slot] = InvObj(obj, count)
        } else {
            repeat(count) {
                val slot = player.inv.indexOfFirst { it == null }
                player.inv[slot] = InvObj(obj)
            }
        }
    }

    private fun GameTestScope.fillInventory() {
        for (slot in player.inv.indices) {
            player.inv[slot] = InvObj(objs.coins)
        }
    }

    private fun GameTestScope.queueRandom(vararg values: Int) {
        values.forEach { random.then = it }
    }

    private fun GameTestState.runFishingTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            childModule = null,
            FishingScript::class,
            testBody = body,
        )

    data class Dependencies
    @Inject
    constructor(
        val processing: SpecialEelProcessing,
        val held: HeldInteractions,
        val heldU: HeldUInteractions,
        val events: EventBus,
        val objs: ObjTypeList,
        val seqs: SeqTypeList,
    )

    private data class InfernalCase(
        val roll: Int,
        val count: Int,
        val output: ObjType,
        val animation: Int,
    )
}
