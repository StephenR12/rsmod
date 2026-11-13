package org.rsmod.content.skills.skillcapes

import org.rsmod.api.config.refs.jingles
import org.rsmod.api.config.refs.objs
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statBase
import org.rsmod.game.entity.Player
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.stat.StatType

public object SkillcapeSale {
    public fun isTrimmed(player: Player): Boolean =
        SkillcapeTrimming.definitions.count { player.statBase(it.stat) >= it.stat.maxLevel } > 1

    public suspend fun offer(access: ProtectedAccess, stat: StatType, hood: ObjType): Unit =
        with(access) {
            if (!player.members || player.statBase(stat) < stat.maxLevel) {
                mes("You must master this skill before you can buy its cape.")
                return
            }
            if (!choice2("Buy the cape and hood for 99,000 coins.", true, "No thanks.", false))
                return
            purchase(access, stat, hood)
        }

    public fun purchase(access: ProtectedAccess, stat: StatType, hood: ObjType): Boolean =
        with(access) {
            if (!player.members || player.statBase(stat) < stat.maxLevel) return false
            val definition = SkillcapeTrimming.definitions.first { it.stat.id == stat.id }
            val cape = if (isTrimmed(player)) definition.trimmed else definition.untrimmed
            val result =
                player.invTransaction(inv) {
                    val bag = select(inv)
                    delete(bag, objs.coins.id, 99_000)
                    add(bag, cape.id, 1)
                    add(bag, hood.id, 1)
                }
            if (result.failure) {
                mes("You'll need 99,000 coins and room for the cape and hood.")
                return false
            }
            midiJingle(jingles.cape_of_accomplishment)
            true
        }
}
