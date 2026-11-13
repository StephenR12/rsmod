package org.rsmod.content.bosses.slayer

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.npc.isAssignedSlayerTarget
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.death.NpcDropRolls.independent
import org.rsmod.api.death.NpcDropRolls.item
import org.rsmod.api.death.NpcDropRolls.sequence
import org.rsmod.api.death.NpcDropTable
import org.rsmod.api.death.NpcDropTableRegistry
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.stat.stat
import org.rsmod.api.script.*
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.game.hit.HitType
import org.rsmod.game.movement.MoveRestrict
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object CoveNpcs : NpcReferences() {
    val pool = find("slayer_kraken_sub")
    val kraken = find("slayer_kraken")
}

internal object CoveLocs : LocReferences() {
    val enter = find("slayer_cave_kraken_maincave_entrance")
    val leave = find("slayer_cave_kraken_maincave_exit")
    val boss = find("slayer_cave_kraken_boss_entrance")
    val bossExit = find("slayer_cave_kraken_boss_exit")
}

internal object CoveDrops : ObjReferences() {
    val empty = find("tots_uncharged")
    val tentacle = find("kraken_tentacle")
}

internal object CoveSeq : SeqReferences() {
    val arise = find("slayer_kraken_arise")
}

internal object CovePopulation : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<KrakenCove>("kraken-cove.toml")
    }
}

internal object CoveStats : NpcEditor() {
    init {
        for (type in listOf(CoveNpcs.pool, CoveNpcs.kraken)) edit(type) {
            defaultMode = NpcMode.None
            moveRestrict = MoveRestrict.NoMove
            attackRange = 10
            hitpoints = 125
            attack = 1
            strength = 1
            defence = 150
            magic = 120
            ranged = 1
            respawnRate = 28
            param[params.defence_magic] = -63
            param[params.defence_ranged] = 100
        }
    }
}

class KrakenCove
@Inject
constructor(private val types: NpcTypeList, private val drops: NpcDropTableRegistry) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(CoveLocs.enter) { telejump(CoordGrid(2276, 9988)) }
        onOpLoc1(CoveLocs.leave) { telejump(CoordGrid(2278, 3610)) }
        onOpLoc1(CoveLocs.boss) {
            if (
                types[SlayerBossNpcs.kraken].isAssignedSlayerTarget(player, it.loc.coords) &&
                    player.stat(stats.slayer) >= 87
            )
                telejump(CoordGrid(2280, 10022))
            else mes("You need a cave kraken assignment to enter.")
        }
        onOpLoc1(CoveLocs.bossExit) { telejump(CoordGrid(2280, 10016)) }
        onOpNpc2(CoveNpcs.pool) {
            val npc = it.npc
            if (npc.visType.id != CoveNpcs.pool.id) return@onOpNpc2
            if (
                player.stat(stats.slayer) < 87 ||
                    !types[CoveNpcs.kraken].isAssignedSlayerTarget(player, npc.coords)
            ) {
                mes("You can only disturb this whirlpool while assigned cave krakens.")
                return@onOpNpc2
            }
            npc.transmog(types[CoveNpcs.kraken], Int.MAX_VALUE)
            npc.anim(CoveSeq.arise)
            opNpc2(npc)
        }
        for (type in listOf(CoveNpcs.kraken, CoveNpcs.pool)) {
            onAiApPlayer2(type) { cast(this, it.target) }
            onAiOpPlayer2(type) { cast(this, it.target) }
        }
        val table =
            NpcDropTable(
                sequence(
                    independent(denominator = 200, roll = item(CoveDrops.empty)),
                    independent(denominator = 1200, roll = item(CoveDrops.tentacle)),
                )
            )
        drops.register(listOf(CoveNpcs.pool, CoveNpcs.kraken), table)
    }

    private fun cast(access: StandardNpcAccess, target: Player): Unit =
        with(access) {
            if (
                npc.visType.id != CoveNpcs.kraken.id ||
                    actionDelay > mapClock ||
                    !isWithinDistance(target, 10)
            )
                return
            actionDelay = mapClock + 6
            target.queueHit(
                npc,
                2,
                HitType.Magic,
                if (target.vars[varbits.protect_from_magic] != 0) 0 else random.of(14),
            )
        }
}
