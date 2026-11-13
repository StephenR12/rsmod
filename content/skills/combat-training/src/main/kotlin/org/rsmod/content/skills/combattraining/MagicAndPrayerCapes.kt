package org.rsmod.content.skills.combattraining

import jakarta.inject.Inject
import java.time.LocalDate
import java.time.ZoneOffset
import org.rsmod.api.combat.commons.magic.Spellbook
import org.rsmod.api.config.refs.*
import org.rsmod.api.invtx.*
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.*
import org.rsmod.api.shops.Shops
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.editors.inv.InvEditor
import org.rsmod.api.type.refs.inv.InvReferences
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.content.skills.skillcapes.SkillcapeSale
import org.rsmod.game.entity.Npc
import org.rsmod.game.type.inv.InvScope
import org.rsmod.game.type.inv.InvStackType
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object GuildCapeTrainers : NpcReferences() {
    val jered = find("brother_jered")
    val sinina = find("magic_store_owner2")
}

internal object GuildCapeLocs : LocReferences() {
    val ladder = find("monasteryladder")
}

internal object GuildCapeStock : InvReferences() {
    val lists =
        listOf("magicguildshop2", "magicguildshop2_skillcape", "magicguildshop2_skillcape_trimmed")
            .map { find(it) }
}

internal object GuildCapeItems : ObjReferences() {
    val robes =
        listOf(
                "mystic_hat",
                "mystic_robe_top",
                "mystic_robe_bottom",
                "mystic_gloves",
                "mystic_boots",
            )
            .map { find(it) }
    val holy = find("blessedstar")
}

internal object GuildCapeVars : VarpReferences() {
    val swaps = find("magic_cape_daily_uses")
}

internal object GuildCapeVarsBuild : VarpBuilder() {
    init {
        build("magic_cape_daily_uses") {
            permanent = true
            transmitNever = true
        }
    }
}

internal object MysticStock : InvEditor() {
    init {
        for ((i, inv) in GuildCapeStock.lists.withIndex()) edit(inv) {
            scope = InvScope.Shared
            stack = InvStackType.Always
            autoSize = true
            restock = true
            for (robe in GuildCapeItems.robes) stock += stock(robe, 1000, 100)
            if (i > 0)
                stock += stock(if (i == 1) objs.magic_skillcape else objs.magic_skillcape_t, 1, 1)
        }
    }
}

public class MagicAndPrayerCapes
@Inject
constructor(
    private val npcs: NpcRepository,
    private val types: NpcTypeList,
    private val shops: Shops,
    private val ground: ObjRepository,
) : PluginScript() {
    override fun ScriptContext.startup() {
        val jered = GuildCapeTrainers.jered
        val sinina = GuildCapeTrainers.sinina
        for ((t, c) in
            listOf(jered to CoordGrid(3052, 3491, 1), sinina to CoordGrid(2591, 3088, 1))) if (
            npcs.findAll(c).none { it.id == t.id }
        )
            npcs.add(Npc(types[t], c), Int.MAX_VALUE)
        onOpLoc1(GuildCapeLocs.ladder) {
            if (player.statBase(stats.prayer) < 31)
                mes("You need level 31 Prayer to enter the monastery upstairs.")
            else telejump(CoordGrid(it.loc.x, it.loc.z + 1, 1))
        }
        onOpNpc1(jered) {
            if (
                objs.unblessed_symbol in inv &&
                    choice2("Bless a symbol.", true, "Ask about the Prayer cape.", false)
            )
                bless(null)
            else SkillcapeSale.offer(this, stats.prayer, objs.prayer_hood)
        }
        onOpNpcU(jered, objs.unblessed_symbol) { bless(it.invSlot) }
        onOpNpc1(sinina) { SkillcapeSale.offer(this, stats.magic, objs.magic_hood) }
        onOpNpc3(sinina) {
            if (!player.members) return@onOpNpc3
            val i =
                if (player.statBase(stats.magic) < 99) 0
                else if (SkillcapeSale.isTrimmed(player)) 2 else 1
            shops.open(
                player,
                "Magic Guild Store - Mystic Robes",
                GuildCapeStock.lists[i],
                50.0,
                100.0,
                1.0,
            )
        }
        onEvent<org.rsmod.api.player.events.ObjTradeEvents.Purchased> {
            if (
                shop.id in GuildCapeStock.lists.map { it.id } &&
                    obj.id in listOf(objs.magic_skillcape.id, objs.magic_skillcape_t.id)
            )
                player.invAddOrDrop(ground, objs.magic_hood, count)
        }
        for (c in listOf(objs.magic_skillcape, objs.magic_skillcape_t)) {
            onOpHeld3(c) { swap() }
            onOpHeld4(c) { checkUses() }
            onOpWorn3(c) { swap() }
            onOpWorn4(c) { checkUses() }
        }
    }

    private fun ProtectedAccess.bless(slot: Int?) {
        if (
            player
                .invTransaction(inv) {
                    val bag = select(inv)
                    delete(bag, objs.unblessed_symbol.id, 1, slot = slot)
                    add(bag, GuildCapeItems.holy.id, 1, slot = slot)
                }
                .success
        )
            mes("Brother Jered blesses your symbol.")
    }

    public suspend fun swapSpellbook(access: ProtectedAccess): Unit = with(access) { swap() }

    public suspend fun swapSpellbook(access: ProtectedAccess, book: Spellbook): Unit = with(access) { swap(book) }

    public fun checkSpellbookUses(access: ProtectedAccess): Unit = with(access) { checkUses() }

    private fun day(): Int = LocalDate.now(ZoneOffset.UTC).toEpochDay().toInt()

    private fun ProtectedAccess.used(today: Int): Int =
        vars[GuildCapeVars.swaps].let { if (it / 8 == today) it % 8 else 0 }

    private fun ProtectedAccess.checkUses() {
        mes("You have ${5-used(day())} spellbook changes left today.")
    }

    private suspend fun ProtectedAccess.swap(requested: Spellbook? = null) {
        if (
            !player.members ||
                player.statBase(stats.magic) < 99 ||
                vars[varbits.lunar_spellbook_change] != 0
        )
            return
        if (used(day()) >= 5) {
            checkUses()
            return
        }
        val books =
            buildList {
                    add(Spellbook.Standard)
                    if (vars[varbits.desert_treasure_main] >= 15) add(Spellbook.Ancients)
                    if (vars[varbits.lunar_quest_main] >= 190) add(Spellbook.Lunars)
                    if (vars[varbits.arceuus_spellbook_unlocked] != 0) add(Spellbook.Arceuus)
                }
                .filter { it.varValue != vars[varbits.spellbook] }
        if (books.isEmpty()) {
            mes("You haven't unlocked another spellbook.")
            return
        }
        val choice = requested ?: books.getOrNull(menu("Choose a spellbook", true, books.map { it.name })) ?: return
        if (choice !in books) {
            mes("You cannot switch to that spellbook.")
            return
        }
        val today = day()
        vars[varbits.spellbook] = choice.varValue
        vars[GuildCapeVars.swaps] = today * 8 + used(today) + 1
        mes("Your spellbook changes.")
    }
}
