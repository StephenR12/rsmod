package org.rsmod.api.clue

import jakarta.inject.Inject
import java.security.MessageDigest
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.obj.UnpackedObjType

/** Exact cache-233.4 initial-step pools used by medium, hard and elite clue rewards. */
public class Cache233ClueSteps @Inject constructor(objTypes: ObjTypeList) {
    private val stepsByTier: Map<ClueTier, List<UnpackedObjType>> =
        CONTRACTS.mapValues { (tier, contract) ->
            objTypes.values
                .asSequence()
                .filter { type ->
                    val internalName = type.internalName.orEmpty()
                    type.name == "Clue scroll (${tier.key})" &&
                        type.iop.getOrNull(0) == "Read" &&
                        type.iop.getOrNull(1) == "Check steps" &&
                        (internalName.startsWith("trail_clue_${tier.key}_") ||
                            internalName.startsWith("trail_${tier.key}_"))
                }
                .sortedBy { it.id }
                .toList()
                .also { resolved ->
                    val ids = resolved.map { it.id }
                    check(ids.size == contract.size) {
                        "Cache-233.4 ${tier.displayName} clue pool size changed: ${ids.size}"
                    }
                    check(
                        ids.firstOrNull() == contract.first && ids.lastOrNull() == contract.last
                    ) {
                        "Cache-233.4 ${tier.displayName} clue pool endpoints changed: " +
                            "${ids.firstOrNull()}..${ids.lastOrNull()}"
                    }
                    check(ids.sha256() == contract.sha256) {
                        "Cache-233.4 ${tier.displayName} clue pool identities changed: $ids"
                    }
                }
        }

    public fun steps(tier: ClueTier): List<UnpackedObjType> =
        stepsByTier[tier]
            ?: error("No cache-233.4 initial-step pool for ${tier.displayName} clues.")

    public fun select(tier: ClueTier, index: Int): ObjType {
        val steps = steps(tier)
        require(index in steps.indices) {
            "${tier.displayName} clue step index is outside 0..${steps.lastIndex}: $index"
        }
        return steps[index]
    }

    private fun List<Int>.sha256(): String {
        val input = joinToString(",").toByteArray(Charsets.UTF_8)
        return MessageDigest.getInstance("SHA-256").digest(input).joinToString("") { byte ->
            (byte.toInt() and 0xff).toString(16).padStart(2, '0')
        }
    }

    private data class Contract(val size: Int, val first: Int, val last: Int, val sha256: String)

    public companion object {
        public const val MEDIUM_SIZE: Int = 165
        public const val HARD_SIZE: Int = 185
        public const val ELITE_SIZE: Int = 135

        private val CONTRACTS: Map<ClueTier, Contract> =
            mapOf(
                ClueTier.Medium to
                    Contract(
                        MEDIUM_SIZE,
                        first = 2_801,
                        last = 30_935,
                        sha256 = "c82c7cdaba9f9c2e6a69bb5e2cd8a0fef968a0c0d35d20193a7ae0e26c7a7677",
                    ),
                ClueTier.Hard to
                    Contract(
                        HARD_SIZE,
                        first = 2_722,
                        last = 30_931,
                        sha256 = "ef27ab91ce81430005081b7dd4e954a70b5a4b20efe4edef2b9ecec13a0867ad",
                    ),
                ClueTier.Elite to
                    Contract(
                        ELITE_SIZE,
                        first = 12_073,
                        last = 30_932,
                        sha256 = "d74c0e9f41f75bc230b160b28e19be384b7412ec0ee630b45c6ddf0186d7738c",
                    ),
            )
    }
}
