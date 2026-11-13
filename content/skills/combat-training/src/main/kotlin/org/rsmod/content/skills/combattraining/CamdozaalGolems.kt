package org.rsmod.content.skills.combattraining

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.clue.ClueTier
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.categories
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.death.*
import org.rsmod.api.npc.events.NpcHitEvents
import org.rsmod.api.npc.events.NpcKillEvents
import org.rsmod.api.player.events.PlayerDeathEvent
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.*
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.game.hit.HitType
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjTypeList

internal enum class CamGolem(
    val hp: Int,
    val att: Int,
    val str: Int,
    val def: Int,
    val strBonus: Int,
    val shardNumerator: Int,
    val shardDenominator: Int,
    val shardLow: Int,
    val guardRate: Int,
    val clueRate: Int,
) {
    Flawed(25, 7, 12, 6, 15, 1, 16, 6, 800, 128),
    Mind(40, 14, 31, 25, 23, 2, 15, 8, 500, 50),
    Body(60, 33, 43, 45, 28, 2, 10, 10, 250, 62),
    Chaos(71, 45, 55, 59, 35, 3, 10, 12, 150, 100),
}

internal object RuinsRubble : NpcReferences() {
    val active = CamGolem.entries.map { find("camdozaal_golem_${it.name.lowercase()}") }
    val rocks = CamGolem.entries.map { find("camdozaal_golem_${it.name.lowercase()}_rock") }
}

internal object RuinsObjects : ObjReferences() {
    val guard = find("barronite_mace_3")
    val shards = find("camdozaal_barronite_shard")
    val cores = listOf("mind", "body", "chaos").map { find("camdozaal_golem_core_$it") }
}

internal object RuinsBits : VarBitReferences() {
    val quest = find("bim")
    val luck = find("camdozaal_rewards_buff")
    val timed = find("camdozaal_timed_multi_buff")
}

internal object RuinsAnims : SeqReferences() {
    val emerge = find("ancient_guardian_awaken")
    val attack = find("ancient_guardian_attack_melee")
    val crush = find("ancient_guardian_attack_melee_variation")
    val death = find("ancient_guardian_death")
}

internal object RuinsGround : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<CamdozaalGolems>("camdozaal-rubble.toml")
    }
}

internal object RuinsStats : NpcEditor() {
    init {
        for (g in CamGolem.entries) for (type in
            listOf(RuinsRubble.active[g.ordinal], RuinsRubble.rocks[g.ordinal])) edit(type) {
            hitpoints = g.hp
            attack = g.att
            strength = g.str
            defence = g.def
            magic = 1
            ranged = 1
            timer = 2
            defaultMode = NpcMode.None
            respawnRate = 25
            param[params.golem] = 1
            param[params.attackrate] = 5
            param[params.melee_strength] = g.strBonus
            param[params.attack_anim] =
                if (g == CamGolem.Chaos) RuinsAnims.crush else RuinsAnims.attack
            param[params.death_anim] = RuinsAnims.death
            param[params.npc_attack_type] =
                when (g) {
                    CamGolem.Chaos -> categories.attacktype_crush
                    CamGolem.Body -> categories.attacktype_slash
                    else -> categories.attacktype_stab
                }
            param[params.defence_stab] = 5
            param[params.defence_slash] = 5
            param[params.defence_magic] = 2
            param[params.defence_light] = 10
            param[params.defence_standard] = 10
            param[params.defence_heavy] = 10
            param[params.elemental_weakness_type] = constants.elemental_weakness_earth
            param[params.elemental_weakness_percent] = 35
        }
    }
}

