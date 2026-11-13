package org.rsmod.content.skills.maxcape

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.magic.Spellbook
import org.rsmod.api.config.refs.objs
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.ui.IfItemSubOp
import org.rsmod.api.script.*
import org.rsmod.content.skills.agility.AgilityCape
import org.rsmod.content.skills.combattraining.*
import org.rsmod.content.skills.construction.ConstructionCape
import org.rsmod.content.skills.farming.FarmingCape
import org.rsmod.content.skills.hunter.experts.HunterCapeDestination
import org.rsmod.content.skills.hunter.experts.HunterCapeTeleport
import org.rsmod.content.skills.skillcapes.SkillcapeTrimming
import org.rsmod.game.type.interf.IfButtonOp
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class MaxCape
@Inject
constructor(
    private val house: ConstructionCape,
    private val farming: FarmingCape,
    private val agility: AgilityCape,
    private val hunter: HunterCapeTeleport,
    private val production: ProductionCapePerks,
    private val melee: CombatSkillcapes,
    private val magic: MagicAndPrayerCapes,
    private val defence: DefenceCapeProtection,
    private val ava: AvaMetalAttraction,
) : PluginScript() {
    private val bookNames = listOf("Standard", "Ancient", "Lunar", "Arceuus", "Check")
    private val books =
        listOf(Spellbook.Standard, Spellbook.Ancients, Spellbook.Lunars, Spellbook.Arceuus)
    private val features = listOf("Search", "Ring of Life", "Commune", "Stamina Boost")
    private val teleports =
        listOf(
            "Warrior's Guild",
            "Fishing Guild",
            "Crafting Guild",
            "Farming Guild",
            "Otto's Grotto",
            "Feldip Hills",
            "Black chinchompas",
            "Hunter Guild",
            "Home",
            "Rimmington",
            "Taverley",
            "Pollnivneach",
            "Hosidius",
            "Aldarin",
            "Rellekka",
            "Brimhaven",
            "Yanille",
            "Prifddinas",
        )
    private val guilds = listOf(2, 3, 1, 7, 0)
    private val areas = listOf(4, 5, 6)

    override fun ScriptContext.startup() {
        val bag = objs.max_cape_inv
        val worn = objs.max_cape_worn
        onOpHeld1(bag) {
            if (player.members && SkillcapeTrimming.definitions.all { statBase(it.stat) >= 99 })
                invEquip(it.slot)
            else mes("You need to have level 99 in all of the skills to wear this cape.")
        }
        onOpHeld2(bag) { chooseTele(teleports.indices.toList()) }
        onOpHeld3(bag) { selectBook(menu("Spellbook", true, bookNames)) }
        onOpHeld4(bag) { feature(menu("Features", true, features)) }
        onOpWorn2(worn) { house.teleport(this, 0) }
        onOpWorn3(worn) { production.craftingGuild(this) }
        onOpWorn4(worn) { chooseTele(guilds) }
        onOpWorn5(worn) { chooseTele(areas) }
        onOpWorn6(worn) { house.selectPortal(this) }
        onOpWorn7(worn) { selectBook(menu("Spellbook", true, bookNames)) }
        onOpWorn8(worn) { feature(menu("Features", true, features)) }

        onProtectedEvent<IfItemSubOp>(bag.id) { if (!it.worn) submenu(it) }
        onProtectedEvent<IfItemSubOp>(worn.id) { if (it.worn) submenu(it) }
    }

    private suspend fun ProtectedAccess.submenu(event: IfItemSubOp) {
        if (!player.members) return
        val index = event.subop - 1
        val worn = event.worn
        val op = event.op
        val size =
            if (worn)
                when (op) {
                    IfButtonOp.Op4 -> guilds.size
                    IfButtonOp.Op5 -> areas.size
                    IfButtonOp.Op6 -> 10
                    IfButtonOp.Op7 -> books.size + 1
                    IfButtonOp.Op8 -> features.size
                    else -> 0
                }
            else
                when (op) {
                    IfButtonOp.Op3 -> teleports.size
                    IfButtonOp.Op4 -> bookNames.size
                    IfButtonOp.Op6 -> features.size
                    else -> 0
                }
        if (index !in 0 until size) return
        if (worn)
            when (op) {
                IfButtonOp.Op4 -> travel(guilds[index])
                IfButtonOp.Op5 -> travel(areas[index])
                IfButtonOp.Op6 -> house.teleport(this, index)
                IfButtonOp.Op7 -> selectBook(index)
                IfButtonOp.Op8 -> feature(index)
                else -> Unit
            }
        else
            when (op) {
                IfButtonOp.Op3 -> travel(index)
                IfButtonOp.Op4 -> selectBook(index)
                IfButtonOp.Op6 -> feature(index)
                else -> Unit
            }
    }

    private suspend fun ProtectedAccess.chooseTele(choices: List<Int>) {
        val i = menu("Where would you like to go?", true, choices.map { teleports[it] })
        choices.getOrNull(i)?.let { travel(it) }
    }

    private suspend fun ProtectedAccess.travel(i: Int) {
        when (i) {
            0 -> melee.teleportToWarriorsGuild(this)
            1 -> production.fishingGuild(this)
            2 -> production.craftingGuild(this)
            3 -> farming.teleportToGuild(this)
            4 -> production.grotto(this)
            5 -> hunter.teleport(this, HunterCapeDestination.FeldipHunterArea)
            6 -> hunter.teleport(this, HunterCapeDestination.WildernessHunterArea)
            7 -> hunter.teleport(this, HunterCapeDestination.HunterGuild)
            in 8..17 -> house.teleport(this, i - 8)
        }
    }

    private suspend fun ProtectedAccess.selectBook(i: Int) {
        if (i == books.size) magic.checkSpellbookUses(this)
        else books.getOrNull(i)?.let { magic.swapSpellbook(this, it) }
    }

    private suspend fun ProtectedAccess.feature(i: Int) {
        when (i) {
            0 ->
                when (
                    menu(
                        "Search for what?",
                        true,
                        listOf("Mithril grapple and bronze crossbow", "Pestle and mortar"),
                    )
                ) {
                    0 -> production.grapple(this)
                    1 -> production.pestle(this)
                }
            1 ->
                when (
                    menu(
                        "Ring of life",
                        true,
                        listOf("Toggle protection", "Toggle Ardougne teleport"),
                    )
                ) {
                    0 -> defence.toggleLifeProtection(this)
                    1 -> defence.toggleRespawnDestination(this)
                }
            2 -> ava.communeWithDevice(this)
            3 -> agility.restoreEnergy(this)
        }
    }
}
