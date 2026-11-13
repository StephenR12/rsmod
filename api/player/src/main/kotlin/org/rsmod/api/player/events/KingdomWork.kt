package org.rsmod.api.player.events

import org.rsmod.api.config.refs.objs
import org.rsmod.api.player.output.mes
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player
import org.rsmod.game.type.obj.ObjType
import org.rsmod.map.CoordGrid

public enum class KingdomWork(public val chance: Int, private val etceteria: Boolean = false) {
    Coal(100),
    Maple(50),
    Fish(50),
    Hardwood(100, true),
    Gardens(100),
    EasternGardens(100, true),
    Flowers(50);

    public fun permitted(player: Player, explain: Boolean = true): Boolean {
        val reason =
            when {
                !player.members || player.vars[KingdomVars.throne] < 90 ->
                    "You must first have permission to help in the kingdom."
                etceteria && player.vars[KingdomBits.royal] < 30 ->
                    "The people of Etceteria are not yet working for you."
                player.vars[KingdomBits.approval] >= 127 ->
                    "Your subjects already give you their full support."
                else -> return true
            }
        if (explain) player.mes(reason)
        return false
    }

    public companion object {
        public fun tree(spot: CoordGrid, product: ObjType): KingdomWork? =
            when {
                spot.level != 0 -> null
                product.id == objs.maple_logs.id && spot.x in 2539..2557 && spot.z in 3859..3876 ->
                    Maple
                (product.id == objs.teak_logs.id || product.id == objs.mahogany_logs.id) &&
                    spot.x in 2592..2615 &&
                    spot.z in 3886..3897 -> Hardwood
                else -> null
            }

        public fun mine(spot: CoordGrid, product: ObjType): KingdomWork? =
            if (
                spot.level == 0 &&
                    spot.x in 2525..2530 &&
                    spot.z in 3889..3896 &&
                    product.id == objs.coal.id
            )
                Coal
            else null

        public fun fish(spot: CoordGrid): KingdomWork? =
            if (spot.level == 0 && spot.x in 2571..2583 && spot.z in 3850..3861) Fish else null
    }
}

internal object KingdomBits : VarBitReferences() {
    public val approval = find("misc_approval")
    public val royal = find("royal_quest")
}

internal object KingdomVars : VarpReferences() {
    public val throne = find("misc_quest")
}

public data class KingdomContribution(public val player: Player, public val work: KingdomWork) :
    UnboundEvent
