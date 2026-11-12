package org.rsmod.content.areas.city.lumbridge.npcs

import jakarta.inject.Inject
import net.rsprot.protocol.game.outgoing.interfaces.IfSetText
import net.rsprot.protocol.game.outgoing.misc.player.RunClientScript
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.NpcUInteractions
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.areas.city.lumbridge.configs.lumbridge_npcs
import org.rsmod.game.entity.Npc
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList

@Execution(ExecutionMode.SAME_THREAD)
class BobRepairTest {
    @Test
    fun GameTestState.`cache 233 Bob and representative repair states are pinned`() =
        runBobTest { deps ->
            val bob = deps.npcs[lumbridge_npcs.bob]
            assertEquals(10619, bob.id)
            assertEquals(listOf("Talk-to", null, "Trade", "Repair", null), bob.op.toList())
            assertEquals(4708, objs.ahrims_hood.id)
            assertEquals(4856, objs.ahrims_hood_100.id)
            assertEquals(4860, objs.ahrims_hood_0.id)
            assertEquals(30445, objs.ahrims_hood_or.id)
            assertEquals(30543, objs.ahrims_hood_or_0.id)
            assertEquals(29010, objs.eclipse_moon_helm.id)
            assertEquals(29035, objs.eclipse_moon_helm_used.id)
            assertEquals(29055, objs.eclipse_moon_helm_broken.id)
            assertEquals("Check", deps.objs[objs.eclipse_moon_helm_used].iop[2])
            assertEquals(1_000, BobRepairService.BARROWS_MAX_POINTS)
            assertEquals(3_000, BobRepairService.MOON_MAX_POINTS)
        }

    @Test
    fun GameTestState.`fully broken Barrows set costs 330000 and repairs atomically`() =
        runBobTest { deps ->
            player.inv[0] = InvObj(objs.ahrims_hood_0)
            player.inv[1] = InvObj(objs.ahrims_robetop_0)
            player.inv[2] = InvObj(objs.ahrims_robeskirt_0)
            player.inv[3] = InvObj(objs.ahrims_staff_0)
            player.inv[4] = InvObj(objs.coins, 330_000)

            val plan = quoteAll(deps.repairs)
            assertEquals(330_000, plan.totalCost)
            assertEquals(listOf(60_000, 90_000, 80_000, 100_000), plan.quotes.map { it.cost })
            assertEquals(BobRepairResult.Repaired(4, 330_000), repair(deps.repairs, plan))

            assertEquals(objs.ahrims_hood.id, player.inv[0]?.id)
            assertEquals(objs.ahrims_robetop.id, player.inv[1]?.id)
            assertEquals(objs.ahrims_robeskirt.id, player.inv[2]?.id)
            assertEquals(objs.ahrims_staff.id, player.inv[3]?.id)
            assertEquals(0, player.inv.count(deps.objs[objs.coins]))
        }

    @Test
    fun GameTestState.`Barrows prices use exact internal points across named states`() =
        runBobTest { deps ->
            player.inv[0] = InvObj(objs.dharoks_greataxe_100, vars = 999)
            player.inv[1] = InvObj(objs.dharoks_platebody_75, vars = 749)
            player.inv[2] = InvObj(objs.dharoks_platelegs_50, vars = 500)
            player.inv[3] = InvObj(objs.dharoks_helm_25)

            val plan = quoteAll(deps.repairs)
            assertEquals(listOf(100, 22_590, 40_000, 45_000), plan.quotes.map { it.cost })
            assertEquals(107_690, plan.totalCost)
        }

    @Test
    fun GameTestState.`Moon armour costs 500 per missing point and weapons are excluded`() =
        runBobTest { deps ->
            player.inv[0] = InvObj(objs.eclipse_moon_helm_used, vars = 2_999)
            player.inv[1] = InvObj(objs.blue_moon_chestplate_broken)
            player.inv[2] = InvObj(objs.blue_moon_spear)
            player.inv[3] = InvObj(objs.coins, 1_500_500)

            val plan = quoteAll(deps.repairs)
            assertEquals(listOf(500, 1_500_000), plan.quotes.map { it.cost })
            assertEquals(1_500_500, plan.totalCost)
            assertEquals(BobRepairResult.Repaired(2, 1_500_500), repair(deps.repairs, plan))
            assertEquals(objs.eclipse_moon_helm.id, player.inv[0]?.id)
            assertEquals(objs.blue_moon_chestplate.id, player.inv[1]?.id)
            assertEquals(objs.blue_moon_spear.id, player.inv[2]?.id)
        }

    @Test
    fun GameTestState.`failed payment and stale quotes preserve every item`() = runBobTest { deps ->
        player.inv[0] = InvObj(objs.karils_crossbow_0, vars = 17)
        player.inv[1] = InvObj(objs.coins, 99_999)
        val plan = quoteAll(deps.repairs)

        assertEquals(BobRepairResult.InsufficientCoins(100_000), repair(deps.repairs, plan))
        assertEquals(InvObj(objs.karils_crossbow_0, vars = 17), player.inv[0])
        assertEquals(99_999, player.inv[1]?.count)

        player.inv[0] = InvObj(objs.karils_crossbow_25, vars = 200)
        assertEquals(BobRepairResult.InventoryChanged, repair(deps.repairs, plan))
        assertEquals(InvObj(objs.karils_crossbow_25, vars = 200), player.inv[0])
        assertEquals(99_999, player.inv[1]?.count)
    }

