package org.rsmod.content.skills.crafting.penguinbongos

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.interact.HeldUInteractions
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.seq.SeqTypeList

@Execution(ExecutionMode.SAME_THREAD)
class PenguinBongosTest {
    @Test
    fun GameTestState.`cache 233 pins the bongo item ingredients and action sequences`() =
        runPenguinBongoTest { deps ->
            assertEquals(1741, PenguinBongoObjs.leather.id)
            assertEquals(8782, PenguinBongoObjs.mahoganyPlank.id)
            assertEquals(10592, PenguinBongoObjs.bongos.id)
            assertEquals("Penguin bongos", deps.objTypes[PenguinBongoObjs.bongos].name)
            assertEquals("Play", deps.objTypes[PenguinBongoObjs.bongos].iop[0])
            assertEquals(3293, PenguinBongoVarBits.coldWarProgress.id)
            assertEquals(3306, PenguinBongoVarBits.penguinTransmog.id)
            assertEquals(5674, deps.seqTypes[PenguinBongoSeqs.play].id)
            assertEquals(5675, deps.seqTypes[PenguinBongoSeqs.ready].id)
        }

    @Test
    fun GameTestState.`membership quest and unboosted level gates preserve both ingredients`() =
        runPenguinBongoTest { deps ->
            prepareIngredients()
            player.setBaseLevel(stats.crafting, 30)
            player.setCurrentLevel(stats.crafting, 30)
            craft(deps)
            assertMessageSent("You need to be on a members' world to make penguin bongos.")
            assertIngredientsPreserved()

            client.clear()
            player.members = true
            craft(deps)
            assertMessageSent("You need to have started the Cold War quest to make penguin bongos.")
            assertIngredientsPreserved()

            client.clear()
            player.setVarBit(PenguinBongoVarBits.coldWarProgress, 1)
            player.setBaseLevel(stats.crafting, 29)
            player.setCurrentLevel(stats.crafting, 99)
            craft(deps)
            assertMessageSent("You need an unboosted Crafting level of 30 to make penguin bongos.")
            assertIngredientsPreserved()
        }

    @Test
    fun GameTestState.`crafting is atomic in a full inventory and grants twenty five xp`() =
        runPenguinBongoTest { deps ->
            prepareEligiblePlayer()
            player.fillInv()
            player.inv[0] = InvObj(PenguinBongoObjs.leather)
            player.inv[1] = InvObj(PenguinBongoObjs.mahoganyPlank)
            val startXp = player.statMap.getFineXP(stats.crafting)

            craft(deps)

            assertEquals(0, player.count(PenguinBongoObjs.leather))
            assertEquals(0, player.count(PenguinBongoObjs.mahoganyPlank))
            assertEquals(1, player.count(PenguinBongoObjs.bongos))
            assertEquals(250, player.statMap.getFineXP(stats.crafting) - startXp)
            assertMessageSent("You use the leather and plank to create penguin bongos.")
        }

    @Test
    fun GameTestState.`reverse held use reaches the same atomic recipe`() =
        runPenguinBongoTest { deps ->
            prepareEligiblePlayer()
            prepareIngredients()
            val plankSlot = player.inv.indexOfFirst { it?.id == PenguinBongoObjs.mahoganyPlank.id }
            val leatherSlot = player.inv.indexOfFirst { it?.id == PenguinBongoObjs.leather.id }

            player.withProtectedAccess {
                deps.heldU.interact(
                    this,
                    player.inv,
                    deps.objTypes[PenguinBongoObjs.mahoganyPlank],
                    plankSlot,
                    deps.objTypes[PenguinBongoObjs.leather],
                    leatherSlot,
                )
            }

            assertEquals(1, player.count(PenguinBongoObjs.bongos))
            assertEquals(0, player.count(PenguinBongoObjs.leather))
            assertEquals(0, player.count(PenguinBongoObjs.mahoganyPlank))
        }

    @Test
    fun GameTestState.`bongos play only in the penguin suit and are never consumed`() =
        runPenguinBongoTest { deps ->
            player.inv[0] = InvObj(PenguinBongoObjs.bongos)

            play(deps)
            assertMessageSent("You must be wearing the penguin suit to play these.")
            assertEquals(0, player.pendingSequence.id)
            assertEquals(1, player.count(PenguinBongoObjs.bongos))

            client.clear()
            player.setVarBit(PenguinBongoVarBits.penguinTransmog, 1)
            play(deps)
            assertEquals(PenguinBongoSeqs.play.id, player.pendingSequence.id)
            assertEquals(1, player.count(PenguinBongoObjs.bongos))
        }

    private fun GameTestState.runPenguinBongoTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            scripts = arrayOf(PenguinBongos::class),
            testBody = body,
        )

    private fun GameTestScope.prepareEligiblePlayer() {
        player.members = true
        player.setVarBit(PenguinBongoVarBits.coldWarProgress, 1)
        player.stats[stats.crafting] = 30
    }

    private fun GameTestScope.prepareIngredients() {
        player.inv[0] = InvObj(PenguinBongoObjs.leather)
        player.inv[1] = InvObj(PenguinBongoObjs.mahoganyPlank)
    }

    private fun GameTestScope.assertIngredientsPreserved() {
        assertEquals(1, player.count(PenguinBongoObjs.leather))
        assertEquals(1, player.count(PenguinBongoObjs.mahoganyPlank))
        assertEquals(0, player.count(PenguinBongoObjs.bongos))
    }

    private fun GameTestScope.craft(deps: Dependencies) {
        player.withProtectedAccess { with(deps.bongos) { craft() } }
    }

    private fun GameTestScope.play(deps: Dependencies) {
        player.withProtectedAccess { with(deps.bongos) { play() } }
    }

    private data class Dependencies
    @Inject
    constructor(
        val bongos: PenguinBongos,
        val heldU: HeldUInteractions,
        val objTypes: ObjTypeList,
        val seqTypes: SeqTypeList,
    )
}
