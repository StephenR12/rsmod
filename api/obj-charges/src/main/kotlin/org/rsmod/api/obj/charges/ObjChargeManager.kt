package org.rsmod.api.obj.charges

import jakarta.inject.Inject
import kotlin.contracts.contract
import org.rsmod.api.config.refs.params
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.obj.Wearpos
import org.rsmod.game.type.varobjbit.UnpackedVarObjBitType
import org.rsmod.utils.bits.bitMask
import org.rsmod.utils.bits.getBits
import org.rsmod.utils.bits.withBits

public class ObjChargeManager @Inject constructor(private val objTypes: ObjTypeList) {
    public fun getCharges(obj: InvObj?, varobj: UnpackedVarObjBitType): Int {
        return obj?.vars?.getBits(varobj.bits) ?: 0
    }

    /**
     * Adds up to [add] charges. The obj's configured variant determines whether it needs replacing,
     * including when a charged variant has zero stored charges.
     *
     * Once the target and capacity are validated, [takeMaterials] may consume materials for any
     * number of charges up to its argument. It returns the number actually paid for, or zero on
     * failure. It must leave the target slot untouched.
     */
    public fun addCharges(
        inventory: Inventory,
        slot: Int,
        add: Int,
        varobj: UnpackedVarObjBitType,
        max: Int,
        takeMaterials: (Int) -> Int = { it },
    ): Charge {
        require(max in 0..varobj.bits.bitMask) { "Invalid charge capacity $max for $varobj" }
        val obj = inventory[slot] ?: return Charge.Failure.ObjNotFound
        val current = getCharges(obj, varobj)
        if (current >= max) return Charge.Failure.AlreadyFullCharges
        if (add <= 0) return Charge.Failure.NoChargesAdded

        val type = objTypes[obj]
        val charged = type.paramOrNull(params.charged_variant)
        if (charged == null && !type.hasParam(params.uncharged_variant)) {
            return Charge.Failure.UnsupportedObj
        }
        val limit = minOf(add, max - current)
        val paid = takeMaterials(limit)
        require(paid in 0..limit) { "Paid for $paid charges, expected 0..$limit" }
        if (paid == 0) return Charge.Failure.NoChargesAdded

        val total = current + paid
        val vars = obj.vars.withBits(varobj.bits, total)
        if (charged != null) {
            inventory[slot] = InvObj(charged, obj.count, vars)
            return Charge.Success.AddChangeObj(paid, total, charged)
        }
        inventory[slot] = obj.copy(vars = vars)
        return Charge.Success.AddSameObj(paid, total)
    }

    /**
     * Reduces charges from the player's worn obj in the [wearpos] slot.
     *
     * If the obj's **current charges** are `0`, it will immediately be replaced with its uncharged
     * variant, returning [Uncharge.Failure.NotEnoughCharges]. If the remaining charge count **after
     * reduction** reaches `0`, the obj will also be replaced with its uncharged variant, but will
     * return [Uncharge.Success] instead.
     *
     * @return [Uncharge.Success] if exactly [decrement] charges were reduced; otherwise, a
     *   [Uncharge.Failure] indicating the reason for failure.
     * @throws IllegalStateException if the worn obj does not have a defined `uncharged_variant`
     *   param.
     */
    public fun reduceWornCharges(
        player: Player,
        wearpos: Wearpos,
        varobj: UnpackedVarObjBitType,
        decrement: Int,
    ): Uncharge {
        val obj = player.worn[wearpos.slot] ?: return Uncharge.Failure.ObjNotFound
        val type = objTypes[obj]

        // Always ensure that any obj used as a "charge" weapon has an uncharged variant defined.
        val uncharged = type.paramOrNull(params.uncharged_variant)
        if (uncharged == null) {
            val message = "Obj missing `uncharged_variant` param: $obj (type=$type)"
            throw IllegalStateException(message)
        }

        val currentCharges = obj.vars.getBits(varobj.bits)
        if (currentCharges < decrement) {
            if (currentCharges == 0) {
                player.worn[wearpos.slot] = InvObj(uncharged, vars = obj.vars)
            }
            return Uncharge.Failure.NotEnoughCharges
        }

        val decrementedCharges = currentCharges - decrement
        val decrementedPackedVars = obj.vars.withBits(varobj.bits, decrementedCharges)

        if (decrementedCharges == 0) {
            player.worn[wearpos.slot] = InvObj(uncharged, vars = decrementedPackedVars)
        } else {
            player.worn[wearpos.slot] = obj.copy(vars = decrementedPackedVars)
        }

        return Uncharge.Success(chargesLeft = decrementedCharges)
    }

