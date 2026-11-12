package org.rsmod.content.skills.magic.spell.utility.arceuus

import jakarta.inject.Inject
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.projanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.opPlayer2
import org.rsmod.api.player.feet
import org.rsmod.api.player.hat
import org.rsmod.api.player.legs
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.torso
import org.rsmod.api.random.CoreRandom
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.game.entity.Npc
import org.rsmod.game.inv.FreshDropOrigin
import org.rsmod.game.inv.isType
import org.rsmod.game.obj.Obj
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.proj.ProjAnimTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag

public class ArceuusReanimation
@Inject
constructor(
    private val spells: MagicSpellRegistry,
    private val runes: MagicRuneManager,
    private val npcTypes: NpcTypeList,
    private val npcRepo: NpcRepository,
    private val objRepo: ObjRepository,
    private val worldRepo: WorldRepository,
    private val projAnimTypes: ProjAnimTypeList,
    private val aiPlayerInteractions: AiPlayerInteractions,
    private val collision: CollisionFlagMap,
    private val states: ReanimationStateRepository,
    @CoreRandom private val random: GameRandom,
) {
    public fun castInventory(access: ProtectedAccess, spellObj: ObjType, targetSlot: Int): Npc? {
        with(access) {
            val target = player.inv[targetSlot] ?: return null
            val recipe = ReanimationRecipes.byHeadId[target.id] ?: return null
            val inFreshArea =
                target.isType(recipe.initialHead) &&
                    FreshDropOrigin.contains(
                        target.vars,
                        player.coords,
                        radius = FRESH_DROP_AREA_RADIUS,
                    )
            if (!inFreshArea && !isNearDarkAltar()) {
                mes(TOO_FAR_MESSAGE)
                return null
            }
            val spawn = findSpawn(player.coords, npcTypes[recipe.npc].size) ?: return noRoom()
            return cast(
                access = this,
                spellObj = spellObj,
                recipe = recipe,
                targetType = target.id,
                spawn = spawn,
                consume = {
                    invDel(
                            player.inv,
                            ObjTypeResolver.resolve(recipe, target.id),
                            slot = targetSlot,
                        )
                        .success
                },
            )
        }
    }

    public fun castGround(access: ProtectedAccess, spellObj: ObjType, target: Obj): Npc? {
        with(access) {
            val recipe = ReanimationRecipes.byHeadId[target.type] ?: return null
            val inFreshArea =
                target.type == recipe.initialHead.id &&
                    FreshDropOrigin.contains(
                        target.vars,
                        player.coords,
                        radius = FRESH_DROP_AREA_RADIUS,
                    )
            if (!inFreshArea && !isNearDarkAltar()) {
                mes(TOO_FAR_MESSAGE)
                return null
            }
            if (target.type == ReanimationObjs.vorkathHead.id && !isNearDarkAltar()) {
                mes(TOO_FAR_MESSAGE)
                return null
            }
            val spawn = findSpawn(target.coords, npcTypes[recipe.npc].size) ?: return noRoom()
            return cast(
                access = this,
                spellObj = spellObj,
                recipe = recipe,
                targetType = target.type,
                spawn = spawn,
                consume = { objRepo.del(target) },
            )
        }
    }

    private fun cast(
        access: ProtectedAccess,
        spellObj: ObjType,
        recipe: ReanimationRecipe,
        targetType: Int,
        spawn: CoordGrid,
        consume: () -> Boolean,
    ): Npc? {
        with(access) {
            if (!player.members) {
                mes("You need to be on a members' world to cast this spell.")
                return null
            }
            if (states.hasActive(player)) {
                mes("You should finish off your last one first.")
                return null
            }
            if (!states.canCast(player)) {
                return null
            }
            if (recipe.spell.id != spellObj.id) {
                return null
            }

            val spell = requireNotNull(spells.getObjSpell(spellObj))
            if (runes.attemptCast(player, spell).isFailure()) {
                return null
            }

            val preserved = zealotPreservesHead(player)
            if (!preserved) {
                check(consume()) { "Validated reanimation head could not be consumed: $targetType" }
            } else {
                mes("Your Zealot's robes save the ensouled head but still grant you experience.")
            }

            if (vars[varbits.fountain_of_rune_active] == 0) {
                statAdvance(stats.magic, spell.castXp)
            }
            faceSquare(spawn)
            anim(ReanimationSeqs.player)
            spotanim(ReanimationSpotanims.player)
            worldRepo.projAnim(
                player,
                spawn,
                ReanimationSpotanims.projectile,
                projAnimTypes[projanims.magic_spell_low],
            )
            worldRepo.spotanimMap(ReanimationSpotanims.spawning, spawn)

            val npc = Npc(npcTypes[recipe.npc], spawn)
            npc.anim(ReanimationSeqs.creature)
            npcRepo.add(npc, REANIMATED_LIFETIME_CYCLES)
            states.register(player, npc, recipe.prayerXp)
            states.setCastDelay(player, CAST_RATE)

            npc.actionDelay = player.currentMapClock + SPAWN_ANIMATION_CYCLES
            npc.opPlayer2(player, aiPlayerInteractions)
            return npc
        }
    }

    private fun zealotPreservesHead(player: org.rsmod.game.entity.Player): Boolean {
        val pieces = zealotPieceCount(player)
        return pieces > 0 && random.of(maxExclusive = ZEALOT_ROLL_DENOMINATOR) < pieces
    }

    private fun zealotPieceCount(player: org.rsmod.game.entity.Player): Int =
        listOf(
                player.hat?.id == objs.zealots_helm.id,
                player.torso?.id == objs.zealots_robe_top.id,
                player.legs?.id == objs.zealots_robe_bottom.id,
                player.feet?.id == objs.zealots_boots.id,
            )
            .count { it }

    private fun ProtectedAccess.isNearDarkAltar(): Boolean =
        player.coords.level == DARK_ALTAR.level &&
            player.coords.chebyshevDistance(DARK_ALTAR) <= DARK_ALTAR_RADIUS

    private fun ProtectedAccess.noRoom(): Npc? {
        mes("The creature wouldn't have room to re-animate there.")
        return null
    }

    private fun findSpawn(center: CoordGrid, size: Int): CoordGrid? {
        val candidates = sequence {
            yield(center)
            for (radius in 1..MAX_SPAWN_SEARCH_RADIUS) {
                for (x in -radius..radius) {
                    yield(center.translate(x, -radius))
                    yield(center.translate(x, radius))
                }
                for (z in (-radius + 1) until radius) {
                    yield(center.translate(-radius, z))
                    yield(center.translate(radius, z))
                }
            }
        }
        return candidates.firstOrNull { isOpen(it, size) }
    }

    private fun isOpen(southWest: CoordGrid, size: Int): Boolean {
        for (x in southWest.x until southWest.x + size) {
            for (z in southWest.z until southWest.z + size) {
                val flags = collision[x, z, southWest.level]
                if (flags and SPAWN_BLOCK_FLAGS != 0) {
                    return false
                }
            }
        }
        return true
    }

    public companion object {
        public const val CAST_RATE: Int = 5
        public const val SPAWN_ANIMATION_CYCLES: Int = 3
        public const val REANIMATED_LIFETIME_CYCLES: Int = 500
        public const val DARK_ALTAR_RADIUS: Int = 15
        /** A 31-by-31 square centered on the monster-drop origin. */
        public const val FRESH_DROP_AREA_RADIUS: Int = 15
        public val DARK_ALTAR: CoordGrid = CoordGrid(0, 26, 60, 36, 42)
        public const val TOO_FAR_MESSAGE: String =
            "That creature cannot be reanimated so far from where it died, but the power of the crystals by the dark altar will increase the potency of the spell."

        private const val ZEALOT_ROLL_DENOMINATOR = 80
        // The reanimated dragon is five tiles wide, so the search must extend far enough for its
        // footprint to clear the caster's occupied tile.
        private const val MAX_SPAWN_SEARCH_RADIUS = 8
        private const val SPAWN_BLOCK_FLAGS =
            CollisionFlag.LOC or
                CollisionFlag.GROUND_DECOR or
                CollisionFlag.BLOCK_WALK or
                CollisionFlag.BLOCK_NPCS
    }
}

private object ObjTypeResolver {
    fun resolve(recipe: ReanimationRecipe, targetId: Int): ObjType =
        when (targetId) {
            recipe.initialHead.id -> recipe.initialHead
            recipe.processedHead.id -> recipe.processedHead
            ReanimationObjs.vorkathHead.id -> ReanimationObjs.vorkathHead
            else -> error("Unknown reanimation target: $targetId")
        }
}
