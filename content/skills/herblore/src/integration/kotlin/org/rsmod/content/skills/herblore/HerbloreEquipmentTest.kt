package org.rsmod.content.skills.herblore

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import net.rsprot.protocol.game.outgoing.interfaces.IfSetText
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.events.interact.HeldObjEvents
import org.rsmod.api.player.events.interact.HeldUEvents
import org.rsmod.api.player.input.ResumePCountDialogInput
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.HeldUInteractions
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.events.EventBus
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList

@Execution(ExecutionMode.SAME_THREAD)
class HerbloreEquipmentTest {
    @Test
    fun GameTestState.`cache 233 pins equipment identities and inventory operations`() =
        runEquipmentTest { deps ->
            assertEquals(21163, objs.amulet_of_chemistry.id)
            assertEquals(21164, objs.noted_amulet_of_chemistry.id)
            assertEquals(
                listOf(null, "Wear", "Options", "Break", null),
                deps.objs[objs.amulet_of_chemistry].iop.toList(),
            )
            assertTrue(
                deps.eventBus.contains(
                    HeldObjEvents.Op4::class.java,
                    objs.amulet_of_chemistry.id.toLong(),
                )
            )
            assertEquals(29988, objs.alchemists_amulet_uncharged.id)
            assertEquals(29990, objs.alchemists_amulet_charged.id)
            assertEquals(29974, objs.prescription_goggles.id)
            assertEquals(29976, objs.prescription_goggles_focused.id)
            assertEquals(
                listOf(null, "Wear", null, "Charge", null),
                deps.objs[objs.alchemists_amulet_uncharged].iop.toList(),
            )
            assertEquals("Check", deps.objs[objs.alchemists_amulet_charged].iop[2])
            assertEquals("Charge", deps.objs[objs.alchemists_amulet_charged].iop[3])
            assertEquals("Uncharge", deps.objs[objs.alchemists_amulet_charged].iop[4])
            assertEquals(
                listOf(null, "Wear", "Change", null, null),
                deps.objs[objs.prescription_goggles].iop.toList(),
            )
            assertEquals("Change", deps.objs[objs.prescription_goggles_focused].iop[2])
        }

    @Test
    fun GameTestState.`breaking a chemistry amulet destroys it and resets shared charges`() =
        runEquipmentTest { deps ->
            preparePlayer()
            player.inv[5] = InvObj(objs.amulet_of_chemistry)
            repeat(4) { deps.chemistryAmulet.consume(player) }
            assertEquals(1, deps.chemistryAmulet.remaining(player))

            player.withProtectedAccess { deps.equipment.breakChemistryAmulet(this, 5) }

            assertEquals(null, player.inv[5])
            assertEquals(
                ChemistryAmuletRepository.MAX_CHARGES,
                deps.chemistryAmulet.remaining(player),
            )
        }

    @Test
    fun GameTestState.`charging consumes chemistry amulets and preserves the selected slot`() =
        runEquipmentTest { deps ->
            preparePlayer()
            player.inv[4] = InvObj(objs.alchemists_amulet_uncharged)
            add(objs.amulet_of_chemistry, 3)
            player.setVarBit(varbits.alchemists_amulet_charges, 0)

            player.withProtectedAccess { deps.equipment.chargeAlchemistsAmulet(this, 4) }
            player.resumeActiveCoroutine(ResumePCountDialogInput(2))

            assertEquals(objs.alchemists_amulet_charged.id, player.inv[4]?.id)
            assertEquals(1, player.count(objs.amulet_of_chemistry))
            assertEquals(20, player.vars[varbits.alchemists_amulet_charges])
            assertEquals(
                true,
                client.filterIsInstance<IfSetText>().any {
                    it.text == "You apply 20 charges to your Alchemist's amulet."
                },
            )
            resumeMessageBox()
        }

    @Test
    fun GameTestState.`both held-use orders open charging and additional charges cap input`() =
        runEquipmentTest { deps ->
            repeat(2) { reverse ->
                preparePlayer()
                player.inv[3] = InvObj(objs.alchemists_amulet_charged)
                player.inv[7] = InvObj(objs.amulet_of_chemistry)
                player.setVarBit(varbits.alchemists_amulet_charges, 4_990)
                assertTrue(
                    deps.eventBus.contains(
                        HeldUEvents.Type::class.java,
                        EventBus.composeLongKey(
                            objs.amulet_of_chemistry.id,
                            objs.alchemists_amulet_charged.id,
                        ),
                    )
                )
                player.withProtectedAccess {
                    if (reverse == 0) {
                        deps.heldU.interact(
                            this,
                            player.inv,
                            deps.objs[objs.amulet_of_chemistry],
                            7,
                            deps.objs[objs.alchemists_amulet_charged],
                            3,
                        )
                    } else {
                        deps.heldU.interact(
                            this,
                            player.inv,
                            deps.objs[objs.alchemists_amulet_charged],
                            3,
                            deps.objs[objs.amulet_of_chemistry],
                            7,
                        )
                    }
                }
                assertEquals(true, player.isAccessProtected, "reverse=$reverse")
                player.resumeActiveCoroutine(ResumePCountDialogInput(10))

                assertEquals(objs.alchemists_amulet_charged.id, player.inv[3]?.id)
                assertEquals(0, player.count(objs.amulet_of_chemistry), "reverse=$reverse")
                assertEquals(5_000, player.vars[varbits.alchemists_amulet_charges])
                assertEquals(
                    true,
                    client.filterIsInstance<IfSetText>().any {
                        it.text.replace("<br>", " ") ==
                            "You apply an additional 10 charges to your Alchemist's amulet. " +
                                "It now has 5000 charges in total."
                    },
                )
                resumeMessageBox()
            }
        }

