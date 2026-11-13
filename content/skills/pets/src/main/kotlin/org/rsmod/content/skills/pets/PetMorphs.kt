package org.rsmod.content.skills.pets

import jakarta.inject.Inject
import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter.set
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpc4
import org.rsmod.api.script.onOpNpcU
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.varbit.VarBitTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private enum class PetUnlock(val mask: Int = 0, val varbit: Int = 0) {
    BeaverLogs(1), Pheasant(2), Fox(4), BlueHeron(8),
    GreenPhoenix(16), BluePhoenix(32), WhitePhoenix(64), PurplePhoenix(128),
    DarkSquirrel(varbit = 10416), BoneSquirrel(varbit = 11353),
}

private data class Recolour(
    val material: Int,
    val form: Int,
    val quantity: Int = 1,
    val unlock: PetUnlock? = null,
)

public class PetMorphs @Inject constructor(
    private val pets: SkillPets,
    private val npcs: NpcTypeList,
    private val objects: ObjTypeList,
    private val bits: VarBitTypeList,
) : PluginScript() {
    private val recipes = buildMap {
        put(Species.Tangleroot, paints(5312 to 20661,23661 to 24555,22877 to 24557,
            5291 to 24559,22887 to 24561,22871 to 24563))
        put(Species.Rocky, paints(239 to 20663,1951 to 24847,6018 to 24849))
        put(Species.Golem, paints(1480 to 13321,438 to 21187,436 to 21188,440 to 21189,
            668 to 21190,442 to 21191,453 to 21192,444 to 21193,447 to 21194,
            6979 to 21195,6981 to 21195,6983 to 21195,449 to 21196,451 to 21197,
            21347 to 21340,13356 to 21358,2892 to 21359,9632 to 21360))
        put(Species.Beaver, paints(1511 to 13322,1521 to 28229,1519 to 28230,
            1517 to 28231,1515 to 28232,1513 to 28233,19669 to 28234,6333 to 28235,
            6332 to 28236,10810 to 28237) + listOf(
                Recolour(28138,13322,unlock=PetUnlock.BeaverLogs),
                Recolour(28663,28669,unlock=PetUnlock.Pheasant),
                Recolour(28626,28670,unlock=PetUnlock.Fox)))
        put(Species.Squirrel, listOf(Recolour(24733,24701,unlock=PetUnlock.DarkSquirrel),
            Recolour(30042,30151,unlock=PetUnlock.BoneSquirrel)))
        put(Species.Phoenix, listOf(Recolour(7330,24483,250,PetUnlock.GreenPhoenix),
            Recolour(7331,24484,250,PetUnlock.BluePhoenix),Recolour(10327,24485,250,PetUnlock.WhitePhoenix),
            Recolour(10326,24486,250,PetUnlock.PurplePhoenix)))
        put(Species.Heron, listOf(Recolour(25588,25600,3000,PetUnlock.BlueHeron)) +
            RAW_FISH.map { Recolour(it,13320) })
    }

    override fun ScriptContext.startup() {
        for (form in petForms) {
            val item = objects.getValue(form.item)
            val npc = npcs.getValue(form.npc)
            for (recipe in recipes[form.species].orEmpty()) {
                val material = objects.getValue(recipe.material)
                onOpNpcU(npc,material) {
                    if (pets.owns(player,it.npc)) recolour(form,recipe,null,it.invSlot)
                }
                if (form.species != Species.Phoenix) {
                    onOpHeldU(item,material) { recolour(form,recipe,it.firstSlot,it.secondSlot) }
                }
            }
            if (item.iop.indexOf("Metamorphosis") == 2) onOpHeld3(item) { choose(form,it.slot,null) }
            when (npc.op.indexOf("Metamorphosis")) {
                2 -> onOpNpc3(npc) { choose(form,null,it.npc) }
                3 -> onOpNpc4(npc) { choose(form,null,it.npc) }
            }
        }
    }

    private fun ProtectedAccess.recolour(form: PetForm, recipe: Recolour, slot: Int?, materialSlot: Int) {
        if (!player.members || (slot != null && inv[slot]?.id != form.item)) return
        if (inv[materialSlot]?.id != recipe.material) return
        val unlock = recipe.unlock
        val known = unlock != null && player.knows(unlock)
        if (form.species == Species.Beaver && unlock == null && !player.knows(PetUnlock.BeaverLogs)) {
            mes("Use a funky shaped log on your beaver to unlock its log colours.")
            return
        }
        if (known && unlock != PetUnlock.BlueHeron) {
            mes("You have already unlocked this transformation.")
            return
        }
        if (recipe.form == form.item && (unlock == null || known)) return
        val quantity = if (known) 1 else recipe.quantity
        val material = objects.getValue(recipe.material)
        if (!player.invDel(inv,material,quantity,slot=materialSlot).success) {
            mes("You need $quantity ${material.name.lowercase()} for this transformation.")
            return
        }
        if (unlock != null) player.unlock(unlock)
        if (unlock == PetUnlock.BeaverLogs) {
            mes("You can now use logs on your beaver to change its colour.")
        } else {
            pets.morph(player,petForms.first { it.item == recipe.form },slot)
        }
    }

    private suspend fun ProtectedAccess.choose(form: PetForm, slot: Int?, npc: Npc?) {
        fun present() = player.members && if (slot == null) npc != null && pets.owns(player,npc)
            else inv[slot]?.id == form.item
        if (!present()) return
        val target = if (form.species == Species.Chinchompa) {
            if (form.item == GOLD_CHIN && !choice2("Change its golden colour.",true,"Keep the golden colour.",false)) return
            val ordinary = petForms.filter { it.species == form.species && it.item != GOLD_CHIN && it != form }
            if (random.of(10_000) == 0) petForms.first { it.item == GOLD_CHIN } else random.pick(ordinary)
        } else {
            // Log colours require feeding the corresponding log. The free menu offers the beaver,
            // pheasant and fox species, and permanently unlocked colours for the other pets.
            val forms = choices(form).filter { it.second == null || player.knows(checkNotNull(it.second)) }
                .map { it.first }.filter { it.item != form.item }
            if (forms.isEmpty()) {
                mes("You have no other transformations unlocked.")
                return
            }
            if (forms.size == 1) forms.single() else {
                val index = menu("Choose a transformation",true,forms.map(::label))
                forms.getOrNull(index) ?: return
            }
        }
        if (present()) pets.morph(player,target,slot)
    }

    private fun choices(form: PetForm): List<Pair<PetForm,PetUnlock?>> {
        val species = form.species
        val unlocked = recipes[species].orEmpty().filter { it.unlock != null && it.unlock != PetUnlock.BeaverLogs }
        val base = petForms.first { it.species == species }
        return listOf(base to null) + unlocked.map { r -> petForms.first { it.item == r.form } to r.unlock }
    }
    private fun label(form: PetForm): String = when (form.item) {
        20693 -> "Red phoenix"
        24483 -> "Green phoenix"
        24484 -> "Blue phoenix"
        24485 -> "White phoenix"
        24486 -> "Purple phoenix"
        else -> objects.getValue(form.item).name
    }
    private fun Player.knows(unlock: PetUnlock): Boolean = if (unlock.varbit != 0)
        vars[bits.getValue(unlock.varbit)] != 0 else vars[PetVars.forms] and unlock.mask != 0
    private fun Player.unlock(unlock: PetUnlock) {
        if (unlock.varbit != 0) set(this,bits.getValue(unlock.varbit),1)
        else set(this,PetVars.forms,vars[PetVars.forms] or unlock.mask)
    }
    private companion object {
        const val GOLD_CHIN = 13326
        val RAW_FISH = listOf(317,321,327,331,335,3379,341,345,349,353,359,363,371,377,383,389,395,
            7944,3142,3150,5001,10138,11934,13439,11328,11330,11332,22826,22829,22832,22835)
        fun paints(vararg pairs: Pair<Int,Int>) = pairs.map { Recolour(it.first,it.second) }
    }
}
