package org.rsmod.content.bosses.magearena

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.IdentityHashMap
import kotlin.math.min
import org.rsmod.api.config.refs.huntmodes
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.combat.commons.npc.engagePlayer
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.game.hit.Hit
import org.rsmod.game.hit.HitBuilder
import org.rsmod.game.hit.HitType
import org.rsmod.game.type.npc.NpcType
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjType
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey

@Singleton
public class MageArenaController
@Inject
constructor(
    private val npcs: NpcRepository,
    private val npcTypes: NpcTypeList,
    private val random: GameRandom,
    private val interactions: AiPlayerInteractions,
) {
    private val states = IdentityHashMap<Npc, MageArenaState>()

    private val challenges = mutableMapOf<Player, Npc>()
    private val challengers = mutableMapOf<Npc, Player>()

    public fun challenge(player: Player): Npc {
        val phase = (player.vars[varps.magearena] - 1).coerceIn(0, 4)
        val existing = challenges[player]
        if (existing != null && existing.isSlotAssigned && existing.hitpoints > 0) return existing
        val foe = Npc(npcTypes[MageArenaNpcs.kolodionHuman], MageArenaBosses.mageArena)
        npcs.add(foe, 6000)
        challenges[player] = foe
        challengers[foe] = player
        resetKolodion(foe).kolodionPhase = phase
        configureKolodion(foe, phase)
        return foe
    }

    internal fun inChallenge(player: Player): Boolean = player in challenges

    public fun challenger(npc: Npc): Player? = challengers[npc]

    public fun remove(npc: Npc) {
        states.remove(npc)
        val owner = challengers.remove(npc) ?: return
        challenges.remove(owner, npc)
    }

    public fun abandon(player: Player) {
        val npc = challenges[player] ?: return
        remove(npc)
        if (npc.isSlotAssigned && npc.hitpoints > 0) npcs.del(npc, Int.MAX_VALUE)
    }

    public fun resetKolodion(npc: Npc): MageArenaState {
        val phase =
            MageArenaBosses.kolodionForms.indexOfFirst { it.npc.id == npc.id }.coerceAtLeast(0)
        return MageArenaState(kolodionPhase = phase).also { states[npc] = it }
    }

    public fun state(npc: Npc): MageArenaState = states.getOrPut(npc) { MageArenaState() }

    public fun modifyKolodionHit(npc: Npc, hit: HitBuilder) {
        if (hit.type != HitType.Magic) {
            hit.damage = 0
            return
        }
    }

    public fun onKolodionHit(npc: Npc, hit: Hit) {
        if (!hit.isFromPlayer || npc.hitpoints > 1) {
            return
        }
        val state = state(npc)
        if (state.kolodionPhase >= MageArenaBosses.kolodionForms.lastIndex) {
            return
        }
        if (!state.transition || hit.type != HitType.Magic) return
        state.transition = false
        state.kolodionPhase++
        challengers[npc]?.let {
            VarPlayerIntMapSetter.set(it, varps.magearena, state.kolodionPhase + 1)
        }
        configureKolodion(npc, state.kolodionPhase)
        npc.say("You cannot defeat me so easily!")
    }

    public fun modifyFollowerHit(npc: Npc, hit: HitBuilder) {
        val allowed =
            hit.type == HitType.Magic &&
                when (npc.id) {
                    MageArenaNpcs.zachariah.id ->
                        hit.isSecondaryObj(objs.spell_saradomin_strike) &&
                            hit.usesAny(MageArenaObjs.saradominStaff, MageArenaObjs.staffOfLight)
                    MageArenaNpcs.derwen.id ->
                        hit.isSecondaryObj(objs.spell_claws_of_guthix) &&
                            hit.usesAny(
                                MageArenaObjs.guthixStaff,
                                MageArenaObjs.staffOfBalance,
                                MageArenaObjs.voidKnightMace,
                            )
                    MageArenaNpcs.porazdir.id ->
                        hit.isSecondaryObj(objs.spell_flames_of_zamorak) &&
                            hit.usesAny(
                                MageArenaObjs.zamorakStaff,
                                MageArenaObjs.staffOfTheDead,
                                MageArenaObjs.toxicStaffOfTheDead,
                                objs.toxic_staff_uncharged,
                                objs.thammarons_sceptre_a,
                                objs.thammarons_sceptre_au,
                                objs.accursed_sceptre_a,
                                objs.accursed_sceptre_au,
                            )
                    else -> false
                }
        if (!allowed) {
            hit.damage = 0
        }
    }

    public fun summonDerwenEnergy(derwen: Npc) {
        val existing =
            npcs.findAll(ZoneKey.from(derwen.coords), 1).any {
                it.id == MageArenaNpcs.derwenEnergy.id && it.hitpoints > 0
            }
        if (existing) {
            return
        }
        val npc =
            Npc(
                npcTypes[MageArenaNpcs.derwenEnergy],
                CoordGrid(derwen.coords.x + 3, derwen.coords.z, derwen.coords.level),
            )
        npc.apply {
            hitpoints = DERWEN_ENERGY_HITPOINTS
            baseHitpointsLvl = DERWEN_ENERGY_HITPOINTS
            mode = NpcMode.Wander
            setHunt(12)
            setHuntMode(huntmodes.constant_melee)
        }
        npcs.add(npc, FOLLOWER_LIFETIME)
    }

    public fun healDerwen(energy: Npc) {
        val derwen =
            npcs.findAll(ZoneKey.from(energy.coords), 1).firstOrNull {
                it.id == MageArenaNpcs.derwen.id && distance(it.coords, energy.coords) <= 8
            } ?: return
        derwen.hitpoints = min(derwen.baseHitpointsLvl, derwen.hitpoints + DERWEN_HEAL)
    }

    public suspend fun activateSymbol(access: ProtectedAccess) {
        with(access) {
            if (
                !player.members ||
                    vars[ArenaQuestBits.progress] < 2 ||
                    vars[varbits.inside_wilderness] != 1 ||
                    coords.level != 0 ||
                    coords.z !in 3520..3967
            ) {
                mes("The symbol doesn't seem to work here.")
                return
            }
            val available =
                gods.indices.filter { index ->
                    vars[ArenaQuestBits.progress] >= 3 ||
                        (vars[ArenaQuestBits.components[index]] == 0 &&
                            remains[index] !in inv &&
                            remains[index] !in bank)
                }
            if (available.isEmpty()) {
                mes("Return to Kolodion with your components.")
                return
            }
            val index =
                available.getOrNull(
                    menu("Which follower do you want to locate?", true, available.map { gods[it] })
                ) ?: return
            locateFollower(this, index)
        }
    }

    internal fun locateFollower(access: ProtectedAccess, index: Int) {
        with(access) {
            val rotation = (System.currentTimeMillis() / 2_700_000).toInt()
            if (vars[ArenaSaved.rotation] != 0 && vars[ArenaSaved.rotation] != rotation)
                mes("The followers have moved to new locations.")
            vars[ArenaSaved.rotation] = rotation
            val point = followerLocation(player.accountHash, rotation, index)
            val range = distance(coords, point)
            val heat =
                when {
                    range >= 200 -> "very cold"
                    range >= 150 -> "cold"
                    range >= 100 -> "warm"
                    range >= 70 -> "hot"
                    range >= 30 -> "very hot"
                    range >= 15 -> "incredibly hot"
                    else -> "visibly shaking"
                }
            mes("The symbol is $heat.")
            player.queueHit(1, HitType.Typeless, random.of(0..16))
            if (range >= 15) return
            if (
                npcs.findAll(ZoneKey.from(point), 2).any {
                    it.id in MageArenaBosses.followerIds &&
                        it.hitpoints > 0 &&
                        distance(it.coords, point) < 15
                }
            ) {
                mes("A magical being is already here.")
                return
            }
            val type =
                listOf(MageArenaNpcs.zachariah, MageArenaNpcs.derwen, MageArenaNpcs.porazdir)[index]
            val follower = Npc(npcTypes[type], point)
            configureFollower(follower, type)
            npcs.add(follower, FOLLOWER_LIFETIME)
            follower.engagePlayer(player, interactions)
        }
    }

    public fun configureKolodion(npc: Npc, phase: Int) {
        val form = MageArenaBosses.kolodionForms[phase]
        npc.apply {
            transmog(npcTypes[form.npc], Int.MAX_VALUE)
            hitpoints = form.hitpoints
            baseHitpointsLvl = form.hitpoints
            attackLvl = form.attack
            baseAttackLvl = form.attack
            strengthLvl = form.strength
            baseStrengthLvl = form.strength
            defenceLvl = form.defence
            baseDefenceLvl = form.defence
            magicLvl = form.magic
            baseMagicLvl = form.magic
            mode = NpcMode.Wander
            setHunt(0)
        }
    }

    public fun configureFollower(npc: Npc, type: NpcType) {
        npc.apply {
            hitpoints = FOLLOWER_HITPOINTS
            baseHitpointsLvl = FOLLOWER_HITPOINTS
            attackLvl = 250
            baseAttackLvl = 250
            strengthLvl = 150
            baseStrengthLvl = 150
            defenceLvl = 100
            baseDefenceLvl = 100
            magicLvl = 180
            baseMagicLvl = 180
            mode = NpcMode.Wander
            setHunt(18)
            setHuntMode(huntmodes.constant_ranged)
        }
        if (type.id == MageArenaNpcs.zachariah.id) {
            npc.strengthLvl = 200
            npc.baseStrengthLvl = 200
        }
    }

    private fun HitBuilder.usesAny(vararg objects: ObjType): Boolean = objects.any(::isRighthandObj)

    private fun distance(a: CoordGrid, b: CoordGrid): Int =
        maxOf(kotlin.math.abs(a.x - b.x), kotlin.math.abs(a.z - b.z))

    public companion object {
        public const val FOLLOWER_HITPOINTS: Int = 320
        public const val DERWEN_ENERGY_HITPOINTS: Int = 20
        public const val DERWEN_HEAL: Int = 5
        private val locations =
            listOf(
                CoordGrid(3172, 3898),
                CoordGrid(3304, 3942),
                CoordGrid(3296, 3876),
                CoordGrid(3260, 3886),
                CoordGrid(3233, 3909),
                CoordGrid(3215, 3883),
                CoordGrid(3231, 3873),
                CoordGrid(3276, 3842),
                CoordGrid(3248, 3831),
                CoordGrid(3261, 3800),
                CoordGrid(3168, 3796),
                CoordGrid(3158, 3842),
                CoordGrid(3022, 3831),
                CoordGrid(3150, 3878),
                CoordGrid(3334, 3903),
                CoordGrid(3172, 3866),
                CoordGrid(3282, 3821),
            )

        public fun followerLocation(account: Long, rotation: Int, index: Int): CoordGrid {
            val personal =
                java.util.Random(
                    account xor (rotation.toLong() shl 24) xor (0x5deece66dL * (index + 1))
                )
            return locations[personal.nextInt(locations.size)]
        }

        private const val FOLLOWER_LIFETIME: Int = 500
    }
}

public data class MageArenaState(
    var kolodionPhase: Int = 0,
    var attacks: Int = 0,
    var transition: Boolean = false,
)