@Singleton
class CamdozaalGolems
@Inject
constructor(
    private val registry: NpcDropTableRegistry,
    private val types: ObjTypeList,
    private val npcs: NpcTypeList,
    private val repo: NpcRepository,
    private val players: PlayerList,
    private val clues: NpcClueDropRolls,
) : org.rsmod.plugin.scripts.PluginScript() {
    private val awakened = mutableMapOf<Player, Npc>()

    override fun org.rsmod.plugin.scripts.ScriptContext.startup() {
        val primary =
            checkNotNull(CamdozaalGolems::class.java.getResourceAsStream("camdozaal-primary.tsv"))
                .bufferedReader()
                .useLines { lines ->
                    lines
                        .filter { !it.startsWith("#") && it.isNotBlank() }
                        .map { it.split('\t') }
                        .toList()
                        .groupBy { CamGolem.valueOf(it[0]) }
                }
        for (g in CamGolem.entries) {
            val rubble = RuinsRubble.rocks[g.ordinal]
            val active = RuinsRubble.active[g.ordinal]
            val drops =
                primary.getValue(g).map { r ->
                    NpcWeightedDrop(
                        r[4].toInt(),
                        NpcDropRolls.item(types.getValue(r[1].toInt()), r[2].toInt()..r[3].toInt()),
                    )
                }
            check(drops.sumOf { it.weight } == 52)
            val rolls =
                mutableListOf(
                    NpcDropRolls.weighted(drops),
                    NpcDropRolls.independent(
                        g.shardNumerator,
                        g.shardDenominator,
                        NpcDropRolls.item(RuinsObjects.shards, g.shardLow..g.shardLow * 2),
                    ),
                )
            if (g != CamGolem.Flawed)
                rolls += rare(2, 15, NpcDropRolls.item(RuinsObjects.cores[g.ordinal - 1]))
            rolls += rare(1, g.guardRate, NpcDropRolls.item(RuinsObjects.guard))
            rolls += NpcDropRoll { c ->
                clues
                    .independent(
                        ClueTier.Beginner,
                        objs.clue_scroll_beginner,
                        bonus(c.hero),
                        100 * g.clueRate,
                    )
                    .roll(c)
            }
            registry.register(listOf(rubble, active), NpcDropTable(NpcDropRolls.sequence(rolls)))
            onOpNpc1(rubble) { awaken(this, it.npc, g) }
            onModifyNpcHit(rubble) { hit.damage = 0 }
            onEvent<NpcHitEvents.ResolveDamage>(rubble.id) { damage = 0 }
            onModifyNpcHit(active) {
                val owner = awakened.entries.firstOrNull { it.value === npc }?.key
                if (owner == null || hit.isFromPlayer && hit.sourceSlot != owner.slotId)
                    hit.damage = 0
                else if (hit.type in setOf(HitType.Melee, HitType.Ranged, HitType.Magic))
                    hit.damage = (hit.damage - 1).coerceAtLeast(0)
            }
            onEvent<NpcHitEvents.ResolveDamage>(active.id) {
                if (hit.isFromPlayer && awakened[hit.resolvePlayerSource(players)] !== npc)
                    damage = 0
            }
            onAiTimer(active) { checkOwner(npc) }
            onAiTimer(rubble) { if (npc.visType.id != rubble.id) checkOwner(npc) }
        }
        onEvent<NpcKillEvents.Credited> { if (awakened[player] === npc) awakened.remove(player) }
        onPlayerLogout { abandon(player) }
        onEvent<PlayerDeathEvent.Completed> { abandon(player) }
    }

    internal fun awaken(a: ProtectedAccess, n: Npc, g: CamGolem): Boolean =
        with(a) {
            if (vars[RuinsBits.quest] < 120 || g == CamGolem.Chaos && !player.members) {
                mes("You cannot awaken this rubble.")
                return false
            }
            if (awakened.containsKey(player)) {
                mes("You're already in combat with a golem. There's no time to awaken another.")
                return false
            }
            if (!n.isVisible || n.visType.id != RuinsRubble.rocks[g.ordinal].id) return false
            awakened[player] = n
            val type = npcs[RuinsRubble.active[g.ordinal]]
            n.transmog(type, Int.MAX_VALUE)
            n.copyStats(type)
            n.anim(RuinsAnims.emerge)
            true
        }

    private fun checkOwner(n: Npc) {
        val player = awakened.entries.firstOrNull { it.value === n }?.key ?: return
        if (player.coords.level != n.level || player.coords.chebyshevDistance(n.spawnCoords) > 12)
            abandon(player)
    }

    private fun abandon(p: Player) {
        val n = awakened.remove(p) ?: return
        if (n.isSlotAssigned && n.isVisible && n.hitpoints > 0) {
            n.resetMovement()
            n.anim(RuinsAnims.death)
            repo.despawn(n, 25)
        }
    }

    private fun bonus(p: Player) =
        100 +
            (if (p.vars[RuinsBits.luck] != 0) 5 else 0) +
            (if (p.vars[RuinsBits.timed] != 0) 5 else 0)

    private fun rare(numerator: Int, denominator: Int, item: NpcDropRoll) = NpcDropRoll { c ->
        if (c.random.of(100 * denominator) < numerator * bonus(c.hero)) item.roll(c)
        else NpcDropRollResult.Miss
    }
}
