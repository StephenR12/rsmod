package org.rsmod.content.bosses.world

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
    val sarachnisPet = find("sarachnispet_orange")
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
                CoordGrid(1842, 9923, 0),
                WorldBossFamily.Sarachnis,
                400,
                250,
                250,
                150,
                150,
                1,
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
