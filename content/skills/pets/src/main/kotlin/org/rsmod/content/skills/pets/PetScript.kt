package org.rsmod.content.skills.pets

import jakarta.inject.Inject
import org.rsmod.api.player.events.PlayerDeathEvent
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.api.script.*
import org.rsmod.api.type.refs.comp.ComponentReferences
import org.rsmod.api.type.refs.interf.InterfaceReferences
import org.rsmod.game.entity.Player
import org.rsmod.game.type.interf.IfButtonOp
import org.rsmod.game.type.interf.IfEvent
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object InsuranceInterface : InterfaceReferences() { val main = find("pet_insurance") }
internal object InsuranceButtons : ComponentReferences() { val pets = find("pet_insurance:pets") }

public class PetScript @Inject constructor(
    private val pets: SkillPets,
    private val npcs: NpcTypeList,
    private val objects: ObjTypeList,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onPlayerLogin { pets.login(player) }
        onPlayerLogout { pets.logout(player) }
        onEvent<PlayerDeathEvent.Unsafe> { pets.death(player) }
        onIfOpen(InsuranceInterface.main) { refresh(player) }
        onIfModalButton(InsuranceButtons.pets) {
            // Script 6138 reserves five subcomponents per species, including each hidden row.
            val row = it.comsub - FIRST_ROW
            if (it.op == IfButtonOp.Op1 && row >= 0 && row % ROW_SIZE == 0) {
                Species.entries.firstOrNull { species -> species.index == row / ROW_SIZE }?.let { species ->
                    pets.reclaim(player,species)
                    refresh(player)
                }
            }
        }
        val eye = objects.getValue(26820)
        for (form in petForms) {
            val npc = npcs.getValue(form.npc)
            val item = objects.getValue(form.item)
            onOpHeld5(item) { pets.release(player,form,it.slot) }
            when (npc.op.indexOf("Pick-up")) {
                2 -> onOpNpc3(npc) { pets.pickup(player,it.npc) }
                3 -> onOpNpc4(npc) { pets.pickup(player,it.npc) }
            }
            onOpNpc1(npc) {
                if (!pets.owns(player,it.npc)) return@onOpNpc1
                val pet = it.npc
                when (form.species) {
                    Species.Chick -> startDialogue(pet) { chatNpc(neutral,"*Chirp!*") }
                    Species.Protector -> startDialogue(pet) {
                        chatNpc(neutral,"We should visit the Abyss more often.")
                        chatPlayer(neutral,"How often?")
                        chatNpc(neutral,"All the time!")
                    }
                    Species.Guardian -> {
                        val choice = menu("Talk to your guardian",false,listOf("How does your rift feel?", "About your colour...", "Leave"))
                        if (choice == 0) startDialogue(pet) { chatNpc(neutral,"I can feel the rift moving inside me.") }
                        if (choice == 1 && pets.owns(player,pet)) {
                            val lock = choice2("Keep your present colour.",true,"Adopt the colour of each altar.",false)
                            if (pets.owns(player,pet)) vars[PetBits.locked] = if (lock) 1 else 0
                        }
                    }
                    else -> mes("You spend a moment with your ${item.name.lowercase()}.")
                }
            }
            if (form.species == Species.Guardian) {
                onOpNpc4(npc) {
                    if (pets.owns(player,it.npc)) {
                        val lock = vars[PetBits.locked] == 0
                        vars[PetBits.locked] = if (lock) 1 else 0
                        mes(if (lock) "Your rift guardian will keep its current colour." else "Your rift guardian will change colour at altars.")
                    }
                }
                onOpNpcU(npc,eye) { if (pets.owns(player,it.npc)) pets.changeGuardian(player,guardianForms.last()) }
                onOpHeldU(item,eye) { pets.transformHeld(player,it.firstSlot) }
            }
        }
    }
    private fun refresh(player: Player) {
        val args = IntArray(15)
        for (species in Species.entries) {
            val i = species.index
            val location = pets.location(player,species)
            if (location == 0 && !pets.insured(player,species)) continue
            val mask = 1 shl (i % 31)
            args[i / 31] = args[i / 31] or mask
            if (location != 0) args[3 + i / 31] = args[3 + i / 31] or mask
            val locationWord = 6 + i / 8
            args[locationWord] = args[locationWord] or (location shl (4 * (i % 8)))
            if (location == 0) {
                val row = FIRST_ROW + i * ROW_SIZE
                player.ifSetEvents(InsuranceButtons.pets,row..row,IfEvent.Op1)
            }
        }
        player.runClientScript(6126,*args.toTypedArray())
    }
    private companion object {
        // Two title/background pairs and two empty-state labels precede the list in clientscript 6137.
        const val FIRST_ROW = 6
        const val ROW_SIZE = 5
    }
}
