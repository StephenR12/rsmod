package org.rsmod.content.skills.herblore

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.events.PlayerConsumableEvent
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.varp.VarpType

public data class PotionQuestRequirement(
    public val varp: VarpType,
    public val minimumState: Int,
    public val failureMessage: String,
)

public data class StandardPotionDose(
    public val potion: ObjType,
    public val replacement: ObjType,
    public val dosesRemaining: Int,
    public val drinkMessage: String,
    public val membersOnly: Boolean,
    public val questRequirement: PotionQuestRequirement? = null,
)

private fun standardPotionDoses(
    four: ObjType,
    three: ObjType,
    two: ObjType,
    one: ObjType,
    drinkMessage: String,
    membersOnly: Boolean,
    questRequirement: PotionQuestRequirement? = null,
): List<StandardPotionDose> =
    listOf(
        StandardPotionDose(four, three, 3, drinkMessage, membersOnly, questRequirement),
        StandardPotionDose(three, two, 2, drinkMessage, membersOnly, questRequirement),
        StandardPotionDose(two, one, 1, drinkMessage, membersOnly, questRequirement),
        StandardPotionDose(one, objs.empty_vial, 0, drinkMessage, membersOnly, questRequirement),
    )

public object AntipoisonPotionDoses {
    public val all: List<StandardPotionDose> =
        standardPotionDoses(
            objs.antipoison_4,
            objs.antipoison_3,
            objs.antipoison_2,
            objs.antipoison_1,
            "You drink some of your antipoison potion.",
            membersOnly = false,
        )
}

public object HigherPoisonRemedyPotionDoses {
    public val superantipoison: List<StandardPotionDose> =
        standardPotionDoses(
            objs.superantipoison_4,
            objs.superantipoison_3,
            objs.superantipoison_2,
            objs.superantipoison_1,
            "You drink some of your super antipoison potion.",
            membersOnly = true,
        )

    public val antidotePlus: List<StandardPotionDose> =
        standardPotionDoses(
            objs.antidote_plus_4,
            objs.antidote_plus_3,
            objs.antidote_plus_2,
            objs.antidote_plus_1,
            "You drink some of your extra strong antipoison potion.",
            membersOnly = true,
        )

    public val antidotePlusPlus: List<StandardPotionDose> =
        standardPotionDoses(
            objs.antidote_plus_plus_4,
            objs.antidote_plus_plus_3,
            objs.antidote_plus_plus_2,
            objs.antidote_plus_plus_1,
            "You drink some of your super strong antipoison potion.",
            membersOnly = true,
        )

    public val antiVenom: List<StandardPotionDose> =
        standardPotionDoses(
            objs.anti_venom_4,
            objs.anti_venom_3,
            objs.anti_venom_2,
            objs.anti_venom_1,
            "You drink some of your antivenom potion.",
            membersOnly = true,
        )

    public val antiVenomPlus: List<StandardPotionDose> =
        standardPotionDoses(
            objs.anti_venom_plus_4,
            objs.anti_venom_plus_3,
            objs.anti_venom_plus_2,
            objs.anti_venom_plus_1,
            "You drink some of your super antivenom potion.",
            membersOnly = true,
        )

    public val extendedAntiVenomPlus: List<StandardPotionDose> =
        standardPotionDoses(
            objs.extended_anti_venom_plus_4,
            objs.extended_anti_venom_plus_3,
            objs.extended_anti_venom_plus_2,
            objs.extended_anti_venom_plus_1,
            "You drink some of your extended super antivenom+ potion",
            membersOnly = true,
        )

    public val families: List<List<StandardPotionDose>> =
        listOf(
            superantipoison,
            antidotePlus,
            antidotePlusPlus,
            antiVenom,
            antiVenomPlus,
            extendedAntiVenomPlus,
        )

    public val all: List<StandardPotionDose> = families.flatten()
}

public object SanfewSerumPotionDoses {
    public val all: List<StandardPotionDose> =
        standardPotionDoses(
            objs.sanfew_serum_4,
            objs.sanfew_serum_3,
            objs.sanfew_serum_2,
            objs.sanfew_serum_1,
            "You drink some of your Sanfew serum.",
            membersOnly = true,
        )
}

public object AntifirePotionDoses {
    public const val QUEST_START_STATE: Int = 1
    public const val QUEST_FAILURE_MESSAGE: String =
        "You must have started the Dragon Slayer I quest to drink this potion."