    @Test
    fun GameTestState.`charging failure states do not mutate equipment or charges`() =
        runEquipmentTest { deps ->
            preparePlayer()
            player.inv[0] = InvObj(objs.alchemists_amulet_uncharged)
            player.setVarBit(varbits.alchemists_amulet_charges, 0)
            player.withProtectedAccess { deps.equipment.chargeAlchemistsAmulet(this, 0) }
            assertMessageSent(
                "You don't appear to have any Amulets of Chemistry which you can use to charge " +
                    "your Alchemist's amulet."
            )
            assertEquals(objs.alchemists_amulet_uncharged.id, player.inv[0]?.id)

            client.clear()
            add(objs.amulet_of_chemistry)
            player.setVarBit(varbits.alchemists_amulet_charges, 5_000)
            player.withProtectedAccess { deps.equipment.chargeAlchemistsAmulet(this, 0) }
            assertMessageSent("Your Alchemist's amulet is already fully charged.")
            assertEquals(1, player.count(objs.amulet_of_chemistry))
        }

    @Test
    fun GameTestState.`uncharging returns one noted amulet per ten charges and loses remainder`() =
        runEquipmentTest { deps ->
            preparePlayer()
            player.inv[6] = InvObj(objs.alchemists_amulet_charged)
            player.setVarBit(varbits.alchemists_amulet_charges, 25)

            player.withProtectedAccess { deps.equipment.unchargeAlchemistsAmulet(this, 6) }
            player.resumeActiveCoroutine(
                ResumePauseButtonInput(components.chatmenu_pbutton, subcomponent = 1)
            )

            assertEquals(objs.alchemists_amulet_uncharged.id, player.inv[6]?.id)
            assertEquals(2, player.count(objs.noted_amulet_of_chemistry))
            assertEquals(0, player.vars[varbits.alchemists_amulet_charges])
            assertEquals(
                true,
                client.filterIsInstance<IfSetText>().any {
                    it.text.replace("<br>", " ") ==
                        "You uncharge your Alchemist's amulet, regaining 2 amulets of chemistry " +
                            "in the process."
                },
            )
            resumeMessageBox()
        }

    @Test
    fun GameTestState.`uncharging is atomic when the noted refund cannot fit`() =
        runEquipmentTest { deps ->
            preparePlayer()
            player.inv[0] = InvObj(objs.alchemists_amulet_charged)
            for (slot in 1 until player.inv.size) {
                player.inv[slot] = InvObj(objs.coins)
            }
            player.setVarBit(varbits.alchemists_amulet_charges, 20)

            player.withProtectedAccess { deps.equipment.unchargeAlchemistsAmulet(this, 0) }
            player.resumeActiveCoroutine(
                ResumePauseButtonInput(components.chatmenu_pbutton, subcomponent = 1)
            )

            assertEquals(objs.alchemists_amulet_charged.id, player.inv[0]?.id)
            assertEquals(0, player.count(objs.noted_amulet_of_chemistry))
            assertEquals(20, player.vars[varbits.alchemists_amulet_charges])
            assertMessageSent("You don't have enough inventory space.")
        }

    @Test
    fun GameTestState.`prescription goggles change appearance in place in both directions`() =
        runEquipmentTest { deps ->
            preparePlayer()
            player.inv[9] = InvObj(objs.prescription_goggles)
            player.withProtectedAccess { deps.equipment.changePrescriptionGoggles(this, 9) }
            assertEquals(objs.prescription_goggles_focused.id, player.inv[9]?.id)
            player.withProtectedAccess { deps.equipment.changePrescriptionGoggles(this, 9) }
            assertEquals(objs.prescription_goggles.id, player.inv[9]?.id)
        }

    private fun GameTestScope.preparePlayer() {
        player.clearAllInvs()
        player.setVarBit(varbits.alchemists_amulet_charges, 0)
        client.clear()
    }

    private fun GameTestScope.add(obj: ObjType, count: Int = 1) {
        repeat(count) {
            val slot = player.inv.indexOfFirst { it == null }
            require(slot >= 0)
            player.inv[slot] = InvObj(obj)
        }
    }

    private fun GameTestScope.resumeMessageBox() {
        player.resumeActiveCoroutine(
            ResumePauseButtonInput(components.objectbox_pbutton, subcomponent = -1)
        )
        player.clearPendingAction()
    }

    private fun GameTestState.runEquipmentTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(Dependencies::class, TestModule, HerbloreScript::class, testBody = body)

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(HerbloreModule())
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val equipment: HerbloreEquipment,
        val heldU: HeldUInteractions,
        val eventBus: EventBus,
        val chemistryAmulet: ChemistryAmuletRepository,
        val objs: ObjTypeList,
    )
}
