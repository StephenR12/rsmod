package org.rsmod.api.player.worn

import org.rsmod.api.config.refs.params
import org.rsmod.game.type.obj.UnpackedObjType
import org.rsmod.game.type.obj.Wearpos

/** The law rift applies the monks' ordinary equipment restrictions before moving the player. */
public object EntranaEquipment {
    private val combatBonuses =
        listOf(
            params.attack_stab,
            params.attack_slash,
            params.attack_crush,
            params.attack_magic,
            params.attack_ranged,
            params.defence_stab,
            params.defence_slash,
            params.defence_crush,
            params.defence_magic,
            params.defence_ranged,
            params.melee_strength,
            params.ranged_strength,
        )
    private val exceptions =
        setOf(
            "Penance gloves",
            "Ice gloves",
            "Boots of lightness",
            "Magic secateurs",
            "Spotted cape",
            "Spottier cape",
            "3rd age cloak",
            "Book of the dead",
            "Chronicle",
            "Holy book",
            "Unholy book",
            "Book of balance",
            "Book of law",
            "Book of war",
            "Book of darkness",
            "Damaged book",
            "Saradomin cape",
            "Zamorak cape",
            "Guthix cape",
            "Saradomin cloak",
            "Guthix cloak",
            "Zamorak cloak",
            "Armadyl cloak",
            "Bandos cloak",
            "Ancient cloak",
        )
    private val banned =
        setOf(
            "Swift blade",
            "Ham joint",
            "Magic butterfly net",
            "Noose wand",
            "Hallowed hammer",
            "Sage's greaves",
            "Teasing stick",
            "Zealot's helm",
            "Extra supply crate",
            "Spoils of war",
            "Cannon base",
            "Cannon stand",
            "Cannon barrels",
            "Cannon furnace",
            "Impling jar",
        )

    public fun prohibited(item: UnpackedObjType): Boolean {
        val name = item.name
        if (
            name in banned ||
                name.contains("godsword blade", true) ||
                name.endsWith(" banner", true) ||
                name.startsWith("Prospector ", true) ||
                name.startsWith("Golden prospector ", true)
        )
            return true
        if (
            name in exceptions ||
                name.startsWith("Tome of water") ||
                name.startsWith("Tome of earth") ||
                name.startsWith("Robe top (") ||
                name.startsWith("Robe bottoms (")
        )
            return false
        return when (item.wearpos1) {
            Wearpos.Quiver.slot -> false
            Wearpos.Front.slot,
            Wearpos.Ring.slot -> false
            Wearpos.RightHand.slot ->
                item.iop[1] == "Wield" && combatBonuses.any { item.param(it) != 0 }
            else -> item.isEquipable && combatBonuses.any { item.param(it) != 0 }
        }
    }
}