    private val questRequirement =
        PotionQuestRequirement(varps.dragonquest, QUEST_START_STATE, QUEST_FAILURE_MESSAGE)

    public val antifire: List<StandardPotionDose> =
        standardPotionDoses(
            objs.antifire_potion_4,
            objs.antifire_potion_3,
            objs.antifire_potion_2,
            objs.antifire_potion_1,
            "You drink some of your dragon potion.",
            membersOnly = true,
            questRequirement = questRequirement,
        )

    public val extendedAntifire: List<StandardPotionDose> =
        standardPotionDoses(
            objs.extended_antifire_4,
            objs.extended_antifire_3,
            objs.extended_antifire_2,
            objs.extended_antifire_1,
            "You drink some of your extended antifire potion.",
            membersOnly = true,
            questRequirement = questRequirement,
        )

    public val superAntifire: List<StandardPotionDose> =
        standardPotionDoses(
            objs.super_antifire_potion_4,
            objs.super_antifire_potion_3,
            objs.super_antifire_potion_2,
            objs.super_antifire_potion_1,
            "You drink some of your super antifire potion.",
            membersOnly = true,
            questRequirement = questRequirement,
        )

    public val extendedSuperAntifire: List<StandardPotionDose> =
        standardPotionDoses(
            objs.extended_super_antifire_4,
            objs.extended_super_antifire_3,
            objs.extended_super_antifire_2,
            objs.extended_super_antifire_1,
            "You drink some of your extended super antifire potion.",
            membersOnly = true,
            questRequirement = questRequirement,
        )

    public val families: List<List<StandardPotionDose>> =
        listOf(antifire, extendedAntifire, superAntifire, extendedSuperAntifire)

    public val all: List<StandardPotionDose> = families.flatten()
}

public object StatRestorePotionDoses {
    public val restore: List<StandardPotionDose> =
        standardPotionDoses(
            objs.restore_potion_4,
            objs.restore_potion_3,
            objs.restore_potion_2,
            objs.restore_potion_1,
            "You drink some of your stat restoration potion.",
            membersOnly = true,
        )

    public val energy: List<StandardPotionDose> =
        standardPotionDoses(
            objs.energy_potion_4,
            objs.energy_potion_3,
            objs.energy_potion_2,
            objs.energy_potion_1,
            "You drink some of your energy potion.",
            membersOnly = false,
        )

    public val prayer: List<StandardPotionDose> =
        standardPotionDoses(
            objs.prayer_potion_4,
            objs.prayer_potion_3,
            objs.prayer_potion_2,
            objs.prayer_potion_1,
            "You drink some of your restore prayer potion.",
            membersOnly = true,
        )

    public val superEnergy: List<StandardPotionDose> =
        standardPotionDoses(
            objs.super_energy_4,
            objs.super_energy_3,
            objs.super_energy_2,
            objs.super_energy_1,
            "You drink some of your super energy potion.",
            membersOnly = true,
        )

    public val superRestore: List<StandardPotionDose> =
        standardPotionDoses(
            objs.super_restore_4,
            objs.super_restore_3,
            objs.super_restore_2,
            objs.super_restore_1,
            "You drink some of your super restore potion.",
            membersOnly = true,
        )

    public val stamina: List<StandardPotionDose> =
        standardPotionDoses(
            objs.stamina_potion_4,
            objs.stamina_potion_3,
            objs.stamina_potion_2,
            objs.stamina_potion_1,
            "You drink some of your stamina potion.",
            membersOnly = true,
        )

    public val families: List<List<StandardPotionDose>> =
        listOf(restore, energy, prayer, superEnergy, superRestore, stamina)

    public val all: List<StandardPotionDose> = families.flatten()
}

/** Timed Prayer-restoration family that is explicitly incompatible with Stat Restore Pot Share. */
public object PrayerRegenerationPotionDoses {
    public val all: List<StandardPotionDose> =
        standardPotionDoses(
            objs.prayer_regeneration_potion_4,
            objs.prayer_regeneration_potion_3,
            objs.prayer_regeneration_potion_2,
            objs.prayer_regeneration_potion_1,
            "You drink some of your prayer regeneration potion.",
            membersOnly = true,
        )
}

