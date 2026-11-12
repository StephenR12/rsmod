package org.rsmod.content.bosses.barrows

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.IdentityHashMap
import org.rsmod.api.config.refs.objs
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.random.GameRandom
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.type.obj.ObjType

@Singleton
public class BarrowsController @Inject constructor(private val random: GameRandom) {
    private val defeated = IdentityHashMap<Player, MutableSet<Int>>()

    public fun onKill(player: Player, npc: Npc) {
        if (npc.id in BarrowsBrothers.byId) {
            defeated.getOrPut(player, ::mutableSetOf).add(npc.id)
        }
    }

    public fun defeatedCount(player: Player): Int = defeated[player]?.size ?: 0

    public fun claim(access: ProtectedAccess) {
        with(access) {
            val kills = defeated[player].orEmpty()
            if (kills.isEmpty()) {
                mes("The chest is empty. Defeat at least one Barrows brother first.")
                return
            }
            val eligible =
                kills.flatMap { id -> BarrowsBrothers.byId.getValue(id).equipment }.toMutableList()
            val granted = mutableSetOf<ObjType>()
            val denominator = 450 - 58 * kills.size
            repeat(kills.size + 1) {
                val unique = eligible.isNotEmpty() && random.of(denominator) == 0
                if (unique) {
                    val item = eligible.removeAt(random.of(eligible.size))
                    if (granted.add(item)) {
                        invAdd(inv, item)
                    }
                } else {
                    val rune =
                        when (random.of(4)) {
                            0 -> objs.mind_rune
                            1 -> objs.chaos_rune
                            2 -> objs.death_rune
                            else -> objs.blood_rune
                        }
                    invAdd(inv, rune, random.of(15..35), strict = false)
                }
            }
            defeated.remove(player)
            mes("You search the chest and claim your Barrows reward.")
        }
    }
}
