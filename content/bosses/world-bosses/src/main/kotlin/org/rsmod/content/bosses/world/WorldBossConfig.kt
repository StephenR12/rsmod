package org.rsmod.content.bosses.world

import org.rsmod.api.config.refs.params
import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.type.npc.NpcType
import org.rsmod.map.CoordGrid

public object WorldBossNpcs : NpcReferences() {
    val derangedArchaeologist = find("fossil_crazy_archaeologist")
    val sarachnis = find("sarachnis")
    val sarachnisMeleeSpawn = find("sarachnis_melee_spawn")
    val sarachnisMagicSpawn = find("sarachnis_mage_spawn")
    val scurrius = find("rat_boss_normal")
    val giantRat = find("rat_boss_giant_rat")
    val revenantMaledictus = find("wild_cave_superior")
}

public object WorldBossObjs : ObjReferences() {
    val sarachnisCudgel = find("sarachnis_cudgel")
    val jarOfEyes = find("jar_of_eyes")
    val sarachnisPet = find("sarachnispet")
    val scurriusSpine = find("rat_boss_spine")
    val scurriusPet = find("scurriuspet")
    val webweaverBow = find("wild_cave_webweaver_charged")
    val ursineChainmace = find("wild_cave_ursine_charged")
    val accursedSceptre = find("wild_cave_accursed_charged")
}

public enum class WorldBossFamily {
    Archaeologist,
    Sarachnis,
    Scurrius,
    Maledictus,
}

public data class WorldBossDefinition(
    val npc: NpcType,
    val spawn: CoordGrid,
    val family: WorldBossFamily,
    val hitpoints: Int,
    val attack: Int,
    val strength: Int,
    val defence: Int,
    val magic: Int,
    val ranged: Int,
    val maxHit: Int,
)

public object WorldBosses {
    public val definitions: List<WorldBossDefinition> =
        listOf(
            WorldBossDefinition(
                WorldBossNpcs.derangedArchaeologist,
                CoordGrid(3677, 3812, 0),
                WorldBossFamily.Archaeologist,
                200,
                1,
                1,
                100,
                200,
                1,
                25,
            ),
            WorldBossDefinition(
                WorldBossNpcs.sarachnis,
                CoordGrid(1840, 9900, 0),
                WorldBossFamily.Sarachnis,
                400,
                200,
                240,
                150,
                150,
                300,
                31,
            ),
            WorldBossDefinition(
                WorldBossNpcs.scurrius,
                CoordGrid(3280, 9867, 0),
                WorldBossFamily.Scurrius,
                500,
                180,
                180,
                100,
                180,
                180,
                24,
            ),
            WorldBossDefinition(
                WorldBossNpcs.revenantMaledictus,
                CoordGrid(3243, 10172, 0),
                WorldBossFamily.Maledictus,
                1_250,
                320,
                320,
                260,
                320,
                320,
                48,
            ),
        )

    public val byId: Map<Int, WorldBossDefinition> = definitions.associateBy { it.npc.id }
}

internal object SpiderSpawnStats : NpcEditor() {
    init {
        for (type in listOf(WorldBossNpcs.sarachnisMeleeSpawn, WorldBossNpcs.sarachnisMagicSpawn)) {
            val melee = type === WorldBossNpcs.sarachnisMeleeSpawn
            edit(type) {
                hitpoints = 30
                attack = if (melee) 150 else 10
                strength = if (melee) 120 else 10
                defence = 50
                magic = if (melee) 1 else 100
                ranged = 1
                param[params.attackrate] = if (melee) 3 else 4
                param[params.attack_melee] = if (melee) 50 else 0
                param[params.defence_stab] = 50
                param[params.defence_slash] = 50
                param[params.defence_crush] = if (melee) 20 else 50
                param[params.defence_magic] = if (melee) -10 else 150
                param[params.defence_light] = if (melee) 150 else 0
                param[params.defence_standard] = if (melee) 150 else 0
                param[params.defence_heavy] = if (melee) 20 else 0
            }
        }
    }
}
