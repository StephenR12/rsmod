package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import kotlin.math.ceil
import kotlin.math.min
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.api.type.refs.interf.InterfaceReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.spot.SpotanimReferences

public class LunarUtilitySpells
@Inject
constructor(
    private val spells: MagicSpellRegistry,
    private val runes: MagicRuneManager,
    private val cooldowns: UtilitySpellCooldownRepository,
    private val random: GameRandom,
    private val objRepo: ObjRepository,
) {
    public fun superglassMake(access: ProtectedAccess) {
        with(access) {
            if (!canUseLunarSpell() || !cooldowns.isReady(player, mapClock)) {
                return
            }
            val spell = requireNotNull(spells.getObjSpell(objs.spell_superglass))
            if (!runes.canCastSpell(player, spell)) {
                return
            }

            val sand = invTotal(player.inv, objs.bucket_of_sand)
            val giantCount = invTotal(player.inv, LunarUtilityObjs.giantSeaweed)
            val giantSand = min(sand, giantCount * GIANT_SEAWEED_SAND)
            val afterGiant = sand - giantSand
            val regularAvailable = REGULAR_SEAWEED.sumOf { invTotal(player.inv, it) }
            val regularSand = min(afterGiant, regularAvailable)
            val glassSand = giantSand + regularSand
            if (glassSand == 0) {
                mes("You need buckets of sand and soda ash or seaweed to cast this spell.")
                return
            }

            val giantsUsed = ceil(giantSand.toDouble() / GIANT_SEAWEED_SAND).toInt()
            val preview =
                player.invTransaction(player.inv, autoCommit = false) {
                    val inventory = select(player.inv)
                    delete(inventory, objs.bucket_of_sand.id, glassSand)
                    if (giantsUsed > 0) {
                        delete(inventory, LunarUtilityObjs.giantSeaweed.id, giantsUsed)
                    }
                    deleteRegularSeaweed(inventory, regularSand, access)
                }
            if (!preview.success || runes.attemptCast(player, spell).isFailure()) {
                return
            }

            val committed =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, objs.bucket_of_sand.id, glassSand)
                    if (giantsUsed > 0) {
                        delete(inventory, LunarUtilityObjs.giantSeaweed.id, giantsUsed)
                    }
                    deleteRegularSeaweed(inventory, regularSand, access)
                }
            check(committed.success) { "Superglass Make ingredients changed after validation." }

            val output =
                glassSand +
                    (0 until giantSand).count { random.of(100) < GIANT_BONUS_PERCENT } +
                    (0 until regularSand).count { random.of(100) < REGULAR_BONUS_PERCENT }
            player.invAddOrDrop(objRepo, objs.molten_glass, output)
            if (vars[varbits.fountain_of_rune_active] == 0) {
                statAdvance(stats.magic, spell.castXp)
            }
            statAdvance(stats.crafting, glassSand * CRAFTING_XP_PER_SAND)
            anim(LunarUtilitySeqs.summon)
            spotanim(LunarUtilitySpotanims.superglass)
            cooldowns.markCast(player, mapClock + SUPERGLASS_CAST_TICKS)
        }
    }

    public fun geomancy(access: ProtectedAccess) {
        with(access) {
            if (!canUseLunarSpell() || !cooldowns.isReady(player, mapClock)) {
                return
            }
            val spell = requireNotNull(spells.getObjSpell(objs.spell_geomancy))
            if (runes.attemptCast(player, spell).isFailure()) {
                return
            }
            if (vars[varbits.fountain_of_rune_active] == 0) {
                statAdvance(stats.magic, spell.castXp)
            }
            anim(LunarUtilitySeqs.geomancy)
            spotanim(LunarUtilitySpotanims.geomancy)
            ifOpenMainModal(LunarUtilityInterfaces.farmingView)
            cooldowns.markCast(player, mapClock + STANDARD_CAST_TICKS)
        }
    }

    public suspend fun npcContact(access: ProtectedAccess) {
        with(access) {
            if (!canUseLunarSpell() || !cooldowns.isReady(player, mapClock)) {
                return
            }
            val contact =
                menu(
                    title = "NPC Contact",
                    hotkeys = false,
                    choices = CONTACTS.map { it.name } + "Cancel",
                )
            if (contact !in CONTACTS.indices) {
                return
            }
            val spell = requireNotNull(spells.getObjSpell(objs.spell_npc_contact))
            if (runes.attemptCast(player, spell).isFailure()) {
                return
            }
            if (vars[varbits.fountain_of_rune_active] == 0) {
                statAdvance(stats.magic, spell.castXp)
            }
            anim(LunarUtilitySeqs.summon)
            spotanim(LunarUtilitySpotanims.contact)
            mes(CONTACTS[contact].reply)
            cooldowns.markCast(player, mapClock + STANDARD_CAST_TICKS)
        }
    }

    private fun ProtectedAccess.canUseLunarSpell(): Boolean {
        if (!player.members) {
            mes("You need to be on a members' world to cast this spell.")
            return false
        }
        if (vars[varbits.lunar_quest_main] < LUNAR_DIPLOMACY_COMPLETE) {
            mes("You need to complete Lunar Diplomacy to cast this spell.")
            return false
        }
        return true
    }

    private fun org.rsmod.objtx.Transaction<org.rsmod.game.inv.InvObj>.deleteRegularSeaweed(
        inventory: org.rsmod.objtx.TransactionInventory<org.rsmod.game.inv.InvObj>,
        requested: Int,
        access: ProtectedAccess,
    ) {
        var remaining = requested
        for (type in REGULAR_SEAWEED) {
            if (remaining == 0) break
            val count = min(remaining, access.invTotal(access.player.inv, type))
            if (count > 0) {
                delete(inventory, type.id, count)
                remaining -= count
            }
        }
    }

    private data class Contact(val name: String, val reply: String)

    private companion object {
        const val LUNAR_DIPLOMACY_COMPLETE = 190
        const val STANDARD_CAST_TICKS = 3
        const val SUPERGLASS_CAST_TICKS = 3
        const val GIANT_SEAWEED_SAND = 6
        const val REGULAR_BONUS_PERCENT = 30
        const val GIANT_BONUS_PERCENT = 60
        const val CRAFTING_XP_PER_SAND = 10.0

        val REGULAR_SEAWEED = listOf(objs.soda_ash, objs.seaweed, objs.swamp_weed)
        val CONTACTS =
            listOf(
                Contact("Dark Mage", "The Dark Mage checks your essence pouches remotely."),
                Contact("Bert", "Bert says your daily sand is waiting in Yanille."),
                Contact("Advisor Ghrim", "Advisor Ghrim says Miscellania is running normally."),
                Contact("Honest Jimmy", "Honest Jimmy tells you to meet him at Trouble Brewing."),
                Contact("Turael", "Turael says he has no new assignment information for you."),
                Contact("Mazchna", "Mazchna says he has no new assignment information for you."),
                Contact("Vannaka", "Vannaka says he has no new assignment information for you."),
                Contact("Chaeldar", "Chaeldar says she has no new assignment information for you."),
                Contact("Duradel", "Duradel says he has no new assignment information for you."),
            )
    }
}

internal object LunarUtilityObjs : ObjReferences() {
    val giantSeaweed = find("giant_seaweed")
}

internal object LunarUtilityInterfaces : InterfaceReferences() {
    val farmingView = find("farming_view")
}

internal object LunarUtilitySeqs : SeqReferences() {
    val summon = find("lunar_human_magic_summon1")
    val geomancy = find("lunar_human_magic_geomancy")
    val curePlant = find("quest_lunar_cure_plant")
    val fertileSoil = find("quest_lunar_spell_raining_fertile_soil")
}

internal object LunarUtilitySpotanims : SpotanimReferences() {
    val superglass = find("quest_lunar_magic_superglass")
    val contact = find("quest_lunar_magic_cure_plants")
    val geomancy = find("quest_lunar_spellbook_geomancy")
    val curePlant = find("quest_lunar_cure_plant_spot")
    val fertileSoil = find("quest_lunar_spellbook_fertile_soil_spot_anim")
}
