package org.rsmod.content.skills.herblore

import jakarta.inject.Inject
import kotlin.math.min
import org.rsmod.api.config.refs.objs
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.type.obj.ObjTypeList

public class HerbloreEquipment
@Inject
constructor(
    private val objTypes: ObjTypeList,
    private val modifiers: PotionProductionModifiers,
    private val chemistryAmulet: ChemistryAmuletRepository,
) {
    public fun breakChemistryAmulet(access: ProtectedAccess, slot: Int) {
        with(access) {
            if (player.inv[slot]?.id != objs.amulet_of_chemistry.id) {
                return
            }
            val result =
                player.invTransaction(player.inv) {
                    delete(select(player.inv), objs.amulet_of_chemistry.id, 1, slot = slot)
                }
            if (result.success) {
                chemistryAmulet.reset(player)
            }
        }
    }

    public suspend fun chargeAlchemistsAmulet(access: ProtectedAccess, slot: Int) {
        with(access) {
            val amulet = player.inv[slot] ?: return
            val uncharged = amulet.id == objs.alchemists_amulet_uncharged.id
            if (!uncharged && amulet.id != objs.alchemists_amulet_charged.id) {
                return
            }
            val available = player.inv.count(objTypes[objs.amulet_of_chemistry])
            if (available == 0) {
                mes(NO_CHEMISTRY_AMULETS)
                return
            }
            val initialCharges = modifiers.alchemistsAmuletCharges(this)
            val capacity = (MAX_ALCHEMISTS_AMULET_CHARGES - initialCharges) / CHARGES_PER_AMULET
            if (capacity == 0) {
                mes(FULLY_CHARGED)
                return
            }
            val maximum = min(available, capacity)
            val requested =
                min(countDialog("How many amulets do you want to apply? (Up to $maximum)"), maximum)
            if (requested <= 0) {
                return
            }
            if (player.inv[slot]?.id != amulet.id) {
                return
            }
            if (modifiers.alchemistsAmuletCharges(this) != initialCharges) {
                return
            }
            val result =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, objs.amulet_of_chemistry.id, requested)
                    if (uncharged) {
                        delete(inventory, objs.alchemists_amulet_uncharged.id, 1, slot = slot)
                        add(inventory, objs.alchemists_amulet_charged.id, 1, slot = slot)
                    }
                }
            if (!result.success) {
                return
            }
            val addedCharges = requested * CHARGES_PER_AMULET
            val totalCharges = initialCharges + addedCharges
            modifiers.setAlchemistsAmuletCharges(this, totalCharges)
            if (uncharged) {
                objbox(
                    objs.alchemists_amulet_charged,
                    ITEM_BOX_ZOOM,
                    "You apply $addedCharges charges to your Alchemist's amulet.",
                )
            } else {
                objbox(
                    objs.alchemists_amulet_charged,
                    ITEM_BOX_ZOOM,
                    "You apply an additional $addedCharges charges to your Alchemist's amulet. " +
                        "It now has $totalCharges charges in total.",
                )
            }
        }
    }

    public fun checkAlchemistsAmulet(access: ProtectedAccess) {
        val charges = modifiers.alchemistsAmuletCharges(access)
        access.mes("Your Alchemist's amulet has $charges charges left.")
    }

    public suspend fun unchargeAlchemistsAmulet(access: ProtectedAccess, slot: Int) {
        with(access) {
            if (player.inv[slot]?.id != objs.alchemists_amulet_charged.id) {
                return
            }
            val initialCharges = modifiers.alchemistsAmuletCharges(this)
            val recovered = initialCharges / CHARGES_PER_AMULET
            val proceed =
                choice2(
                    "Proceed.",
                    true,
                    "Cancel.",
                    false,
                    title =
                        "Uncharge your amulet for all its charges? " +
                            "(regaining $recovered amulets)",
                )
            if (!proceed || player.inv[slot]?.id != objs.alchemists_amulet_charged.id) {
                return
            }
            if (modifiers.alchemistsAmuletCharges(this) != initialCharges) {
                return
            }
            val result =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, objs.alchemists_amulet_charged.id, 1, slot = slot)
                    add(inventory, objs.alchemists_amulet_uncharged.id, 1, slot = slot)
                    if (recovered > 0) {
                        add(inventory, objs.noted_amulet_of_chemistry.id, recovered)
                    }
                }
            if (!result.success) {
                mes("You don't have enough inventory space.")
                return
            }
            modifiers.setAlchemistsAmuletCharges(this, 0)
            objbox(
                objs.alchemists_amulet_uncharged,
                ITEM_BOX_ZOOM,
                "You uncharge your Alchemist's amulet, regaining $recovered amulets of " +
                    "chemistry in the process.",
            )
        }
    }

    public fun changePrescriptionGoggles(access: ProtectedAccess, slot: Int) {
        with(access) {
            val current = player.inv[slot] ?: return
            val replacement =
                when (current.id) {
                    objs.prescription_goggles.id -> objs.prescription_goggles_focused
                    objs.prescription_goggles_focused.id -> objs.prescription_goggles
                    else -> return
                }
            val result =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, current.id, 1, slot = slot)
                    add(inventory, replacement.id, 1, slot = slot)
                }
            check(result.success) { "Prescription-goggles same-slot transform failed." }
        }
    }

    private companion object {
        const val CHARGES_PER_AMULET = 10
        const val MAX_ALCHEMISTS_AMULET_CHARGES = 5_000
        const val ITEM_BOX_ZOOM = 400
        const val NO_CHEMISTRY_AMULETS =
            "You don't appear to have any Amulets of Chemistry which you can use to charge " +
                "your Alchemist's amulet."
        const val FULLY_CHARGED = "Your Alchemist's amulet is already fully charged."
    }
}
