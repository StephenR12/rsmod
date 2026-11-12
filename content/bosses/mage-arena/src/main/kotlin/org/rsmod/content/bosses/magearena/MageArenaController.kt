package org.rsmod.content.bosses.magearena

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.IdentityHashMap
import kotlin.math.min
import org.rsmod.api.config.refs.huntmodes
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.game.entity.Npc
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
constructor(private val npcs: NpcRepository, private val npcTypes: NpcTypeList) {
    private val states = IdentityHashMap<Npc, MageArenaState>()

    public fun resetKolodion(npc: Npc): MageArenaState {
        val phase =
            MageArenaBosses.kolodionForms.indexOfFirst { it.npc.id == npc.id }.coerceAtLeast(0)
        return MageArenaState(kolodionPhase = phase).also { states[npc] = it }
    }

    public fun state(npc: Npc): MageArenaState = states.getOrPut(npc) { MageArenaState() }

    public fun modifyKolodionHit(npc: Npc, hit: HitBuilder) {
        val state = state(npc)
        if (hit.type != HitType.Magic) {
            hit.damage = 0
            return
        }
        if (state.kolodionPhase < MageArenaBosses.kolodionForms.lastIndex) {
            hit.damage = min(hit.damage, (npc.hitpoints - 1).coerceAtLeast(0))
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
        state.kolodionPhase++
        configureKolodion(npc, state.kolodionPhase)
        npc.say("You cannot defeat me so easily!")
    }

    public fun modifyFollowerHit(npc: Npc, hit: HitBuilder) {
        val allowed =
            hit.type == HitType.Magic &&
                when (npc.id) {
                    MageArenaNpcs.zachariah.id ->
                        hit.usesAny(MageArenaObjs.saradominStaff, MageArenaObjs.staffOfLight)
                    MageArenaNpcs.derwen.id ->
                        hit.usesAny(
                            MageArenaObjs.guthixStaff,
                            MageArenaObjs.staffOfBalance,
                            MageArenaObjs.voidKnightMace,
                        )
                    MageArenaNpcs.porazdir.id ->
                        hit.usesAny(
                            MageArenaObjs.zamorakStaff,
                            MageArenaObjs.staffOfTheDead,
                            MageArenaObjs.toxicStaffOfTheDead,
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

    public fun summonNextFollower(access: ProtectedAccess) {
        with(access) {
            if (vars[varbits.inside_wilderness] != 1) {
                mes("The symbol only reacts while you are in the Wilderness.")
                return
            }
            val active =
                npcs.findAll(ZoneKey.from(coords), 1).any {
                    it.id in MageArenaBosses.followerIds && it.hitpoints > 0
                }
            if (active) {
                mes("The symbol is already responding to a powerful magical presence nearby.")
                return
            }
            val type =
                when {
                    invTotal(inv, MageArenaObjs.justiciarsHand) == 0 -> MageArenaNpcs.zachariah
                    invTotal(inv, MageArenaObjs.entsRoots) == 0 -> MageArenaNpcs.derwen
                    invTotal(inv, MageArenaObjs.demonsHeart) == 0 -> MageArenaNpcs.porazdir
                    else -> {
                        mes("You have recovered all three magical artefacts.")
                        return
                    }
                }
            val spawn = CoordGrid(coords.x + 3, coords.z, coords.level)
            val follower = Npc(npcTypes[type], spawn)
            configureFollower(follower, type)
            npcs.add(follower, FOLLOWER_LIFETIME)
            mes("The enchanted symbol burns hot as ${follower.visType.name} appears nearby!")
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
            setHunt(12)
            setHuntMode(huntmodes.constant_ranged)
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
        private const val FOLLOWER_LIFETIME: Int = 500
    }
}

public data class MageArenaState(var kolodionPhase: Int = 0, var attacks: Int = 0)
