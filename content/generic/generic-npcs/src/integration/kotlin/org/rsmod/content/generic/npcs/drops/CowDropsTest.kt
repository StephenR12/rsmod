package org.rsmod.content.generic.npcs.drops

import jakarta.inject.Inject
import net.rsprot.protocol.game.outgoing.interfaces.IfSetText
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.clue.ClueInventory
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.content
import org.rsmod.api.config.refs.npcs
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.death.NpcDeath
import org.rsmod.api.death.NpcDropContext
import org.rsmod.api.death.NpcDropTableRegistry
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.NpcUInteractions
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.generic.npcs.cow.Cow
import org.rsmod.content.generic.npcs.cow.CowCalf
import org.rsmod.game.entity.Npc
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.npc.UnpackedNpcType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList

@Execution(ExecutionMode.SAME_THREAD)
class CowDropsTest {
    @Test
    fun GameTestState.`dated cow and calf family has exact cache contracts`() = runCowTest { deps ->
        val types = deps.cowTypes()
        assertEquals(
            listOf(2_790, 2_791, 2_793, 2_795, 5_842, 2_792, 2_794, 2_801),
            types.map { it.id },
        )
        assertEquals(
            listOf("Cow", "Cow", "Cow", "Cow", "Cow", "Cow calf", "Cow calf", "Cow calf"),
            types.map { it.name },
        )
        assertTrue(types.all { "Attack" in it.op })
        assertTrue(types.all { it.respawnRate == 45 })
        assertTrue(types.take(5).all { it.contentGroup == content.cow.id })
        assertTrue(types.drop(5).all { it.contentGroup == content.cow_calf.id })
        assertEquals(types.map { it.id }.toSet(), deps.registry.registeredTypeIds())

        val table = checkNotNull(deps.registry[types.first()])
        types.forEach { assertSame(table, deps.registry[it]) }
        assertEquals(1_739, objs.cow_hide.id)
        assertEquals(2_132, objs.raw_beef.id)
        assertEquals(526, objs.bones.id)
    }

    @Test
    fun GameTestState
        .`three guaranteed drops and beginner clue preserve exact order and lifetime`() =
        runCowTest { deps ->
            val type = deps.cowTypes().first()
            val table = checkNotNull(deps.registry[type])

            random.next = 127
            assertEquals(
                listOf(objs.cow_hide.id, objs.raw_beef.id, objs.bones.id),
                table.roll(context(deps, type)).map { it.obj.id },
            )
            random.next = 0
            val drops = table.roll(context(deps, type))
            assertEquals(
                listOf(
                    objs.cow_hide.id,
                    objs.raw_beef.id,
                    objs.bones.id,
                    objs.clue_scroll_beginner.id,
                ),
                drops.map { it.obj.id },
            )
            assertEquals(ClueInventory.CLUE_SCROLL_GROUND_DURATION, drops.last().duration)

            val npc = spawnNpc(player.coords, type)
            player.observerUUID = player.uuid
            npc.heroPoints(player, 1)
            deps.death.spawnDrops(npc)
            assertEquals(
                setOf(
                    objs.cow_hide.id,
                    objs.raw_beef.id,
                    objs.bones.id,
                    objs.clue_scroll_beginner.id,
                ),
                deps.objRepo.findAll(npc.coords).map { it.type }.toSet(),
            )
            advance(200)
            assertEquals(
                listOf(objs.clue_scroll_beginner.id),
                deps.objRepo.findAll(npc.coords).map { it.type }.toList(),
            )
        }

    @Test
    fun GameTestState.`beginner clue uses shared ownership boxes and capacity policy`() =
        runCowTest { deps ->
            val type = deps.cowTypes().first()
            val table = checkNotNull(deps.registry[type])

            player.inv[0] = InvObj(objs.clue_scroll_beginner)
            var dropContext = context(deps, type)
            random.next = 42
            var result = table.resolve(dropContext)
            assertEquals(42, random.next)
            assertEquals(3, result.drops.size)
            assertEquals(42, deps.random.of(128))

            player.clearInv()
            player.setVarBit(varbits.x_marks_the_spot, ClueInventory.X_MARKS_COMPLETE)
            dropContext = context(deps, type)
            random.next = 0
            result = table.resolve(dropContext)
            assertEquals(objs.scroll_box_beginner.id, result.drops.last().obj.id)
            assertEquals(ClueInventory.SCROLL_BOX_GROUND_DURATION, result.drops.last().duration)

            player.inv[0] = InvObj(objs.scroll_box_beginner, 2)
            dropContext = context(deps, type)
            random.next = 0
            result = table.resolve(dropContext)
            assertEquals(3, result.drops.size)
            assertEquals(
                listOf(
                    "You have a sneaking suspicion that you would have received a " +
                        "beginner scroll box."
                ),
                result.messages,
            )
        }

