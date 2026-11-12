package org.rsmod.content.generic.npcs.drops

import jakarta.inject.Inject
import org.rsmod.api.config.refs.invs
import org.rsmod.api.config.refs.varps
import org.rsmod.api.death.NpcDropRoll
import org.rsmod.api.death.NpcDropRolls.conditional
import org.rsmod.api.death.NpcDropRolls.item
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory
import org.rsmod.game.type.inv.InvType
import org.rsmod.game.type.inv.InvTypeList
import org.rsmod.game.type.obj.ObjType

/** Shared ownership and hand-in policy for the eight Rag and Bone Man I monster rewards. */
public class RagAndBoneManIDropPolicy @Inject constructor(invTypes: InvTypeList) {
    private val bankType: InvType = invTypes[invs.bank]

    /**
     * Returns a guaranteed quest-item roll which is selected only while this bone is still needed.
     */
    public fun guaranteed(bone: QuestBone): NpcDropRoll =
        conditional({ context -> isNeeded(context.hero, bone) }, item(bone.raw))

    /** Whether the active quest still requires [bone] from a monster kill. */
    public fun isNeeded(player: Player, bone: QuestBone): Boolean {
        if (!player.members || player.vars[varps.rag_and_bone_man_i] != COLLECTION_STATE) {
            return false
        }
        if (player.vars[varps.rag_and_bone_man_i_handed_mask] and bone.handedMask != 0) {
            return false
        }
        val ids = bone.lifecycleIds
        return !player.inv.containsAny(ids) && player.invMap[bankType]?.containsAny(ids) != true
    }

    /** Called by the quest hand-in script after it atomically consumes this polished bone. */
    public fun markHandedIn(player: Player, bone: QuestBone) {
        val updated = player.vars[varps.rag_and_bone_man_i_handed_mask] or bone.handedMask
        VarPlayerIntMapSetter.set(player, varps.rag_and_bone_man_i_handed_mask, updated)
    }

    private fun Inventory.containsAny(ids: Set<Int>): Boolean = any { it?.id in ids }

    public data class QuestBone(
        public val raw: ObjType,
        public val inVinegar: ObjType,
        public val polished: ObjType,
        public val handedIndex: Int,
    ) {
        init {
            require(handedIndex in 0 until QUEST_BONE_COUNT) {
                "Rag and Bone Man I hand-in index must be in 0..${QUEST_BONE_COUNT - 1}: " +
                    handedIndex
            }
            require(setOf(raw.id, inVinegar.id, polished.id).size == 3) {
                "Quest bone lifecycle objects must be distinct."
            }
        }

        public val handedMask: Int = 1 shl handedIndex
        public val lifecycleIds: Set<Int> = setOf(raw.id, inVinegar.id, polished.id)
    }

    public companion object {
        /**
         * Cutoff Quest Helper and quest DB row 114 identify state two as collection/preparation.
         */
        public const val COLLECTION_STATE: Int = 2
        public const val QUEST_COMPLETE_STATE: Int = 4
        public const val QUEST_BONE_COUNT: Int = 8
    }
}
