package org.rsmod.content.bosses.nex

import org.rsmod.api.combat.commons.npc.godWarsEssenceCost
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.boolVarp
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.type.npc.NpcType
import org.rsmod.game.type.obj.ObjType

@Singleton
public class NexAccessController
@Inject
constructor(
    private val protectedAccess: ProtectedAccessLauncher,
    private val objs: ObjRepository,
    private val random: GameRandom,
) {
    public fun onGodWarsKill(player: Player, npc: Npc) {
        val definition = keyPiece(npc.id) ?: return
        val guaranteed = npc.id == definition.general.id
        if (!guaranteed && random.of(BODYGUARD_DROP_DENOMINATOR) != 0) {
            return
        }
        protectedAccess.launch(player) {
            if (permanentAccess || invTotal(inv, definition.piece) > 0) {
                return@launch
            }
            definition.markObtained(this)
            invAddOrDrop(objs, definition.piece)
            mes("You find a piece of a frozen key.")
        }
    }

    public fun addAncientEssence(player: Player, npc: Npc) {
        val amount =
            when (npc.id) {
                NexNpcs.prisonWarrior.id,
                NexNpcs.prisonRanger.id -> 2
                NexNpcs.prisonMage.id,
                NexNpcs.boss.id -> 5
                NexNpcs.prisonBloodReaver.id -> 3
                else -> return
            }
        protectedAccess.launch(player) {
            ancientEssence = (ancientEssence + amount).coerceAtMost(MAX_ESSENCE)
            mes("Your Ancient Prison kill count is now $ancientEssence.")
        }
    }

    public suspend fun assembleFrozenKey(access: ProtectedAccess) {
        with(access) {
            if (permanentAccess) {
                mes("You have already unlocked the Frozen Door permanently.")
                return
            }
            val missing = KEY_PIECES.filter { invTotal(inv, it.piece) == 0 }
            if (missing.isNotEmpty()) {
                mes("You need all four frozen key pieces to assemble the key.")
                return
            }
            KEY_PIECES.forEach { invDel(inv, it.piece) }
            invAdd(inv, NexObjs.frozenKey)
            KEY_PIECES.forEach { it.markObtained(this) }
            mes("You join the four pieces together to make a frozen key.")
        }
    }

    public suspend fun enterFrozenDoor(access: ProtectedAccess) {
        with(access) {
            if (!permanentAccess) {
                if (vars[varbits.desert_treasure_main] < DESERT_TREASURE_COMPLETE) {
                    mes("You need to complete Desert Treasure to pass the Frozen Door.")
                    return
                }
                val failed =
                    listOfNotNull(
                        requirement(statBase(stats.agility), "Agility"),
                        requirement(stat(stats.hitpoints), "Hitpoints"),
                        requirement(statBase(stats.ranged), "Ranged"),
                        requirement(statBase(stats.strength), "Strength"),
                    )
                if (failed.isNotEmpty()) {
                    mes("You need level 70 ${failed.joinToString(", ")} to pass the Frozen Door.")
                    return
                }
                if (invTotal(inv, NexObjs.frozenKey) == 0) {
                    mes("The Frozen Door is locked. You need a completed frozen key.")
                    return
                }
                invDel(inv, NexObjs.frozenKey)
                permanentAccess = true
                frozenDoorOpen = true
                mes("The frozen key dissolves, permanently unlocking the door for you.")
            }
            telejump(NexAccessMap.ancientPrison)
        }
    }

    public fun leaveFrozenDoor(access: ProtectedAccess) {
        access.telejump(NexAccessMap.godWarsSide)
    }

    public suspend fun enterSafeRoom(access: ProtectedAccess) {
        with(access) {
            val cost = player.godWarsEssenceCost()
            when {
                ancientEssence >= cost -> ancientEssence -= cost
                invTotal(inv, NexObjs.ecumenicalKey) > 0 -> invDel(inv, NexObjs.ecumenicalKey)
                else -> {
                    mes(
                        "You need $cost Ancient Prison kill count or an ecumenical key."
                    )
                    return
                }
            }
            telejump(NexAccessMap.safeRoom)
        }
    }

    public fun leaveSafeRoom(access: ProtectedAccess) {
        access.telejump(NexAccessMap.prisonDoorSide)
    }

    public fun enterChamber(access: ProtectedAccess) {
        access.barrierEntered = true
        access.telejump(NexAccessMap.chamberEntry)
    }

    public fun ancientEssence(player: Player): Int = player.ancientEssence

    private fun keyPiece(npc: Int): KeyPiece? =
        KEY_PIECES.firstOrNull { definition ->
            npc == definition.general.id || definition.bodyguards.any { npc == it.id }
        }

    private fun requirement(level: Int, name: String): String? =
        if (level < FROZEN_DOOR_LEVEL) name else null

    private data class KeyPiece(
        val general: NpcType,
        val bodyguards: List<NpcType>,
        val piece: ObjType,
        val markObtained: ProtectedAccess.(Boolean) -> Unit,
    ) {
        fun markObtained(access: ProtectedAccess) = markObtained(access, true)
    }

    public companion object {
        public const val FROZEN_DOOR_LEVEL: Int = 70
        public const val DESERT_TREASURE_COMPLETE: Int = 15
        public const val REQUIRED_ESSENCE: Int = 40
        public const val MAX_ESSENCE: Int = 4000
        public const val BODYGUARD_DROP_DENOMINATOR: Int = 20

        private val KEY_PIECES =
            listOf(
                KeyPiece(
                    NexGodWarsNpcs.kree,
                    listOf(NexGodWarsNpcs.skree, NexGodWarsNpcs.geerin, NexGodWarsNpcs.kilisa),
                    NexObjs.armadylKeyPiece,
                    { armadylPiece = it },
                ),
                KeyPiece(
                    NexGodWarsNpcs.graardor,
                    listOf(
                        NexGodWarsNpcs.strongstack,
                        NexGodWarsNpcs.steelwill,
                        NexGodWarsNpcs.grimspike,
                    ),
                    NexObjs.bandosKeyPiece,
                    { bandosPiece = it },
                ),
                KeyPiece(
                    NexGodWarsNpcs.kril,
                    listOf(NexGodWarsNpcs.tstanon, NexGodWarsNpcs.balfrug, NexGodWarsNpcs.zakln),
                    NexObjs.zamorakKeyPiece,
                    { zamorakPiece = it },
                ),
                KeyPiece(
                    NexGodWarsNpcs.zilyana,
                    listOf(NexGodWarsNpcs.starlight, NexGodWarsNpcs.growler, NexGodWarsNpcs.bree),
                    NexObjs.saradominKeyPiece,
                    { saradominPiece = it },
                ),
            )
    }
}

private var ProtectedAccess.permanentAccess by boolVarp(NexVarps.permanentAccess)
private var ProtectedAccess.frozenDoorOpen by boolVarBit(NexVarBits.frozenDoor)
private var ProtectedAccess.armadylPiece by boolVarBit(NexVarBits.armadylPiece)
private var ProtectedAccess.bandosPiece by boolVarBit(NexVarBits.bandosPiece)
private var ProtectedAccess.zamorakPiece by boolVarBit(NexVarBits.zamorakPiece)
private var ProtectedAccess.saradominPiece by boolVarBit(NexVarBits.saradominPiece)
private var ProtectedAccess.ancientEssence by intVarBit(NexVarBits.ancientEssence)
private var ProtectedAccess.barrierEntered by boolVarBit(NexVarBits.barrier)
private val Player.ancientEssence by intVarBit(NexVarBits.ancientEssence)