/** Timed five-stat restoration family that is not shareable by either Lunar potion spell. */
public object MenaphiteRemedyPotionDoses {
    public val all: List<StandardPotionDose> =
        standardPotionDoses(
            objs.menaphite_remedy_4,
            objs.menaphite_remedy_3,
            objs.menaphite_remedy_2,
            objs.menaphite_remedy_1,
            "You drink some of your Menaphite remedy.",
            membersOnly = true,
        )
}

public object StatBoostPotionDoses {
    public val attack: List<StandardPotionDose> =
        standardPotionDoses(
            objs.attack_potion_4,
            objs.attack_potion_3,
            objs.attack_potion_2,
            objs.attack_potion_1,
            "You drink some of your attack potion.",
            membersOnly = true,
        )

    public val strength: List<StandardPotionDose> =
        standardPotionDoses(
            objs.strength_potion_4,
            objs.strength_potion_3,
            objs.strength_potion_2,
            objs.strength_potion_1,
            "You drink some of your strength potion.",
            membersOnly = false,
        )

    public val defence: List<StandardPotionDose> =
        standardPotionDoses(
            objs.defence_potion_4,
            objs.defence_potion_3,
            objs.defence_potion_2,
            objs.defence_potion_1,
            "You drink some of your defence potion.",
            membersOnly = true,
        )

    public val ranging: List<StandardPotionDose> =
        standardPotionDoses(
            objs.ranging_potion_4,
            objs.ranging_potion_3,
            objs.ranging_potion_2,
            objs.ranging_potion_1,
            "You drink some of your ranging potion.",
            membersOnly = true,
        )

    public val magic: List<StandardPotionDose> =
        standardPotionDoses(
            objs.magic_potion_4,
            objs.magic_potion_3,
            objs.magic_potion_2,
            objs.magic_potion_1,
            "You drink some of your magic potion.",
            membersOnly = true,
        )

    public val superAttack: List<StandardPotionDose> =
        standardPotionDoses(
            objs.super_attack_4,
            objs.super_attack_3,
            objs.super_attack_2,
            objs.super_attack_1,
            "You drink some of your attack potion.",
            membersOnly = true,
        )

    public val superStrength: List<StandardPotionDose> =
        standardPotionDoses(
            objs.super_strength_4,
            objs.super_strength_3,
            objs.super_strength_2,
            objs.super_strength_1,
            "You drink some of your strength potion.",
            membersOnly = true,
        )

    public val superDefence: List<StandardPotionDose> =
        standardPotionDoses(
            objs.super_defence_4,
            objs.super_defence_3,
            objs.super_defence_2,
            objs.super_defence_1,
            "You drink some of your defence potion.",
            membersOnly = true,
        )

    public val superCombat: List<StandardPotionDose> =
        standardPotionDoses(
            objs.super_combat_potion_4,
            objs.super_combat_potion_3,
            objs.super_combat_potion_2,
            objs.super_combat_potion_1,
            "You drink some of your super combat potion.",
            membersOnly = true,
        )

    public val bastion: List<StandardPotionDose> =
        standardPotionDoses(
            objs.bastion_potion_4,
            objs.bastion_potion_3,
            objs.bastion_potion_2,
            objs.bastion_potion_1,
            "You drink some of your bastion potion. It's a little tangy.",
            membersOnly = true,
        )

    public val battlemage: List<StandardPotionDose> =
        standardPotionDoses(
            objs.battlemage_potion_4,
            objs.battlemage_potion_3,
            objs.battlemage_potion_2,
            objs.battlemage_potion_1,
            "You drink some of your battlemage potion. It's a little tangy.",
            membersOnly = true,
        )

    public val agility: List<StandardPotionDose> =
        standardPotionDoses(
            objs.agility_potion_4,
            objs.agility_potion_3,
            objs.agility_potion_2,
            objs.agility_potion_1,
            "You drink some of your agility potion.",
            membersOnly = true,
        )

    public val fishing: List<StandardPotionDose> =
        standardPotionDoses(
            objs.fishing_potion_4,
            objs.fishing_potion_3,
            objs.fishing_potion_2,
            objs.fishing_potion_1,
            "You drink some of your fishing potion.",
            membersOnly = true,
        )

    public val families: List<List<StandardPotionDose>> =
        listOf(
            attack,
            strength,
            defence,
            ranging,
            magic,
            superAttack,
            superStrength,
            superDefence,
            superCombat,
            bastion,
            battlemage,
            agility,
            fishing,
        )

