package org.rsmod.content.skills.woodcutting

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varobjs
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.type.obj.ObjType
import org.rsmod.utils.bits.withBits

/** Crystal pickaxe/harpoon recipes at the shared bowl and NPC singing menu. */
public abstract class CrystalGatheringToolSinging(
    public val dragon: ObjType,
    final override val item: ObjType,
    private val name: String,
) : CrystalSingingRecipe {
    final override suspend fun sing(access: ProtectedAccess, assisted: Boolean) =
        with(access) {
            if (!canSing(this, assisted, true)) return
            startDialogue {
                if (!assisted) anim(seqs.prif_crystal_singing)
                delay(CrystalAxeSinging.CREATION_TICKS)
                if (!assisted) resetAnim()
                if (!canSing(access, assisted, false)) return@startDialogue
                val shardCount =
                    if (assisted) CrystalAxeSinging.ASSISTED_SHARDS
                    else CrystalAxeSinging.PLAYER_SHARDS
                val result =
                    player.invTransaction(player.inv) {
                        val inv = select(player.inv)
                        delete(inv, dragon.id, 1)
                        delete(inv, objs.crystal_tool_seed.id, 1)
                        delete(inv, objs.crystal_shard.id, shardCount)
                        add(
                            inv,
                            item.id,
                            1,
                            vars =
                                0.withBits(
                                    varobjs.crystal_tool_charges.bits,
                                    CrystalAxeSinging.INITIAL_CHARGES,
                                ),
                        )
                    }
                if (!result.success) return@startDialogue
                if (!assisted) {
                    statAdvance(stats.crafting, CrystalAxeSinging.CRAFTING_XP)
                    statAdvance(stats.smithing, CrystalAxeSinging.SMITHING_XP)
                }
                mes("You create a crystal $name.")
            }
        }

    private fun canSing(access: ProtectedAccess, assisted: Boolean, message: Boolean): Boolean =
        with(access) {
            fun reject(text: String): Boolean {
                if (message) mes(text)
                return false
            }
            if (!player.members)
                return reject("You need to be on a members' world to sing crystal equipment.")
            if (
                player.vars[varbits.song_of_the_elves] <
                    CrystalAxeSinging.SONG_OF_THE_ELVES_COMPLETE
            )
                return reject("You need to complete Song of the Elves to sing crystal equipment.")
            if (!assisted && (stat(stats.crafting) < 76 || stat(stats.smithing) < 76))
                return reject("You need 76 Crafting and Smithing to sing a crystal $name.")
            val shards =
                if (assisted) CrystalAxeSinging.ASSISTED_SHARDS else CrystalAxeSinging.PLAYER_SHARDS
            if (
                dragon !in player.inv ||
                    objs.crystal_tool_seed !in player.inv ||
                    player.inv
                        .filterNotNull()
                        .filter { it.id == objs.crystal_shard.id }
                        .sumOf { it.count } < shards
            )
                return reject(
                    "You need a dragon $name, a crystal tool seed and $shards crystal shards."
                )
            true
        }
}

public class CrystalPickaxeSinging @Inject constructor() :
    CrystalGatheringToolSinging(objs.dragon_pickaxe, objs.crystal_pickaxe, "pickaxe")

public class CrystalHarpoonSinging @Inject constructor() :
    CrystalGatheringToolSinging(objs.dragon_harpoon, objs.crystal_harpoon, "harpoon")

public class CrystalFellingAxeSinging @Inject constructor() :
    CrystalGatheringToolSinging(objs.dragon_felling_axe, objs.crystal_felling_axe, "felling axe") {
    override fun available(player: org.rsmod.game.entity.Player, assisted:Boolean) = dragon in player.inv
}
