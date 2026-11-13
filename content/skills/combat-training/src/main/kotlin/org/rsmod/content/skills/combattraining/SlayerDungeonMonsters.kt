package org.rsmod.content.skills.combattraining

import jakarta.inject.Inject
import java.util.WeakHashMap
import org.rsmod.api.combat.commons.npc.isAssignedSlayerTarget
import org.rsmod.api.combat.commons.player.queueCombatRetaliate
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.combat.formulas.AccuracyFormulae
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.categories
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.api.death.*
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.npc.events.NpcHitEvents
import org.rsmod.api.npc.events.NpcKillEvents
import org.rsmod.api.npc.isOutOfCombat
import org.rsmod.api.player.hat
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.lefthand
import org.rsmod.api.script.*
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.hit.HitType
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object DungeonPopulation : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<SlayerDungeonMonsters>("slayer-dungeons.toml")
    }
}

internal object DungeonSpecies : NpcReferences() {
    val skeletal = (1..4).map { find("skeletal_wyvern$it") }
    val fossils =
        listOf("wyvern_long_tailed", "wyvern_taloned", "wyvern_spitting", "ancient_wyvern").map {
            find(it)
        }
    val wyrms = listOf(find("wyrm_dark"), find("wyrm_light"))
    val hydra = find("hydra")
    val tortoise = find("pog_mutated_tortoise")
    val birds = (1..8).map { find("pog_mutated_terrorbird_$it") }
    val byCacheId =
        (465..468).zip(skeletal).toMap() +
            (7792..7795).zip(fossils) +
            (8610..8611).zip(wyrms) +
            (8609 to hydra) +
            (12490 to tortoise) +
            (12491..12498).zip(birds)
}

internal object DungeonAnims : SeqReferences() {
    val skeletal =
        listOf(
                "wyvern_skeleton_bite",
                "wyvern_skeleton_launch_iceball",
                "wyvern_skeleton_ice_breath",
                "wyvern_skeleton_death",
            )
            .map { find(it) }
    val fossil =
        listOf(
                "fossil_npc_wyvern_flight_claw",
                "fossil_npc_wyvern_flight_fire",
                "fossil_npc_wyvern_flight_breath",
                "fossil_npc_wyvern_flight_death",
            )
            .map { find(it) }
    val wyrm =
        listOf("wyrm_attack_melee", "wyrm_attack_magic", "wyrm_attack_magic", "wyrm_death").map {
            find(it)
        }
    val hydra =
        listOf(
                "hydra_lesser_attack_ranged",
                "hydra_lesser_attack_magic",
                "hydra_lesser_attack_special",
                "hydra_lesser_death",
            )
            .map { find(it) }
    val tortoise =
        listOf("pog_mutated_battle_tortoise_attack", "pog_mutated_battle_tortoise_death").map {
            find(it)
        }
    val bird = listOf("terrorbird_attack", "terrorbird_death").map { find(it) }

    fun of(id: Int) =
        when (id) {
            in 465..468 -> skeletal
            in 7792..7795 -> fossil
            8609 -> hydra
            8610,
            8611 -> wyrm
            12490 -> tortoise
            else -> bird
        }
}

private fun rows(file: String) =
    requireNotNull(SlayerDungeonMonsters::class.java.getResourceAsStream(file))
        .bufferedReader()
        .useLines { lines ->
            lines
                .filter { it.isNotBlank() && !it.startsWith("#") }
                .map { line -> line.split('\t').map(String::toInt) }
                .toList()
        }

internal object DungeonMonsterStats : NpcEditor() {
    init {
        for (r in rows("slayer-dungeon-stats.tsv")) {
            val id = r[0]
            edit(DungeonSpecies.byCacheId.getValue(id)) {
                hitpoints = r[1]
                attack = r[2]
                strength = r[3]
                defence = r[4]
                magic = r[5]
                ranged = r[6]
                respawnRate = r[10]
                param[params.attackrate] = r[7]
                param[params.slayer_levelrequire] = r[8]
                param[params.slayer_experience] = r[9]
                param[params.melee_strength] = r[11]
                param[params.attack_magic] = r[12]
                param[params.attack_ranged] = r[13]
                param[params.defence_stab] = r[14]
                param[params.defence_slash] = r[15]
                param[params.defence_crush] = r[16]
                param[params.defence_magic] = r[17]
                param[params.defence_light] = r[19]
                param[params.defence_standard] = r[20]
                param[params.defence_heavy] = r[21]
                param[params.attack_anim] = DungeonAnims.of(id).first()
                param[params.death_anim] = DungeonAnims.of(id).last()
                param[params.npc_attack_type] =
                    if (id >= 12490) categories.attacktype_crush else categories.attacktype_slash
                attackRange =
                    when (id) {
                        12490 -> 1
                        8610,
                        8611 -> 6
                        else -> 8
                    }
                if (id in 8610..8611) timer = 2
                if (id < 12490) {
                    param[params.draconic] = 1
                    param[params.elemental_weakness_type] =
                        when (id) {
                            in 465..468 -> constants.elemental_weakness_fire
                            in 7792..7795 -> constants.elemental_weakness_wind
                            else -> constants.elemental_weakness_earth
                        }
                    param[params.elemental_weakness_percent] =
                        when (id) {
                            7793,
                            7795 -> 35
                            8609 -> 40
                            8610,
                            8611 -> 50
                            else -> 25
                        }
                }
            }
        }
    }
}

