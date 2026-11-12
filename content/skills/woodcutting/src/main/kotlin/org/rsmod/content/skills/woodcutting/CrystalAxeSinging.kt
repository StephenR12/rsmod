package org.rsmod.content.skills.woodcutting

import jakarta.inject.Inject
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.type.obj.ObjTypeList

/** The crystal-axe recipe shared by the Prifddinas singing bowl and elven singers. */
public class CrystalAxeSinging @Inject constructor(private val objTypes: ObjTypeList) {
    public suspend fun selectAtBowl(access: ProtectedAccess) {
        with(access) {
            if (!canAccess(sendFailure = true)) {
                return
            }
            val input =
                skillMulti(
                    title = MENU_TITLE,
                    choices = listOf(objs.crystal_axe),
                    maxQuantity = 1,
                    initialQuantity = 1,
                    actionMode = MAKE_ACTION_MODE,
                )
            if (!components.skillmulti_a.isType(input.component)) {
                return
            }
            create(this, assisted = false)
        }
    }

    public suspend fun selectWithSinger(access: ProtectedAccess) {
        with(access) {
            if (!canAccess(sendFailure = true)) {
                return
            }
            val input =
                skillMulti(
                    title = MENU_TITLE,
                    choices = listOf(objs.crystal_axe),
                    maxQuantity = 1,
                    initialQuantity = 1,
                    actionMode = MAKE_ACTION_MODE,
                )
            if (!components.skillmulti_a.isType(input.component)) {
                return
            }
            create(this, assisted = true)
        }
    }

    public suspend fun create(access: ProtectedAccess, assisted: Boolean) {
        with(access) {
            if (!canCreate(assisted, sendFailure = true)) {
                return
            }
            startDialogue {
                if (!assisted) {
                    anim(seqs.prif_crystal_singing)
                }
                delay(CREATION_TICKS)
                if (!assisted) {
                    resetAnim()
                }
                if (!canCreate(assisted, sendFailure = false)) {
                    return@startDialogue
                }
                val quantity = available(assisted)
                check(quantity > 0) { "Crystal-axe singing reached mutation without materials." }
                val shards = if (assisted) ASSISTED_SHARDS else PLAYER_SHARDS
                val result =
                    player.invTransaction(player.inv) {
                        val inventory = select(player.inv)
                        delete(inventory, objs.dragon_axe.id, quantity)
                        delete(inventory, objs.crystal_tool_seed.id, quantity)
                        delete(inventory, objs.crystal_shard.id, quantity * shards)
                        add(
                            inventory,
                            objs.crystal_axe.id,
                            quantity,
                            vars = CrystalAxeState(INITIAL_CHARGES).pack(),
                        )
                    }
                check(result.success) { "Crystal-axe singing failed after validation." }
                if (!assisted) {
                    statAdvance(stats.crafting, CRAFTING_XP * quantity)
                    statAdvance(stats.smithing, SMITHING_XP * quantity)
                }
            }
        }
    }

    private fun ProtectedAccess.canAccess(sendFailure: Boolean): Boolean {
        fun reject(message: String): Boolean {
            if (sendFailure) {
                mes(message)
            }
            return false
        }
        if (!player.members) {
            return reject("You need to be on a members' world to sing crystal equipment.")
        }
        if (player.vars[varbits.song_of_the_elves] < SONG_OF_THE_ELVES_COMPLETE) {
            return reject("You need to complete Song of the Elves to sing crystal equipment.")
        }
        return true
    }

    private fun ProtectedAccess.canCreate(assisted: Boolean, sendFailure: Boolean): Boolean {
        fun reject(message: String): Boolean {
            if (sendFailure) {
                mes(message)
            }
            return false
        }
        if (!canAccess(sendFailure)) {
            return false
        }
        if (!assisted && stat(stats.crafting) < CRAFTING_LEVEL) {
            return reject("You need a Crafting level of $CRAFTING_LEVEL to sing a crystal axe.")
        }
        if (!assisted && stat(stats.smithing) < SMITHING_LEVEL) {
            return reject("You need a Smithing level of $SMITHING_LEVEL to sing a crystal axe.")
        }
        if (player.inv.count(objTypes[objs.dragon_axe]) == 0) {
            return reject("You need a dragon axe to sing a crystal axe.")
        }
        if (player.inv.count(objTypes[objs.crystal_tool_seed]) == 0) {
            return reject("You need a crystal tool seed to sing a crystal axe.")
        }
        val shards = if (assisted) ASSISTED_SHARDS else PLAYER_SHARDS
        if (player.inv.count(objTypes[objs.crystal_shard]) < shards) {
            return reject("You need $shards crystal shards to sing a crystal axe.")
        }
        return true
    }

    private fun ProtectedAccess.available(assisted: Boolean): Int {
        val shards = if (assisted) ASSISTED_SHARDS else PLAYER_SHARDS
        return minOf(
            player.inv.count(objTypes[objs.dragon_axe]),
            player.inv.count(objTypes[objs.crystal_tool_seed]),
            player.inv.count(objTypes[objs.crystal_shard]) / shards,
        )
    }

    public companion object {
        public const val CRAFTING_LEVEL: Int = 76
        public const val SMITHING_LEVEL: Int = 76
        public const val PLAYER_SHARDS: Int = 120
        public const val ASSISTED_SHARDS: Int = 180
        public const val INITIAL_CHARGES: Int = 10_000
        public const val CRAFTING_XP: Double = 6_000.0
        public const val SMITHING_XP: Double = 6_000.0
        public const val CREATION_TICKS: Int = 3
        public const val SONG_OF_THE_ELVES_COMPLETE: Int = 194
        private const val MENU_TITLE: String = "What would you like to sing?"
        private const val MAKE_ACTION_MODE: Int = 0
    }
}
