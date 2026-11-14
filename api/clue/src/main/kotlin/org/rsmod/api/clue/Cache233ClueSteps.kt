package org.rsmod.api.clue

import jakarta.inject.Inject
import java.security.MessageDigest
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.obj.UnpackedObjType

/** Exact cache-238 initial-step pools used by medium, hard and elite clue rewards. */
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
                        "Cache-238 ${tier.displayName} clue pool size changed: ${ids.size}"
                    }
                    check(
                        ids.firstOrNull() == contract.first && ids.lastOrNull() == contract.last
                    ) {
                        "Cache-238 ${tier.displayName} clue pool endpoints changed: " +
                            "${ids.firstOrNull()}..${ids.lastOrNull()}"
                    }
                    check(ids.sha256() == contract.sha256) {
                        "Cache-238 ${tier.displayName} clue pool identities changed: $ids"
                    }
                }
        }

    public fun steps(tier: ClueTier): List<UnpackedObjType> =
        stepsByTier[tier]
            ?: error("No cache-238 initial-step pool for ${tier.displayName} clues.")

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
        public const val MEDIUM_SIZE: Int = 166
        public const val HARD_SIZE: Int = 187
        public const val ELITE_SIZE: Int = 138

        private val CONTRACTS: Map<ClueTier, Contract> =
            mapOf(
                ClueTier.Medium to
                    Contract(
                        MEDIUM_SIZE,
                        first = 2_801,
                        last = 31_275,
                        sha256 = "2e2c6ec45d1627a8b307bd8e08d05c56ed230d1d4ba42e6ebc142f37e038d495",
                    ),
                ClueTier.Hard to
                    Contract(
                        HARD_SIZE,
                        first = 2_722,
                        last = 31_273,
                        sha256 = "2b766e35c4f697d2e6672199d61682652870c0c17cefcc677a4dc0f0af61157d",
                    ),
                ClueTier.Elite to
                    Contract(
                        ELITE_SIZE,
                        first = 12_073,
                        last = 31_271,
                        sha256 = "d72d689134c554390fa437364fb3b6d922d8f8436a3b618b3f80b293d9f00a16",
                    ),
            )
    }
}
