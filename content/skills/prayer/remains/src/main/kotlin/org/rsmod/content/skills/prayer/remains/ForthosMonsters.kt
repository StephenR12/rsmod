package org.rsmod.content.skills.prayer.remains

import jakarta.inject.Inject
import org.rsmod.api.combat.formulas.AccuracyFormulae
import org.rsmod.api.config.refs.*
import org.rsmod.api.death.*
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.random.GameRandom
import org.rsmod.api.script.*
import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object CryptMobs : NpcReferences() {
    val druid = find("hosdun_druid")
    val spider = find("hosdun_spider")
    val babies = listOf("babyreddragon", "babyreddragon2", "babyreddragon3").map { find(it) }
    val dragons =
        listOf("red_dragon", "red_dragon2", "red_dragon3", "red_dragon4", "red_dragon5").map {
            find(it)
        }
    val sarachnis = find("sarachnis")
}

internal object CryptDrops : ObjReferences() {
    val key = find("hosdun_grubby_key")
}

internal object ForthosMonsterStats : NpcEditor() {
    init {
        edit(CryptMobs.druid) {
            attackRange = 8
            hitpoints = 140
            attack = 60
            strength = 50
            defence = 60
            magic = 115
            ranged = 1
            param[params.attackrate] = 4
        }
        edit(CryptMobs.spider) {
            hitpoints = 70
            attack = 120
            strength = 50
            defence = 10
            magic = 1
            ranged = 1
            param[params.attackrate] = 4
        }
        for (t in CryptMobs.babies) edit(t) {
            hitpoints = 50
            attack = 40
            strength = 40
            defence = 40
            magic = 1
            ranged = 1
            param[params.attackrate] = 4
        }
        for (t in CryptMobs.dragons) edit(t) {
            hitpoints = 140
            attack = 130
            strength = 130
            defence = 130
            magic = 1
            ranged = 1
            param[params.attackrate] = 4
            param[params.defence_slash] = 70
            param[params.defence_crush] = 70
            param[params.defence_magic] = 60
            param[params.defence_ranged] = 50
        }
    }
}

internal object ForthosPopulation : org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<ForthosMonsters>("forthos-npcs.toml")
    }
}

public class ForthosMonsters
@Inject
constructor(
    private val drops: NpcDropTableRegistry,
    private val accuracy: AccuracyFormulae,
    private val random: GameRandom,
) : PluginScript() {
    override fun ScriptContext.startup() {
        val monsters =
            listOf(
                CryptMobs.spider to (30 to 100),
                CryptMobs.druid to (20 to 75),
                CryptMobs.sarachnis to (5 to 15),
            ) +
                CryptMobs.dragons.map { it to (10 to 50) } +
                CryptMobs.babies.map { it to (25 to 80) }
        for ((monster, rates) in monsters) {
            val loot = buildList {
                when (monster) {
                    CryptMobs.druid -> add(NpcDropRolls.item(objs.bones))
                    in CryptMobs.dragons -> {
                        add(NpcDropRolls.item(objs.dragon_bones))
                        add(NpcDropRolls.item(objs.red_dragonhide))
                    }
                    in CryptMobs.babies -> add(NpcDropRolls.item(objs.babydragon_bones))
                    else -> Unit
                }
                add(
                    NpcDropRolls.independent(
                        denominator = rates.second,
                        roll = NpcDropRolls.item(CryptDrops.key),
                    )
                )
                add(
                    NpcDropRolls.conditional(
                        { it.hero.vars[KnowledgeBits.noPages] == 0 },
                        NpcDropRolls.independent(
                            denominator = rates.first,
                            roll =
                                NpcDropRoll { ctx ->
                                    val player = ctx.hero
                                    val bank = player.invMap[invs.bank]
                                    val counts =
                                        KnowledgeItems.pages.mapIndexed { i, page ->
                                            player.vars[KnowledgeBits.pages[i]] +
                                                player.inv.sumOf {
                                                    if (it?.id == page.id) it.count else 0
                                                } +
                                                (bank?.sumOf {
                                                    if (it?.id == page.id) it.count else 0
                                                } ?: 0)
                                        }
                                    val least = counts.min()
                                    val tied = counts.indices.filter { counts[it] == least }
                                    val i = tied[ctx.random.of(tied.size)]
                                    NpcDropRolls.item(KnowledgeItems.pages[i]).roll(ctx)
                                },
                        ),
                    )
                )
            }
            drops.registerTertiary(
                monster,
                NpcDropRolls.conditional(
                    {
                        it.hero.members &&
                            it.coords.level == 0 &&
                            it.coords.x in 1792..1855 &&
                            it.coords.z in 9888..9983
                    },
                    NpcDropRolls.sequence(loot),
                ),
            )
        }
        onAiOpPlayer2(CryptMobs.druid) { druidAttack(this, it.target) }
        onAiApPlayer2(CryptMobs.druid) { druidAttack(this, it.target) }
    }

    private fun druidAttack(access: StandardNpcAccess, target: Player) {
        with(access) {
            if (actionDelay > mapClock || !isWithinDistance(target, 8)) return
            actionDelay = mapClock + 4
            val accurate = accuracy.rollMagicAccuracy(npc, target, random)
            target.queueHit(npc, 2, HitType.Magic, if (accurate) random.of(0..22) else 0)
        }
    }
}
