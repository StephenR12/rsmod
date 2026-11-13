package org.rsmod.content.skills.skillcapes

import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statBase
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.stat.StatType

/** Irreversibly trims ordinary skillcapes when a player reaches a second level 99. */
public class SkillcapeTrimming {
    public fun groundPickupType(player: Player, obj: Int): Int? {
        if (masteredSkills(player::statBase) < 2) {
            return null
        }
        return trimmedByUntrimmed[obj]?.id
    }

    public fun onLevelUp(access: ProtectedAccess, advanced: StatType) {
        with(access) {
            if (
                player.statBase(advanced) != advanced.maxLevel ||
                    masteredSkills(player::statBase) < 2
            ) {
                return
            }

            trim(inv)
            val wornChanged = trim(worn)
            trim(bank)
            if (wornChanged) {
                player.rebuildAppearance()
            }
        }
    }

    private fun trim(inventory: Inventory): Boolean {
        var changed = false
        for (slot in inventory.indices) {
            val cape = inventory[slot] ?: continue
            val trimmed = trimmedByUntrimmed[cape.id] ?: continue
            inventory[slot] = InvObj(trimmed, cape.count, cape.vars)
            changed = true
        }
        return changed
    }

    private fun masteredSkills(level: (StatType) -> Int): Int =
        definitions.count { level(it.stat) >= it.stat.maxLevel }

    public companion object {
        public val definitions: List<SkillcapeDefinition> =
            listOf(
                SkillcapeDefinition(objs.attack_skillcape, objs.attack_skillcape_t, stats.attack),
                SkillcapeDefinition(
                    objs.defence_skillcape,
                    objs.defence_skillcape_t,
                    stats.defence,
                ),
                SkillcapeDefinition(
                    objs.strength_skillcape,
                    objs.strength_skillcape_t,
                    stats.strength,
                ),
                SkillcapeDefinition(
                    objs.hitpoints_skillcape,
                    objs.hitpoints_skillcape_t,
                    stats.hitpoints,
                ),
                SkillcapeDefinition(objs.ranged_skillcape, objs.ranged_skillcape_t, stats.ranged),
                SkillcapeDefinition(objs.prayer_skillcape, objs.prayer_skillcape_t, stats.prayer),
                SkillcapeDefinition(objs.magic_skillcape, objs.magic_skillcape_t, stats.magic),
                SkillcapeDefinition(
                    objs.cooking_skillcape,
                    objs.cooking_skillcape_t,
                    stats.cooking,
                ),
                SkillcapeDefinition(
                    objs.woodcutting_skillcape,
                    objs.woodcutting_skillcape_t,
                    stats.woodcutting,
                ),
                SkillcapeDefinition(
                    objs.fletching_skillcape,
                    objs.fletching_skillcape_t,
                    stats.fletching,
                ),
                SkillcapeDefinition(
                    objs.fishing_skillcape,
                    objs.fishing_skillcape_t,
                    stats.fishing,
                ),
                SkillcapeDefinition(
                    objs.firemaking_skillcape,
                    objs.firemaking_skillcape_t,
                    stats.firemaking,
                ),
                SkillcapeDefinition(
                    objs.crafting_skillcape,
                    objs.crafting_skillcape_t,
                    stats.crafting,
                ),
                SkillcapeDefinition(
                    objs.smithing_skillcape,
                    objs.smithing_skillcape_t,
                    stats.smithing,
                ),
                SkillcapeDefinition(objs.mining_skillcape, objs.mining_skillcape_t, stats.mining),
                SkillcapeDefinition(
                    objs.herblore_skillcape,
                    objs.herblore_skillcape_t,
                    stats.herblore,
                ),
                SkillcapeDefinition(
                    objs.agility_skillcape,
                    objs.agility_skillcape_t,
                    stats.agility,
                ),
                SkillcapeDefinition(
                    objs.thieving_skillcape,
                    objs.thieving_skillcape_t,
                    stats.thieving,
                ),
                SkillcapeDefinition(objs.slayer_skillcape, objs.slayer_skillcape_t, stats.slayer),
                SkillcapeDefinition(
                    objs.farming_skillcape,
                    objs.farming_skillcape_t,
                    stats.farming,
                ),
                SkillcapeDefinition(
                    objs.runecrafting_skillcape,
                    objs.runecrafting_skillcape_t,
                    stats.runecrafting,
                ),
                SkillcapeDefinition(objs.hunter_skillcape, objs.hunter_skillcape_t, stats.hunter),
                SkillcapeDefinition(
                    objs.construction_skillcape,
                    objs.construction_skillcape_t,
                    stats.construction,
                ),
            )

        public val capePairs: List<Pair<ObjType, ObjType>> =
            definitions.map { it.untrimmed to it.trimmed }

        private val trimmedByUntrimmed: Map<Int, ObjType> =
            capePairs.associate { (untrimmed, trimmed) -> untrimmed.id to trimmed } +
                (WornAgilityCapes.plain.id to WornAgilityCapes.trimmed)
    }
}

internal object WornAgilityCapes:org.rsmod.api.type.refs.obj.ObjReferences(){
    val plain=find("skillcape_agility_worn")
    val trimmed=find("skillcape_agility_trimmed_worn")
}

public data class SkillcapeDefinition(
    public val untrimmed: ObjType,
    public val trimmed: ObjType,
    public val stat: StatType,
)
