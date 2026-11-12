package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.timers
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.events.PlayerConsumableEvent
import org.rsmod.api.player.events.PlayerDeathEvent
import org.rsmod.api.player.events.PlayerTeleportEvent
import org.rsmod.api.player.stat.statBoost
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class PotionProtectionScript
@Inject
constructor(
    private val poison: PoisonProtectionRepository,
    private val disease: DiseaseImmunityRepository,
    private val antifire: AntifireProtectionRepository,
    private val goading: GoadingPotionRepository,
    private val stamina: StaminaEffectRepository,
    private val prayerRegeneration: PrayerRegenerationPotionRepository,
    private val menaphiteRemedy: MenaphiteRemedyRepository,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onPlayerSoftTimer(timers.toxins) { poison.tick(player) }
        onPlayerSoftTimer(timers.antifire_effect) { antifire.tick(player) }
        onPlayerSoftTimer(timers.goading_effect) { goading.tick(player) }
        onPlayerSoftTimer(timers.prayer_regeneration_effect) { prayerRegeneration.tick(player) }
        onPlayerSoftTimer(timers.menaphite_remedy_effect) { menaphiteRemedy.tick(player) }
        onEvent<PlayerDeathEvent.Completed> {
            disease.clear(player)
            prayerRegeneration.clear(player)
            menaphiteRemedy.clear(player)
        }
        onEvent<PlayerTeleportEvent.Start> { goading.cancel(player) }
        onEvent<PlayerConsumableEvent.Consumed> {
            if (obj == objs.zamorak_mix_1 || obj == objs.zamorak_mix_2) {
                DirectBrewPotionEffects.applyZamorakMix(player)
                return@onEvent
            }
            if (effectObj == objs.relicyms_balm_2) {
                VarPlayerIntMapSetter.set(player, varps.disease, 0)
                return@onEvent
            }
            if (effectObj == objs.magic_essence_2) {
                player.statBoost(stats.magic, constant = 3, percent = 0)
                return@onEvent
            }
            val directBrewFamily = DirectBrewPotionFamily.forObj(effectObj.id)
            if (directBrewFamily != null) {
                DirectBrewPotionEffects.apply(player, directBrewFamily)
                return@onEvent
            }
            val directOnlyBoostFamily = DirectOnlyBoostPotionFamily.forObj(effectObj.id)
            if (directOnlyBoostFamily != null) {
                DirectOnlyBoostPotionEffects.apply(player, directOnlyBoostFamily)
                return@onEvent
            }
            val boostFamily = BoostPotionFamily.forObj(effectObj.id)
            if (boostFamily != null && boostFamily in directStatBoostFamilies) {
                BoostPotionEffects.apply(player, boostFamily, poison, disease, antifire, goading)
                return@onEvent
            }
            when (effectObj) {
                objs.cup_of_tea -> goading.cancel(player)
                objs.goading_potion_1,
                objs.goading_potion_2,
                objs.goading_potion_3,
                objs.goading_potion_4 -> goading.apply(player)
                objs.prayer_regeneration_potion_1,
                objs.prayer_regeneration_potion_2,
                objs.prayer_regeneration_potion_3,
                objs.prayer_regeneration_potion_4 -> prayerRegeneration.apply(player)
                objs.menaphite_remedy_1,
                objs.menaphite_remedy_2,
                objs.menaphite_remedy_3,
                objs.menaphite_remedy_4 -> menaphiteRemedy.apply(player)
                objs.antipoison_1,
                objs.antipoison_2,
                objs.antipoison_3,
                objs.antipoison_4 -> poison.apply(player, PoisonProtectionStrength.Antipoison)
                objs.superantipoison_1,
                objs.superantipoison_2,
                objs.superantipoison_3,
                objs.superantipoison_4 ->
                    poison.apply(player, PoisonProtectionStrength.Superantipoison)
                objs.antidote_plus_1,
                objs.antidote_plus_2,
                objs.antidote_plus_3,
                objs.antidote_plus_4 -> poison.apply(player, PoisonProtectionStrength.AntidotePlus)
                objs.antidote_plus_plus_1,
                objs.antidote_plus_plus_2,
                objs.antidote_plus_plus_3,
                objs.antidote_plus_plus_4 ->
                    poison.apply(player, PoisonProtectionStrength.AntidotePlusPlus)
                objs.anti_venom_1,
                objs.anti_venom_2,
                objs.anti_venom_3,
                objs.anti_venom_4 -> poison.apply(player, PoisonProtectionStrength.AntiVenom)
                objs.anti_venom_plus_1,
                objs.anti_venom_plus_2,
                objs.anti_venom_plus_3,
                objs.anti_venom_plus_4 ->
                    poison.apply(player, PoisonProtectionStrength.AntiVenomPlus)
                objs.extended_anti_venom_plus_1,
                objs.extended_anti_venom_plus_2,
                objs.extended_anti_venom_plus_3,
                objs.extended_anti_venom_plus_4 ->
                    poison.apply(player, PoisonProtectionStrength.ExtendedAntiVenomPlus)
                objs.sanfew_serum_1,
                objs.sanfew_serum_2,
                objs.sanfew_serum_3,
                objs.sanfew_serum_4 -> StatRestorePotionEffects.applySanfew(player, poison, disease)
                objs.antifire_potion_1,
                objs.antifire_potion_2,
                objs.antifire_potion_3,
                objs.antifire_potion_4 ->
                    antifire.apply(player, AntifireProtectionStrength.Antifire)
                objs.extended_antifire_1,
                objs.extended_antifire_2,
                objs.extended_antifire_3,
                objs.extended_antifire_4 ->
                    antifire.apply(player, AntifireProtectionStrength.ExtendedAntifire)
                objs.super_antifire_potion_1,
                objs.super_antifire_potion_2,
                objs.super_antifire_potion_3,
                objs.super_antifire_potion_4 ->
                    antifire.apply(player, AntifireProtectionStrength.SuperAntifire)
                objs.extended_super_antifire_1,
                objs.extended_super_antifire_2,
                objs.extended_super_antifire_3,
                objs.extended_super_antifire_4 ->
                    antifire.apply(player, AntifireProtectionStrength.ExtendedSuperAntifire)
                objs.restore_potion_1,
                objs.restore_potion_2,
                objs.restore_potion_3,
                objs.restore_potion_4 ->
                    StatRestorePotionEffects.apply(
                        player,
                        StatRestorePotionFamily.Restore,
                        stamina,
                        poison,
                        disease,
                    )
                objs.energy_potion_1,
                objs.energy_potion_2,
                objs.energy_potion_3,
                objs.energy_potion_4 ->
                    StatRestorePotionEffects.apply(
                        player,
                        StatRestorePotionFamily.Energy,
                        stamina,
                        poison,
                        disease,
                    )
                objs.prayer_potion_1,
                objs.prayer_potion_2,
                objs.prayer_potion_3,
                objs.prayer_potion_4 ->
                    StatRestorePotionEffects.apply(
                        player,
                        StatRestorePotionFamily.Prayer,
                        stamina,
                        poison,
                        disease,
                    )
                objs.super_energy_1,
                objs.super_energy_2,
                objs.super_energy_3,
                objs.super_energy_4 ->
                    StatRestorePotionEffects.apply(
                        player,
                        StatRestorePotionFamily.SuperEnergy,
                        stamina,
                        poison,
                        disease,
                    )
                objs.super_restore_1,
                objs.super_restore_2,
                objs.super_restore_3,
                objs.super_restore_4 ->
                    StatRestorePotionEffects.apply(
                        player,
                        StatRestorePotionFamily.SuperRestore,
                        stamina,
                        poison,
                        disease,
                    )
                objs.stamina_potion_1,
                objs.stamina_potion_2,
                objs.stamina_potion_3,
                objs.stamina_potion_4 ->
                    StatRestorePotionEffects.apply(
                        player,
                        StatRestorePotionFamily.Stamina,
                        stamina,
                        poison,
                        disease,
                    )
                else -> Unit
            }
        }
        onPlayerLogin {
            poison.resume(player)
            antifire.resume(player)
            goading.resume(player)
            prayerRegeneration.resume(player)
            menaphiteRemedy.resume(player)
        }
        onPlayerLogout {
            poison.pause(player)
            antifire.pause(player)
            goading.pause(player)
            prayerRegeneration.pause(player)
            menaphiteRemedy.pause(player)
        }
    }

    private companion object {
        val directStatBoostFamilies =
            setOf(
                BoostPotionFamily.Attack,
                BoostPotionFamily.Strength,
                BoostPotionFamily.Defence,
                BoostPotionFamily.Ranging,
                BoostPotionFamily.Magic,
                BoostPotionFamily.SuperAttack,
                BoostPotionFamily.SuperStrength,
                BoostPotionFamily.SuperDefence,
                BoostPotionFamily.SuperCombat,
                BoostPotionFamily.Bastion,
                BoostPotionFamily.Battlemage,
                BoostPotionFamily.Agility,
                BoostPotionFamily.Fishing,
            )
    }
}