class SlayerDungeonMonsters
@Inject
constructor(
    private val accuracy: AccuracyFormulae,
    private val players: PlayerList,
    private val npcs: NpcTypeList,
    private val items: ObjTypeList,
    private val drops: NpcDropTableRegistry,
) : PluginScript() {
    private val hydraAttacks = WeakHashMap<Npc, Int>()
    private val lastFreeze = WeakHashMap<Player, Int>()

    override fun ScriptContext.startup() {
        val loot = rows("slayer-dungeon-loot.tsv").groupBy { it.first() }
        for ((id, type) in DungeonSpecies.byCacheId) {
            val family =
                when (id) {
                    in 465..468 -> 465
                    in 7792..7794 -> 7792
                    8611 -> 8610
                    in 12491..12498 -> 12491
                    else -> id
                }
            val table = loot.getValue(family)
            val weights =
                table.map { r -> NpcWeightedDrop(r[4], item(r[1], r[2]..r[3])) }.toMutableList()
            val gemSlots =
                when (family) {
                    12490,
                    12491 -> 3
                    8610,
                    8609 -> 1
                    else -> 0
                }
            if (gemSlots > 0)
                weights +=
                    NpcWeightedDrop(
                        gemSlots,
                        NpcGemDropRolls.standard(
                            items.getValue(1462),
                            NpcGemDropRolls::wearingWealth,
                        ) {
                            it.hero.vars[varps.legends_quest] >= 75
                        },
                    )
            if (family == 8610 || family == 8609)
                weights += NpcWeightedDrop(4, NpcHerbDropRolls.standard())
            if (family == 465)
                weights += NpcWeightedDrop(7, NpcDropRolls.repeated(3, NpcHerbDropRolls.standard()))
            val unused = table.first()[5] - weights.sumOf { it.weight }
            if (unused > 0) weights += NpcWeightedDrop(unused, NpcDropRolls.nothing())
            val bones =
                when (family) {
                    465,
                    7792,
                    7795 -> 6812
                    8609 -> 22786
                    8610 -> 22780
                    12490 -> 532
                    else -> 526
                }
            val rolls = mutableListOf(item(bones), NpcDropRolls.weighted(weights))
            when (family) {
                12490,
                12491 -> rolls += rare(320, 28583)
                465 -> {
                    rolls += rare(512, 6809)
                    rolls += rare(10000, 11286)
                }
                7792,
                7795 -> {
                    rolls += rare(if (id == 7795) 600 else 2560, 21643)
                    rolls += rare(if (id == 7795) 600 else 512, 21646)
                    rolls += rare(if (id == 7795) 10000 else 12000, 21637)
                }
                8610 -> {
                    for ((obj, q) in
                        listOf(
                            21009 to 1..1,
                            21028 to 1..1,
                            22804 to 75..150,
                            20849 to 75..150,
                        )) rolls +=
                        NpcDropRoll { c ->
                            NpcDropRolls.independent(
                                    denominator =
                                        if (c.npc.isAssignedSlayerTarget(c.hero)) 2000 else 10000,
                                    roll = item(obj, q),
                                )
                                .roll(c)
                        }
                }
            }
            drops.register(type, NpcDropTable(NpcDropRolls.sequence(rolls)))
            onAiOpPlayer2(type) { attack(this, it.target) }
            onAiApPlayer2(type) { attack(this, it.target) }
            if (id >= 12490) {
                onModifyNpcHit(type) {
                    if (
                        hit.isFromPlayer &&
                            hit.sourceSlot
                                ?.let { players[it] }
                                ?.inv
                                ?.contains(DungeonTools.chime) != true
                    )
                        hit.damage = 0
                }
                onEvent<NpcHitEvents.ResolveDamage>(type.id) {
                    if (
                        hit.isFromPlayer &&
                            hit.resolvePlayerSource(players)?.inv?.contains(DungeonTools.chime) !=
                                true
                    )
                        damage = 0
                }
            }
            if (id in 8610..8611)
                onAiTimer(type) {
                    if (
                        npc.id == 8610 &&
                            npc.visType.id == 8611 &&
                            npc.hitpoints > 0 &&
                            npc.isOutOfCombat()
                    )
                        npc.transmog(npcs.getValue(8610), Int.MAX_VALUE)
                }
        }
        onEvent<NpcKillEvents.Credited> { hydraAttacks.remove(npc) }
    }

    private fun item(id: Int, qty: IntRange = 1..1) = NpcDropRolls.item(items.getValue(id), qty)

    private fun rare(rate: Int, id: Int) =
        NpcDropRolls.independent(denominator = rate, roll = item(id))

    private fun attack(a: StandardNpcAccess, target: Player) =
        with(a) {
            val id = npc.visType.id
            if (actionDelay > mapClock || !isWithinDistance(target, npc.visType.attackRange))
                return@with
            if (id == 8610) npc.transmog(npcs.getValue(8611), Int.MAX_VALUE)
            val adjacent = isWithinDistance(target, 1)
            val type =
                when (id) {
                    8609 -> {
                        val tick = hydraAttacks.getOrPut(npc) { if (random.of(2) == 0) 0 else 3 }
                        hydraAttacks[npc] = (tick + 1) % 6
                        if (tick < 3) HitType.Ranged else HitType.Magic
                    }
                    12490 -> HitType.Melee
                    in 12491..12498 ->
                        if (
                            adjacent &&
                                target.vars[varbits.protect_from_melee] == 0 &&
                                (target.vars[varbits.protect_from_missiles] != 0 ||
                                    random.of(2) == 0)
                        )
                            HitType.Melee
                        else HitType.Ranged
                    8610,
                    8611 -> if (adjacent && random.of(2) == 0) HitType.Melee else HitType.Magic
                    else ->
                        if (random.of(3) == 0) HitType.Typeless
                        else if (adjacent && random.of(2) == 0) HitType.Melee
                        else if (id in 465..468 || id == 7794) HitType.Ranged else HitType.Magic
                }
            actionDelay = mapClock + npc.visType.param(params.attackrate)
            val sequence = DungeonAnims.of(id)
            anim(
                when {
                    type == HitType.Typeless -> sequence[2]
                    type == HitType.Melee || id >= 12490 || id == 8609 && type == HitType.Ranged ->
                        sequence.first()
                    else -> sequence[1]
                }
            )
            val shield = target.lefthand?.id
            val limit =
                when (type) {
                    HitType.Typeless ->
                        when {
                            shield in iceShields -> 10
                            target.vars[varbits.protect_from_magic] != 0 -> 20
                            id == 7795 -> 44
                            else -> 55
                        }
                    HitType.Melee ->
                        when (id) {
                            7793,
                            12490 -> 12
                            7794 -> 9
                            7795 -> 16
                            in 465..468,
                            7792 -> 13
                            else -> 10
                        }
                    HitType.Magic ->
                        when (id) {
                            8609 -> 22
                            8610,
                            8611 -> 13
                            else -> 10
                        }
                    HitType.Ranged ->
                        when (id) {
                            8609 -> 22
                            7794 -> 8
                            in 465..468 -> 13
                            in 12491..12498 -> {
                                val helm = items.getOrNull(target.hat)
                                val muffled =
                                    helm?.id == objs.earmuffs.id ||
                                        helm?.param(params.slayer_helm) == 1 ||
                                        helm?.param(params.slayer_helm_imbued) == 1
                                val max = if (muffled) 5 else 10
                                if (target.vars[varbits.protect_from_missiles] != 0) max / 2
                                else max
                            }
                            else -> 10
                        }
                }
            val hit =
                when (type) {
                    HitType.Typeless -> true
                    HitType.Melee ->
                        target.vars[varbits.protect_from_melee] == 0 &&
                            accuracy.rollMeleeAccuracy(
                                npc,
                                target,
                                if (id >= 12490) MeleeAttackType.Crush else MeleeAttackType.Slash,
                                random,
                            )
                    HitType.Magic ->
                        (id in 7792..7795 || target.vars[varbits.protect_from_magic] == 0) &&
                            accuracy.rollMagicAccuracy(npc, target, random)
                    HitType.Ranged ->
                        (id in 12491..12498 || target.vars[varbits.protect_from_missiles] == 0) &&
                            accuracy.rollRangedAccuracy(npc, target, random)
                }
            target.queueCombatRetaliate(npc)
            target.queueHit(
                npc,
                if (type == HitType.Melee) 1 else 2,
                type,
                if (hit) random.of(0..limit) else 0,
            )
            if (
                type == HitType.Typeless &&
                    shield !in setOf(21633, 21634) &&
                    (lastFreeze[target] ?: Int.MIN_VALUE) <= mapClock
            ) {
                target.abortRoute()
                target.delay(11)
                target.actionDelay = maxOf(target.actionDelay, mapClock + 11)
                lastFreeze[target] = mapClock + 16
            }
        }

    private companion object {
        val iceShields = setOf(2890, 9731, 11283, 11284, 22002, 22003, 21633, 21634)
    }
}
