package org.rsmod.content.bosses.sporadic

import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.type.npc.NpcType
import org.rsmod.map.CoordGrid

public object SporadicBossNpcs : NpcReferences() {
    val obor = find("hillgiant_boss")
    val bryophyta = find("gb_mossgiant")
    val growthling = find("gb_growthling")
    val mimic = find("trail_mimic_combat")
    val thirdAgeWarrior = find("trail_mimic_spawn_melee")
    val thirdAgeRanger = find("trail_mimic_spawn_ranger")
    val thirdAgeMage = find("trail_mimic_spawn_mage")
    val hespori = find("hespori")
    val hesporiFlower = find("hespori_healer_active")
    val skotizo = find("cata_boss")
    val skotizoDemon = find("cata_boss_minion")
    val northAltar = find("cata_boss_altar_open_n")
    val eastAltar = find("cata_boss_altar_open_e")
    val southAltar = find("cata_boss_altar_open_s")
    val westAltar = find("cata_boss_altar_open_w")
}

public object SporadicBossObjs : ObjReferences() {
    val hillGiantClub = find("hillgiant_boss_club")
    val bryophytasEssence = find("gb_moss_essence")
    val ringOfThirdAge = find("ring_of_3rd_age")
    val bottomlessCompostBucket = find("bottomless_compost_bucket")
    val attasSeed = find("attas_seed")
    val iasorSeed = find("iasor_seed")
    val kronosSeed = find("kronos_seed")
    val ancientShard = find("cata_shard")
    val darkTotem = find("cata_totem")
    val darkClaw = find("cata_boss_claw")
    val jarOfDarkness = find("jar_of_darkness")
    val skotos = find("skotizopet")
}

public enum class SporadicBossFamily {
    Obor,
    Bryophyta,
    Mimic,
    Hespori,
    Skotizo,
}

public data class SporadicBossDefinition(
    val npc: NpcType,
    val spawn: CoordGrid,
    val family: SporadicBossFamily,
    val hitpoints: Int,
    val attack: Int,
    val strength: Int,
    val defence: Int,
    val magic: Int,
    val ranged: Int,
    val maxHit: Int,
)

public object SporadicBosses {
    public val definitions: List<SporadicBossDefinition> =
        listOf(
            SporadicBossDefinition(
                SporadicBossNpcs.obor,
                CoordGrid(3096, 9833, 0),
                SporadicBossFamily.Obor,
                120,
                120,
                120,
                60,
                1,
                80,
                22,
            ),
            SporadicBossDefinition(
                SporadicBossNpcs.bryophyta,
                CoordGrid(3216, 9934, 0),
                SporadicBossFamily.Bryophyta,
                115,
                100,
                100,
                100,
                100,
                1,
                18,
            ),
            SporadicBossDefinition(
                SporadicBossNpcs.mimic,
                CoordGrid(1761, 4978, 0),
                SporadicBossFamily.Mimic,
                230,
                180,
                180,
                120,
                180,
                180,
                23,
            ),
            SporadicBossDefinition(
                SporadicBossNpcs.hespori,
                CoordGrid(1246, 10086, 0),
                SporadicBossFamily.Hespori,
                300,
                1,
                1,
                100,
                150,
                150,
                31,
            ),
            SporadicBossDefinition(
                SporadicBossNpcs.skotizo,
                CoordGrid(1696, 9874, 0),
                SporadicBossFamily.Skotizo,
                450,
                240,
                240,
                200,
                240,
                1,
                38,
            ),
        )

    public val byId: Map<Int, SporadicBossDefinition> = definitions.associateBy { it.npc.id }
}

internal object SporadicBossAttackRanges : NpcEditor() {
    init {
        with(SporadicBossNpcs) {
            edit(skotizo) { attackRange = 10 }
        }
    }
}
