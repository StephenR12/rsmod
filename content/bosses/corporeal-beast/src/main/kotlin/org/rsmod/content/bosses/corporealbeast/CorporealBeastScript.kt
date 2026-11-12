package org.rsmod.content.bosses.corporealbeast

import jakarta.inject.Inject
import org.rsmod.api.config.refs.huntmodes
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onAiApPlayer2
import org.rsmod.api.script.onAiOpPlayer2
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onNpcHit
import org.rsmod.api.script.onOpLoc1
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class CorporealBeastScript
@Inject
constructor(
    private val combat: CorporealBeastCombat,
    private val controller: CorporealBeastController,
    private val npcs: NpcRepository,
    private val npcTypes: NpcTypeList,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onAiOpPlayer2(CorporealBeastNpcs.boss) { combat.bossAttack(this, it.target) }
        onAiApPlayer2(CorporealBeastNpcs.boss) { combat.bossAttack(this, it.target) }
        onAiOpPlayer2(CorporealBeastNpcs.darkCore) { combat.coreAttack(this, it.target) }
        onAiApPlayer2(CorporealBeastNpcs.darkCore) { combat.coreAttack(this, it.target) }
        onNpcHit(CorporealBeastNpcs.boss) { controller.onHit(npc, hit) }
        onEvent<NpcStateEvents.Respawn> {
            if (npc.id == CorporealBeastNpcs.boss.id) {
                configure(npc)
                controller.reset(npc)
            }
        }
        onOpLoc1(CorporealBeastLocs.wildernessEntrance) { telejump(CorporealBeastMap.lobby) }
        onOpLoc1(CorporealBeastLocs.lobbyEntrance) { telejump(CorporealBeastMap.bossRoom) }
        onOpLoc1(CorporealBeastLocs.lobbyExit) { telejump(CorporealBeastMap.wilderness) }
        val existing =
            npcs.findAll(CorporealBeastMap.bossRoom).firstOrNull {
                it.id == CorporealBeastNpcs.boss.id
            }
        val boss =
            existing
                ?: Npc(npcTypes[CorporealBeastNpcs.boss], CorporealBeastMap.bossRoom).also {
                    npcs.add(it, Int.MAX_VALUE)
                }
        configure(boss)
        controller.reset(boss)
    }

    private fun configure(npc: Npc) {
        npc.apply {
            hitpoints = CorporealBeastStats.HITPOINTS
            baseHitpointsLvl = CorporealBeastStats.HITPOINTS
            attackLvl = CorporealBeastStats.ATTACK
            baseAttackLvl = CorporealBeastStats.ATTACK
            strengthLvl = CorporealBeastStats.STRENGTH
            baseStrengthLvl = CorporealBeastStats.STRENGTH
            defenceLvl = CorporealBeastStats.DEFENCE
            baseDefenceLvl = CorporealBeastStats.DEFENCE
            magicLvl = CorporealBeastStats.MAGIC
            baseMagicLvl = CorporealBeastStats.MAGIC
            rangedLvl = CorporealBeastStats.RANGED
            baseRangedLvl = CorporealBeastStats.RANGED
            mode = NpcMode.Wander
            setHunt(18)
            setHuntMode(huntmodes.constant_ranged)
        }
    }
}
