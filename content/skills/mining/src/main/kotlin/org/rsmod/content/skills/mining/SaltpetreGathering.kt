package org.rsmod.content.skills.mining

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.diary.recordDiaryOre
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.api.repo.controller.ControllerRepository
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onAiConTimer
import org.rsmod.api.script.onGameStartup
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.type.builders.controller.ControllerBuilder
import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.game.entity.Controller
import org.rsmod.game.entity.Npc
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public object SaltpetreNpcs : NpcReferences() {
    val konoo = find("hosidius_saltpetre_miner")
}

public object SaltpetreSeqs : SeqReferences() {
    val dig = find("human_dig")
}

public object SaltpetreControllers : ControllerBuilder() {
    val deposit = build("hosidius_saltpetre_rotation")
}

internal object KonooEditor : NpcEditor() {
    init {
        edit(SaltpetreNpcs.konoo) { wanderRange = 0 }
    }
}

/**
 * Native deposits and five dated positions; three-tick digging and 25-yield/600-tick rotation are
 * approximate.
 */
@Singleton
public class SaltpetreGathering
@Inject
constructor(
    private val locs: LocRepository,
    private val npcs: NpcRepository,
    private val npcTypes: NpcTypeList,
    private val controllers: ControllerRepository,
) {
    private var index = 0
    private var yields = 0
    private var active: LocInfo? = null
    private var konoo: Npc? = null

    public fun startup() {
        rotate()
        val controller = Controller(SaltpetreControllers.deposit, centres[0])
        controller.aiTimer(600)
        controllers.add(controller, Int.MAX_VALUE)
    }

    public fun rotate() {
        active?.let { locs.del(it, Int.MAX_VALUE) }
        konoo?.let { npcs.del(it, Int.MAX_VALUE) }
        val coords = centres[index]
        active =
            locs.add(
                coords,
                KourendMineralLocs.saltpetre[index % 4],
                Int.MAX_VALUE,
                LocAngle.West,
                LocShape.CentrepieceStraight,
            )
        konoo =
            Npc(npcTypes[SaltpetreNpcs.konoo], coords.translate(-1, 0)).also {
                npcs.add(it, Int.MAX_VALUE)
            }
        index = (index + 1) % centres.size
        yields = 0
    }

    public suspend fun dig(access: ProtectedAccess, loc: BoundLocInfo): Unit =
        with(access) {
            if (!player.members) return
            if (objs.spade !in player.inv) {
                mes("You need a spade to dig for saltpetre.")
                return
            }
            val origin = player.coords
            while (true) {
                if (player.inv.isFull()) {
                    mes("Your inventory is too full to hold any more saltpetre.")
                    return
                }
                anim(SaltpetreSeqs.dig)
                gatheringDelay(3)
                if (
                    player.coords != origin ||
                        objs.spade !in player.inv ||
                        locs.findExact(loc.coords, loc.shape)?.id != loc.id
                )
                    return
                val double = random.of(100) < player.stat(stats.mining).coerceIn(1, 99)
                val count = if (double && player.inv.freeSpace() >= 2) 2 else 1
                if (!invAdd(inv, KourendMineralObjs.saltpetre, count).success) return
                player.recordDiaryOre(KourendMineralObjs.saltpetre.id, loc.coords)
                mes("You dig up some saltpetre.")
                yields++
                if (active?.coords == loc.coords && yields >= 25) {
                    rotate()
                    resetAnim()
                    return
                }
            }
        }

    public fun spade(access: ProtectedAccess): Unit =
        with(access) {
            if (!player.members) return
            if (objs.spade in player.inv) {
                mes("Use your spade to dig saltpetre from the active deposit.")
                return
            }
            if (invAdd(inv, objs.spade, 1).success) mes("Konoo gives you a spade.")
        }

    public companion object {
        public val centres: List<CoordGrid> =
            listOf(1708 to 3545, 1716 to 3518, 1687 to 3512, 1686 to 3529, 1702 to 3508).map {
                (x, z) ->
                CoordGrid(x, z)
            }
    }
}

public class SaltpetreScript @Inject constructor(private val gathering: SaltpetreGathering) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onGameStartup { gathering.startup() }
        onAiConTimer(SaltpetreControllers.deposit) {
            gathering.rotate()
            controller.aiTimer(600)
        }
        for (deposit in KourendMineralLocs.saltpetre) {
            onOpLoc1(deposit) { gathering.dig(this, it.loc) }
            onOpLocU(deposit, objs.spade) { gathering.dig(this, it.loc) }
        }
        onOpNpc1(SaltpetreNpcs.konoo) { gathering.spade(this) }
    }
}
