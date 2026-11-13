package org.rsmod.content.skills.agility

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.*
import org.rsmod.api.player.EnduranceRing
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.ring
import org.rsmod.api.script.*
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object EnduranceItems : ObjReferences() {
    val empty = find("ring_of_endurance_nocharges")
    val former = find("ring_of_endurance_uncharged")
}

public class RingOfEndurance @Inject constructor() : PluginScript() {
    private val potions
        get() =
            listOf(
                objs.vial_empty,
                objs.stamina_potion_1,
                objs.stamina_potion_2,
                objs.stamina_potion_3,
                objs.stamina_potion_4,
            )

    private val mixes
        get() = listOf(objs.vial_empty, objs.stamina_mix_1, objs.stamina_mix_2)

    override fun ScriptContext.startup() {
        onOpHeld2(EnduranceItems.former) {
            if (player.members && inv[it.slot]?.id == EnduranceItems.former.id)
                inv[it.slot] = InvObj(EnduranceItems.empty)
        }
        for (ring in listOf(EnduranceItems.empty, objs.ring_of_endurance)) {
            onOpHeld2(ring) {
                if (!player.members || statBase(stats.agility) < 70) {
                    mes("You need an Agility level of 70 to wear this ring.")
                    return@onOpHeld2
                }
                invEquip(it.slot)
            }
            for (family in listOf(potions, mixes)) for (dose in 1 until family.size) {
                onOpHeldU(ring, family[dose]) { e ->
                    val ringSlot =
                        if (inv[e.firstSlot]?.id == ring.id) e.firstSlot else e.secondSlot
                    val vialSlot = if (ringSlot == e.firstSlot) e.secondSlot else e.firstSlot
                    charge(ringSlot, vialSlot, dose, family)
                }
            }
        }
        onOpHeld4(objs.ring_of_endurance) { checkCharges(inv[it.slot]) }
        onOpHeld5(objs.ring_of_endurance) {
            val item = inv[it.slot] ?: return@onOpHeld5
            if (
                !choice2(
                    "Lose all charges and uncharge the ring.",
                    true,
                    "Keep the charges.",
                    false,
                ) || item != inv[it.slot]
            )
                return@onOpHeld5
            inv[it.slot] = InvObj(EnduranceItems.empty)
        }
        onOpWorn2(objs.ring_of_endurance) { checkCharges(player.ring) }
    }

    private fun ProtectedAccess.charge(
        ringSlot: Int,
        vialSlot: Int,
        dose: Int,
        family: List<ObjType>,
    ) {
        if (!player.members) return
        val ring = inv[ringSlot] ?: return
        if (inv[vialSlot]?.id != family[dose].id) return
        val current = EnduranceRing.charges(ring)
        val added = minOf(EnduranceRing.MAX_CHARGES - current, dose)
        if (added == 0) {
            mes("Your ring is already fully charged.")
            return
        }
        if (
            player
                .invTransaction(inv) {
                    val bag = select(inv)
                    delete(bag, ring.id, 1, ringSlot)
                    delete(bag, family[dose].id, 1, vialSlot)
                    add(
                        bag,
                        objs.ring_of_endurance.id,
                        1,
                        vars = EnduranceRing.withCharges(ring, current + added).vars,
                        slot = ringSlot,
                    )
                    add(bag, family[dose - added].id, 1, slot = vialSlot)
                }
                .success
        )
            checkCharges(inv[ringSlot])
    }

    private fun ProtectedAccess.checkCharges(item: InvObj?) {
        val amount = EnduranceRing.charges(item)
        mes("Your ring of endurance has $amount charge${if(amount==1)"" else "s"} remaining.")
    }
}
