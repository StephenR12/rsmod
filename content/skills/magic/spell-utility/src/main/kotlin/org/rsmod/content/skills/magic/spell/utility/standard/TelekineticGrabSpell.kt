package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.config.Constants
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.projanims
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.synths
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.registry.obj.ObjRegistry
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.game.obj.Obj
import org.rsmod.game.proj.ProjAnim
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.proj.ProjAnimTypeList

public class TelekineticGrabSpell
@Inject
constructor(
    private val spells: MagicSpellRegistry,
    private val runes: MagicRuneManager,
    private val objRegistry: ObjRegistry,
    private val objRepo: ObjRepository,
    private val worldRepo: WorldRepository,
    private val projAnimTypes: ProjAnimTypeList,
    private val cooldowns: UtilitySpellCooldownRepository,
) {
    public fun cast(access: ProtectedAccess, target: Obj, allowQueue: Boolean = true) {
        with(access) {
            if (!cooldowns.isReady(player, mapClock)) {
                if (allowQueue && telekinetic_grab_queues.cast !in player.queueList) {
                    queue(
                        telekinetic_grab_queues.cast,
                        cooldowns.cyclesUntilReady(player, mapClock),
                        target,
                    )
                }
                return
            }
            if (target.type in excludedTargets) {
                mes("I can't use Telekinetic Grab on this object.")
                return
            }
            if (!isCurrentTarget(target)) {
                mes(Constants.dm_take_taken)
                return
            }
            if (!isWithinDistance(target.coords, MAX_RANGE)) {
                return
            }
            val add = player.invAdd(player.inv, target.type, target.count, autoCommit = false)
            if (add.failure) {
                mes(Constants.dm_take_invspace)
                return
            }

            val spell = requireNotNull(spells.getObjSpell(objs.spell_tele_grab))
            if (
                !runes.canCastSpell(player, spell) || runes.attemptCast(player, spell).isFailure()
            ) {
                return
            }
            if (vars[varbits.fountain_of_rune_active] == 0) {
                statAdvance(stats.magic, spell.castXp)
            }

            anim(seqs.human_casttelegrab)
            spotanim(spotanims.telegrab_casting)
            soundSynth(synths.telegrab_all)
            spawnProjectile(target)
            queue(
                telekinetic_grab_queues.impact,
                IMPACT_DELAY,
                TelekineticGrabImpact(target, player.level),
            )
            cooldowns.markCast(player, mapClock + CAST_RATE)
        }
    }

    internal fun impact(access: ProtectedAccess, request: TelekineticGrabImpact) {
        with(access) {
            val target = request.obj
            if (player.level != request.castLevel) {
                mes("Your telegrab fizzles as you move too far away.")
                return
            }
            if (!isCurrentTarget(target)) {
                mes(Constants.dm_take_taken)
                return
            }
            val add = player.invAdd(player.inv, target.type, target.count, autoCommit = false)
            if (add.failure) {
                mes(Constants.dm_take_invspace)
                return
            }
            if (!objRepo.del(target)) {
                mes(Constants.dm_take_taken)
                return
            }
            add.commitAll()
            spotanimMap(worldRepo, spotanims.telegrab_impact, target.coords)
        }
    }

    private fun ProtectedAccess.isCurrentTarget(target: Obj): Boolean =
        objRegistry.findAll(target.coords).any { it === target && it.isVisibleTo(player) }

    private fun ProtectedAccess.spawnProjectile(target: Obj) {
        val base =
            ProjAnim.fromPlayerToCoord(
                player,
                target.coords,
                spotanims.telegrab_travel.id,
                projAnimTypes[projanims.magic_spell_low],
            )
        worldRepo.projAnim(base.copy(startTime = PROJECTILE_START, endTime = PROJECTILE_END))
    }

    private companion object {
        const val MAX_RANGE = 10
        const val CAST_RATE = 3
        const val IMPACT_DELAY = 2
        const val PROJECTILE_START = 35
        const val PROJECTILE_END = 45

        val excludedTargets: Set<Int> =
            setOf(
                    objs.telegrab_ahabs_beer,
                    objs.telegrab_raw_pheasant,
                    objs.telegrab_blue_flowers,
                    objs.telegrab_red_flowers,
                    objs.telegrab_dragon_token,
                    objs.looting_bag,
                    objs.looting_bag_open,
                    objs.diary_karamja_gloves_1,
                    objs.diary_karamja_gloves_2,
                    objs.diary_karamja_gloves_3,
                    objs.diary_karamja_gloves_4,
                    objs.diary_varrock_armour_1,
                    objs.diary_varrock_armour_2,
                    objs.diary_varrock_armour_3,
                    objs.diary_varrock_armour_4,
                    objs.diary_wilderness_sword_1,
                    objs.diary_wilderness_sword_2,
                    objs.diary_wilderness_sword_3,
                    objs.diary_wilderness_sword_4,
                    objs.diary_morytania_legs_1,
                    objs.diary_morytania_legs_2,
                    objs.diary_morytania_legs_3,
                    objs.diary_morytania_legs_4,
                    objs.diary_falador_shield_1,
                    objs.diary_falador_shield_2,
                    objs.diary_falador_shield_3,
                    objs.diary_falador_shield_4,
                    objs.diary_ardougne_cloak_1,
                    objs.diary_ardougne_cloak_2,
                    objs.diary_ardougne_cloak_3,
                    objs.diary_ardougne_cloak_4,
                    objs.diary_explorers_ring_1,
                    objs.diary_explorers_ring_2,
                    objs.diary_explorers_ring_3,
                    objs.diary_explorers_ring_4,
                    objs.diary_fremennik_boots_1,
                    objs.diary_fremennik_boots_2,
                    objs.diary_fremennik_boots_3,
                    objs.diary_fremennik_boots_4,
                    objs.diary_desert_amulet_1,
                    objs.diary_desert_amulet_2,
                    objs.diary_desert_amulet_3,
                    objs.diary_desert_amulet_4,
                    objs.diary_kandarin_headgear_1,
                    objs.diary_kandarin_headgear_2,
                    objs.diary_kandarin_headgear_3,
                    objs.diary_kandarin_headgear_4,
                    objs.diary_western_banner_1,
                    objs.diary_western_banner_2,
                    objs.diary_western_banner_3,
                    objs.diary_western_banner_4,
                    objs.diary_radas_blessing_1,
                    objs.diary_radas_blessing_2,
                    objs.diary_radas_blessing_3,
                    objs.diary_radas_blessing_4,
                )
                .mapTo(mutableSetOf(), ObjType::id)
    }
}
