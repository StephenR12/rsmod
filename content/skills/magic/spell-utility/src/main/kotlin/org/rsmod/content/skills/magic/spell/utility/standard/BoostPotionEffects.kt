package org.rsmod.content.skills.magic.spell.utility.standard

import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.stat.statBoost
import org.rsmod.game.entity.Player
import org.rsmod.game.type.obj.ObjType

/** Cache-233 immediate effects for ordinary stat potions accepted by Boost Potion Share. */
public object BoostPotionEffects {
    public fun apply(
        player: Player,
        family: BoostPotionFamily,
        poison: PoisonProtectionRepository,
        disease: DiseaseImmunityRepository,
        antifire: AntifireProtectionRepository,
        goading: GoadingPotionRepository,
    ) {
        when (family) {
            BoostPotionFamily.Attack -> player.statBoost(stats.attack, constant = 3, percent = 10)
            BoostPotionFamily.Strength ->
                player.statBoost(stats.strength, constant = 3, percent = 10)
            BoostPotionFamily.Defence -> player.statBoost(stats.defence, constant = 3, percent = 10)
            BoostPotionFamily.Ranging -> player.statBoost(stats.ranged, constant = 4, percent = 10)
            BoostPotionFamily.Magic -> player.statBoost(stats.magic, constant = 4, percent = 0)
            BoostPotionFamily.SuperAttack ->
                player.statBoost(stats.attack, constant = 5, percent = 15)
            BoostPotionFamily.SuperStrength ->
                player.statBoost(stats.strength, constant = 5, percent = 15)
            BoostPotionFamily.SuperDefence ->
                player.statBoost(stats.defence, constant = 5, percent = 15)
            BoostPotionFamily.SuperCombat -> {
                player.statBoost(stats.attack, constant = 5, percent = 15)
                player.statBoost(stats.strength, constant = 5, percent = 15)
                player.statBoost(stats.defence, constant = 5, percent = 15)
            }
            BoostPotionFamily.Bastion -> {
                player.statBoost(stats.ranged, constant = 4, percent = 10)
                player.statBoost(stats.defence, constant = 5, percent = 15)
            }
            BoostPotionFamily.Battlemage -> {
                player.statBoost(stats.magic, constant = 4, percent = 0)
                player.statBoost(stats.defence, constant = 5, percent = 15)
            }
            BoostPotionFamily.Agility -> player.statBoost(stats.agility, constant = 3, percent = 0)
            BoostPotionFamily.Fishing -> player.statBoost(stats.fishing, constant = 3, percent = 0)
            BoostPotionFamily.Antipoison ->
                poison.apply(player, PoisonProtectionStrength.Antipoison)
            BoostPotionFamily.Superantipoison ->
                poison.apply(player, PoisonProtectionStrength.Superantipoison)
            BoostPotionFamily.AntidotePlus ->
                poison.apply(player, PoisonProtectionStrength.AntidotePlus)
            BoostPotionFamily.AntidotePlusPlus ->
                poison.apply(player, PoisonProtectionStrength.AntidotePlusPlus)
            BoostPotionFamily.AntiVenom -> poison.apply(player, PoisonProtectionStrength.AntiVenom)
            BoostPotionFamily.AntiVenomPlus ->
                poison.apply(player, PoisonProtectionStrength.AntiVenomPlus)
            BoostPotionFamily.ExtendedAntiVenomPlus ->
                poison.apply(player, PoisonProtectionStrength.ExtendedAntiVenomPlus)
            BoostPotionFamily.SanfewSerum ->
                StatRestorePotionEffects.applySanfew(player, poison, disease)
            BoostPotionFamily.Antifire ->
                antifire.apply(player, AntifireProtectionStrength.Antifire)
            BoostPotionFamily.ExtendedAntifire ->
                antifire.apply(player, AntifireProtectionStrength.ExtendedAntifire)
            BoostPotionFamily.SuperAntifire ->
                antifire.apply(player, AntifireProtectionStrength.SuperAntifire)
            BoostPotionFamily.ExtendedSuperAntifire ->
                antifire.apply(player, AntifireProtectionStrength.ExtendedSuperAntifire)
            BoostPotionFamily.Goading -> goading.apply(player)
        }
    }
}

