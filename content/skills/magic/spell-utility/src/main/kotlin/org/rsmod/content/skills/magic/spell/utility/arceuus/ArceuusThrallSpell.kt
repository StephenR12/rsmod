package org.rsmod.content.skills.magic.spell.utility.arceuus

import jakarta.inject.Inject
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.timers
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag

internal var Player.thrallCooldown by intVarBit(varbits.arceuus_resurrection_cooldown)
internal var Player.thrallUsed by intVarBit(varbits.arceuus_resurrection_used)
internal var Player.thrallActive by intVarBit(varbits.arceuus_resurrection_active)

public class ArceuusThrallSpell
@Inject
constructor(
    private val spells: MagicSpellRegistry,
    private val runes: MagicRuneManager,
    private val npcTypes: NpcTypeList,
    private val npcRepo: NpcRepository,
    private val collision: CollisionFlagMap,
    private val states: ThrallStateRepository,
) {
    public fun cast(access: ProtectedAccess, recipe: ThrallRecipe): Npc? {
        with(access) {
            if (!canAccess(recipe)) {
                return null
            }
            val spawn =
                findSpawn()
                    ?: run {
                        mes("There isn't enough room to resurrect a thrall here.")
                        return null
                    }
            val spell = requireNotNull(spells.getObjSpell(recipe.spell))
            val duration =
                stat(stats.magic) * if (vars[varbits.ca_tier_status_master] == 2) 2 else 1
            if (runes.attemptCast(player, spell).isFailure()) {
                return null
            }

            statSub(stats.prayer, constant = recipe.prayerCost, percent = 0)
            if (vars[varbits.fountain_of_rune_active] == 0) {
                statAdvance(stats.magic, spell.castXp)
            }

            states.remove(player)?.npc?.let { old ->
                if (old.isSlotAssigned) {
                    npcRepo.del(old, Int.MAX_VALUE)
                }
            }

            anim(ThrallSeqs.playerCast)
            spotanim(ThrallSpotanims.playerCast)
            val npc = Npc(npcTypes[recipe.npc], spawn)
            npc.anim(recipe.style.spawnSeq)
            npcRepo.add(npc, duration)
            npc.timer(timers.magic_arceuus_thrall_attack, FIRST_ACTION_DELAY)
            states.register(player, npc, recipe)

            player.thrallUsed = recipe.usedValue
            player.thrallActive = 1
            player.thrallCooldown = 1
            softTimer(timers.magic_arceuus_thrall_cooldown, RECAST_COOLDOWN_CYCLES)
            return npc
        }
    }

    private fun ProtectedAccess.canAccess(recipe: ThrallRecipe): Boolean {
        if (!player.members) {
            mes("You need to be on a members' world to cast this spell.")
            return false
        }
        if (vars[varbits.a_kingdom_divided] < A_KINGDOM_DIVIDED_COMPLETE) {
            mes("You need to complete A Kingdom Divided to cast this spell.")
            return false
        }
        if (
            !player.inv.contains(objs.book_of_the_dead) &&
                !player.worn.contains(objs.book_of_the_dead)
        ) {
            mes("You need a Book of the dead to cast this spell.")
            return false
        }
        if (player.thrallCooldown != 0) {
            return false
        }
        if (stat(stats.prayer) < recipe.prayerCost) {
            mes("You do not have enough Prayer points to cast this spell.")
            return false
        }
        return true
    }

    private fun ProtectedAccess.findSpawn(): org.rsmod.map.CoordGrid? {
        val center = player.coords
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
        return candidates.firstOrNull { square ->
            collision[square.x, square.z, square.level] and SPAWN_BLOCK_FLAGS == 0
        }
    }

    public companion object {
        public const val RECAST_COOLDOWN_CYCLES: Int = 17
        public const val FIRST_ACTION_DELAY: Int = 1
        private const val A_KINGDOM_DIVIDED_COMPLETE = 150
        private const val MAX_SPAWN_SEARCH_RADIUS = 3
        private const val SPAWN_BLOCK_FLAGS =
            CollisionFlag.LOC or
                CollisionFlag.GROUND_DECOR or
                CollisionFlag.BLOCK_WALK or
                CollisionFlag.BLOCK_NPCS
    }
}