    /**
     * Removes all charges from the obj in the given [slot] of the provided [inventory].
     *
     * The obj will always be replaced with its uncharged variant, regardless of the current charge
     * value. This requires the obj type to define an `uncharged_variant` param; otherwise, an
     * [IllegalStateException] is thrown.
     *
     * This function assumes that a valid obj exists in the given [slot].
     *
     * _Note: Only the [varobj] var is reset to `0`. All other varobj values on the obj will
     * persist._
     *
     * @return The number of charges the obj had before being reset. This may be `0` if the obj was
     *   already uncharged.
     * @throws IllegalStateException if the obj does not define an `uncharged_variant` param.
     * @throws NoSuchElementException if no obj exists in the given [inventory] slot.
     */
    public fun removeAllCharges(
        inventory: Inventory,
        slot: Int,
        varobj: UnpackedVarObjBitType,
    ): Int {
        val obj = inventory.getValue(slot) // Should not call this without a valid obj in `slot`.
        val type = objTypes[obj]

        val uncharged = type.paramOrNull(params.uncharged_variant)
        if (uncharged == null) {
            val message = "Obj missing `uncharged_variant` param: $obj (type=$type)"
            throw IllegalStateException(message)
        }

        val previousCharges = obj.vars.getBits(varobj.bits)

        val resetVarValue = obj.vars.withBits(varobj.bits, 0)
        inventory[slot] = InvObj(uncharged, vars = resetVarValue)

        return previousCharges
    }

    public sealed class Charge {
        public sealed class Success : Charge() {
            public abstract val added: Int
            public abstract val total: Int

            /** Indicates that charges were successfully added to an already charged obj. */
            public data class AddSameObj(override val added: Int, override val total: Int) :
                Success()

            /**
             * Indicates that charges were added to an "uncharged" variant of an obj, resulting in
             * the inventory obj being changed to the [charged] variant.
             */
            public data class AddChangeObj(
                override val added: Int,
                override val total: Int,
                public val charged: ObjType,
            ) : Success()
        }

        public sealed class Failure : Charge() {
            public data object ObjNotFound : Failure()

            public data object AlreadyFullCharges : Failure()

            public data object NoChargesAdded : Failure()

            public data object UnsupportedObj : Failure()
        }
    }

    public sealed class Uncharge {
        public data class Success(val chargesLeft: Int) : Uncharge() {
            public val fullyUncharged: Boolean
                get() = chargesLeft == 0
        }

        public sealed class Failure : Uncharge() {
            public data object ObjNotFound : Failure()

            public data object NotEnoughCharges : Failure()
        }
    }

    public companion object {
        // Important: These contracts assume the only valid `Charge` results are `Success` or
        // `Failure`. Do not introduce new base result types. Subclasses of `Success` and `Failure`
        // are fine.
        public fun Charge.isFailure(): Boolean {
            contract {
                returns(true) implies (this@isFailure is Charge.Failure)
                returns(false) implies (this@isFailure is Charge.Success)
            }
            return this is Charge.Failure
        }

        // Important: These contracts assume the only valid `Uncharge` results are `Success` or
        // `Failure`. Do not introduce new base result types. Subclasses of `Success` and `Failure`
        // are fine.
        public fun Uncharge.isFailure(): Boolean {
            contract {
                returns(true) implies (this@isFailure is Uncharge.Failure)
                returns(false) implies (this@isFailure is Uncharge.Success)
            }
            return this is Uncharge.Failure
        }
    }
}
