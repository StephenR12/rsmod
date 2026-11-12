package org.rsmod.content.skills.magic.spell.utility.standard

import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.params
import org.rsmod.game.entity.Npc
import org.rsmod.game.type.npc.UnpackedNpcType

/** Formats the single pipe-delimited argument consumed by cache clientscript 1179. */
public object MonsterInspectFormatter {
    public fun format(npc: Npc, type: UnpackedNpcType = npc.visType): String {
        val sections =
            listOf(
                type.name.replace('|', ' '),
                stats(npc, type),
                aggressive(type),
                defensive(type),
                other(type),
            )
        check(sections.none { '|' in it }) { "Monster Inspect sections cannot contain pipes." }
        return sections.joinToString("|")
    }

    private fun stats(npc: Npc, type: UnpackedNpcType): String =
        lines(
            "Stats",
            "Combat level: ${type.vislevel}",
            "Hitpoints: ${npc.hitpoints}",
            "Attack: ${npc.attackLvl}",
            "Defence: ${npc.defenceLvl}",
            "Strength: ${npc.strengthLvl}",
            "Magic: ${npc.magicLvl}",
            "Ranged: ${npc.rangedLvl}",
        )

    private fun aggressive(type: UnpackedNpcType): String {
        val values =
            mutableListOf("Aggressive stats", "Attack speed: ${type.param(params.attackrate)}")
        values.addBonus("Attack bonus", type.param(params.attack_melee))
        values.addBonus("Magic bonus", type.param(params.attack_magic))
        values.addBonus("Ranged bonus", type.param(params.attack_ranged))
        values.addBonus("Strength bonus", type.param(params.melee_strength))
        values.addBonus("Ranged strength bonus", type.param(params.ranged_strength))
        values.addPercentBonus("Magic strength bonus", type.param(params.npc_magic_damage_bonus))
        return lines(values)
    }

    private fun defensive(type: UnpackedNpcType): String =
        lines(
            "Defensive stats",
            "Stab bonus: ${signed(type.param(params.defence_stab))}",
            "Slash bonus: ${signed(type.param(params.defence_slash))}",
            "Crush bonus: ${signed(type.param(params.defence_crush))}",
            "Magic bonus: ${signed(type.param(params.defence_magic))}",
            "Light ranged bonus: ${signed(type.param(params.defence_light))}",
            "Standard ranged bonus: ${signed(type.param(params.defence_standard))}",
            "Heavy ranged bonus: ${signed(type.param(params.defence_heavy))}",
        )

    private fun other(type: UnpackedNpcType): String {
        val poisonable = type.param(params.poison_immunity) == constants.npc_immunity_none
        val venom =
            when (type.param(params.venom_immunity)) {
                constants.npc_venom_full_immunity -> "No"
                constants.npc_venom_partial_immunity -> "Poisons instead"
                else -> "Yes"
            }
        val weaknessType = type.paramOrNull(params.elemental_weakness_type)
        val weakness =
            when (weaknessType) {
                constants.elemental_weakness_wind -> "Air"
                constants.elemental_weakness_water -> "Water"
                constants.elemental_weakness_earth -> "Earth"
                constants.elemental_weakness_fire -> "Fire"
                else -> "None"
            }
        val weaknessPercent = type.param(params.elemental_weakness_percent)
        val weaknessText =
            if (weaknessType == null) {
                weakness
            } else {
                "$weakness ($weaknessPercent%)"
            }
        return lines(
            "Other attributes",
            "Slayer monster: ${yesNo(type.param(params.slayer_experience) > 0)}",
            "Poisonable: ${yesNo(poisonable)}",
            "Venomable: $venom",
            "Immune to thralls: ${yesNo(type.param(params.thrall_immunity) != 0)}",
            "Immune to burn: ${yesNo(type.param(params.burn_immunity) != 0)}",
            "Freeze resistance: ${type.param(params.freeze_resistance)}%",
            "Demonic: ${yesNo(type.param(params.demon) != 0)}",
            "Draconic: ${yesNo(type.param(params.draconic) != 0)}",
            "Rat: ${yesNo(type.param(params.rat) != 0)}",
            "Golem: ${yesNo(type.param(params.golem) != 0)}",
            "Elemental weakness: $weaknessText",
        )
    }

    private fun MutableList<String>.addBonus(label: String, value: Int) {
        if (value != 0) {
            add("$label: ${signed(value)}")
        }
    }

    private fun MutableList<String>.addPercentBonus(label: String, value: Int) {
        if (value != 0) {
            add("$label: ${signed(value)}%")
        }
    }

    private fun signed(value: Int): String = if (value > 0) "+$value" else value.toString()

    private fun yesNo(value: Boolean): String = if (value) "Yes" else "No"

    private fun lines(vararg values: String): String = lines(values.asList())

    private fun lines(values: List<String>): String = values.joinToString("<br>")
}