    @Test
    fun GameTestState.`right click Repair offers the exact all-items transaction`() =
        runBobTest { deps ->
            player.inv[0] = InvObj(objs.veracs_helm_0)
            player.inv[1] = InvObj(objs.coins, 60_000)
            val bob = spawnNpc(player.coords.translate(1, 0), deps.npcs[lumbridge_npcs.bob])

            player.opNpc4(bob)
            advance()
            assertMenu("Select an option", "Repair all items: 60,000 coins.", "Cancel.")
            choose(1)

            assertEquals(objs.veracs_helm.id, player.inv[0]?.id)
            assertEquals(0, player.inv.count(deps.objs[objs.coins]))
            assertDialogueText("The item is repaired.")
            resume(components.messagebox_pbutton)
            assertDialogueText("There you go, happy doing business with you!")
        }

    @Test
    fun GameTestState.`using repairable equipment on Bob offers its exact transaction`() =
        runBobTest { deps ->
            val bob = spawnNpc(player.coords, deps.npcs[lumbridge_npcs.bob])
            player.inv[0] = InvObj(objs.guthans_chainskirt_0)
            player.inv[1] = InvObj(objs.coins, 80_000)
            useOnBob(deps, bob, 0, objs.guthans_chainskirt_0)
            assertMenu("Select an option", "Repair that item: 80,000 coins.", "Cancel.")
            choose(1)
            assertEquals(objs.guthans_chainskirt.id, player.inv[0]?.id)
            assertDialogueText("The item is repaired.")
            resume(components.messagebox_pbutton)
            assertDialogueText("There you go, happy doing business with you!")
        }

    @Test
    fun GameTestState.`using an ordinary item on Bob follows the exact refusal branch`() =
        runBobTest { deps ->
            val bob = spawnNpc(player.coords, deps.npcs[lumbridge_npcs.bob])
            player.inv[0] = InvObj(objs.beer)
            useOnBob(deps, bob, 0, objs.beer)
            assertDialogueText("Sorry friend, but I can't do anything with that.")
        }

    @Test
    fun GameTestState.`right click Repair with no eligible equipment uses the exact refusal`() =
        runBobTest { deps ->
            val bob = spawnNpc(player.coords.translate(1, 0), deps.npcs[lumbridge_npcs.bob])
            player.inv[0] = InvObj(objs.beer)
            player.opNpc4(bob)
            advance()
            assertDialogueText("You don't have anything I can repair.")
        }

    private fun GameTestScope.quoteAll(service: BobRepairService): BobRepairPlan {
        var result: BobRepairPlan? = null
        player.withProtectedAccess { result = service.quoteAll(this) }
        return checkNotNull(result)
    }

    private fun GameTestScope.repair(
        service: BobRepairService,
        plan: BobRepairPlan,
    ): BobRepairResult {
        var result: BobRepairResult? = null
        player.withProtectedAccess { result = service.repair(this, plan) }
        return checkNotNull(result)
    }

    private fun GameTestScope.useOnBob(deps: Dependencies, npc: Npc, slot: Int, obj: ObjType) {
        player.withProtectedAccess {
            deps.interactions.interactOp(
                this,
                npc,
                player.inv,
                invSlot = slot,
                npc.type,
                deps.objs[obj],
            )
        }
    }

    private fun GameTestScope.choose(option: Int) {
        resume(components.chatmenu_pbutton, option)
    }

    private fun GameTestScope.resume(
        component: org.rsmod.game.type.comp.ComponentType,
        sub: Int = -1,
    ) {
        player.resumeActiveCoroutine(ResumePauseButtonInput(component, subcomponent = sub))
    }

    private fun GameTestScope.assertDialogueText(text: String) {
        val texts = client.filterIsInstance<IfSetText>().map { it.text.replace("<br>", " ") }
        assertTrue(texts.contains(text)) { "Dialogue text not found: $text | actual=$texts" }
    }

    private fun GameTestScope.assertMenu(title: String, vararg options: String) {
        val expected = listOf(title, options.joinToString("|"))
        val menus = client.filterIsInstance<RunClientScript>().filter { it.id == 58 }
        assertTrue(menus.any { it.values == expected }) {
            "Dialogue menu not found: $expected | actual=${menus.map { it.values }}"
        }
    }

    private fun GameTestState.runBobTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(Dependencies::class, scripts = arrayOf(Bob::class), testBody = body)

    data class Dependencies
    @Inject
    constructor(
        val repairs: BobRepairService,
        val npcs: NpcTypeList,
        val objs: ObjTypeList,
        val interactions: NpcUInteractions,
    )
}
