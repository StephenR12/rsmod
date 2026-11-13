package org.rsmod.content.skills.combattraining

import jakarta.inject.Inject
import org.rsmod.api.clue.ClueTier
import org.rsmod.api.combat.commons.npc.inWildernessGodWars
import org.rsmod.api.combat.commons.player.queueCombatRetaliate
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.combat.formulas.AccuracyFormulae
import org.rsmod.api.config.refs.categories
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.api.death.*
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.npc.events.NpcHitEvents
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.script.*
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private fun spiritualRows(file: String): List<List<String>> =
    requireNotNull(MainSpiritualCreatures::class.java.getResourceAsStream(file))
        .bufferedReader()
        .useLines { lines ->
            lines.filter { it.isNotBlank() && !it.startsWith('#') }.map { it.split('\t') }.toList()
        }

internal object MainSpiritualSpawns : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<MainSpiritualCreatures>("main-spiritual-npcs.toml")
    }
}

private val spiritualStats = spiritualRows("spiritual-monster-stats.tsv")

internal object SpiritualNpcs : NpcReferences() {
    val types = spiritualStats.associate { it[1].toInt() to find(it[0]) }
}

internal object SpiritualSequences : SeqReferences() {
    val humanSlash = find("human_sword_slash")
    val humanBash = find("human_blunt_pound")
    val humanBow = find("human_bow")
    val humanCast = find("human_casting")
    val humanDeath = find("human_death")
    val orkAttack = find("ork_update_double_grip_attack")
    val orkCast = find("godwars_spiritual_ork_mage_magic_attack")
    val orkRange = find("godwars_spiritual_ork_ranger_ranged_attack")
    val orkDeath = find("ork_update_death")
    val avianWarrior = find("godwars_spiritual_armadyl_warrior_magic")
    val avianAttack = find("godwars_armadyl_spear_attack")
    val avianDeath = find("godwars_armadyl_death")

    fun attackFor(id: Int) =
        when (id) {
            2210 -> humanBash
            3159 -> humanSlash
            2211,
            3160 -> humanBow
            2212,
            3161 -> humanCast
            2242 -> orkRange
            2243 -> orkAttack
            2244 -> orkCast
            3166 -> avianWarrior
            else -> avianAttack
        }

    fun deathFor(id: Int) =
        when (id) {
            in 2242..2244 -> orkDeath
            in 3166..3168 -> avianDeath
            else -> humanDeath
        }
}

internal object SpiritualCombatStats : NpcEditor() {
    init {
        for (row in spiritualStats) {
            val (id, hp, att, str, def) = row.drop(1).take(5).map(String::toInt)
            val values = row.map { it.toIntOrNull() ?: 0 }
            edit(SpiritualNpcs.types.getValue(id)) {
                hitpoints = hp
                attack = att
                strength = str
                defence = def
                magic = values[6]
                ranged = values[7]
                param[params.attackrate] = values[8]
                param[params.slayer_levelrequire] = values[9]
                param[params.slayer_experience] = values[10] * 10
                param[params.attack_anim] = SpiritualSequences.attackFor(id)
                param[params.death_anim] = SpiritualSequences.deathFor(id)
                param[params.melee_strength] = values[13]
                param[params.attack_stab] = values[12]
                param[params.attack_slash] = values[12]
                param[params.attack_crush] = values[12]
                param[params.attack_ranged] = values[14]
                param[params.attack_magic] = values[15]
                param[params.defence_stab] = values[16]
                param[params.defence_slash] = values[17]
                param[params.defence_crush] = values[18]
                param[params.defence_magic] = values[19]
                param[params.defence_light] = values[20]
                param[params.defence_standard] = values[21]
                param[params.defence_heavy] = values[22]
                val reach = if (id in listOf(3159, 2210, 2243)) 1 else 6
                attackRange = reach
                param[params.attackrange] = reach
                respawnRate = values[23]
            }
        }
    }
}

public class MainSpiritualCreatures
@Inject
constructor(
    private val accuracy: AccuracyFormulae,
    private val items: ObjTypeList,
    private val drops: NpcDropTableRegistry,
    private val clues: NpcClueDropRolls,
) : PluginScript() {
    override fun ScriptContext.startup() {
        val loot = spiritualRows("spiritual-monster-loot.tsv").groupBy { it.first() }
        for (row in spiritualStats) {
            val id = row[1].toInt()
            val role = row.first().substringAfterLast('_')
            val type = SpiritualNpcs.types.getValue(id)
            val hitType =
                when {
                    role == "mage" -> HitType.Magic
                    role == "ranger" || id == 3166 -> HitType.Ranged
                    else -> HitType.Melee
                }
            val maximum = row[11].toInt()
            onAiOpPlayer2(type) { attack(it.target, id, hitType, maximum) }
            onAiApPlayer2(type) { attack(it.target, id, hitType, maximum) }
            if (id in 3166..3168) {
                onEvent<NpcHitEvents.ResolveDamage>(id) {
                    if (
                        hit.isFromPlayer &&
                            hit.type == HitType.Melee &&
                            hit.righthandType(items)?.isCategoryType(categories.halberd) != true
                    )
                        damage = 0
                }
            }
            val main =
                loot.getValue(role).map { entry ->
                    val item = entry[2].toInt()
                    val roll =
                        when (item) {
                            0 -> NpcDropRolls.nothing()
                            -1 ->
                                NpcGemDropRolls.standard(
                                    items.getValue(1452),
                                    NpcGemDropRolls::wearingWealth,
                                ) {
                                    it.hero.vars[varps.legends_quest] >= 75
                                }
                            else ->
                                NpcDropRolls.item(
                                    items.getValue(item),
                                    entry[3].toInt()..entry[4].toInt(),
                                )
                        }
                    NpcWeightedDrop(entry[1].toInt(), roll)
                }
            val hardClue = NpcDropRoll { c ->
                val imbued =
                    c.hero.worn.any { it?.id == 12785 || it != null && it.id in 20786..20790 }
                val rate = if (c.npc.spawnCoords.inWildernessGodWars() && imbued) 64 else 128
                clues.independent(ClueTier.Hard, items.getValue(2722), denominator = rate).roll(c)
            }
            drops.register(
                type,
                NpcDropTable(NpcDropRolls.sequence(NpcDropRolls.weighted(main), hardClue)),
            )
        }
    }

    private fun StandardNpcAccess.attack(target: Player, id: Int, type: HitType, maximum: Int) {
        if (
            actionDelay > mapClock ||
                !isWithinDistance(target, npc.visType.param(params.attackrange))
        )
            return
        actionDelay = mapClock + npc.visType.param(params.attackrate)
        anim(SpiritualSequences.attackFor(id))
        val hits =
            when (type) {
                HitType.Melee ->
                    target.vars[varbits.protect_from_melee] == 0 &&
                        accuracy.rollMeleeAccuracy(
                            npc,
                            target,
                            if (id == 3159) MeleeAttackType.Slash else MeleeAttackType.Crush,
                            random,
                        )
                HitType.Ranged ->
                    target.vars[varbits.protect_from_missiles] == 0 &&
                        accuracy.rollRangedAccuracy(npc, target, random)
                HitType.Magic ->
                    target.vars[varbits.protect_from_magic] == 0 &&
                        accuracy.rollMagicAccuracy(npc, target, random)
                else -> false
            }
        target.queueCombatRetaliate(npc)
        target.queueHit(
            npc,
            if (type == HitType.Melee) 1 else 2,
            type,
            if (hits) random.of(0..maximum) else 0,
        )
    }
}
