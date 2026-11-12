package org.rsmod.content.skills.hunter.moths

import jakarta.inject.Inject
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.npc.isValidTarget
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.player.stat.stat
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

/** Exact-evidence cache-233 core for catchable sunlight and moonlight moth variants. */
public class MothHunting
@Inject
constructor(private val npcRepo: NpcRepository, private val random: GameRandom) {
    public suspend fun catch(access: ProtectedAccess, npc: Npc) {
        val species = MothSpeciesList.byNpcId[npc.id] ?: return
        if (!access.player.members) {
            access.mes("You need to be on a members' world to catch moths.")
            return
        }
        val level = access.player.stat(stats.hunter)
        if (level < species.level) {
            access.mes("You need a Hunter level of ${species.level} to catch this moth.")
            return
        }
        val method = method(access.player, species)
        if (method == null) {
            access.mes("You need to wield a butterfly net to catch this moth.")
            return
        }
        with(access) {
            faceEntitySquare(npc)
            anim(MothSeqs.catch)
            delay(CATCH_TICKS)
            if (
                !npc.isValidTarget() ||
                    !isWithinDistance(npc, distance = 1) ||
                    method(player, species) != method
            ) {
                resetAnim()
                return
            }
            if (random.randomDouble() >= catchChance(player, species, method)) {
                return
            }

            val jarred =
                method != MothCatchMethod.Barehanded && player.inv.contains(MothObjs.emptyJar)
            if (jarred) {
                val result =
                    player.invTransaction(player.inv) {
                        val inventory = select(player.inv)
                        delete(inventory, MothObjs.emptyJar.id, 1)
                        add(inventory, species.jar.id, 1)
                    }
                if (!result.success) {
                    return
                }
            }

            statAdvance(stats.hunter, species.xp)
            if (!jarred) {
                MothEffects.apply(this, species.effect, directCatch = true)
            }
            npcRepo.despawn(npc, npc.type.respawnRate)
        }
    }

    public fun release(access: ProtectedAccess, slot: Int) {
        val obj = access.player.inv[slot] ?: return
        val species = MothSpeciesList.byJarId[obj.id] ?: return
        if (!access.player.members) {
            access.mes("You need to be on a members' world to release this moth.")
            return
        }
        val result =
            access.player.invTransaction(access.player.inv) {
                val inventory = select(access.player.inv)
                delete(inventory, species.jar.id, 1, slot)
                add(inventory, MothObjs.emptyJar.id, 1)
            }
        if (!result.success) {
            return
        }
        access.anim(MothSeqs.openJar)
        MothEffects.apply(access, species.effect, directCatch = false)
    }

    public fun method(player: Player, species: MothSpecies): MothCatchMethod? {
        if (!player.members) {
            return null
        }
        val level = player.stat(stats.hunter)
        if (level < species.level) {
            return null
        }
        return when (player.righthand?.id) {
            MothObjs.magicButterflyNet.id -> MothCatchMethod.MagicNet
            MothObjs.butterflyNet.id -> MothCatchMethod.RegularNet
            else -> {
                if (level >= species.barehandedLevel) {
                    MothCatchMethod.Barehanded
                } else {
                    null
                }
            }
        }
    }

    public fun catchChance(player: Player, species: MothSpecies, method: MothCatchMethod): Double {
        val curve =
            when (method) {
                MothCatchMethod.RegularNet -> species.regularRate
                MothCatchMethod.MagicNet,
                MothCatchMethod.Barehanded -> species.enhancedRate
            }
        val effectiveCurve =
            if (species.effect == MothEffect.Sunlight && method == MothCatchMethod.Barehanded) {
                species.regularRate
            } else {
                curve
            }
        return effectiveCurve.chance(player.stat(stats.hunter))
    }

    public companion object {
        public const val CATCH_TICKS: Int = 1
    }
}
