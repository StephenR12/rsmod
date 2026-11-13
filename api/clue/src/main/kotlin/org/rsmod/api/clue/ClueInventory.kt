package org.rsmod.api.clue

import jakarta.inject.Inject
import org.rsmod.api.config.refs.invs
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.varbits
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory
import org.rsmod.game.type.inv.InvType
import org.rsmod.game.type.inv.InvTypeList
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.varbit.VarBitType

/** Cache-233 clue tier metadata shared by skilling and NPC drop resolvers. */
public enum class ClueTier(
    public val key: String,
    public val displayName: String,
    public val scrollBox: ObjType,
    public val minorCase: VarBitType,
    public val majorCase: VarBitType,
    public val members: Boolean,
) {
    Beginner(
        "beginner",
        "Beginner",
        objs.scroll_box_beginner,
        varbits.scroll_case_beginner_minor,
        varbits.scroll_case_beginner_major,
        false,
    ),
    Easy(
        "easy",
        "Easy",
        objs.scroll_box_easy,
        varbits.scroll_case_easy_minor,
        varbits.scroll_case_easy_major,
        true,
    ),
    Medium(
        "medium",
        "Medium",
        objs.scroll_box_medium,
        varbits.scroll_case_medium_minor,
        varbits.scroll_case_medium_major,
        true,
    ),
    Hard(
        "hard",
        "Hard",
        objs.scroll_box_hard,
        varbits.scroll_case_hard_minor,
        varbits.scroll_case_hard_major,
        true,
    ),
    Elite(
        "elite",
        "Elite",
        objs.scroll_box_elite,
        varbits.scroll_case_elite_minor,
        varbits.scroll_case_elite_major,
        true,
    ),
    Master(
        "master",
        "Master",
        objs.scroll_box_master,
        varbits.scroll_case_master_minor,
        varbits.scroll_case_master_major,
        true,
    ),
}

public data class ClueInventoryState(
    public val xMarksComplete: Boolean,
    public val owned: Int,
    public val capacity: Int,
) {
    public val canReceive: Boolean
        get() = owned < capacity
}

/** One clue reward resolved after its source-specific chance roll succeeds. */
public data class ClueReward(
    public val obj: ObjType?,
    public val groundDuration: Int?,
    public val message: String?,
) {
    init {
        require((obj == null) == (groundDuration == null)) {
            "A clue reward object and ground duration must be present together."
        }
    }
}

/**
 * Resolves the clue and container count held in the inventories that participate in ownership.
 *
 * Ground items deliberately do not count, preserving clue juggling. Death's Office storage will be
 * added when that inventory exists in the server model.
 */
public class ClueInventory @Inject constructor(objTypes: ObjTypeList, invTypes: InvTypeList) {
    private val bankType: InvType = invTypes[invs.bank]

    private val ownershipIds: Map<ClueTier, Set<Int>> =
        ClueTier.entries.associateWith { tier ->
            val suffix = "(${tier.key})"
            objTypes.values
                .asSequence()
                .filterNot { it.internalName.orEmpty().startsWith("placeholder_") }
                .filter { type ->
                    val name = type.name.lowercase()
                    name.endsWith(suffix) &&
                        (name.startsWith("clue scroll ") ||
                            name.startsWith("clue nest ") ||
                            name.startsWith("clue bottle ") ||
                            name.startsWith("clue geode ") ||
                            name.startsWith("scroll box "))
                }
                .map { it.id }
                .toSet()
        }

    private val scrollIds = ClueTier.entries.associateWith { tier ->
        objTypes.values.asSequence().filter {
            it.name.equals("Clue scroll (${tier.key})", ignoreCase = true) &&
                !it.internalName.orEmpty().startsWith("placeholder_")
        }.map { it.id }.toSet()
    }

    public fun ownsScroll(player: Player, tier: ClueTier): Boolean {
        val scrolls = scrollIds.getValue(tier)
        return player.inv.any { it?.id in scrolls } || player.invMap[bankType]?.any { it?.id in scrolls } == true
    }

    public fun state(
        player: Player,
        inventory: Inventory,
        bank: Inventory,
        tier: ClueTier,
    ): ClueInventoryState {
        val complete = player.vars[varbits.x_marks_the_spot] >= X_MARKS_COMPLETE
        val capacity = capacity(player, tier, complete)
        val ids = ownershipIds.getValue(tier)
        val owned = inventory.count(ids) + bank.count(ids)
        return ClueInventoryState(complete, owned, capacity)
    }

    /** Resolves ownership from the player's live inventory and bank without creating a bank. */
    public fun state(player: Player, tier: ClueTier): ClueInventoryState {
        val bank = player.invMap[bankType]
        val complete = player.vars[varbits.x_marks_the_spot] >= X_MARKS_COMPLETE
        val capacity = capacity(player, tier, complete)
        val ids = ownershipIds.getValue(tier)
        val owned = player.inv.count(ids) + (bank?.count(ids) ?: 0)
        return ClueInventoryState(complete, owned, capacity)
    }

    /** Converts a successful clue roll into its cache-233 pre- or post-quest reward. */
    public fun reward(state: ClueInventoryState, tier: ClueTier, clueScroll: ObjType): ClueReward {
        return reward(state, tier) { clueScroll }
    }

    /** Lazily resolves a clue step only when a pre-X-Marks scroll will actually be awarded. */
    public fun reward(
        state: ClueInventoryState,
        tier: ClueTier,
        clueScroll: () -> ObjType,
    ): ClueReward {
        if (!state.canReceive) {
            val message =
                if (state.xMarksComplete) {
                    "You have a sneaking suspicion that you would have received a " +
                        "${tier.key} scroll box."
                } else {
                    null
                }
            return ClueReward(obj = null, groundDuration = null, message = message)
        }
        return if (state.xMarksComplete) {
            ClueReward(tier.scrollBox, SCROLL_BOX_GROUND_DURATION, message = null)
        } else {
            ClueReward(clueScroll(), CLUE_SCROLL_GROUND_DURATION, message = null)
        }
    }

    public fun ownsAny(inventory: Inventory, bank: Inventory, tier: ClueTier): Boolean {
        val ids = ownershipIds.getValue(tier)
        return inventory.any { it?.id in ids } || bank.any { it?.id in ids }
    }

    private fun Inventory.count(ids: Set<Int>): Int = sumOf { obj ->
        if (obj != null && obj.id in ids) obj.count else 0
    }

    private fun capacity(player: Player, tier: ClueTier, complete: Boolean): Int {
        val capacity =
            if (!complete) {
                1
            } else {
                BASE_SCROLL_BOX_CAPACITY +
                    player.vars[tier.minorCase].coerceIn(0, 1) +
                    player.vars[tier.majorCase].coerceIn(0, 1) +
                    player.vars[varbits.scroll_case_mimic].coerceIn(0, 1)
            }
        check(capacity <= MAX_SCROLL_BOX_CAPACITY) {
            "Clue capacity exceeds cache limit: $capacity"
        }
        return capacity
    }

    public companion object {
        public const val X_MARKS_COMPLETE: Int = 8
        public const val BASE_SCROLL_BOX_CAPACITY: Int = 2
        public const val MAX_SCROLL_BOX_CAPACITY: Int = 5
        public const val CLUE_SCROLL_GROUND_DURATION: Int = 6_000
        public const val SCROLL_BOX_GROUND_DURATION: Int = 300
    }
}
