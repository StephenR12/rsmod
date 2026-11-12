package org.rsmod.api.game.process.npc.mode

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import org.rsmod.api.npc.apObj5
import org.rsmod.api.npc.events.interact.AiObjDefaultEvents
import org.rsmod.api.npc.events.interact.ApDefaultEvent
import org.rsmod.api.npc.interact.AiObjInteractions
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.factory.npcTypeFactory
import org.rsmod.api.testing.factory.objFactory
import org.rsmod.api.testing.factory.objTypeFactory
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.game.interact.InteractionOp

class NpcObjectModeTest {
    @Test
    fun GameTestState.`npc fifth object approach selects object mode`() =
        runInjectedGameTest(Dependencies::class) { deps ->
            val npc = spawnNpc(player.coords.translateX(1), targetNpc)
            val type = objTypeFactory.create(id = 1)
            val obj = objFactory.create(type, coords = player.coords)

            npc.apObj5(obj, deps.interactions)

            assertEquals(NpcMode.ApObj5, npc.mode)
        }

    @Test
    fun GameTestState.`default npc object approach options resolve their matching events`() =
        runInjectedGameTest(Dependencies::class) { deps ->
            val type = objTypeFactory.create(id = 1)
            val obj = objFactory.create(type, coords = player.coords)
            eventBus.subscribeSuspend(
                AiObjDefaultEvents.Ap1::class.java,
                ApDefaultEvent.ID.toLong(),
            ) {}
            eventBus.subscribeSuspend(
                AiObjDefaultEvents.Ap2::class.java,
                ApDefaultEvent.ID.toLong(),
            ) {}
            eventBus.subscribeSuspend(
                AiObjDefaultEvents.Ap3::class.java,
                ApDefaultEvent.ID.toLong(),
            ) {}
            eventBus.subscribeSuspend(
                AiObjDefaultEvents.Ap4::class.java,
                ApDefaultEvent.ID.toLong(),
            ) {}
            eventBus.subscribeSuspend(
                AiObjDefaultEvents.Ap5::class.java,
                ApDefaultEvent.ID.toLong(),
            ) {}

            assertInstanceOf(
                AiObjDefaultEvents.Ap1::class.java,
                deps.interactions.apTrigger(obj, InteractionOp.Op1, type),
            )
            assertInstanceOf(
                AiObjDefaultEvents.Ap2::class.java,
                deps.interactions.apTrigger(obj, InteractionOp.Op2, type),
            )
            assertInstanceOf(
                AiObjDefaultEvents.Ap3::class.java,
                deps.interactions.apTrigger(obj, InteractionOp.Op3, type),
            )
            assertInstanceOf(
                AiObjDefaultEvents.Ap4::class.java,
                deps.interactions.apTrigger(obj, InteractionOp.Op4, type),
            )
            assertInstanceOf(
                AiObjDefaultEvents.Ap5::class.java,
                deps.interactions.apTrigger(obj, InteractionOp.Op5, type),
            )
        }

    private class Dependencies @Inject constructor(val interactions: AiObjInteractions)

    private companion object {
        private val targetNpc =
            npcTypeFactory.create {
                name = "Object-mode test NPC"
                size = 1
                maxRange = 100
                attackRange = 1
            }
    }
}
