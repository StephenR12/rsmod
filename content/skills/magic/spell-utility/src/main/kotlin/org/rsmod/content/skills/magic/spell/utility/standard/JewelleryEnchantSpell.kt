package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import org.rsmod.content.skills.magic.spell.utility.arena.ArenaRoom
import org.rsmod.content.skills.magic.spell.utility.arena.ArenaPoints
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
import org.rsmod.api.player.diary.recordDiaryAmuletEnchant
import org.rsmod.api.player.isInCombat
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.seq.SeqType
import org.rsmod.game.type.spot.SpotanimType
import org.rsmod.game.type.synth.SynthType

public class JewelleryEnchantSpell
@Inject
internal constructor(
    private val points:ArenaPoints,
    private val spells: MagicSpellRegistry,
    private val runes: MagicRuneManager,
    private val recipes: JewelleryEnchantRecipeRegistry,
    private val cooldowns: UtilitySpellCooldownRepository,
) {
    public fun castManual(access: ProtectedAccess, spellType: ObjType, clickedTargetId: Int) {
        with(access) {
            val tier = JewelleryEnchantTier.forSpell(spellType) ?: return
            val recipe = recipes.find(spellType, clickedTargetId)
            if (recipe == null) {
                mes(tier.invalidTargetMessage)
                return
            }

            clearQueue(jewellery_enchant_queues.auto)
            if (!cooldowns.isReady(player, mapClock)) {
                if (jewellery_enchant_queues.manual !in player.queueList) {
                    val delay = cooldowns.cyclesUntilReady(player, mapClock)
                    queue(
                        jewellery_enchant_queues.manual,
                        delay,
                        JewelleryEnchantRequest(
                            spellType,
                            recipe.source,
                            player.coords,
                            mapClock + delay,
                        ),
                    )
                }
                return
            }

            clearQueue(jewellery_enchant_queues.manual)
            cast(this, tier, recipe)
        }
    }

    internal fun castQueued(access: ProtectedAccess, request: JewelleryEnchantRequest) {
        with(access) {
            if (
                mapClock != request.expectedCycle ||
                    player.coords != request.origin ||
                    player.isInCombat()
            ) {
                return
            }
            val tier = JewelleryEnchantTier.forSpell(request.spell) ?: return
            val recipe = recipes.find(request.spell, request.source.id) ?: return
            cast(this, tier, recipe)
        }
    }

    private fun cast(
        access: ProtectedAccess,
        tier: JewelleryEnchantTier,
        recipe: JewelleryEnchantRecipe,
    ) {
        with(access) {
            if (recipe.membersOnly && !player.members) {
                mes("You need to be on a members' world to enchant that item.")
                return
            }
            if (!passesSpecialGate(recipe)) {
                return
            }

            val sourceSlot =
                player.inv.indices.firstOrNull { player.inv[it]?.id == recipe.source.id } ?: return
            val spell = requireNotNull(spells.getObjSpell(tier.spell))
            if (!runes.canCastSpell(player, spell)) {
                return
            }

            val preflight = transform(recipe, sourceSlot, autoCommit = false)
            if (!preflight.success || runes.attemptCast(player, spell).isFailure()) {
                return
            }
            val committed = transform(recipe, sourceSlot, autoCommit = true)
            check(committed.success) { "Jewellery enchant transaction failed: $recipe" }
            if(recipe.gate==JewelleryEnchantRecipe.Gate.Arena) points.enchanted(player,recipe.source,tier.ordinal+1)
            player.recordDiaryAmuletEnchant(recipe.output.id, coords)

            if (vars[varbits.fountain_of_rune_active] == 0) {
                statAdvance(stats.magic, spell.castXp)
            }
            anim(tier.sequence)
            spotanim(tier.spotanim, height = GRAPHIC_HEIGHT)
            soundSynth(if (recipe.ringSound) tier.ringSound else tier.otherSound)
            cooldowns.markCast(player, mapClock + MANUAL_CAST_RATE)

            if (hasSource(recipe.source)) {
                clearQueue(jewellery_enchant_queues.auto)
                queue(
                    jewellery_enchant_queues.auto,
                    AUTO_CAST_RATE,
                    JewelleryEnchantRequest(
                        tier.spell,
                        recipe.source,
                        player.coords,
                        mapClock + AUTO_CAST_RATE,
                    ),
                )
            }
        }
    }

    internal fun useTablet(access: ProtectedAccess, recipe: JewelleryEnchantRecipe, sourceSlot:Int, tablet: ObjType, tabletSlot:Int) {
        with(access) {
            if (!player.members || !passesSpecialGate(recipe)) return
            if (transform(recipe, sourceSlot, true, tablet, tabletSlot).success) {
                anim(seqs.poh_smash_magic_tablet)
            }
        }
    }

    private fun ProtectedAccess.passesSpecialGate(recipe: JewelleryEnchantRecipe): Boolean =
        when (recipe.gate) {
            JewelleryEnchantRecipe.Gate.Arena -> ArenaRoom.Enchantment.contains(coords)
            JewelleryEnchantRecipe.Gate.None -> true
            JewelleryEnchantRecipe.Gate.DigsitePendant -> {
                if (vars[varbits.vm_necklace] == 0 && vars[varbits.vm_digsite_talisman] == 0) {
                    mes("You need to have discovered the Digsite pendant.")
                    false
                } else {
                    true
                }
            }
            JewelleryEnchantRecipe.Gate.NatureAmulet -> {
                val alreadyOwned =
                    objs.amulet_of_nature in player.inv ||
                        objs.amulet_of_nature in player.worn ||
                        objs.amulet_of_nature in bank
                if (alreadyOwned) {
                    mes("You can only have one amulet of nature at a time.")
                    false
                } else {
                    true
                }
            }
        }

    private fun ProtectedAccess.transform(
        recipe: JewelleryEnchantRecipe,
        sourceSlot: Int,
        autoCommit: Boolean,
        tablet: ObjType? = null,
        tabletSlot:Int = -1,
    ) =
        player.invTransaction(player.inv, autoCommit = autoCommit) {
            val inventory = select(player.inv)
            if (tablet != null) delete(inventory,tablet.id,1,tabletSlot)
            delete(inventory, recipe.source.id, count = 1, slot = sourceSlot)
            add(inventory, recipe.output.id, count = 1, slot = sourceSlot)
        }

    private fun ProtectedAccess.hasSource(source: ObjType): Boolean =
        player.inv.any { it?.id == source.id }

    private enum class JewelleryEnchantTier(
        val spell: ObjType,
        val sequence: SeqType,
        val spotanim: SpotanimType,
        val otherSound: SynthType,
        val ringSound: SynthType,
        val invalidTargetMessage: String,
    ) {
        Level1(
            objs.spell_enchant_lvl_1,
            seqs.human_enchantamuletlvl1,
            spotanims.enchant_amulet_lvl1,
            synths.enchant_sapphire_amulet,
            synths.enchant_sapphire_ring,
            "This spell can only be cast on sapphire or opal amulets, necklaces, rings, and " +
                "bracelets, and on shapes in the Mage Training Arena.",
        ),
        Level2(
            objs.spell_enchant_lvl_2,
            seqs.human_enchantamuletlvl2,
            spotanims.enchant_amulet_lvl2,
            synths.enchant_emerald_amulet,
            synths.enchant_emerald_ring,
            "This spell can only be cast on emerald or jade amulets, necklaces, rings, and " +
                "bracelets, and on shapes in the Mage Training Arena.",
        ),
        Level3(
            objs.spell_enchant_lvl_3,
            seqs.human_enchantamuletlvl3,
            spotanims.enchant_amulet_lvl3,
            synths.enchant_ruby_amulet,
            synths.enchant_ruby_ring,
            "This spell can only be cast on ruby or topaz amulets, necklaces, rings, and " +
                "bracelets, and on shapes in the Mage Training Arena.",
        ),
        Level4(
            objs.spell_enchant_lvl_4,
            seqs.human_enchantamuletlvl1,
            spotanims.enchant_amulet_lvl4,
            synths.enchant_diamond_amulet,
            synths.enchant_diamond_ring,
            "This spell can only be cast on diamond amulets, necklaces, rings, and bracelets, " +
                "and on shapes in the Mage Training Arena.",
        ),
        Level5(
            objs.spell_enchant_lvl_5,
            seqs.human_enchantamuletlvl2,
            spotanims.enchant_amulet_lvl5,
            synths.enchant_dragon_amulet,
            synths.enchant_dragon_ring,
            "This spell can only be cast on dragonstone amulets, necklaces, rings, and " +
                "bracelets, and on shapes in the Mage Training Arena.",
        ),
        Level6(
            objs.spell_enchant_lvl_6,
            seqs.human_enchantamuletlvl3,
            spotanims.enchant_amulet_lvl6,
            synths.enchant_onyx_amulet,
            synths.enchant_onyx_ring,
            "This spell can only be cast on onyx amulets, necklaces, rings, and bracelets, and " +
                "on shapes in the Mage Training Arena.",
        ),
        Level7(
            objs.spell_enchant_lvl_7,
            seqs.human_enchantamuletlvl3,
            spotanims.enchant_amulet_lvl6,
            synths.enchant_onyx_amulet,
            synths.enchant_onyx_ring,
            "This spell can only be cast on zenyte amulets, necklaces, rings, and bracelets, and " +
                "on shapes in the Mage Training Arena.",
        );

        companion object {
            fun forSpell(spell: ObjType): JewelleryEnchantTier? =
                entries.firstOrNull { it.spell.id == spell.id }
        }
    }

    private companion object {
        const val MANUAL_CAST_RATE = 3
        const val AUTO_CAST_RATE = 7
        const val GRAPHIC_HEIGHT = 100
    }
}
