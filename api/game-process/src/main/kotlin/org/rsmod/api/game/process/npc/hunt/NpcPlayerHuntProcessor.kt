package org.rsmod.api.game.process.npc.hunt

import jakarta.inject.Inject
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.npcs
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.hunt.Hunt
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.isValidTarget
import org.rsmod.api.npc.mapMultiway
import org.rsmod.api.npc.opPlayer2
import org.rsmod.api.player.isInPvnCombat
import org.rsmod.api.player.isValidTarget
import org.rsmod.api.player.stat.stat
import org.rsmod.api.random.CoreRandom
import org.rsmod.api.random.GameRandom
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.PlayerUid
import org.rsmod.game.type.hunt.HuntCheckNotTooStrong
import org.rsmod.game.type.hunt.HuntModeTypeList
import org.rsmod.game.type.hunt.HuntType
import org.rsmod.game.type.hunt.HuntVis
import org.rsmod.game.type.hunt.UnpackedHuntModeType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.varn.VarnTypeList
import org.rsmod.game.type.varp.VarpTypeList

public class NpcPlayerHuntProcessor
@Inject
constructor(
    @CoreRandom private val random: GameRandom,
    private val mapClock: MapClock,
    private val hunt: Hunt,
    private val huntModes: HuntModeTypeList,
    private val varpTypes: VarpTypeList,
    private val varnTypes: VarnTypeList,
    private val objTypes: ObjTypeList,
    private val areaChecker: AreaChecker,
    private val interactions: AiPlayerInteractions,
) {
    public fun process(npc: Npc) {
        if (!npc.isValidTarget() || npc.isDelayed) {
            return
        }

        if (!npc.hasInteraction() && npc.tryGoad()) {
            return
        }

        val huntMode = npc.huntMode
        val huntDisabled = npc.huntRange == 0 || huntMode == null
        if (huntDisabled) {
            return
        }

        val skipHunt = !npc.isAnyoneNear()
        if (skipHunt) {
            return
        }

        val huntType = huntModes.getValue(huntMode)
        val huntDelayed = npc.huntClock < huntType.rate - 1
        if (huntDelayed) {
            return
        }

        if (huntType.type == HuntType.Player && !npc.hasInteraction()) {
            npc.huntPlayer(huntType)
        }
    }

    private fun Npc.tryGoad(): Boolean {
        if (!isAttackableMonster() || id in GOADING_IMMUNE_NPCS) {
            return false
        }
        val multiway = mapMultiway(areaChecker)
        if (!multiway && Math.floorMod(mapClock.cycle, SINGLEWAY_RECHECK_CYCLES) != 0) {
            return false
        }

        val slayerRequirement = visType.param(params.slayer_levelrequire)
        var target: Player? = null
        var count = 0
        for (player in hunt.findPlayers(coords, GOADING_RADIUS, HuntVis.Off)) {
            if (!player.isValidTarget() || player.vars[varbits.goading_potion_timer] <= 0) {
                continue
            }
            if (player.stat(stats.slayer) < slayerRequirement) {
                continue
            }
            if (!multiway && player.isInPvnCombat()) {
                continue
            }
            count++
            if (random.of(minInclusive = 0, maxInclusive = count - 1) == 0) {
                target = player
            }
        }
        if (target == null) {
            return false
        }
        opPlayer2(target, interactions)
        return true
    }

    private fun Npc.isAttackableMonster(): Boolean =
        visType.op.getOrNull(ATTACK_OP_INDEX) == "Attack"

    private fun Npc.huntPlayer(mode: UnpackedHuntModeType) {
        var target = PlayerUid.NULL
        var count = 0

        val players = hunt.findPlayers(coords, huntRange, mode.checkVis)
        for (player in players) {
            if (player.isInvisible) {
                continue
            }

            if (mode.checkNotBusy && player.isBusy) {
                continue
            }

            if (mode.checkAfk && player.isAfk()) {
                continue
            }

            if (mode.checkNotTooStrong == HuntCheckNotTooStrong.OutsideWilderness) {
                if (player.combatLevel > type.vislevel * 2 && !player.isInWilderness()) {
                    continue
                }
            }

            if (!player.isInMulti()) {
                if (mode.checkNotCombat != -1) {
                    val varp = varpTypes.getValue(mode.checkNotCombat)
                    val delay = player.vars[varp] + constants.combat_activecombat_delay
                    if (delay > mapClock.cycle) {
                        continue
                    }
                }

                if (mode.checkNotCombatSelf != -1) {
                    val varn = varnTypes.getValue(mode.checkNotCombatSelf)
                    val delay = vars[varn] + constants.combat_activecombat_delay
                    if (delay > mapClock.cycle) {
                        continue
                    }
                }
            }

            val checkVar1 = mode.checkVar1
            if (checkVar1 != null) {
                val varp = varpTypes.getValue(checkVar1.varp)
                val actual = player.vars[varp]
                if (!checkVar1.evaluate(actual)) {
                    continue
                }
            }

            val checkVar2 = mode.checkVar2
            if (checkVar2 != null) {
                val varp = varpTypes.getValue(checkVar2.varp)
                val actual = player.vars[varp]
                if (!checkVar2.evaluate(actual)) {
                    continue
                }
            }

            val checkVar3 = mode.checkVar3
            if (checkVar3 != null) {
                val varp = varpTypes.getValue(checkVar3.varp)
                val actual = player.vars[varp]
                if (!checkVar3.evaluate(actual)) {
                    continue
                }
            }

            val checkInvObj = mode.checkInvObj
            if (checkInvObj != null) {
                val inventory = player.invMap.backing[checkInvObj.inv]

                val obj = checkInvObj.type
                val count = inventory?.sumOf { if (it?.id == obj) it.count else 0 } ?: 0

                if (!checkInvObj.evaluate(count)) {
                    continue
                }
            }

            val checkInvParam = mode.checkInvParam
            if (checkInvParam != null) {
                val inventory = player.invMap.backing[checkInvParam.inv]
                val param = checkInvParam.type

                var count = 0
                if (inventory != null) {
                    for (invObj in inventory) {
                        val objType = objTypes.getOrNull(invObj) ?: continue
                        val value = objType.paramMap?.primitiveMap?.get(param) ?: continue
                        if (value !is Int) {
                            val message = "Expected param value to be Int: $value (param=$param)"
                            throw IllegalStateException(message)
                        }
                        count += value
                    }
                }

                if (!checkInvParam.evaluate(count)) {
                    continue
                }
            }

            count++
            if (random.of(minInclusive = 0, maxInclusive = count) == 0) {
                target = player.uid
            }
        }

        if (target != PlayerUid.NULL) {
            huntPlayer = target
        }
    }

    // TODO: Investigate what `checkAfk` actually checks.
    private fun Player.isAfk(): Boolean {
        return false
    }

    // TODO(combat): Wilderness indicator.
    private fun Player.isInWilderness(): Boolean {
        return false
    }

    // Hunt can be quite expensive if not careful. We are assuming that using a possibly delayed
    // multiway indicator will not have any inaccuracies in emulation. If it does, we can change
    // this to a dynamic lookup on `AreaIndex` instead.
    private fun Player.isInMulti(): Boolean {
        return vars[varbits.multiway_indicator] == 1
    }

    private companion object {
        const val ATTACK_OP_INDEX = 1
        const val GOADING_RADIUS = 4
        const val SINGLEWAY_RECHECK_CYCLES = 5
        val GOADING_IMMUNE_NPCS =
            setOf(
                npcs.fight_caves_jad_healer.id,
                npcs.inferno_nibbler.id,
                npcs.inferno_jad_healer.id,
                npcs.inferno_jad_healer_finalwave.id,
                npcs.jad_challenge_healer.id,
            )
    }
}
