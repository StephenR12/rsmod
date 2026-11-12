package org.rsmod.content.generic.npcs.cow

import jakarta.inject.Inject
import kotlin.math.round
import net.rsprot.protocol.game.outgoing.interfaces.IfSetText
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.content
import org.rsmod.api.config.refs.stats
import org.rsmod.api.hit.plugin.PlayerHitScript
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.api.utils.skills.SkillingSuccessRate
import org.rsmod.content.skills.thieving.ThievingStunStateRepository
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.seq.SeqTypeList

@Execution(ExecutionMode.SAME_THREAD)
class DairyCowTest {
    @Test
    fun GameTestState.`cache 233 cowbell identities and success curve are exact`() =
        runCowbellTest { deps ->
            val cow = deps.locTypes[CowLocs.fat_cow]
            assertEquals(8689, cow.id)
            assertEquals("Dairy cow", cow.name)
            assertEquals("Milk", cow.op[0])
            assertEquals("Steal-cowbell", cow.op[1])
            assertEquals(content.dairy_cow.id, cow.contentGroup)
            assertEquals(10593, CowObjs.cowbells.id)
            assertEquals(3293, CowVarBits.coldWarProgress.id)
            assertEquals("Play", deps.objTypes[CowObjs.cowbells].iop[0])
            assertEquals(3306, CowVarBits.penguinTransmog.id)
            assertEquals(5673, deps.seqTypes[CowSeqs.cowbellPlay].id)

            assertEquals(0.54296875, successRate(level = 15))
            assertEquals(0.625, successRate(level = 43))
            assertEquals(0.78515625, successRate(level = 99))
        }

    @Test
    fun GameTestState.`membership quest level and capacity gates preserve inventory`() =
        runCowbellTest { deps ->
            player.stats[stats.thieving] = 15

            attemptCowbell(deps)
            assertMessageSent("You need to be on a members' world to steal a cowbell.")
            assertEquals(0, player.count(CowObjs.cowbells))

            client.clear()
            player.members = true
            player.setVarBit(CowVarBits.coldWarProgress, 1)
            player.stats[stats.thieving] = 14
            attemptCowbell(deps)
            assertMessageSent("You need a Thieving level of 15 to steal a cowbell.")
            assertEquals(0, player.count(CowObjs.cowbells))

            client.clear()
            player.stats[stats.thieving] = 15
            player.fillInv()
            attemptCowbell(deps)
            assertMessageSent("You don't have enough inventory space.")
            assertEquals(0, player.count(CowObjs.cowbells))

            client.clear()
            player.clearInv()
            player.setVarBit(CowVarBits.coldWarProgress, 0)
            attemptCowbell(deps)
            assertDialogueContains("You need to have started the Cold War quest to attempt this.")
            assertEquals(0, player.count(CowObjs.cowbells))
            player.resumeActiveCoroutine(
                ResumePauseButtonInput(components.messagebox_pbutton, subcomponent = -1)
            )
        }

    @Test
    fun GameTestState.`successful theft grants one bell and sixteen xp`() = runCowbellTest { deps ->
        player.members = true
        player.setVarBit(CowVarBits.coldWarProgress, 1)
        player.stats[stats.thieving] = 15
        val startXp = player.statMap.getFineXP(stats.thieving)

        random.next = 0
        attemptCowbell(deps)

        assertMessageSent("You steal a cowbell.")
        assertEquals(1, player.count(CowObjs.cowbells))
        assertEquals(160, player.statMap.getFineXP(stats.thieving) - startXp)
    }

    @Test
    fun GameTestState.`failed theft applies the cow kick and thieving stun`() =
        runCowbellTest { deps ->
            player.members = true
            player.setVarBit(CowVarBits.coldWarProgress, 1)
            player.stats[stats.thieving] = 15
            player.stats[stats.hitpoints] = 10

            random.next = 99
            attemptCowbell(deps)

            assertMessageSent("You fail to steal a cowbell.")
            assertMessageSent("The cow kicks you and stuns you!")
            assertMessageSent("MOO")
            assertEquals(0, player.count(CowObjs.cowbells))
            assertTrue(deps.stunStates.isMovementBlocked(player))
            assertTrue(deps.stunStates.remainingCycles(player) in 1..DairyCow.FAILURE_BLOCK_CYCLES)
            advance(2)
            assertEquals(9, player.hitpoints)
        }

    @Test
    fun GameTestState.`cowbells play only in the penguin suit`() = runCowbellTest { deps ->
        attemptCowbellPlay(deps)
        assertMessageSent("You must be wearing the penguin suit to play these.")
        assertEquals(0, player.pendingSequence.id)

        client.clear()
        player.setVarBit(CowVarBits.penguinTransmog, 1)
        attemptCowbellPlay(deps)
        assertEquals(CowSeqs.cowbellPlay.id, player.pendingSequence.id)
    }

    private fun GameTestState.runCowbellTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            scripts = arrayOf(DairyCow::class, PlayerHitScript::class),
            testBody = body,
        )

    private fun successRate(level: Int): Double =
        SkillingSuccessRate.successRate(
                DairyCow.CHANCE_LOW,
                DairyCow.CHANCE_HIGH,
                level,
                maxLevel = 99,
            )
            .let { round(it * 100_000_000) / 100_000_000 }

    private fun GameTestScope.assertDialogueContains(text: String) {
        val actual = client.filterIsInstance<IfSetText>().map { it.text.replace("<br>", " ") }
        assertTrue(actual.any { text in it }) { "Dialogue text not found: $text | actual=$actual" }
    }

    private fun GameTestScope.attemptCowbell(deps: Dependencies) {
        player.withProtectedAccess { with(deps.dairyCow) { resolveCowbellTheft() } }
    }

    private fun GameTestScope.attemptCowbellPlay(deps: Dependencies) {
        player.withProtectedAccess { with(deps.dairyCow) { resolveCowbellPlay() } }
    }

    private data class Dependencies
    @Inject
    constructor(
        val dairyCow: DairyCow,
        val locTypes: LocTypeList,
        val objTypes: ObjTypeList,
        val seqTypes: SeqTypeList,
        val stunStates: ThievingStunStateRepository,
    )
}
