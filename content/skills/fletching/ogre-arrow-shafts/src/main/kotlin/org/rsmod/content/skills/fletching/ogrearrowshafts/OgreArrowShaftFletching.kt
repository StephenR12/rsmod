package org.rsmod.content.skills.fletching.ogrearrowshafts

import jakarta.inject.Inject
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.api.random.GameRandom

public class OgreArrowShaftFletching @Inject constructor(private val random: GameRandom) {
    public suspend fun select(access: ProtectedAccess) {
        with(access) {
            if (!canOpen(sendFailure = true)) {
                return
            }
            val input =
                skillMulti(
                    title = MENU_TITLE,
                    choices = listOf(objs.ogre_arrow_shaft, objs.unstrung_comp_ogre_bow),
                    maxQuantity = 1,
                    initialQuantity = 1,
                    actionMode = MAKE_ACTION_MODE,
                )
            val product =
                when {
                    components.skillmulti_a.isType(input.component) -> OgreLogProduct.ArrowShafts
                    components.skillmulti_b.isType(input.component) -> OgreLogProduct.CompositeBow
                    else -> return
                }
            start(this, product)
        }
    }

    public fun start(access: ProtectedAccess, product: OgreLogProduct) {
        with(access) {
            if (!canFletch(product, sendFailure = true)) {
                return
            }
            clearFletchingQueues()
            anim(seqs.human_ogre_fletching)
            weakQueue(
                fletching_queues.ogreArrowShafts,
                ACTION_TICKS,
                OgreLogFletchingRequest(product),
            )
        }
    }

    public fun process(access: ProtectedAccess, request: OgreLogFletchingRequest) {
        with(access) {
            if (!canFletch(request.product, sendFailure = false)) {
                resetAnim()
                return
            }
            val output = output(request.product)
            val outputCount =
                if (request.product == OgreLogProduct.ArrowShafts) {
                    random.of(MIN_SHAFT_OUTPUT..MAX_SHAFT_OUTPUT)
                } else {
                    1
                }
            val result =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, objs.achey_tree_logs.id, 1)
                    if (request.product == OgreLogProduct.CompositeBow) {
                        delete(inventory, objs.wolf_bones.id, 1)
                    }
                    add(inventory, output.id, outputCount)
                }
            check(result.success) {
                "Ogre-log Fletching transaction failed after validation: " +
                    "product=${request.product}, output=$outputCount"
            }
            val xp =
                when (request.product) {
                    OgreLogProduct.ArrowShafts -> outputCount * XP_PER_SHAFT
                    OgreLogProduct.CompositeBow -> COMPOSITE_BOW_XP
                }
            statAdvance(stats.fletching, xp)
            resetAnim()
        }
    }

    private fun ProtectedAccess.canOpen(sendFailure: Boolean): Boolean {
        fun reject(message: String): Boolean {
            if (sendFailure) {
                mes(message)
            }
            return false
        }
        if (!player.members) {
            return reject("You need to be on a members' world to fletch Achey tree logs.")
        }
        if (!hasInventoryKnife()) {
            return reject("You need a knife to fletch Achey tree logs.")
        }
        if (!player.inv.contains(objs.achey_tree_logs)) {
            return reject("You do not have any Achey tree logs left to fletch.")
        }
        return true
    }

    private fun ProtectedAccess.canFletch(product: OgreLogProduct, sendFailure: Boolean): Boolean {
        fun reject(message: String): Boolean {
            if (sendFailure) {
                mes(message)
            }
            return false
        }
        if (!canOpen(sendFailure)) {
            return false
        }
        when (product) {
            OgreLogProduct.ArrowShafts -> {
                if (player.vars[varps.big_chompy_bird_hunting] == BIG_CHOMPY_UNSTARTED_STATE) {
                    return reject("You need to start Big Chompy Bird Hunting first.")
                }
                if (player.stat(stats.fletching) < SHAFT_LEVEL) {
                    return reject(
                        "You need a Fletching level of $SHAFT_LEVEL to make these shafts."
                    )
                }
            }
            OgreLogProduct.CompositeBow -> {
                if (player.vars[varbits.comp_ogre_bow_unlocked] == 0) {
                    return reject("You need to learn how to make a composite ogre bow first.")
                }
                if (player.stat(stats.fletching) < COMPOSITE_BOW_LEVEL) {
                    return reject(
                        "You need a Fletching level of $COMPOSITE_BOW_LEVEL to make this bow."
                    )
                }
                if (!player.inv.contains(objs.wolf_bones)) {
                    return reject("You need some wolf bones to make a composite ogre bow.")
                }
            }
        }
        return true
    }

    private fun output(product: OgreLogProduct) =
        when (product) {
            OgreLogProduct.ArrowShafts -> objs.ogre_arrow_shaft
            OgreLogProduct.CompositeBow -> objs.unstrung_comp_ogre_bow
        }

    private fun ProtectedAccess.hasInventoryKnife(): Boolean =
        player.inv.contains(objs.knife) || player.inv.contains(objs.fletching_knife)

    private companion object {
        const val SHAFT_LEVEL = 5
        const val COMPOSITE_BOW_LEVEL = 30
        const val MIN_SHAFT_OUTPUT = 2
        const val MAX_SHAFT_OUTPUT = 6
        const val XP_PER_SHAFT = 1.6
        const val COMPOSITE_BOW_XP = 45.0
        const val ACTION_TICKS = 3
        const val BIG_CHOMPY_UNSTARTED_STATE = 0
        const val MAKE_ACTION_MODE = 0
        const val MENU_TITLE = "What would you like to make?"
    }
}

public enum class OgreLogProduct {
    ArrowShafts,
    CompositeBow,
}

public data class OgreLogFletchingRequest(val product: OgreLogProduct)
