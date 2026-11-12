package org.rsmod.content.skills.hunter.experts

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

public class HunterExpertSkillcapePurchase @Inject constructor(private val objTypes: ObjTypeList) {
    public fun purchase(access: ProtectedAccess): HunterSkillcapePurchaseResult {
        with(access) {
            if (player.statBase(stats.hunter) < HUNTER_LEVEL) {
                return HunterSkillcapePurchaseResult.NotEligible
            }
            if (player.inv.count(objTypes[objs.coins]) < PRICE) {
                return HunterSkillcapePurchaseResult.InsufficientCoins
            }

            val cape =
                if (OTHER_SKILLS.any { player.statBase(it) >= HUNTER_LEVEL }) {
                    objs.hunter_skillcape_t
                } else {
                    objs.hunter_skillcape
                }
            val preflight =
                player.invTransaction(player.inv, autoCommit = false) {
                    val inventory = select(player.inv)
                    delete(inventory, objs.coins.id, PRICE)
                    add(inventory, cape.id, 1)
                    add(inventory, objs.hunter_hood.id, 1)
                }
            if (!preflight.success) {
                return HunterSkillcapePurchaseResult.NoInventorySpace
            }
            val committed =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, objs.coins.id, PRICE)
                    add(inventory, cape.id, 1)
                    add(inventory, objs.hunter_hood.id, 1)
                }
            check(committed.success) { "Hunter Skillcape transaction failed after validation." }
            midiJingle(jingles.cape_of_accomplishment)
            return HunterSkillcapePurchaseResult.Purchased
        }
    }

    public companion object {
        public const val HUNTER_LEVEL: Int = 99
        public const val PRICE: Int = 99_000

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
                stats.woodcutting,
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
                stats.construction,
            )
    }
}

public enum class HunterSkillcapePurchaseResult {
    Purchased,
    NotEligible,
    InsufficientCoins,
    NoInventorySpace,
}
