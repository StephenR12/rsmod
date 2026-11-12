package org.rsmod.content.skills.magic.spell.utility.standard

import kotlin.math.min
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.output.UpdateRun
import org.rsmod.api.player.stat.statHeal
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.entity.Player
import org.rsmod.game.type.obj.ObjType

/** Cache-233 immediate effects for the non-barbarian potions accepted by Stat Restore Pot Share. */
public object StatRestorePotionEffects {
    public fun apply(
        player: Player,
        family: StatRestorePotionFamily,
        stamina: StaminaEffectRepository,
        poison: PoisonProtectionRepository,
        disease: DiseaseImmunityRepository,
    ) {
        when (family) {
            StatRestorePotionFamily.Restore ->
                restoreCombatStats(player, constant = 10, percent = 30)
            StatRestorePotionFamily.Energy -> restoreRunEnergy(player, percent = 10)
            StatRestorePotionFamily.Prayer -> restorePrayer(player, constant = 7, percent = 25)
            StatRestorePotionFamily.SuperEnergy -> restoreRunEnergy(player, percent = 20)
            StatRestorePotionFamily.SuperRestore -> {
                restoreAllNonHitpoints(player, constant = 8, percent = 25)
                restorePrayer(player, constant = 8, percent = 25)
            }
            StatRestorePotionFamily.SanfewSerum -> {
                applySanfew(player, poison, disease)
            }
            StatRestorePotionFamily.Stamina -> {
                restoreRunEnergy(player, percent = 20)
                stamina.activate(player)
            }
        }
    }

    public fun applySanfew(
        player: Player,
        poison: PoisonProtectionRepository,
        disease: DiseaseImmunityRepository,
    ) {
        restoreAllNonHitpoints(player, constant = 4, percent = 30)
        restorePrayer(player, constant = 4, percent = 30)
        poison.apply(player, PoisonProtectionStrength.Superantipoison)
        VarPlayerIntMapSetter.set(player, varps.disease, 0)
        disease.activate(player)
    }

    private fun restoreCombatStats(player: Player, constant: Int, percent: Int) {
        for (stat in combatStats) {
            player.statHeal(stat, constant, percent)
        }
    }

    private fun restoreAllNonHitpoints(player: Player, constant: Int, percent: Int) {
        for (stat in nonHitpointsNonPrayerStats) {
            player.statHeal(stat, constant, percent)
        }
    }

    private fun restorePrayer(player: Player, constant: Int, percent: Int) {
        val modifier = if (hasHolyWrenchEffect(player)) 2 else 0
        player.statHeal(stats.prayer, constant, percent + modifier)
    }

    private fun restoreRunEnergy(player: Player, percent: Int) {
        val amount = (constants.run_max_energy * percent) / 100
        player.runEnergy = min(constants.run_max_energy, player.runEnergy + amount)
        UpdateRun.energy(player, player.runEnergy)
    }

    private fun hasHolyWrenchEffect(player: Player): Boolean {
        val inventoryEffect =
            player.inv.contains(objs.holy_wrench) ||
                player.inv.contains(objs.prayer_skillcape) ||
                player.inv.contains(objs.prayer_skillcape_t) ||
                player.inv.contains(objs.max_cape_inv) ||
                player.inv.contains(objs.max_cape_worn)
        val wornCapeEffect =
            player.worn.contains(objs.prayer_skillcape) ||
                player.worn.contains(objs.prayer_skillcape_t) ||
                player.worn.contains(objs.max_cape_inv) ||
                player.worn.contains(objs.max_cape_worn)
        val wornRingEffect =
            player.worn.contains(objs.ring_of_the_gods_i) ||
                player.worn.contains(objs.ring_of_the_gods_i_soul_wars) ||
                player.worn.contains(objs.ring_of_the_gods_i_pvp_arena)
        return inventoryEffect || wornCapeEffect || wornRingEffect
    }

    private val combatStats =
        listOf(stats.attack, stats.strength, stats.defence, stats.ranged, stats.magic)

    private val nonHitpointsNonPrayerStats =
        listOf(
            stats.attack,
            stats.defence,
            stats.strength,
            stats.ranged,
            stats.magic,
            stats.cooking,
            stats.woodcutting,
            stats.fletching,
            stats.fishing,
            stats.firemaking,
            stats.crafting,
            stats.smithing,
            stats.mining,
            stats.herblore,
            stats.agility,
            stats.thieving,
            stats.slayer,
            stats.farming,
            stats.runecrafting,
            stats.hunter,
            stats.construction,
        )
}

public enum class StatRestorePotionFamily(private val doses: List<ObjType>) {
    Restore(
        listOf(
            objs.restore_potion_1,
            objs.restore_potion_2,
            objs.restore_potion_3,
            objs.restore_potion_4,
        )
    ),
    Energy(
        listOf(
            objs.energy_potion_1,
            objs.energy_potion_2,
            objs.energy_potion_3,
            objs.energy_potion_4,
        )
    ),
    Prayer(
        listOf(
            objs.prayer_potion_1,
            objs.prayer_potion_2,
            objs.prayer_potion_3,
            objs.prayer_potion_4,
        )
    ),
    SuperEnergy(
        listOf(objs.super_energy_1, objs.super_energy_2, objs.super_energy_3, objs.super_energy_4)
    ),
    SuperRestore(
        listOf(
            objs.super_restore_1,
            objs.super_restore_2,
            objs.super_restore_3,
            objs.super_restore_4,
        )
    ),
    SanfewSerum(
        listOf(objs.sanfew_serum_1, objs.sanfew_serum_2, objs.sanfew_serum_3, objs.sanfew_serum_4)
    ),
    Stamina(
        listOf(
            objs.stamina_potion_1,
            objs.stamina_potion_2,
            objs.stamina_potion_3,
            objs.stamina_potion_4,
        )
    );

    public fun doseFor(objId: Int): Int = doses.indexOfFirst { it.id == objId } + 1

    public fun remainder(dose: Int, consumed: Int): ObjType {
        val remaining = dose - consumed
        return if (remaining == 0) objs.empty_vial else doses[remaining - 1]
    }

    public companion object {
        public fun forObj(objId: Int): StatRestorePotionFamily? =
            entries.firstOrNull { it.doseFor(objId) > 0 }
    }
}
