package org.rsmod.content.skills.magic.spell.utility.arceuus

import org.rsmod.api.config.refs.objs
import org.rsmod.api.type.refs.comp.ComponentReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.spot.SpotanimReferences
import org.rsmod.game.type.npc.NpcType
import org.rsmod.game.type.obj.ObjType

public object ReanimationComponents : ComponentReferences() {
    val basic = find("magic_spellbook:reanimation_basic")
    val adept = find("magic_spellbook:reanimation_adept")
    val expert = find("magic_spellbook:reanimation_expert")
    val master = find("magic_spellbook:reanimation_master")
}

public object ReanimationSeqs : SeqReferences() {
    val creature = find("arceuus_necromancy_anim")
    val player = find("arceuus_necromancy_playeranim")
}

public object ReanimationSpotanims : SpotanimReferences() {
    val player = find("arceuus_necromancy_playerspot")
    val projectile = find("arceuus_necromancy_projanim")
    val spawning = find("arceuus_necromancy_spawning")
}

public object ReanimationObjs : ObjReferences() {
    val goblinInitial = find("arceuus_corpse_goblin_initial")
    val goblin = find("arceuus_corpse_goblin")
    val monkeyInitial = find("arceuus_corpse_monkey_initial")
    val monkey = find("arceuus_corpse_monkey")
    val impInitial = find("arceuus_corpse_imp_initial")
    val imp = find("arceuus_corpse_imp")
    val minotaurInitial = find("arceuus_corpse_minotaur_initial")
    val minotaur = find("arceuus_corpse_minotaur")
    val scorpionInitial = find("arceuus_corpse_scorpion_initial")
    val scorpion = find("arceuus_corpse_scorpion")
    val bearInitial = find("arceuus_corpse_bear_initial")
    val bear = find("arceuus_corpse_bear")
    val unicornInitial = find("arceuus_corpse_unicorn_initial")
    val unicorn = find("arceuus_corpse_unicorn")
    val dogInitial = find("arceuus_corpse_dog_initial")
    val dog = find("arceuus_corpse_dog")
    val chaosDruidInitial = find("arceuus_corpse_chaosdruid_initial")
    val chaosDruid = find("arceuus_corpse_chaosdruid")
    val giantInitial = find("arceuus_corpse_giant_initial")
    val giant = find("arceuus_corpse_giant")
    val ogreInitial = find("arceuus_corpse_ogre_initial")
    val ogre = find("arceuus_corpse_ogre")
    val elfInitial = find("arceuus_corpse_elf_initial")
    val elf = find("arceuus_corpse_elf")
    val trollInitial = find("arceuus_corpse_troll_initial")
    val troll = find("arceuus_corpse_troll")
    val horrorInitial = find("arceuus_corpse_horror_initial")
    val horror = find("arceuus_corpse_horror")
    val kalphiteInitial = find("arceuus_corpse_kalphite_initial")
    val kalphite = find("arceuus_corpse_kalphite")
    val dagannothInitial = find("arceuus_corpse_dagannoth_initial")
    val dagannoth = find("arceuus_corpse_dagannoth")
    val bloodveldInitial = find("arceuus_corpse_bloodveld_initial")
    val bloodveld = find("arceuus_corpse_bloodveld")
    val tzhaarInitial = find("arceuus_corpse_tzhaar_initial")
    val tzhaar = find("arceuus_corpse_tzhaar")
    val demonInitial = find("arceuus_corpse_demon_initial")
    val demon = find("arceuus_corpse_demon")
    val hellhoundInitial = find("arceuus_corpse_hellhound_initial")
    val hellhound = find("arceuus_corpse_hellhound")
    val aviansieInitial = find("arceuus_corpse_aviansie_initial")
    val aviansie = find("arceuus_corpse_aviansie")
    val abyssalInitial = find("arceuus_corpse_abyssal_initial")
    val abyssal = find("arceuus_corpse_abyssal")
    val dragonInitial = find("arceuus_corpse_dragon_initial")
    val dragon = find("arceuus_corpse_dragon")
    val vorkathHead = find("vorkath_head")
}

public object ReanimationNpcs : NpcReferences() {
    val goblin = find("arceuus_reanimated_goblin")
    val monkey = find("arceuus_reanimated_monkey")
    val imp = find("arceuus_reanimated_imp")
    val minotaur = find("arceuus_reanimated_minotaur")
    val scorpion = find("arceuus_reanimated_scorpion")
    val bear = find("arceuus_reanimated_bear")
    val unicorn = find("arceuus_reanimated_unicorn")
    val dog = find("arceuus_reanimated_dog")
    val chaosDruid = find("arceuus_reanimated_chaosdruid")
    val giant = find("arceuus_reanimated_giant")
    val ogre = find("arceuus_reanimated_ogre")
    val elf = find("arceuus_reanimated_elf")
    val troll = find("arceuus_reanimated_troll")
    val horror = find("arceuus_reanimated_horror")
    val kalphite = find("arceuus_reanimated_kalphite")
    val dagannoth = find("arceuus_reanimated_dagannoth")
    val bloodveld = find("arceuus_reanimated_bloodveld")
    val tzhaar = find("arceuus_reanimated_tzhaar")
    val demon = find("arceuus_reanimated_demon")
    val hellhound = find("arceuus_reanimated_hellhound")
    val aviansie = find("arceuus_reanimated_aviansie")
    val abyssal = find("arceuus_reanimated_abyssal")
    val dragon = find("arceuus_reanimated_dragon")
}

