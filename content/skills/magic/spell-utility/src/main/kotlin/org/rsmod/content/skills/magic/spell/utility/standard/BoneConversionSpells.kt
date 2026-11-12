package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import kotlin.math.min
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.synths
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjType

public class BoneConversionSpells
@Inject
constructor(
    private val spells: MagicSpellRegistry,
    private val runes: MagicRuneManager,
    private val cooldowns: UtilitySpellCooldownRepository,
) {
    public fun cast(access: ProtectedAccess, spellType: ObjType) {
        with(access) {
            val definition = BoneConversionDefinition.forSpell(spellType) ?: return
            if (!cooldowns.isReady(player, mapClock)) {
                return
            }
            if (
                definition == BoneConversionDefinition.Peaches &&
                    vars[varbits.magictraining_bonespeaches] == 0
            ) {
                mes("You have not unlocked this spell.")
                return
            }

            val conversions =
                player.inv.indices.mapNotNull { slot ->
                    val obj = player.inv[slot] ?: return@mapNotNull null
                    val yield = boneYield(obj) ?: return@mapNotNull null
                    BoneConversion(slot, obj, yield)
                }
            if (conversions.isEmpty()) {
                mes("You aren't holding any bones!")
                return
            }

            val spell = requireNotNull(spells.getObjSpell(spellType))
            if (!runes.canCastSpell(player, spell)) {
                return
            }

            val initialCapacity = player.inv.freeSpace() + conversions.size
            val preflight =
                createTransaction(conversions, definition.output, initialCapacity, false)
            if (!preflight.success) {
                return
            }
            if (runes.attemptCast(player, spell).isFailure()) {
                return
            }

            val capacity = player.inv.freeSpace() + conversions.size
            val outputCount = min(conversions.sumOf(BoneConversion::yield), capacity)
            val committed = createTransaction(conversions, definition.output, outputCount, true)
            check(committed.success) { "Bone conversion failed after validation: $conversions" }

            statAdvance(stats.magic, spell.castXp)
            anim(seqs.human_castbonestobananas)
            spotanim(spotanims.bonestobananas_casting)
            soundSynth(synths.bones_to_bananas_all)
            cooldowns.markCast(player, mapClock + 1)
        }
    }

    private fun ProtectedAccess.createTransaction(
        conversions: List<BoneConversion>,
        output: ObjType,
        outputLimit: Int,
        autoCommit: Boolean,
    ) =
        player.invTransaction(player.inv, autoCommit = autoCommit) {
            val inventory = select(player.inv)
            for (conversion in conversions) {
                delete(inventory, conversion.obj.id, conversion.obj.count, conversion.slot)
            }
            val requested = conversions.sumOf(BoneConversion::yield)
            add(inventory, output.id, min(requested, outputLimit))
        }

    private fun boneYield(obj: InvObj): Int? = convertibleBones[obj.id]

    private enum class BoneConversionDefinition(val spell: ObjType, val output: ObjType) {
        Bananas(objs.spell_bones_to_bananas, objs.banana),
        Peaches(objs.spell_bones_to_peaches, objs.peach);

        companion object {
            fun forSpell(spell: ObjType): BoneConversionDefinition? =
                entries.firstOrNull { spell.id == it.spell.id }
        }
    }

    private data class BoneConversion(val slot: Int, val obj: InvObj, val yield: Int)

    private companion object {
        val convertibleBones =
            mapOf(
                objs.bones.id to 1,
                objs.burnt_bones.id to 1,
                objs.bat_bones.id to 1,
                objs.ground_bat_bones.id to 1,
                objs.big_bones.id to 1,
                objs.wolf_bones.id to 1,
                objs.jogre_bones.id to 1,
                objs.small_ninja_monkey_bones.id to 1,
                objs.medium_ninja_monkey_bones.id to 1,
                objs.gorilla_bones.id to 1,
                objs.bearded_gorilla_bones.id to 1,
                objs.monkey_bones.id to 1,
                objs.small_zombie_monkey_bones.id to 1,
                objs.large_zombie_monkey_bones.id to 1,
                objs.monkey_skeleton_bones.id to 1,
                objs.animals_bones_1.id to 1,
                objs.animals_bones_2.id to 2,
                objs.animals_bones_3.id to 3,
                objs.animals_bones_4.id to 4,
                objs.bleached_bones.id to 1,
                objs.alans_bones.id to 1,
            )
    }
}
