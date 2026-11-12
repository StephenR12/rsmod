package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.WeakHashMap
import kotlin.math.max
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
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
import org.rsmod.api.spells.runes.MagicRunes
import org.rsmod.content.other.coalbag.CoalBagStorage
import org.rsmod.game.entity.Player
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.seq.SeqType
import org.rsmod.game.type.spot.SpotanimType
import org.rsmod.game.type.synth.SynthType

public class AlchemySpells
@Inject
constructor(
    private val spells: MagicSpellRegistry,
    private val runes: MagicRuneManager,
    private val objTypes: ObjTypeList,
    private val cooldowns: UtilitySpellCooldownRepository,
    private val coalBag: CoalBagStorage,
) {
    public fun cast(access: ProtectedAccess, spellType: ObjType, targetSlot: Int) {
        with(access) {
            val definition = AlchemyDefinition.forSpell(spellType) ?: return
            if (!cooldowns.isReady(player, mapClock)) {
                return
            }

            val target = player.inv[targetSlot] ?: return
            val targetType = objTypes[target]
            val isCoalBag = target.id == objs.coal_bag.id || target.id == objs.coal_bag_open.id
            if (targetType.param(params.no_alchemy) != 0 && !isCoalBag) {
                mes("You can't alchemise that!")
                return
            }

            val spell = requireNotNull(spells.getObjSpell(spellType))
            if (!runes.canCastSpell(player, spell)) {
                return
            }
            val targetRuneUse =
                runes
                    .validateSpell(player, spell)
                    .filterIsInstance<MagicRunes.Validation.Valid.HasEnough>()
                    .flatMap { it.sources }
                    .filterIsInstance<MagicRunes.Source.InvSource>()
                    .filter { it.slot == targetSlot }
                    .sumOf { it.count }
            if (target.count <= targetRuneUse) {
                mes("You do not have enough runes to cast this spell.")
                return
            }

            val coinCount = definition.coinValue(targetType.cost)
            val transaction =
                player.invTransaction(player.inv, autoCommit = false) {
                    val inventory = select(player.inv)
                    delete(inventory, target.id, count = 1, slot = targetSlot)
                    if (coinCount > 0) {
                        add(inventory, objs.coins.id, count = coinCount)
                    }
                }
            if (!transaction.success) {
                return
            }

            val castResult = runes.attemptCast(player, spell)
            if (castResult.isFailure()) {
                return
            }

            val committed =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, target.id, count = 1, slot = targetSlot)
                    if (coinCount > 0) {
                        add(inventory, objs.coins.id, count = coinCount)
                    }
                }
            check(committed.success) { "Alchemy transaction failed after validation: $target" }
            if (isCoalBag) {
                coalBag.clear(player)
            }

            if (vars[varbits.fountain_of_rune_active] == 0) {
                statAdvance(stats.magic, spell.castXp)
            }
            anim(definition.sequence)
            spotanim(definition.spotanim)
            soundSynth(definition.sound)
            cooldowns.markCast(player, mapClock + definition.castRate)
        }
    }

    private enum class AlchemyDefinition(
        val spell: ObjType,
        val castRate: Int,
        val sequence: SeqType,
        val spotanim: SpotanimType,
        val sound: SynthType,
    ) {
        Low(
            objs.spell_low_alchemy,
            castRate = 3,
            seqs.human_castlowlvlalchemy,
            spotanims.lowlvlalchemy_casting,
            synths.low_alchemy,
        ),
        High(
            objs.spell_high_alchemy,
            castRate = 5,
            seqs.human_casthighlvlalchemy,
            spotanims.highlvlalchemy_casting,
            synths.high_alchemy,
        );

        fun coinValue(itemValue: Int): Int {
            val percent = if (this == Low) 40 else 60
            return (itemValue.toLong() * percent / 100).toInt()
        }

        companion object {
            fun forSpell(spell: ObjType): AlchemyDefinition? =
                entries.firstOrNull { spell.id == it.spell.id }
        }
    }
}

@Singleton
public class UtilitySpellCooldownRepository @Inject constructor() {
    private val readyAt = WeakHashMap<Player, Int>()

    public fun isReady(player: Player, mapClock: Int): Boolean =
        mapClock >= readyAt.getOrDefault(player, Int.MIN_VALUE)

    public fun markCast(player: Player, mapClock: Int) {
        readyAt[player] = mapClock
    }

    public fun cyclesUntilReady(player: Player, mapClock: Int): Int =
        max(1, readyAt.getOrDefault(player, mapClock) - mapClock)
}
