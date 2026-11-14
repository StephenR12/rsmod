package org.rsmod.api.clue

import jakarta.inject.Inject
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.obj.UnpackedObjType

/** Exact initial-step pool for ordinary cache-238 Easy clue rewards. */
public class Cache233EasyClueSteps @Inject constructor(objTypes: ObjTypeList) {
    public val steps: List<UnpackedObjType> =
        objTypes.values
            .asSequence()
            .filter { type ->
                val name = type.internalName.orEmpty()
                (name.startsWith("trail_clue_easy_") || name.startsWith("trail_easy_")) &&
                    !name.contains("_casket")
            }
            .sortedBy { it.id }
            .toList()
            .also { resolved ->
                check(resolved.map { it.id } == CACHE_238_IDS) {
                    "Cache-238 Easy clue pool changed: ${resolved.map { it.id }}"
                }
            }

    public fun select(index: Int): ObjType {
        require(index in steps.indices) {
            "Easy clue step index is outside 0..${steps.lastIndex}: $index"
        }
        return steps[index]
    }

    public companion object {
        public const val SIZE: Int = 166

        public val CACHE_233_IDS: List<Int>
            get() = CACHE_238_IDS.dropLast(1)

        /** Sorted IDs independently cross-checked against RuneLite 1.12.28's four clue decoders. */
        public val CACHE_238_IDS: List<Int> =
            buildList(SIZE) {
                    addAll(2_677..2_713)
                    add(2_716)
                    add(2_719)
                    addAll(3_490..3_510)
                    addAll(3_512..3_516)
                    add(3_518)
                    add(7_236)
                    add(7_238)
                    addAll((10_180..10_232).filter { it % 2 == 0 })
                    add(12_162)
                    add(12_164)
                    addAll(12_166..12_170)
                    addAll(12_172..12_179)
                    addAll(12_181..12_192)
                    add(19_814)
                    addAll(19_816..19_826)
                    addAll(19_828..19_831)
                    add(19_833)
                    add(22_001)
                    addAll(23_149..23_166)
                    addAll(25_788..25_789)
                    addAll(28_913..28_914)
                    addAll(29_853..29_854)
                    add(30_928)
                    add(31_268)
                }
                .also { check(it.size == SIZE) }
    }
}
