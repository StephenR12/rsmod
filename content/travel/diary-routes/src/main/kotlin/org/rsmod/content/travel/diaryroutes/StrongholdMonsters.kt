package org.rsmod.content.travel.diaryroutes

import org.rsmod.api.config.refs.categories
import org.rsmod.api.config.refs.huntmodes
import org.rsmod.api.config.refs.params
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.seq.SeqReferences

internal object StrongholdPlacements : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<StrongholdOfSecurity>("stronghold-npcs.toml")
    }
}

internal object MonsterStats : NpcReferences() {
    data class Monster(
        val npc:org.rsmod.game.type.npc.NpcType,
        val health:Int,val attack:Int,val strength:Int,val defence:Int,val magic:Int,val ranged:Int,
        val attackTicks:Int,val respawnTicks:Int,val accuracy:Int,val strengthBonus:Int,
        val stabDef:Int,val slashDef:Int,val crushDef:Int,val magicDef:Int,val rangedDef:Int,
        val style:String,val aggressive:Boolean
    )
    val all = requireNotNull(javaClass.getResourceAsStream("stronghold-monsters.tsv")).bufferedReader().useLines { lines ->
        lines.filter { it.isNotBlank() && !it.startsWith("#") }.map {
            val row = it.split('\t')
            val n = row.subList(1,16).map(String::toInt)
            Monster(find(row[0]),n[0],n[1],n[2],n[3],n[4],n[5],n[6],n[7],n[8],n[9],n[10],n[11],n[12],n[13],n[14],row[16],row[17].toBooleanStrict())
        }.toList()
    }
}

internal object StrongholdSequences : SeqReferences() {
    val byCreature = mapOf(
        "minotaur" to listOf("mino_attack","mino_parry","mino_death"),
        "fleshcrawler" to listOf("crawler_attack","crawler_parry","crawler_death"),
        "catablepon" to listOf("catpon_attack","catpon_parry","catpon_death"),
        "ankou" to listOf("skeleton_trans_attack","skeleton_trans_block","skeleton_trans_death")
    ).mapValues { (_,names) -> names.map { find(it) } }
}

internal object StrongholdMonsterTypes : NpcEditor() {
    init {
        for (monster in MonsterStats.all) edit(monster.npc) {
            hitpoints = monster.health
            attack = monster.attack
            strength = monster.strength
            defence = monster.defence
            magic = monster.magic
            ranged = monster.ranged
            respawnRate = monster.respawnTicks
            if (monster.aggressive) {
                huntMode = huntmodes.aggressive_melee
                huntRange = 5
            }
            param[params.attackrate] = monster.attackTicks
            param[params.npc_attack_type] = when (monster.style) {
                "stab" -> categories.attacktype_stab
                "slash" -> categories.attacktype_slash
                else -> categories.attacktype_crush
            }
            param[params.attack_melee] = monster.accuracy
            param[params.melee_strength] = monster.strengthBonus
            param[params.defence_stab] = monster.stabDef
            param[params.defence_slash] = monster.slashDef
            param[params.defence_crush] = monster.crushDef
            param[params.defence_magic] = monster.magicDef
            for (paramType in listOf(params.defence_light,params.defence_standard,params.defence_heavy)) {
                param[paramType] = monster.rangedDef
            }
            val anims = StrongholdSequences.byCreature.entries.firstOrNull { it.key in monster.npc.internalNameValue }?.value
            if (anims != null) {
                param[params.attack_anim] = anims[0]
                param[params.defend_anim] = anims[1]
                param[params.death_anim] = anims[2]
            }
        }
    }
}
