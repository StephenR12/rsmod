package org.rsmod.content.skills.hunter.moths

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.invtx.*
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statBoost
import org.rsmod.api.player.stat.statHeal
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpPlayerU
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.content.skills.herblore.PotionConsumptionRepository
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.type.obj.ObjType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal enum class HunterMixEffect(val suffix: String, val jar: ObjType) {
    Ruby("ruby", ButterflyObjs.ruby),
    Sapphire("glacialis", ButterflyObjs.sapphire),
    Snowy("snowy", ButterflyObjs.snowy),
    Warlock("warlock", ButterflyObjs.black),
    Sunlight("sunmoth", MothObjs.sunlightJar),
    Moonlight("moonmoth", MothObjs.moonlightJar);

    fun apply(p: Player, mixed: Boolean) {
        when (this) {
            Ruby -> p.statBoost(stats.attack, 4, 15)
            Sapphire -> p.statBoost(stats.defence, 4, 15)
            Warlock -> p.statBoost(stats.strength, 4, 15)
            Snowy -> p.statHeal(stats.hitpoints, if (mixed) 8 else 15, 0)
            Sunlight -> MothEffects.apply(p, MothEffect.Sunlight, false)
            Moonlight -> MothEffects.apply(p, MothEffect.Moonlight, false)
        }
    }

    companion object {
        fun fromButterfly(effect: ButterflyEffect) =
            when (effect) {
                ButterflyEffect.Attack -> Ruby
                ButterflyEffect.Defence -> Sapphire
                ButterflyEffect.Strength -> Warlock
                ButterflyEffect.Hitpoints -> Snowy
            }
    }
}

internal object HunterMixItems : ObjReferences() {
    val doses =
        HunterMixEffect.entries.associateWith { effect ->
            (1..2).map { find("hunter_mix_${effect.suffix}_${it}dose") }
        }
}

internal object HunterAidBits : VarBitReferences() {
    val ironman = find("ironman")
}

public class HunterMixes
@Inject
constructor(private val players: PlayerList, private val potions: PotionConsumptionRepository) :
    PluginScript() {
    private val meats =
        listOf(
            objs.raw_wild_kebbit,
            objs.raw_barb_tailed_kebbit,
            objs.raw_dashing_kebbit,
            objs.raw_larupia,
            objs.raw_graahk,
            objs.raw_kyatt,
            objs.raw_pyre_fox,
            objs.raw_sunlight_antelope,
            objs.raw_moonlight_antelope,
        )

    override fun ScriptContext.startup() {
        for ((effect, doses) in HunterMixItems.doses) {
            for (meat in meats) onOpHeldU(effect.jar, meat) {
                if (!player.members) return@onOpHeldU
                if (objs.pestle_and_mortar !in inv) {
                    mes("You need a pestle and mortar to prepare a hunter's mix.")
                    return@onOpHeldU
                }
                if (
                    player
                        .invTransaction(inv) {
                            val bag = select(inv)
                            delete(bag, effect.jar.id, 1, slot = it.firstSlot)
                            delete(bag, meat.id, 1, slot = it.secondSlot)
                            add(bag, doses.last().id, 1, slot = it.firstSlot)
                        }
                        .success
                ) {
                    mes("You mix the meat into the butterfly jar... Don't think too hard about it.")
                    delay(3)
                }
            }
            for ((index, dose) in doses.withIndex()) onOpHeld1(dose) {
                val next = if (index == 0) MothObjs.emptyJar else doses[index - 1]
                if (!consume(it.slot, dose, next)) return@onOpHeld1
                effect.apply(player, true)
                if (vars[varbits.multiway_indicator] != 0) share(player, player, effect, true)
            }
            onOpPlayerU(effect.jar) {
                val target = it.target
                if (
                    vars[varbits.multiway_indicator] == 0 || !accepts(player, target, effect, false)
                )
                    return@onOpPlayerU
                if (!consume(it.invSlot, effect.jar, MothObjs.emptyJar)) return@onOpPlayerU
                effect.apply(target, false)
                share(player, target, effect, false)
            }
        }
    }

    private fun ProtectedAccess.consume(slot: Int, source: ObjType, remainder: ObjType): Boolean {
        if (!player.members || !potions.isReady(player) || inv[slot]?.id != source.id) return false
        if (
            !player
                .invTransaction(inv) {
                    val bag = select(inv)
                    delete(bag, source.id, 1, slot = slot)
                    add(bag, remainder.id, 1, slot = slot)
                }
                .success
        )
            return false
        potions.consume(player, 3)
        anim(MothSeqs.openJar)
        return true
    }

    private fun share(source: Player, centre: Player, effect: HunterMixEffect, mixed: Boolean) {
        players
            .asSequence()
            .filter {
                it !== centre &&
                    it.level == centre.level &&
                    it.coords.chebyshevDistance(centre.coords) <= 2 &&
                    accepts(source, it, effect, mixed)
            }
            .take(3)
            .forEach { effect.apply(it, mixed) }
    }

    private fun accepts(
        source: Player,
        target: Player,
        effect: HunterMixEffect,
        mixed: Boolean,
    ): Boolean {
        if (
            target.vars[varbits.multiway_indicator] == 0 ||
                target.vars[varbits.option_acceptaid] == 0
        )
            return false
        val mode = target.vars[HunterAidBits.ironman]
        if (mode == 0) return true
        if (mode !in 4..6 || effect == HunterMixEffect.Moonlight && !mixed) return false
        return source.vars[HunterAidBits.ironman] in 4..6 &&
            source.observerUUID != null &&
            source.observerUUID == target.observerUUID
    }
}
