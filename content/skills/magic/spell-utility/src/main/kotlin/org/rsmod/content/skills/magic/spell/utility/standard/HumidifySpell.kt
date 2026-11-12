package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjType

public class HumidifySpell
@Inject
constructor(
    private val spells: MagicSpellRegistry,
    private val runes: MagicRuneManager,
    private val cooldowns: UtilitySpellCooldownRepository,
) {
    public fun cast(access: ProtectedAccess) {
        with(access) {
            if (!cooldowns.isReady(player, mapClock) || !canAccess()) {
                return
            }

            val conversions =
                player.inv.indices.mapNotNull { slot ->
                    val obj = player.inv[slot] ?: return@mapNotNull null
                    val output = HumidifyRecipes.byInput[obj.id] ?: return@mapNotNull null
                    HumidifyConversion(slot, obj, output)
                }
            if (conversions.isEmpty()) {
                mes(NO_ELIGIBLE_ITEMS_MESSAGE)
                return
            }

            val spell = requireNotNull(spells.getObjSpell(objs.spell_humidify))
            val preflight = transform(conversions, autoCommit = false)
            if (!preflight.success || runes.attemptCast(player, spell).isFailure()) {
                return
            }

            val committed = transform(conversions, autoCommit = true)
            check(committed.success) {
                "Humidify transaction failed after validation: $conversions"
            }

            if (vars[varbits.fountain_of_rune_active] == 0) {
                statAdvance(stats.magic, spell.castXp)
            }
            anim(seqs.dream_player_humidify_spell)
            spotanim(spotanims.dream_humidify_spell)
            cooldowns.markCast(player, mapClock + CAST_RATE)
        }
    }

    private fun ProtectedAccess.canAccess(): Boolean {
        if (!player.members) {
            mes("You need to be on a members' world to cast this spell.")
            return false
        }
        if (vars[varbits.lunar_quest_main] < LUNAR_DIPLOMACY_COMPLETE) {
            mes("You need to complete Lunar Diplomacy to cast this spell.")
            return false
        }
        if (vars[varbits.dream_mentor] < DREAM_MENTOR_COMPLETE) {
            mes("You need to complete Dream Mentor to cast this spell.")
            return false
        }
        return true
    }

    private fun ProtectedAccess.transform(
        conversions: List<HumidifyConversion>,
        autoCommit: Boolean,
    ) =
        player.invTransaction(player.inv, autoCommit = autoCommit) {
            val inventory = select(player.inv)
            for (conversion in conversions) {
                delete(inventory, conversion.input.id, conversion.input.count, conversion.slot)
                add(inventory, conversion.output.id, conversion.input.count, slot = conversion.slot)
            }
        }

    private data class HumidifyConversion(val slot: Int, val input: InvObj, val output: ObjType)

    private companion object {
        const val CAST_RATE = 3
        const val LUNAR_DIPLOMACY_COMPLETE = 190
        const val DREAM_MENTOR_COMPLETE = 28
        const val NO_ELIGIBLE_ITEMS_MESSAGE = "You do not have anything that can be humidified."
    }
}

public data class HumidifyRecipe(val input: ObjType, val output: ObjType)

public object HumidifyRecipes {
    public val all: List<HumidifyRecipe> =
        listOf(
                objs.vial_empty to objs.vial_water,
                objs.water_skin_0 to objs.water_skin_4,
                objs.water_skin_1 to objs.water_skin_4,
                objs.water_skin_2 to objs.water_skin_4,
                objs.water_skin_3 to objs.water_skin_4,
                objs.bowl_empty to objs.bowl_water,
                objs.bucket_empty to objs.bucket_water,
                objs.jug_empty to objs.jug_water,
                objs.empty_cup to objs.cup_of_water,
                objs.watering_can_0 to objs.watering_can_8,
                objs.watering_can_1 to objs.watering_can_8,
                objs.watering_can_2 to objs.watering_can_8,
                objs.watering_can_3 to objs.watering_can_8,
                objs.watering_can_4 to objs.watering_can_8,
                objs.watering_can_5 to objs.watering_can_8,
                objs.watering_can_6 to objs.watering_can_8,
                objs.watering_can_7 to objs.watering_can_8,
                objs.fishbowl_empty to objs.fishbowl_water,
                objs.clay to objs.soft_clay,
                objs.raids_vial_empty to objs.raids_vial_water,
                objs.oak_seedling to objs.oak_seedling_watered,
                objs.willow_seedling to objs.willow_seedling_watered,
                objs.maple_seedling to objs.maple_seedling_watered,
                objs.yew_seedling to objs.yew_seedling_watered,
                objs.magic_seedling to objs.magic_seedling_watered,
                objs.spirit_seedling to objs.spirit_seedling_watered,
                objs.apple_seedling to objs.apple_seedling_watered,
                objs.banana_seedling to objs.banana_seedling_watered,
                objs.orange_seedling to objs.orange_seedling_watered,
                objs.curry_seedling to objs.curry_seedling_watered,
                objs.pineapple_seedling to objs.pineapple_seedling_watered,
                objs.papaya_seedling to objs.papaya_seedling_watered,
                objs.palm_seedling to objs.palm_seedling_watered,
                objs.calquat_seedling to objs.calquat_seedling_watered,
                objs.teak_seedling to objs.teak_seedling_watered,
                objs.mahogany_seedling to objs.mahogany_seedling_watered,
                objs.celastrus_seedling to objs.celastrus_seedling_watered,
                objs.redwood_seedling to objs.redwood_seedling_watered,
                objs.dragonfruit_seedling to objs.dragonfruit_seedling_watered,
                objs.crystal_seedling to objs.crystal_seedling_watered,
            )
            .map { (input, output) -> HumidifyRecipe(input, output) }

    public val byInput: Map<Int, ObjType> = all.associate { it.input.id to it.output }
}
