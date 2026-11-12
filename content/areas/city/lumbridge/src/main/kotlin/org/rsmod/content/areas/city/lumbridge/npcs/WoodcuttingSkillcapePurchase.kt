package org.rsmod.content.areas.city.lumbridge.npcs

import jakarta.inject.Inject
import org.rsmod.api.config.refs.jingles
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statBase
import org.rsmod.game.type.obj.ObjTypeList

/** Atomic cache-233 Woodsman tutor Skillcape sale. */
class WoodcuttingSkillcapePurchase @Inject constructor(private val objTypes: ObjTypeList) {
    fun purchase(access: ProtectedAccess): WoodcuttingSkillcapePurchaseResult {
        with(access) {
            if (!player.members) {
                return WoodcuttingSkillcapePurchaseResult.MembersOnly
            }
            if (player.statBase(stats.woodcutting) < REQUIRED_LEVEL) {
                return WoodcuttingSkillcapePurchaseResult.NotEligible
            }
            if (player.inv.count(objTypes[objs.coins]) < PRICE) {
                return WoodcuttingSkillcapePurchaseResult.InsufficientCoins
            }

            val cape =
                if (OTHER_SKILLS.any { player.statBase(it) >= REQUIRED_LEVEL }) {
                    objs.woodcutting_skillcape_t
                } else {
                    objs.woodcutting_skillcape
                }
            val preflight =
                player.invTransaction(player.inv, autoCommit = false) {
                    val inventory = select(player.inv)
                    delete(inventory, objs.coins.id, PRICE)
                    add(inventory, cape.id, 1)
                    add(inventory, objs.woodcutting_hood.id, 1)
                }
            if (!preflight.success) {
                return WoodcuttingSkillcapePurchaseResult.NoInventorySpace
            }
            val committed =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, objs.coins.id, PRICE)
                    add(inventory, cape.id, 1)
                    add(inventory, objs.woodcutting_hood.id, 1)
                }
            check(committed.success) {
                "Woodcutting Skillcape transaction failed after validation."
            }
            midiJingle(jingles.cape_of_accomplishment)
            return WoodcuttingSkillcapePurchaseResult.Purchased
        }
    }

    companion object {
        const val REQUIRED_LEVEL: Int = 99
        const val PRICE: Int = 99_000

        private val OTHER_SKILLS =
            listOf(
                stats.attack,
                stats.defence,
                stats.strength,
                stats.hitpoints,
                stats.ranged,
                stats.prayer,
                stats.magic,
                stats.cooking,
                stats.fletching,
                stats.fishing,
                stats.firemaking,
                stats.crafting,
                stats.smithing,
                stats.mining,
                stats.herblore,
                stats.agility,
                stats.thieving,
                stats.slayer,
                stats.farming,
                stats.runecrafting,
                stats.hunter,
                stats.construction,
            )
    }
}

enum class WoodcuttingSkillcapePurchaseResult {
    Purchased,
    MembersOnly,
    NotEligible,
    InsufficientCoins,
    NoInventorySpace,
}
