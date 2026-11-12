package org.rsmod.content.bosses.magearena

import jakarta.inject.Inject
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onAiApPlayer2
import org.rsmod.api.script.onAiOpPlayer2
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onModifyNpcHit
import org.rsmod.api.script.onNpcHit
import org.rsmod.api.script.onOpHeld1
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class MageArenaScript
@Inject
constructor(
    private val combat: MageArenaCombat,
    private val controller: MageArenaController,
    private val npcs: NpcRepository,
    private val npcTypes: NpcTypeList,
) : PluginScript() {
    override fun ScriptContext.startup() {
        MageArenaBosses.kolodionForms.forEach { form ->
            onAiOpPlayer2(form.npc) { combat.kolodionAttack(this, it.target) }
            onAiApPlayer2(form.npc) { combat.kolodionAttack(this, it.target) }
            onModifyNpcHit(form.npc) { controller.modifyKolodionHit(npc, hit) }
            onNpcHit(form.npc) { controller.onKolodionHit(npc, hit) }
        }
        listOf(MageArenaNpcs.zachariah, MageArenaNpcs.derwen, MageArenaNpcs.porazdir).forEach { type
            ->
            onAiOpPlayer2(type) { combat.followerAttack(this, it.target) }
            onAiApPlayer2(type) { combat.followerAttack(this, it.target) }
            onModifyNpcHit(type) { controller.modifyFollowerHit(npc, hit) }
        }
        onAiOpPlayer2(MageArenaNpcs.derwenEnergy) { combat.energyTick(this, it.target) }
        onAiApPlayer2(MageArenaNpcs.derwenEnergy) { combat.energyTick(this, it.target) }
        onOpHeld1(MageArenaObjs.enchantedSymbol) { controller.summonNextFollower(this) }
        onEvent<NpcStateEvents.Respawn> {
            if (npc.id in MageArenaBosses.kolodionIds) {
                val state = controller.resetKolodion(npc)
                controller.configureKolodion(npc, state.kolodionPhase)
            }
        }
        val existing =
            npcs.findAll(MageArenaBosses.mageArena).firstOrNull {
                it.id == MageArenaNpcs.kolodionHuman.id
            }
        val kolodion =
            existing
                ?: Npc(npcTypes[MageArenaNpcs.kolodionHuman], MageArenaBosses.mageArena).also {
                    npcs.add(it, Int.MAX_VALUE)
                }
        controller.resetKolodion(kolodion)
        controller.configureKolodion(kolodion, 0)
    }
}