    @Test
    fun GameTestState.`cow and calf item responses cover every dated bucket branch`() =
        runCowTest { deps ->
            val cow = spawnNpc(player.coords, deps.npcTypes[npcs.cow])
            val calf = spawnNpc(player.coords, deps.npcTypes[npcs.cow_calf2])

            useOnNpc(deps, cow, objs.bucket_empty)
            assertMessageSent("Only dairy cows are suitable for milking.")

            client.clear()
            useOnNpc(deps, cow, objs.beer)
            assertMessageSent("The cow doesn't want that.")

            client.clear()
            useOnNpc(deps, calf, objs.bucket_empty)
            assertMessageSent("Calves are too young to be milked.")

            client.clear()
            useOnNpc(deps, calf, objs.bucket_of_milk)
            assertMessageSent("Calves aren't really used to drinking milk from buckets!")

            client.clear()
            useOnNpc(deps, calf, objs.beer)
            assertMessageSent("The calf doesn't want that.")
        }

    @Test
    fun GameTestState.`Zanaris cow standard transcript is exact`() = runCowTest { deps ->
        val cow = spawnNpc(player.coords, deps.npcTypes[npcs.cow_zanaris])
        player.opNpc1(cow)
        advance()
        assertDialogueText(
            "Hmm, interesting, there appears to be a human trying to talk to me. Is it aware " +
                "that cows can't talk I wonder? Maybe if I just ignore it, it might go away."
        )
        resumeDialogue(components.chat_left_pbutton)
        assertDialogueText("The cow is studiously ignoring you.")
    }

    @Test
    fun GameTestState.`Zanaris cow certificate transcript is exact`() = runCowTest { deps ->
        val cow = spawnNpc(player.coords, deps.npcTypes[npcs.cow_zanaris])
        useOnNpc(deps, cow, objs.nuffs_certificate)
        assertDialogueText("You show the certificate to the cow.")
        resumeDialogue(components.messagebox_pbutton)
        assertDialogueText("Do you know what these strange marks mean?")
        resumeDialogue(components.chat_right_pbutton)
        assertDialogueText(
            "Can't the human take a hint? I've been ignoring you! Do you think I would ignore " +
                "you if I wanted to discuss your Rorschach ink-blots ?"
        )
    }

    private fun GameTestState.runCowTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            scripts = arrayOf(CowDrops::class, Cow::class, CowCalf::class),
            testBody = body,
        )

    private fun GameTestScope.context(deps: Dependencies, type: UnpackedNpcType): NpcDropContext {
        val npc = spawnNpc(player.coords, type)
        return NpcDropContext(npc, player, npc.coords, deps.random)
    }

    private fun GameTestScope.useOnNpc(deps: Dependencies, npc: Npc, obj: ObjType) {
        player.inv[0] = InvObj(obj)
        player.withProtectedAccess {
            deps.interactions.interactOp(
                this,
                npc,
                player.inv,
                invSlot = 0,
                npc.type,
                deps.objTypes[obj],
            )
        }
    }

    private fun GameTestScope.resumeDialogue(component: org.rsmod.game.type.comp.ComponentType) {
        player.resumeActiveCoroutine(ResumePauseButtonInput(component, subcomponent = -1))
    }

    private fun GameTestScope.assertDialogueText(text: String) {
        val texts = client.filterIsInstance<IfSetText>().map { it.text.replace("<br>", " ") }
        assertTrue(texts.contains(text)) { "Dialogue text not found: $text | actual=$texts" }
    }

    private data class Dependencies
    @Inject
    constructor(
        val registry: NpcDropTableRegistry,
        val death: NpcDeath,
        val objRepo: ObjRepository,
        val npcTypes: NpcTypeList,
        val objTypes: ObjTypeList,
        val random: GameRandom,
        val interactions: NpcUInteractions,
    ) {
        fun cowTypes(): List<UnpackedNpcType> =
            listOf(
                npcTypes[npcs.cow],
                npcTypes[npcs.cow2],
                npcTypes[npcs.cow3],
                npcTypes[npcs.cow_isle_of_souls],
                npcTypes[npcs.cow_zanaris],
                npcTypes[npcs.cow_calf2],
                npcTypes[npcs.cow_calf3],
                npcTypes[npcs.cow_calf_farmland],
            )
    }
}
