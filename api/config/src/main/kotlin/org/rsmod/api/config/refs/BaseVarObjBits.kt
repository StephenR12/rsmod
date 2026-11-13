@file:Suppress("SpellCheckingInspection", "unused")

package org.rsmod.api.config.refs

import org.rsmod.api.type.builders.varobjbit.VarObjBitBuilder

typealias varobjs = BaseVarObjBits

object BaseVarObjBits : VarObjBitBuilder() {
    val mixology_potion_state =
        build("mixology_potion_state") {
            startBit = 0
            endBit = 2
        }

    val blood_essence_charges =
        build("blood_essence_charges") {
            startBit = 0
            endBit = 9
        }

    val gnomish_firelighter_charges =
        build("gnomish_firelighter_charges") {
            startBit = 0
            endBit = 9
        }

    val gnomish_firelighter_colour =
        build("gnomish_firelighter_colour") {
            startBit = 10
            endBit = 12
        }

    val charges_16383 =
        build("charges_16383") {
            startBit = 0
            endBit = 13
        }

    val tumeken_charges =
        build("tumeken_charges") {
            startBit = 0
            endBit = 14
        }

    val tome_of_water_charges =
        build("tome_of_water_charges") {
            startBit = 0
            endBit = 14
        }

    val stamina_charges =
        build("stamina_charges") {
            startBit = 0
            endBit = 10
        }

    val infernal_tool_charges =
        build("infernal_tool_charges") {
            startBit = 0
            endBit = 12
        }

    val crystal_tool_charges =
        build("crystal_tool_charges") {
            startBit = 0
            endBit = 14
        }

    val snakeboss_blowpipe_darttype =
        build("snakeboss_blowpipe_darttype") {
            startBit = 0
            endBit = 3
        }

    @Deprecated(
        message = "Use snakeboss_blowpipe_darttype.",
        replaceWith = ReplaceWith("snakeboss_blowpipe_darttype"),
    )
    val snakeboss_blowpipe_darrtype = snakeboss_blowpipe_darttype

    val snakeboss_blowpipe_dartcount =
        build("snakeboss_blowpipe_dartcount") {
            startBit = 4
            endBit = 17
        }

    val snakeboss_blowpipe_flakes =
        build("snakeboss_blowpipe_flakes") {
            startBit = 18
            endBit = 31
        }
}
