package org.rsmod.content.skills.prayer.remains

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.invtx.*
import org.rsmod.api.player.events.interact.BankedObjEvents
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.*
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.content.interfaces.bank.scripts.BankInvScript
import org.rsmod.game.inv.isType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object SoulBearerItems : ObjReferences() {
    val journal = find("arceuus_library_soulbearerbook")
    val damaged = find("arceuus_soulbearer_damaged")
    val bearer = find("arceuus_soulbearer")
}

internal object SoulBearerPeople : NpcReferences() {
    val author = find("arceuus_soulguardian")
    val keeper = find("keeper_of_keys")
}

internal object SoulBearerBits : VarBitReferences() {
    val story = find("arceuus_soulbearer_story")
    val bankCharges = find("charges_soul_bearer_quantity")
}

internal object SoulBearerPrefs : org.rsmod.api.type.refs.varp.VarpReferences() {
    val skipConfirmation = find("soul_bearer_skip_confirmation")
}

internal object SoulBearerSettings : org.rsmod.api.type.builders.varp.VarpBuilder() {
    init {
        build("soul_bearer_skip_confirmation") {
            permanent = true
            transmitNever = true
        }
    }
}

internal object SoulBearerSequences : SeqReferences() {
    val dig = find("human_dig")
}

public class SoulBearer
@Inject
constructor(private val banking: BankInvScript, private val types: ObjTypeList) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeld1(SoulBearerItems.journal) {
            if (!player.members) return@onOpHeld1
            mes(
                "Aretha describes an artefact that carries souls to safety through the River of Souls."
            )
            if (vars[SoulBearerBits.story] == 0) vars[SoulBearerBits.story] = 1
            mes(
                "The author left its burial place a secret. Perhaps she will tell you where to look."
            )
        }
        onOpNpc1(SoulBearerPeople.author) { speakToAuthor() }
        onOpNpcU(SoulBearerPeople.author, SoulBearerItems.journal) { speakToAuthor() }
        onOpHeld1(objs.spade) {
            anim(SoulBearerSequences.dig)
            val location = coords
            delay(1)
            if (coords == location && objs.spade in inv) {
                val site = org.rsmod.api.player.events.SpadeDigDestination(player,location)
                publish(site)
                val destination = site.destination
                if (destination != null) telejump(destination) else dig(location)
            }
        }
        onOpNpc1(SoulBearerPeople.keeper) { repair() }
        onOpNpcU(SoulBearerPeople.keeper, SoulBearerItems.damaged) { repair() }
        onOpHeld3(SoulBearerItems.damaged) {
            mes("This soul bearer needs repair. The Key Master may be able to help.")
        }
        onOpHeld1(SoulBearerItems.bearer) { fill(it.slot) }
        onOpHeld3(SoulBearerItems.bearer) { mes("Charges remaining: ${it.obj.vars}.") }
        onOpHeld4(SoulBearerItems.bearer) { uncharge(it.slot) }
        for (rune in listOf(objs.blood_rune, objs.soul_rune)) {
            onOpHeldU(SoulBearerItems.bearer, rune) { charge(it.firstSlot) }
        }
        onProtectedEvent<BankedObjEvents.ConfigureCharges>(SoulBearerItems.bearer.id) {
            if (player.members)
                vars[SoulBearerBits.bankCharges] =
                    countDialog("How many charges should the soul bearer hold on withdrawal?")
                        .coerceIn(0, capacity)
        }
        onProtectedEvent<BankedObjEvents.Autocharge>(SoulBearerItems.bearer.id) {
            if (!player.members || inv[it.slot] != it.obj) return@onProtectedEvent
            val needed = vars[SoulBearerBits.bankCharges].coerceIn(0, capacity) - it.obj.vars
            if (needed > 0) {
                val pairs = banking.takeBankRunePairs(this, objs.blood_rune, objs.soul_rune, needed)
                if (pairs > 0) inv[it.slot] = it.obj.copy(vars = it.obj.vars + pairs)
                if (pairs < needed) mes("You need a blood rune and a soul rune for every charge.")
            }
        }
    }

    private fun ProtectedAccess.speakToAuthor() {
        if (!player.members) return
        if (vars[SoulBearerBits.story] == 0 || SoulBearerItems.journal !in inv) {
            mes("Aretha asks if you have read her book, The Journey of Souls.")
            return
        }
        if (vars[SoulBearerBits.story] == 1) vars[SoulBearerBits.story] = 2
        mes("Aretha tells you to dig in the crypt of the Arceuus church, then seek the Key Master.")
    }

    private fun ProtectedAccess.dig(tile: CoordGrid) {
        val stage = vars[SoulBearerBits.story]
        if (
            !player.members ||
                stage < 2 ||
                tile.level != 0 ||
                tile.x !in 1690..1710 ||
                tile.z !in 3786..3801
        ) {
            mes("You dig a hole in the ground.")
            return
        }
        if (
            listOf(SoulBearerItems.damaged, SoulBearerItems.bearer).any { it in inv || it in bank }
        ) {
            mes("You already have a soul bearer.")
            return
        }
        val item = if (stage == 3) SoulBearerItems.bearer else SoulBearerItems.damaged
        if (invAdd(inv, item).success) mes("You uncover a ${types[item].name.lowercase()}.")
        else mes("You need a free inventory space.")
    }

    private fun ProtectedAccess.repair() {
        if (!player.members) return
        if (vars[SoulBearerBits.story] == 2 && SoulBearerItems.damaged in inv) {
            if (invReplace(inv, SoulBearerItems.damaged, 1, SoulBearerItems.bearer).success) {
                vars[SoulBearerBits.story] = 3
                mes("The Key Master repairs your soul bearer.")
            }
        } else
            mes(
                "The Key Master guards the passages to Cerberus. Only assigned slayers may fight her."
            )
    }

    private suspend fun ProtectedAccess.charge(slot: Int) {
        if (!player.members) return
        val maximum =
            minOf(
                inv.count(types[objs.blood_rune]),
                inv.count(types[objs.soul_rune]),
                capacity - (inv[slot]?.vars ?: capacity),
            )
        if (maximum <= 0) {
            mes("You need both blood and soul runes, and space for the charges.")
            return
        }
        val requested = countDialog("How many charges would you like to add?").coerceIn(0, maximum)
        val held = inv[slot] ?: return
        if (!held.isType(SoulBearerItems.bearer)) return
        val quantity = minOf(requested, capacity - held.vars)
        if (quantity <= 0) return
        val result =
            player.invTransaction(inv) {
                val bag = select(inv)
                delete(bag, objs.blood_rune.id, quantity)
                delete(bag, objs.soul_rune.id, quantity)
            }
        if (result.success) {
            inv[slot] = held.copy(vars = held.vars + quantity)
            mes("Charges remaining: ${held.vars + quantity}.")
        }
    }

    private fun ProtectedAccess.uncharge(slot: Int) {
        val held = inv[slot] ?: return
        if (!player.members || !held.isType(SoulBearerItems.bearer) || held.vars <= 0) return
        val result =
            player.invTransaction(inv) {
                val bag = select(inv)
                add(bag, objs.blood_rune.id, held.vars)
                add(bag, objs.soul_rune.id, held.vars)
            }
        if (result.success) {
            inv[slot] = held.copy(vars = 0)
            mes("You recover ${held.vars} blood runes and ${held.vars} soul runes.")
        } else mes("You need enough space for both sets of runes.")
    }

    private suspend fun ProtectedAccess.fill(slot: Int) {
        if (!player.members) return
        val original = inv[slot] ?: return
        if (original.vars <= 0) {
            mes("Your soul bearer has no charges.")
            return
        }
        fun isHead(index: Int): Boolean {
            val item = inv[index] ?: return false
            val type = types[item]
            return !type.isCert &&
                type.name.startsWith("Ensouled ") &&
                type.desc.contains("reanimate", ignoreCase = true)
        }
        if (inv.indices.none(::isHead)) {
            mes("You have no ensouled heads to send to the bank.")
            return
        }
        if (vars[SoulBearerPrefs.skipConfirmation] == 0) {
            when (
                choice3(
                    "Send them to my bank.",
                    1,
                    "Yes, don't ask again.",
                    2,
                    "Leave them here.",
                    0,
                )
            ) {
                0 -> return
                2 -> vars[SoulBearerPrefs.skipConfirmation] = 1
            }
        }
        val held = inv[slot] ?: return
        if (!held.isType(SoulBearerItems.bearer)) return
        var moved = 0
        for (headSlot in inv.indices) {
            if (moved >= held.vars) break
            if (isHead(headSlot) && banking.depositInventorySlot(this, headSlot, 1)) moved++
        }
        if (moved > 0) {
            inv[slot] = held.copy(vars = held.vars - moved)
            mes("You send ${moved} ensouled head${if (moved == 1) "" else "s"} to your bank.")
        }
    }

    private companion object {
        const val capacity = 1000
    }
}
