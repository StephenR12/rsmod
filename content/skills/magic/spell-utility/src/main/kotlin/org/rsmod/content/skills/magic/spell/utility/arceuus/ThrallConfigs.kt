package org.rsmod.content.skills.magic.spell.utility.arceuus

import org.rsmod.api.config.refs.objs
import org.rsmod.api.type.refs.comp.ComponentReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.spot.SpotanimReferences
import org.rsmod.game.hit.HitType
import org.rsmod.game.type.comp.ComponentType
import org.rsmod.game.type.npc.NpcType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.seq.SeqType
import org.rsmod.game.type.spot.SpotanimType

public object ThrallComponents : ComponentReferences() {
    val lesserGhost = find("magic_spellbook:resurrect_lesser_ghost")
    val lesserSkeleton = find("magic_spellbook:resurrect_lesser_skeleton")
    val lesserZombie = find("magic_spellbook:resurrect_lesser_zombie")
    val superiorGhost = find("magic_spellbook:resurrect_superior_ghost")
    val superiorSkeleton = find("magic_spellbook:resurrect_superior_skeleton")
    val superiorZombie = find("magic_spellbook:resurrect_superior_zombie")
    val greaterGhost = find("magic_spellbook:resurrect_greater_ghost")
    val greaterSkeleton = find("magic_spellbook:resurrect_greater_skeleton")
    val greaterZombie = find("magic_spellbook:resurrect_greater_zombie")
}

public object ThrallNpcs : NpcReferences() {
    val lesserGhost = find("arceuus_thrall_ghost_lesser")
    val superiorGhost = find("arceuus_thrall_ghost_superior")
    val greaterGhost = find("arceuus_thrall_ghost_greater")
    val lesserSkeleton = find("arceuus_thrall_skeleton_lesser")
    val superiorSkeleton = find("arceuus_thrall_skeleton_superior")
    val greaterSkeleton = find("arceuus_thrall_skeleton_greater")
    val lesserZombie = find("arceuus_thrall_zombie_lesser")
    val superiorZombie = find("arceuus_thrall_zombie_superior")
    val greaterZombie = find("arceuus_thrall_zombie_greater")
}

public object ThrallSeqs : SeqReferences() {
    val playerCast = find("arceuus_necromancy_playeranim")
    val ghostSpawn = find("ghost_update_thrall_spawn")
    val skeletonSpawn = find("skeleton_update_thrall_spawn")
    val zombieSpawn = find("zombie_update_thrawl_spawn")
    val ghostAttack = find("ghost_update_tendrill_attack")
    val skeletonAttack = find("skeleton_update_champion_attack")
    val zombieAttack = find("zombie_update_attack_normal")
}

public object ThrallSpotanims : SpotanimReferences() {
    val playerCast = find("arceuus_necromancy_playerspot")
    val rangedTravel = find("thrall_ranged_travel")
    val magicTravel = find("thrall_magic_travel")
    val magicImpact = find("thrall_magic_impact")
}

public enum class ThrallStyle(
    public val hitType: HitType,
    public val range: Int,
    public val spawnSeq: SeqType,
    public val attackSeq: SeqType,
    public val projectile: SpotanimType?,
) {
    Ghost(
        HitType.Magic,
        6,
        ThrallSeqs.ghostSpawn,
        ThrallSeqs.ghostAttack,
        ThrallSpotanims.magicTravel,
    ),
    Skeleton(
        HitType.Ranged,
        6,
        ThrallSeqs.skeletonSpawn,
        ThrallSeqs.skeletonAttack,
        ThrallSpotanims.rangedTravel,
    ),
    Zombie(HitType.Melee, 1, ThrallSeqs.zombieSpawn, ThrallSeqs.zombieAttack, null),
}

public data class ThrallRecipe(
    val component: ComponentType,
    val spell: ObjType,
    val npc: NpcType,
    val style: ThrallStyle,
    val maxHit: Int,
    val prayerCost: Int,
    val usedValue: Int,
)

public object ThrallRecipes {
    public val all: List<ThrallRecipe> =
        listOf(
            ThrallRecipe(
                ThrallComponents.lesserGhost,
                objs.spell_resurrect_lesser_ghost,
                ThrallNpcs.lesserGhost,
                ThrallStyle.Ghost,
                1,
                2,
                0,
            ),
            ThrallRecipe(
                ThrallComponents.lesserSkeleton,
                objs.spell_resurrect_lesser_skeleton,
                ThrallNpcs.lesserSkeleton,
                ThrallStyle.Skeleton,
                1,
                2,
                1,
            ),
            ThrallRecipe(
                ThrallComponents.lesserZombie,
                objs.spell_resurrect_lesser_zombie,
                ThrallNpcs.lesserZombie,
                ThrallStyle.Zombie,
                1,
                2,
                2,
            ),
            ThrallRecipe(
                ThrallComponents.superiorGhost,
                objs.spell_resurrect_superior_ghost,
                ThrallNpcs.superiorGhost,
                ThrallStyle.Ghost,
                2,
                4,
                3,
            ),
            ThrallRecipe(
                ThrallComponents.superiorSkeleton,
                objs.spell_resurrect_superior_skeleton,
                ThrallNpcs.superiorSkeleton,
                ThrallStyle.Skeleton,
                2,
                4,
                4,
            ),
            ThrallRecipe(
                ThrallComponents.superiorZombie,
                objs.spell_resurrect_superior_zombie,
                ThrallNpcs.superiorZombie,
                ThrallStyle.Zombie,
                2,
                4,
                5,
            ),
            ThrallRecipe(
                ThrallComponents.greaterGhost,
                objs.spell_resurrect_greater_ghost,
                ThrallNpcs.greaterGhost,
                ThrallStyle.Ghost,
                3,
                6,
                6,
            ),
            ThrallRecipe(
                ThrallComponents.greaterSkeleton,
                objs.spell_resurrect_greater_skeleton,
                ThrallNpcs.greaterSkeleton,
                ThrallStyle.Skeleton,
                3,
                6,
                7,
            ),
            ThrallRecipe(
                ThrallComponents.greaterZombie,
                objs.spell_resurrect_greater_zombie,
                ThrallNpcs.greaterZombie,
                ThrallStyle.Zombie,
                3,
                6,
                8,
            ),
        )

    public val npcTypes: Set<NpcType> = all.mapTo(linkedSetOf()) { it.npc }
}