    public val all: List<StandardPotionDose> = families.flatten()
}

/** Direct stat-boost families that are explicitly incompatible with Boost Potion Share. */
public object DirectOnlyStatBoostPotionDoses {
    public val combat: List<StandardPotionDose> =
        standardPotionDoses(
            objs.combat_potion_4,
            objs.combat_potion_3,
            objs.combat_potion_2,
            objs.combat_potion_1,
            "You drink some of your combat potion.",
            membersOnly = true,
        )

    public val hunter: List<StandardPotionDose> =
        standardPotionDoses(
            objs.hunter_potion_4,
            objs.hunter_potion_3,
            objs.hunter_potion_2,
            objs.hunter_potion_1,
            "You drink some of your hunter potion.",
            membersOnly = true,
        )

    public val families: List<List<StandardPotionDose>> = listOf(combat, hunter)

    public val all: List<StandardPotionDose> = families.flatten()
}

/** Direct brew families that both Lunar potion-share spells explicitly reject. */
public object DirectBrewPotionDoses {
    public val saradomin: List<StandardPotionDose> =
        standardPotionDoses(
            objs.saradomin_brew_4,
            objs.saradomin_brew_3,
            objs.saradomin_brew_2,
            objs.saradomin_brew_1,
            "You drink some of the foul liquid.",
            membersOnly = true,
        )

    public val zamorak: List<StandardPotionDose> =
        standardPotionDoses(
            objs.zamorak_brew_4,
            objs.zamorak_brew_3,
            objs.zamorak_brew_2,
            objs.zamorak_brew_1,
            "You drink some of the foul liquid.",
            membersOnly = true,
        )

    public val ancient: List<StandardPotionDose> =
        standardPotionDoses(
            objs.ancient_brew_4,
            objs.ancient_brew_3,
            objs.ancient_brew_2,
            objs.ancient_brew_1,
            "You drink some of your ancient brew.",
            membersOnly = true,
        )

    public val forgotten: List<StandardPotionDose> =
        standardPotionDoses(
            objs.forgotten_brew_4,
            objs.forgotten_brew_3,
            objs.forgotten_brew_2,
            objs.forgotten_brew_1,
            "You drink some of your forgotten brew.",
            membersOnly = true,
        )

    public val families: List<List<StandardPotionDose>> =
        listOf(saradomin, zamorak, ancient, forgotten)

    public val all: List<StandardPotionDose> = families.flatten()
}

/** Shared direct-drinking path for standard cache-233 non-barbarian potion doses. */
public class StandardPotionDrinking
@Inject
constructor(private val potions: PotionConsumptionRepository) {
    public fun drink(access: ProtectedAccess, slot: Int, dose: StandardPotionDose) {
        with(access) {
            if (slot !in player.inv.indices) {
                return
            }
            val selected = player.inv[slot]
            if (selected?.id != dose.potion.id || !potions.isReady(player)) {
                return
            }
            if (dose.membersOnly && !player.members) {
                mes(MEMBERS_MESSAGE)
                return
            }
            val questRequirement = dose.questRequirement
            if (
                questRequirement != null &&
                    player.vars[questRequirement.varp] < questRequirement.minimumState
            ) {
                mes(questRequirement.failureMessage)
                return
            }

            val replacement =
                if (dose.dosesRemaining == 0 && player.vars[varbits.auto_smash_vials] != 0) {
                    null
                } else {
                    dose.replacement
                }
            val result =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, dose.potion.id, count = 1, slot = slot)
                    replacement?.let { add(inventory, it.id, count = 1, slot = slot) }
                }
            check(result.success) { "Potion replacement failed after validation." }

            stopAction()
            anim(seqs.human_eat)
            mes(dose.drinkMessage)
            if (dose.dosesRemaining == 0) {
                mes(FINISHED_MESSAGE)
            } else {
                val doseWord = if (dose.dosesRemaining == 1) "dose" else "doses"
                mes("You have ${dose.dosesRemaining} $doseWord of potion left.")
            }
            potions.consume(player, POTION_DELAY_CYCLES)
            publish(PlayerConsumableEvent.Consumed(player, dose.potion))
        }
    }

    public companion object {
        public const val POTION_DELAY_CYCLES: Int = 3
        public const val FINISHED_MESSAGE: String = "You have finished your potion."
        public const val MEMBERS_MESSAGE: String =
            "You need to be on a members' world to use this item."
    }
}
