package org.rsmod.api.player.worn

import org.rsmod.api.config.refs.objs
import org.rsmod.api.player.back
import org.rsmod.api.player.righthand
import org.rsmod.game.entity.Player
import org.rsmod.game.type.obj.UnpackedObjType
import org.rsmod.game.type.obj.Wearpos

internal object GodStaffEquipment {
    private val staves by lazy {
        listOf(objs.saradomin_staff.id, objs.guthix_staff.id, objs.zamorak_staff.id)
    }
    private val capes by lazy {
        listOf(
            setOf(
                objs.saradomin_cape.id,
                objs.imbued_saradomin_cape.id,
                objs.imbued_saradomin_cape_trouver.id,
                objs.saradomin_max_cape.id,
                objs.imbued_saradomin_max_cape.id,
                objs.imbued_saradomin_max_cape_trouver.id,
            ),
            setOf(
                objs.guthix_cape.id,
                objs.imbued_guthix_cape.id,
                objs.imbued_guthix_cape_trouver.id,
                objs.guthix_max_cape.id,
                objs.imbued_guthix_max_cape.id,
                objs.imbued_guthix_max_cape_trouver.id,
            ),
            setOf(
                objs.zamorak_cape.id,
                objs.imbued_zamorak_cape.id,
                objs.imbued_zamorak_cape_trouver.id,
                objs.zamorak_max_cape.id,
                objs.imbued_zamorak_max_cape.id,
                objs.imbued_zamorak_max_cape_trouver.id,
            ),
        )
    }

    fun conflicts(player: Player, item: UnpackedObjType): Boolean {
        val hand: Int?
        val back: Int?
        when (item.wearpos1) {
            Wearpos.RightHand.slot -> {
                hand = item.id
                back = player.back?.id
            }
            Wearpos.Back.slot -> {
                back = item.id
                hand = player.righthand?.id
            }
            else -> return false
        }
        val staffGod = staves.indexOf(hand)
        if (staffGod < 0 || back == null) return false
        val capeGod = capes.indexOfFirst { back in it }
        return capeGod >= 0 && staffGod != capeGod
    }
}
