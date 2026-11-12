package org.rsmod.content.skills.fletching.toxicblowpipe

import jakarta.inject.Inject
import kotlin.math.min
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.synths
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.isAnyType

/** Cache-233 charging, ammunition and recovery operations for the standard toxic blowpipe. */
public class ToxicBlowpipeLifecycle @Inject constructor() {
    public fun applyOrnament(access: ProtectedAccess, blowpipeSlot: Int, kitSlot: Int) {
        with(access) {
            if (!requireMembers()) {
                return
            }
            val blowpipe = player.inv[blowpipeSlot]
            val kit = player.inv[kitSlot]
            if (
                blowpipe?.id != objs.toxic_blowpipe_empty.id ||
                    kit?.id != objs.trailblazer_reloaded_blowpipe_ornament_kit.id
            ) {
                return
            }
            val result =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, blowpipe.id, 1, blowpipeSlot)
                    delete(inventory, kit.id, 1, kitSlot)
                    add(inventory, objs.blazing_blowpipe_empty.id, 1, slot = blowpipeSlot)
                }
            check(result.success) { "Validated blazing-blowpipe creation transaction failed." }
            soundSynth(synths.found_gem)
            mes("You apply the ornament kit to the toxic blowpipe.")
        }
    }

    public fun loadScales(access: ProtectedAccess, blowpipeSlot: Int, scaleSlot: Int) {
        with(access) {
            if (!requireMembers()) {
                return
            }
            val blowpipe = validBlowpipe(blowpipeSlot) ?: return
            val input = player.inv[scaleSlot]
            if (input?.id != objs.zulrahs_scales.id) {
                return
            }
            val state = ToxicBlowpipeState.unpack(blowpipe)
            val amount = min(input.count, ToxicBlowpipeState.MAX_CAPACITY - state.scales)
            if (amount <= 0) {
                mes("The toxic blowpipe can't hold any more Zulrah's scales.")
                return
            }
            val updated = state.copy(scales = state.scales + amount)
            val result =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, objs.zulrahs_scales.id, amount, scaleSlot)
                    delete(inventory, blowpipe.id, 1, blowpipeSlot)
                    add(
                        inventory,
                        blowpipe.loadedType.id,
                        1,
                        vars = updated.pack(),
                        slot = blowpipeSlot,
                    )
                }
            check(result.success) { "Validated blowpipe scale transaction failed." }
            mes("You add $amount Zulrah's scales to the toxic blowpipe.")
        }
    }

    public fun loadDarts(
        access: ProtectedAccess,
        blowpipeSlot: Int,
        dartSlot: Int,
        dart: ToxicBlowpipeDart,
    ) {
        with(access) {
            if (!requireMembers()) {
                return
            }
            val blowpipe = validBlowpipe(blowpipeSlot) ?: return
            val input = player.inv[dartSlot]
            if (input?.id != dart.obj.id) {
                return
            }
            val state = ToxicBlowpipeState.unpack(blowpipe)
            val sameDart = state.dart == null || state.dart == dart
            val currentCount = if (sameDart) state.dartCount else 0
            val amount = min(input.count, ToxicBlowpipeState.MAX_CAPACITY - currentCount)
            if (amount <= 0) {
                mes("The toxic blowpipe can't hold any more darts.")
                return
            }
            val updated = state.copy(dart = dart, dartCount = currentCount + amount)
            val result =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, dart.obj.id, amount, dartSlot)
                    delete(inventory, blowpipe.id, 1, blowpipeSlot)
                    add(
                        inventory,
                        blowpipe.loadedType.id,
                        1,
                        vars = updated.pack(),
                        slot = blowpipeSlot,
                    )
                    if (!sameDart) {
                        add(inventory, requireNotNull(state.dart).obj.id, state.dartCount)
                    }
                }
            if (!result.success) {
                mes("You don't have enough inventory space to swap the darts in the blowpipe.")
                return
            }
            mes("You load $amount ${dart.displayName} into the toxic blowpipe.")
        }
    }

    public fun check(access: ProtectedAccess, blowpipe: InvObj?) {
        with(access) {
            if (!requireMembers()) {
                return
            }
            val state = ToxicBlowpipeState.unpack(blowpipe)
            val scalePercent = state.scales * 100 / ToxicBlowpipeState.MAX_CAPACITY
            mes("Scales: ${state.scales} ($scalePercent%).")
            val dartDescription = state.dart?.displayName ?: "darts"
            mes("Darts: ${state.dartCount} $dartDescription.")
        }
    }

    public fun unloadDarts(access: ProtectedAccess, blowpipeSlot: Int) {
        with(access) {
            if (!requireMembers()) {
                return
            }
            val blowpipe = validBlowpipe(blowpipeSlot) ?: return
            val state = ToxicBlowpipeState.unpack(blowpipe)
            val dart = state.dart
            if (dart == null || state.dartCount == 0) {
                mes("The toxic blowpipe has no darts to unload.")
                return
            }
            val updated = state.copy(dart = null, dartCount = 0)
            val result =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, blowpipe.id, 1, blowpipeSlot)
                    add(
                        inventory,
                        blowpipe.typeFor(updated).id,
                        1,
                        vars = updated.pack(),
                        slot = blowpipeSlot,
                    )
                    add(inventory, dart.obj.id, state.dartCount)
                }
            if (!result.success) {
                mes("You don't have enough inventory space to unload the darts.")
                return
            }
            mes("You unload ${state.dartCount} ${dart.displayName} from the toxic blowpipe.")
        }
    }

    public suspend fun uncharge(access: ProtectedAccess, blowpipeSlot: Int) {
        with(access) {
            if (!requireMembers()) {
                return
            }
            val blowpipe = validBlowpipe(blowpipeSlot) ?: return
            val state = ToxicBlowpipeState.unpack(blowpipe)
            if (state.isEmpty) {
                normalizeEmpty(blowpipeSlot, blowpipe)
                return
            }
            val confirmed =
                choice2(
                    "Proceed.",
                    true,
                    "Cancel.",
                    false,
                    title = "Uncharge the toxic blowpipe and recover all ammunition?",
                )
            if (!confirmed || player.inv[blowpipeSlot] != blowpipe) {
                return
            }
            val result =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, blowpipe.id, 1, blowpipeSlot)
                    add(inventory, blowpipe.emptyType.id, 1, slot = blowpipeSlot)
                    if (state.dart != null) {
                        add(inventory, state.dart.obj.id, state.dartCount)
                    }
                    if (state.scales > 0) {
                        add(inventory, objs.zulrahs_scales.id, state.scales)
                    }
                }
            if (!result.success) {
                mes("You don't have enough inventory space to uncharge the toxic blowpipe.")
                return
            }
            soundSynth(synths.destroy_object)
            mes("You uncharge the toxic blowpipe and recover all of its darts and scales.")
        }
    }

    public suspend fun dismantle(access: ProtectedAccess, blowpipeSlot: Int) {
        with(access) {
            if (!requireMembers()) {
                return
            }
            val blowpipe = player.inv[blowpipeSlot] ?: return
            if (!blowpipe.isAnyType(objs.toxic_blowpipe_empty, objs.blazing_blowpipe_empty)) {
                return
            }
            val blazing = blowpipe.id == objs.blazing_blowpipe_empty.id
            val confirmed =
                choice2(
                    "Dismantle it.",
                    true,
                    "Cancel.",
                    false,
                    title =
                        if (blazing) {
                            "Remove the ornament kit from the blazing blowpipe?"
                        } else {
                            "Dismantle the toxic blowpipe for 20,000 Zulrah's scales?"
                        },
                )
            if (!confirmed || player.inv[blowpipeSlot] != blowpipe) {
                return
            }
            val result =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, blowpipe.id, 1, blowpipeSlot)
                    if (blazing) {
                        add(inventory, objs.toxic_blowpipe_empty.id, 1, slot = blowpipeSlot)
                        add(inventory, objs.trailblazer_reloaded_blowpipe_ornament_kit.id, 1)
                    } else {
                        add(inventory, objs.zulrahs_scales.id, DISMANTLE_SCALES)
                    }
                }
            if (!result.success) {
                mes("You don't have enough inventory space to dismantle the blazing blowpipe.")
                return
            }
            if (blazing) {
                mes("You remove the ornament kit from the blazing blowpipe.")
            } else {
                mes("You dismantle the toxic blowpipe and receive 20,000 Zulrah's scales.")
            }
        }
    }

    private fun ProtectedAccess.requireMembers(): Boolean {
        if (!player.members) {
            mes("You need to be on a members' world to use a toxic blowpipe.")
            return false
        }
        return true
    }

    private fun ProtectedAccess.validBlowpipe(slot: Int): InvObj? {
        val blowpipe = player.inv[slot]
        return if (
            blowpipe.isAnyType(
                objs.toxic_blowpipe_empty,
                objs.toxic_blowpipe,
                objs.blazing_blowpipe_empty,
                objs.blazing_blowpipe,
            )
        ) {
            blowpipe
        } else {
            null
        }
    }

    private fun ProtectedAccess.normalizeEmpty(slot: Int, blowpipe: InvObj) {
        if (blowpipe.id == blowpipe.emptyType.id && blowpipe.vars == 0) {
            return
        }
        val result = invReplaceSlot(player.inv, slot, 1, blowpipe.emptyType)
        check(result.success) { "Unable to normalize empty toxic blowpipe." }
    }

    private val InvObj.isBlazing: Boolean
        get() = isAnyType(objs.blazing_blowpipe_empty, objs.blazing_blowpipe)

    private val InvObj.emptyType
        get() = if (isBlazing) objs.blazing_blowpipe_empty else objs.toxic_blowpipe_empty

    private val InvObj.loadedType
        get() = if (isBlazing) objs.blazing_blowpipe else objs.toxic_blowpipe

    private fun InvObj.typeFor(state: ToxicBlowpipeState) =
        if (state.isEmpty) emptyType else loadedType

    private companion object {
        const val DISMANTLE_SCALES = 20_000
    }
}