public data class ReanimationRecipe(
    public val initialHead: ObjType,
    public val processedHead: ObjType,
    public val npc: NpcType,
    public val spell: ObjType,
    public val prayerXp: Double,
)

public object ReanimationRecipes {
    public val all by lazy {
        listOf(
            recipe(
                ReanimationObjs.goblinInitial,
                ReanimationObjs.goblin,
                ReanimationNpcs.goblin,
                objs.spell_basic_reanimation,
                130.0,
            ),
            recipe(
                ReanimationObjs.monkeyInitial,
                ReanimationObjs.monkey,
                ReanimationNpcs.monkey,
                objs.spell_basic_reanimation,
                182.0,
            ),
            recipe(
                ReanimationObjs.impInitial,
                ReanimationObjs.imp,
                ReanimationNpcs.imp,
                objs.spell_basic_reanimation,
                286.0,
            ),
            recipe(
                ReanimationObjs.minotaurInitial,
                ReanimationObjs.minotaur,
                ReanimationNpcs.minotaur,
                objs.spell_basic_reanimation,
                364.0,
            ),
            recipe(
                ReanimationObjs.scorpionInitial,
                ReanimationObjs.scorpion,
                ReanimationNpcs.scorpion,
                objs.spell_basic_reanimation,
                454.0,
            ),
            recipe(
                ReanimationObjs.bearInitial,
                ReanimationObjs.bear,
                ReanimationNpcs.bear,
                objs.spell_basic_reanimation,
                480.0,
            ),
            recipe(
                ReanimationObjs.unicornInitial,
                ReanimationObjs.unicorn,
                ReanimationNpcs.unicorn,
                objs.spell_basic_reanimation,
                494.0,
            ),
            recipe(
                ReanimationObjs.dogInitial,
                ReanimationObjs.dog,
                ReanimationNpcs.dog,
                objs.spell_adept_reanimation,
                520.0,
            ),
            recipe(
                ReanimationObjs.chaosDruidInitial,
                ReanimationObjs.chaosDruid,
                ReanimationNpcs.chaosDruid,
                objs.spell_adept_reanimation,
                584.0,
            ),
            recipe(
                ReanimationObjs.giantInitial,
                ReanimationObjs.giant,
                ReanimationNpcs.giant,
                objs.spell_adept_reanimation,
                650.0,
            ),
            recipe(
                ReanimationObjs.ogreInitial,
                ReanimationObjs.ogre,
                ReanimationNpcs.ogre,
                objs.spell_adept_reanimation,
                716.0,
            ),
            recipe(
                ReanimationObjs.elfInitial,
                ReanimationObjs.elf,
                ReanimationNpcs.elf,
                objs.spell_adept_reanimation,
                754.0,
            ),
            recipe(
                ReanimationObjs.trollInitial,
                ReanimationObjs.troll,
                ReanimationNpcs.troll,
                objs.spell_adept_reanimation,
                780.0,
            ),
            recipe(
                ReanimationObjs.horrorInitial,
                ReanimationObjs.horror,
                ReanimationNpcs.horror,
                objs.spell_adept_reanimation,
                832.0,
            ),
            recipe(
                ReanimationObjs.kalphiteInitial,
                ReanimationObjs.kalphite,
                ReanimationNpcs.kalphite,
                objs.spell_expert_reanimation,
                884.0,
            ),
            recipe(
                ReanimationObjs.dagannothInitial,
                ReanimationObjs.dagannoth,
                ReanimationNpcs.dagannoth,
                objs.spell_expert_reanimation,
                936.0,
            ),
            recipe(
                ReanimationObjs.bloodveldInitial,
                ReanimationObjs.bloodveld,
                ReanimationNpcs.bloodveld,
                objs.spell_expert_reanimation,
                1040.0,
            ),
            recipe(
                ReanimationObjs.tzhaarInitial,
                ReanimationObjs.tzhaar,
                ReanimationNpcs.tzhaar,
                objs.spell_expert_reanimation,
                1104.0,
            ),
            recipe(
                ReanimationObjs.demonInitial,
                ReanimationObjs.demon,
                ReanimationNpcs.demon,
                objs.spell_expert_reanimation,
                1170.0,
            ),
            recipe(
                ReanimationObjs.hellhoundInitial,
                ReanimationObjs.hellhound,
                ReanimationNpcs.hellhound,
                objs.spell_expert_reanimation,
                1200.0,
            ),
            recipe(
                ReanimationObjs.aviansieInitial,
                ReanimationObjs.aviansie,
                ReanimationNpcs.aviansie,
                objs.spell_master_reanimation,
                1234.0,
            ),
            recipe(
                ReanimationObjs.abyssalInitial,
                ReanimationObjs.abyssal,
                ReanimationNpcs.abyssal,
                objs.spell_master_reanimation,
                1300.0,
            ),
            recipe(
                ReanimationObjs.dragonInitial,
                ReanimationObjs.dragon,
                ReanimationNpcs.dragon,
                objs.spell_master_reanimation,
                1560.0,
            ),
        )
    }

    public val byHeadId by lazy {
        buildMap {
            for (recipe in all) {
                put(recipe.initialHead.id, recipe)
                put(recipe.processedHead.id, recipe)
            }
            put(ReanimationObjs.vorkathHead.id, all.last())
        }
    }

    public val npcTypes by lazy { all.map(ReanimationRecipe::npc) }

    private fun recipe(
        initialHead: ObjType,
        processedHead: ObjType,
        npc: NpcType,
        spell: ObjType,
        prayerXp: Double,
    ) = ReanimationRecipe(initialHead, processedHead, npc, spell, prayerXp)
}