public enum class BoostPotionFamily(private val doses: List<ObjType>) {
    Attack(
        listOf(
            objs.attack_potion_1,
            objs.attack_potion_2,
            objs.attack_potion_3,
            objs.attack_potion_4,
        )
    ),
    Strength(
        listOf(
            objs.strength_potion_1,
            objs.strength_potion_2,
            objs.strength_potion_3,
            objs.strength_potion_4,
        )
    ),
    Defence(
        listOf(
            objs.defence_potion_1,
            objs.defence_potion_2,
            objs.defence_potion_3,
            objs.defence_potion_4,
        )
    ),
    Ranging(
        listOf(
            objs.ranging_potion_1,
            objs.ranging_potion_2,
            objs.ranging_potion_3,
            objs.ranging_potion_4,
        )
    ),
    Magic(
        listOf(objs.magic_potion_1, objs.magic_potion_2, objs.magic_potion_3, objs.magic_potion_4)
    ),
    SuperAttack(
        listOf(objs.super_attack_1, objs.super_attack_2, objs.super_attack_3, objs.super_attack_4)
    ),
    SuperStrength(
        listOf(
            objs.super_strength_1,
            objs.super_strength_2,
            objs.super_strength_3,
            objs.super_strength_4,
        )
    ),
    SuperDefence(
        listOf(
            objs.super_defence_1,
            objs.super_defence_2,
            objs.super_defence_3,
            objs.super_defence_4,
        )
    ),
    SuperCombat(
        listOf(
            objs.super_combat_potion_1,
            objs.super_combat_potion_2,
            objs.super_combat_potion_3,
            objs.super_combat_potion_4,
        )
    ),
    Bastion(
        listOf(
            objs.bastion_potion_1,
            objs.bastion_potion_2,
            objs.bastion_potion_3,
            objs.bastion_potion_4,
        )
    ),
    Battlemage(
        listOf(
            objs.battlemage_potion_1,
            objs.battlemage_potion_2,
            objs.battlemage_potion_3,
            objs.battlemage_potion_4,
        )
    ),
    Agility(
        listOf(
            objs.agility_potion_1,
            objs.agility_potion_2,
            objs.agility_potion_3,
            objs.agility_potion_4,
        )
    ),
    Fishing(
        listOf(
            objs.fishing_potion_1,
            objs.fishing_potion_2,
            objs.fishing_potion_3,
            objs.fishing_potion_4,
        )
    ),
    Antipoison(listOf(objs.antipoison_1, objs.antipoison_2, objs.antipoison_3, objs.antipoison_4)),
    Superantipoison(
        listOf(
            objs.superantipoison_1,
            objs.superantipoison_2,
            objs.superantipoison_3,
            objs.superantipoison_4,
        )
    ),
    AntidotePlus(
        listOf(
            objs.antidote_plus_1,
            objs.antidote_plus_2,
            objs.antidote_plus_3,
            objs.antidote_plus_4,
        )
    ),
    AntidotePlusPlus(
        listOf(
            objs.antidote_plus_plus_1,
            objs.antidote_plus_plus_2,
            objs.antidote_plus_plus_3,
            objs.antidote_plus_plus_4,
        )
    ),
    AntiVenom(listOf(objs.anti_venom_1, objs.anti_venom_2, objs.anti_venom_3, objs.anti_venom_4)),
    AntiVenomPlus(
        listOf(
            objs.anti_venom_plus_1,
            objs.anti_venom_plus_2,
            objs.anti_venom_plus_3,
            objs.anti_venom_plus_4,
        )
    ),
    ExtendedAntiVenomPlus(
        listOf(
            objs.extended_anti_venom_plus_1,
            objs.extended_anti_venom_plus_2,
            objs.extended_anti_venom_plus_3,
            objs.extended_anti_venom_plus_4,
        )
    ),
    SanfewSerum(
        listOf(objs.sanfew_serum_1, objs.sanfew_serum_2, objs.sanfew_serum_3, objs.sanfew_serum_4)
    ),
    Antifire(
        listOf(
            objs.antifire_potion_1,
            objs.antifire_potion_2,
            objs.antifire_potion_3,
            objs.antifire_potion_4,
        )
    ),
    ExtendedAntifire(
        listOf(
            objs.extended_antifire_1,
            objs.extended_antifire_2,
            objs.extended_antifire_3,
            objs.extended_antifire_4,
        )
    ),
    SuperAntifire(
        listOf(
            objs.super_antifire_potion_1,
            objs.super_antifire_potion_2,
            objs.super_antifire_potion_3,
            objs.super_antifire_potion_4,
        )
    ),
    ExtendedSuperAntifire(
        listOf(
            objs.extended_super_antifire_1,
            objs.extended_super_antifire_2,
            objs.extended_super_antifire_3,
            objs.extended_super_antifire_4,
        )
    ),
    Goading(
        listOf(
            objs.goading_potion_1,
            objs.goading_potion_2,
            objs.goading_potion_3,
            objs.goading_potion_4,
        )
    );

    public fun doseFor(objId: Int): Int = doses.indexOfFirst { it.id == objId } + 1

    public fun remainder(dose: Int, consumed: Int): ObjType {
        val remaining = dose - consumed
        return if (remaining == 0) objs.empty_vial else doses[remaining - 1]
    }

    public companion object {
        public fun forObj(objId: Int): BoostPotionFamily? =
            entries.firstOrNull { it.doseFor(objId) > 0 }
    }
}

/** Cache-233 direct boosts that Boost Potion Share explicitly rejects. */
public object DirectOnlyBoostPotionEffects {
    public fun apply(player: Player, family: DirectOnlyBoostPotionFamily) {
        when (family) {
            DirectOnlyBoostPotionFamily.Combat -> {
                player.statBoost(stats.attack, constant = 3, percent = 10)
                player.statBoost(stats.strength, constant = 3, percent = 10)
            }
            DirectOnlyBoostPotionFamily.Hunter ->
                player.statBoost(stats.hunter, constant = 3, percent = 0)
        }
    }
}

public enum class DirectOnlyBoostPotionFamily(private val doses: List<ObjType>) {
    Combat(
        listOf(
            objs.combat_potion_1,
            objs.combat_potion_2,
            objs.combat_potion_3,
            objs.combat_potion_4,
        )
    ),
    Hunter(
        listOf(
            objs.hunter_potion_1,
            objs.hunter_potion_2,
            objs.hunter_potion_3,
            objs.hunter_potion_4,
        )
    );

    public companion object {
        public fun forObj(objId: Int): DirectOnlyBoostPotionFamily? =
            entries.firstOrNull { family -> family.doses.any { it.id == objId } }
    }
}
