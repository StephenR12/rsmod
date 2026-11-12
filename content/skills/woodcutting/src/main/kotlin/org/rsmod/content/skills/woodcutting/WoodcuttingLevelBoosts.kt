package org.rsmod.content.skills.woodcutting

import jakarta.inject.Inject
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.config.refs.areas
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.isValidTarget
import org.rsmod.api.repo.player.PlayerRepository
import org.rsmod.api.route.BoundValidator
import org.rsmod.api.stats.levelmod.InvisibleLevelMod
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.InteractionLocOp
import org.rsmod.game.interact.InteractionOp
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.loc.UnpackedLocType
import org.rsmod.map.zone.ZoneKey

class WoodcuttingLevelBoosts
@Inject
constructor(
    private val areaChecker: AreaChecker,
    private val locTypes: LocTypeList,
    private val playerRepo: PlayerRepository,
    private val boundValidator: BoundValidator,
) : InvisibleLevelMod(stats.woodcutting) {
    override fun Player.calculateBoost(): Int {
        return if (areaChecker.inArea(areas.woodcutting_guild, coords)) {
            WOODCUTTING_GUILD_BOOST
        } else {
            groupBoost()
        }
    }

    private fun Player.groupBoost(): Int {
        val tree = activeTree() ?: return 0
        if (!locTypes[tree].hasDespawnTimer) {
            return 0
        }
        return playerRepo
            .findAll(ZoneKey.from(tree.coords), zoneRadius = 1)
            .filter { it.isActivelyChopping(tree) }
            .take(MAX_GROUP_BOOST)
            .count()
    }

    private fun Player.activeTree(): BoundLocInfo? {
        val interaction = interaction as? InteractionLocOp ?: return null
        return interaction.target.takeIf { interaction.op == InteractionOp.Op3 }
    }

    private fun Player.isActivelyChopping(tree: BoundLocInfo): Boolean =
        isValidTarget() && isInOpRange(tree) && activeTree() == tree

    private fun Player.isInOpRange(tree: BoundLocInfo): Boolean =
        boundValidator.collides(source = avatar, target = tree) ||
            boundValidator.touches(source = avatar, target = tree)

    companion object {
        const val WOODCUTTING_GUILD_BOOST: Int = 7
        const val MAX_GROUP_BOOST: Int = 10

        private val UnpackedLocType.hasDespawnTimer: Boolean
            get() = hasParam(params.despawn_time)
    }
}
