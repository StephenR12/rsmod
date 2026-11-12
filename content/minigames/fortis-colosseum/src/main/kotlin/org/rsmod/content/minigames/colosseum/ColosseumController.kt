package org.rsmod.content.minigames.colosseum

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.Collections
import java.util.IdentityHashMap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.opPlayer2
import org.rsmod.api.player.events.CombatAchievementEvents
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.stat.baseHitpointsLvl
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.stat.statSub
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.repo.region.RegionRepository
import org.rsmod.api.repo.region.RegionTemplate
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity.Companion.INVALID_SLOT
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.region.Region
import org.rsmod.game.type.npc.NpcType
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjType
import org.rsmod.map.CoordGrid

@Singleton
public class ColosseumController
@Inject
constructor(
    private val regions: RegionRepository,
    private val npcs: NpcRepository,
    private val npcTypes: NpcTypeList,
    private val interactions: AiPlayerInteractions,
    private val protectedAccess: ProtectedAccessLauncher,
    private val objRepo: ObjRepository,
) {
    private val sessions = IdentityHashMap<Player, ColosseumSession>()
    private val owners = IdentityHashMap<Npc, ColosseumSession>()
    private val failedLogouts = mutableSetOf<String>()

    private var Player.bestGlory by intVarp(ColosseumVarps.glory)
    private var Player.totalWaves by intVarp(ColosseumVarps.totalWaves)
    private var Player.currentGlory by intVarp(ColosseumVarps.currentGlory)
    private var Player.waveStart by intVarp(ColosseumVarps.waveStart)
    private var Player.waveDamage by intVarp(ColosseumVarps.waveDamage)
    private var Player.lastModifierGlory by intVarp(ColosseumVarps.lastModifierGlory)
    private var Player.lastWaveDuration by intVarp(ColosseumVarps.lastWaveDuration)
    private var Player.totalDuration by intVarp(ColosseumVarps.totalDuration)
    private var Player.killtime by intVarp(ColosseumVarps.killtime)
    private var Player.totalSolKills by intVarp(ColosseumVarps.totalSolKills)
    private var Player.selectedModifier by intVarBit(ColosseumVarBits.selectedModifier)
    private var Player.blasphemy by intVarBit(ColosseumVarBits.blasphemy)
    private var Player.reentry by intVarBit(ColosseumVarBits.reentry)
    private var Player.myopia by intVarBit(ColosseumVarBits.myopia)
    private var Player.frailty by intVarBit(ColosseumVarBits.frailty)
    private var Player.solarflare by intVarBit(ColosseumVarBits.solarflare)
    private var Player.relentless by intVarBit(ColosseumVarBits.relentless)
    private var Player.volatility by intVarBit(ColosseumVarBits.volatility)
    private var Player.mantimayhem by intVarBit(ColosseumVarBits.mantimayhem)
    private var Player.doom by intVarBit(ColosseumVarBits.doom)
    private var Player.lootOpened by intVarBit(ColosseumVarBits.lootOpened)
    private var Player.highestWave by intVarBit(ColosseumVarBits.highestWave)

    public suspend fun enter(access: ProtectedAccess) {
        val player = access.player
        if (!player.members) {
            access.mes("You need to be on a members' world to enter the Fortis Colosseum.")
            return
        }
        sessions.remove(player)?.let(::destroy)
        val session =
            create(player)
                ?: run {
                    access.mes("The Colosseum is currently full. Please try again shortly.")
                    return
                }
        clearHud(player)
        player.totalDuration = 0
        player.currentGlory = 0
        access.telejump(session.arena(24, 34))
        access.mes("The crowd roars as you enter the Fortis Colosseum.")
        access.delay(2)
        offerModifier(access, session, first = true)
    }

    public suspend fun leave(access: ProtectedAccess) {
        val session = sessions.remove(access.player) ?: return
        val claim = session.betweenWaves && session.rewards.isNotEmpty()
        if (claim) claimRewards(access.player, session)
        destroy(session)
        clearHud(access.player)
        access.telejump(OUTSIDE)
        access.mes(
            if (claim) "You claim your rewards and leave the Colosseum."
            else "You abandon the Colosseum. Your unclaimed rewards are lost."
        )
    }

    public fun onLogout(player: Player) {
        val session = sessions.remove(player) ?: return
        failedLogouts += player.username
        destroy(session)
        clearHud(player)
    }

    public fun onLogin(player: Player) {
        if (!failedLogouts.remove(player.username)) return
        protectedAccess.launch(player) {
            telejump(OUTSIDE)
            mes("Your Colosseum attempt ended when you logged out; unclaimed rewards were lost.")
        }
    }

    public fun onDeath(player: Player) {
        val session = sessions.remove(player) ?: return
        persistGlory(player, session)
        destroy(session)
        clearHud(player)
        protectedAccess.launch(player) {
            telejump(OUTSIDE)
            mes("You were defeated on wave ${session.wave}. Your unclaimed rewards were lost.")
            mes("Your run earned ${session.glory} Glory.")
        }
    }

    public fun ownerFor(npc: Npc): Player? = owners[npc]?.player

    public fun modifierLevel(npc: Npc, modifier: ColosseumModifier): Int =
        owners[npc]?.modifiers?.get(modifier) ?: 0

    public fun isCurrent(npc: Npc, player: Player): Boolean = owners[npc]?.player === player

    public fun onPlayerHit(player: Player, damage: Int) {
        if (damage <= 0) return
        val session = sessions[player] ?: return
        if (session.betweenWaves) return
        session.damageThisWave += damage
        player.waveDamage = session.damageThisWave
        val blasphemy = session.level(ColosseumModifier.Blasphemy)
        if (blasphemy > 0) player.statSub(stats.prayer, constant = blasphemy, percent = 0)
        val doomLevel = session.level(ColosseumModifier.Doom)
        if (doomLevel > 0) {
            session.doomStacks++
            player.doom = session.doomStacks
            if (session.doomStacks >= ColosseumModifierRules.doomLimit(doomLevel)) {
                player.mes("Doom has claimed you.")
                player.queueHit(0, HitType.Typeless, player.hitpoints)
            }
        }
    }

    public fun onNpcHit(npc: Npc) {
        val session = owners[npc] ?: return
        if (
            session.level(ColosseumModifier.Totemic) > 0 &&
                !session.totemSpawned &&
                npc.hitpoints * 2 <= npc.baseHitpointsLvl
        ) {
            session.totemSpawned = true
            spawn(session, ColosseumNpcs.healingTotem, 39, 34, support = true, attack = false)
            session.alive.forEach { ally ->
                ally.hitpoints =
                    min(ally.baseHitpointsLvl, ally.hitpoints + max(1, ally.baseHitpointsLvl / 10))
            }
            session.player.mes("A healing totem restores the Colosseum's combatants!")
        }
    }

    public fun healAllies(source: Npc) {
        val session = owners[source] ?: return
        session.alive
            .filter { it !== source && it.hitpoints > 0 }
            .minByOrNull { it.hitpoints * 100 / max(1, it.baseHitpointsLvl) }
            ?.let { it.hitpoints = min(it.baseHitpointsLvl, it.hitpoints + 15) }
    }

    public fun onKill(player: Player, npc: Npc) {
        val session = owners.remove(npc) ?: return
        if (session.player !== player || !session.alive.remove(npc)) return
        if (
            session.level(ColosseumModifier.Volatility) > 0 &&
                distance(player.coords, npc.coords) <= 2
        ) {
            val maxHit = 8 * session.level(ColosseumModifier.Volatility)
            player.queueHit(npc, 1, HitType.Typeless, Random.nextInt(maxHit + 1))
        }
        if (npc.id == ColosseumNpcs.sol.id) {
            complete(session)
            return
        }
        if (session.alive.isEmpty() && session.reinforcementsSpawned) finishWave(session)
    }

    private fun create(player: Player): ColosseumSession? {
        val template =
            RegionTemplate.create {
                copy(ARENA_ZONE_X, ARENA_ZONE_Z, 0) {
                    zoneWidth = 8
                    zoneLength = 8
                    regionZoneX = 4
                    regionZoneZ = 4
                }
            }
        val region = regions.add(template) ?: return null
        return ColosseumSession(player, region, runStartClock = player.currentMapClock).also {
            sessions[player] = it
        }
    }

    private suspend fun offerModifier(
        access: ProtectedAccess,
        session: ColosseumSession,
        first: Boolean,
    ) {
        if (sessions[access.player] !== session) return
        session.betweenWaves = true
        val offers =
            if (first) ColosseumModifierRules.firstWave()
            else ColosseumModifierRules.offers(session.wave, session.modifiers)
        if (offers.size < 3) {
            startWave(session)
            return
        }
        access.startDialogue {
            val option =
                if (first) {
                    choice3(
                        "${offers[0].displayName} I",
                        0,
                        "${offers[1].displayName} I",
                        1,
                        "${offers[2].displayName} I",
                        2,
                        title = "Choose a handicap for wave 1.",
                    )
                } else {
                    choice4(
                        "${offers[0].displayName} ${session.level(offers[0]) + 1}",
                        0,
                        "${offers[1].displayName} ${session.level(offers[1]) + 1}",
                        1,
                        "${offers[2].displayName} ${session.level(offers[2]) + 1}",
                        2,
                        "Claim rewards and leave.",
                        -1,
                        title = "Choose a handicap for wave ${session.wave + 1}.",
                    )
                }
            if (sessions[access.player] !== session) return@startDialogue
            if (option == -1) {
                sessions.remove(access.player)
                claimRewards(access.player, session)
                persistGlory(access.player, session)
                destroy(session)
                clearHud(access.player)
                access.telejump(OUTSIDE)
                access.mes("You claim your accumulated rewards.")
                return@startDialogue
            }
            val modifier = offers[option]
            session.modifiers[modifier] = session.level(modifier) + 1
            access.player.selectedModifier = modifier.ordinal + 1
            syncModifiers(access.player, session)
            if (!first) session.wave++
            startWave(session)
        }
    }

    private fun startWave(session: ColosseumSession) {
        if (sessions[session.player] !== session) return
        session.betweenWaves = false
        session.damageThisWave = 0
        session.totemSpawned = false
        session.reinforcementsSpawned = false
        session.waveStartClock = session.player.currentMapClock
        session.player.waveStart = session.waveStartClock
        session.player.waveDamage = 0
        session.player.mes("Wave ${session.wave} begins!")
        capHitpointsForFrailty(session)
        val wave = ColosseumWaves.get(session.wave)
        val initial = wave.initial.toMutableList()
        if (session.level(ColosseumModifier.Quartet) > 0 && session.wave < 12) {
            initial +=
                listOf(ColosseumMonster.Meleer, ColosseumMonster.Ranger, ColosseumMonster.Mager)[
                    session.wave % 3]
        }
        initial.forEachIndexed { index, monster ->
            val point =
                if (monster in WARBAND) WARBAND_SPAWNS[index % WARBAND_SPAWNS.size]
                else SPAWNS[(session.wave * 3 + index * 5) % SPAWNS.size]
            spawn(session, typeFor(session, monster), point.first, point.second)
        }
        if (wave.reinforcements.isEmpty()) {
            session.reinforcementsSpawned = true
        } else {
            val waveNumber = session.wave
            protectedAccess.launch(session.player) {
                delay(REINFORCEMENT_DELAY)
                if (
                    sessions[player] !== session ||
                        session.wave != waveNumber ||
                        session.betweenWaves
                )
                    return@launch
                val reinforcements = wave.reinforcements.toMutableList()
                if (
                    session.level(ColosseumModifier.DynamicDuo) > 0 &&
                        ColosseumMonster.Minotaur in reinforcements
                )
                    reinforcements += ColosseumMonster.Minotaur
                reinforcements.forEachIndexed { index, monster ->
                    spawn(session, typeFor(session, monster), 49, 30 + index * 4)
                }
                session.reinforcementsSpawned = true
                player.mes("Reinforcements enter through the eastern gate!")
                if (session.alive.isEmpty()) finishWave(session)
            }
        }
        startHazards(session)
    }

    private fun startHazards(session: ColosseumSession) {
        val waveNumber = session.wave
        if (session.level(ColosseumModifier.Bees) > 0) {
            spawn(session, ColosseumNpcs.bees, 32, 39, support = true, attack = false)
            protectedAccess.launch(session.player) {
                while (
                    sessions[player] === session &&
                        session.wave == waveNumber &&
                        !session.betweenWaves
                ) {
                    val warned = player.coords
                    mes("The swarm closes in—move!")
                    delay(3)
                    if (player.coords == warned)
                        player.queueHit(
                            0,
                            HitType.Typeless,
                            2 * session.level(ColosseumModifier.Bees),
                        )
                    delay(9)
                }
            }
        }
        if (session.level(ColosseumModifier.Solarflare) > 0) {
            spawn(session, ColosseumNpcs.solarFlare, 40, 34, support = true, attack = false)
            protectedAccess.launch(session.player) {
                while (
                    sessions[player] === session &&
                        session.wave == waveNumber &&
                        !session.betweenWaves
                ) {
                    val warned = player.coords
                    mes("Solarfire gathers beneath you!")
                    delay(4)
                    if (player.coords == warned)
                        player.queueHit(
                            0,
                            HitType.Typeless,
                            5 + 3 * session.level(ColosseumModifier.Solarflare),
                        )
                    delay(12)
                }
            }
        }
    }

    private fun finishWave(session: ColosseumSession) {
        if (session.betweenWaves || sessions[session.player] !== session) return
        session.betweenWaves = true
        clearSupport(session)
        val duration = max(1, session.player.currentMapClock - session.waveStartClock)
        val modifierGlory =
            session.modifiers.entries.sumOf { (modifier, level) -> modifier.glory * level }
        val completion = session.wave * 100
        val noDamage = if (session.damageThisWave == 0) session.wave * 100 else 0
        val timeBonus = max(0, 300 - duration * 2)
        val earned = completion + noDamage + modifierGlory + timeBonus
        session.glory += earned
        session.totalTicks += duration
        session.player.totalWaves++
        session.player.highestWave = max(session.player.highestWave, session.wave)
        session.player.currentGlory = session.glory
        session.player.lastModifierGlory = modifierGlory
        session.player.lastWaveDuration = duration
        session.player.totalDuration = session.totalTicks
        addWaveReward(session)
        session.player.mes(
            "Wave ${session.wave} complete: +$earned Glory (${session.glory} total)."
        )
        protectedAccess.launch(session.player) {
            delay(4)
            if (sessions[player] !== session) return@launch
            offerModifier(this, session, first = false)
        }
    }

    private fun complete(session: ColosseumSession) {
        if (sessions.remove(session.player) !== session) return
        session.betweenWaves = true
        val duration = max(1, session.player.currentMapClock - session.waveStartClock)
        session.totalTicks += duration
        val modifierGlory =
            session.modifiers.entries.sumOf { (modifier, level) -> modifier.glory * level }
        val earned =
            1200 +
                (if (session.damageThisWave == 0) 1200 else 0) +
                modifierGlory +
                max(0, 300 - duration * 2)
        session.glory += earned
        session.player.totalWaves++
        session.player.highestWave = max(session.player.highestWave, 12)
        addWaveReward(session)
        session.rewards += ColosseumReward(ColosseumObjs.quiver, 1)
        session.rewards += ColosseumReward(ColosseumObjs.splinters, 1_000)
        if (Random.nextInt(200) == 0) session.rewards += ColosseumReward(ColosseumObjs.pet, 1)
        val completionCount = ++session.player.totalSolKills
        session.player.killtime = session.totalTicks
        session.player.lastModifierGlory = modifierGlory
        session.player.lastWaveDuration = duration
        persistGlory(session.player, session)
        protectedAccess.launch(session.player) {
            delay(4)
            claimRewards(player, session)
            destroy(session)
            clearHud(player)
            telejump(OUTSIDE)
            mes("You have defeated Sol Heredit and conquered the Fortis Colosseum!")
            mes(
                "You receive Dizana's quiver. Completion count: <col=ff0000>$completionCount</col>."
            )
            publish(
                CombatAchievementEvents.EncounterCompleted(
                    player,
                    "Fortis Colosseum",
                    completionCount,
                    deaths = 0,
                    durationTicks = session.totalTicks,
                )
            )
        }
    }

    private fun addWaveReward(session: ColosseumSession) {
        session.rewards +=
            ColosseumReward(
                ColosseumObjs.splinters,
                if (session.wave == 1) 80 else 80 + session.wave * 20,
            )
        if (session.wave < 4) return
        val denominators =
            mapOf(
                4 to 124,
                5 to 110,
                6 to 96,
                7 to 82,
                8 to 68,
                9 to 54,
                10 to 40,
                11 to 26,
                12 to 12,
            )
        if (Random.nextInt(denominators.getValue(session.wave)) != 0) {
            session.rewards +=
                ColosseumReward(
                    if (session.wave % 2 == 0) objs.death_rune else objs.runite_ore,
                    5 + session.wave * 2,
                )
            return
        }
        val unique =
            if (session.wave <= 6) {
                if (Random.nextInt(10) < 4) ColosseumObjs.echoCrystal
                else ColosseumObjs.sunfireArmour
            } else
                when (Random.nextInt(16)) {
                    in 0..5 -> ColosseumObjs.echoCrystal
                    15 -> ColosseumObjs.tonalztics
                    else -> ColosseumObjs.sunfireArmour
                }
        session.rewards += ColosseumReward(unique, 1)
    }

    private fun claimRewards(player: Player, session: ColosseumSession) {
        session.rewards.forEach { player.invAddOrDrop(objRepo, it.obj, it.count) }
        session.rewards.clear()
    }

    private fun persistGlory(player: Player, session: ColosseumSession) {
        player.bestGlory = max(player.bestGlory, session.glory)
        player.currentGlory = session.glory
        player.totalDuration = session.totalTicks
    }

    private fun syncModifiers(player: Player, session: ColosseumSession) {
        player.blasphemy = session.level(ColosseumModifier.Blasphemy)
        player.reentry = session.level(ColosseumModifier.Reentry)
        player.myopia = session.level(ColosseumModifier.Myopia)
        player.frailty = session.level(ColosseumModifier.Frailty)
        player.solarflare = session.level(ColosseumModifier.Solarflare)
        player.relentless = session.level(ColosseumModifier.Relentless)
        player.volatility = session.level(ColosseumModifier.Volatility)
        player.mantimayhem = session.level(ColosseumModifier.Mantimayhem)
    }

    private fun capHitpointsForFrailty(session: ColosseumSession) {
        val level = session.level(ColosseumModifier.Frailty)
        if (level == 0) return
        val cap =
            max(
                1,
                session.player.baseHitpointsLvl * ColosseumModifierRules.frailtyPercent(level) / 100,
            )
        if (session.player.hitpoints > cap) {
            session.player.statSub(
                stats.hitpoints,
                constant = session.player.hitpoints - cap,
                percent = 0,
            )
        }
    }

    private fun clearHud(player: Player) {
        player.selectedModifier = 0
        player.blasphemy = 0
        player.reentry = 0
        player.myopia = 0
        player.frailty = 0
        player.solarflare = 0
        player.relentless = 0
        player.volatility = 0
        player.mantimayhem = 0
        player.doom = 0
        player.lootOpened = 0
        player.waveDamage = 0
    }

    private fun spawn(
        session: ColosseumSession,
        type: NpcType,
        localX: Int,
        localZ: Int,
        support: Boolean = false,
        attack: Boolean = true,
    ): Npc {
        val npc = Npc(npcTypes[type], session.arena(localX, localZ))
        npcs.add(npc, NPC_LIFETIME)
        owners[npc] = session
        if (support) session.support += npc else session.alive += npc
        if (attack) npc.opPlayer2(session.player, interactions)
        return npc
    }

    private fun typeFor(session: ColosseumSession, monster: ColosseumMonster): NpcType =
        when (monster) {
            ColosseumMonster.Meleer -> ColosseumNpcs.meleer
            ColosseumMonster.Ranger -> ColosseumNpcs.ranger
            ColosseumMonster.Mager -> ColosseumNpcs.mager
            ColosseumMonster.Shaman -> ColosseumNpcs.shaman
            ColosseumMonster.Jaguar -> ColosseumNpcs.jaguar
            ColosseumMonster.Javelin -> ColosseumNpcs.javelin
            ColosseumMonster.Manticore -> ColosseumNpcs.manticore
            ColosseumMonster.Shockwave -> ColosseumNpcs.shockwave
            ColosseumMonster.Minotaur ->
                if (session.level(ColosseumModifier.RedFlag) > 0) ColosseumNpcs.routefindingMinotaur
                else ColosseumNpcs.minotaur
            ColosseumMonster.Sol -> ColosseumNpcs.sol
        }

    private fun clearSupport(session: ColosseumSession) {
        session.support.toList().forEach { npc ->
            owners.remove(npc)
            if (npc.slotId != INVALID_SLOT) npcs.del(npc, Int.MAX_VALUE)
        }
        session.support.clear()
    }

    private fun destroy(session: ColosseumSession) {
        (session.alive + session.support).toList().forEach { npc ->
            owners.remove(npc)
            if (npc.slotId != INVALID_SLOT) npcs.del(npc, Int.MAX_VALUE)
        }
        session.alive.clear()
        session.support.clear()
    }

    private fun distance(a: CoordGrid, b: CoordGrid): Int = max(abs(a.x - b.x), abs(a.z - b.z))

    public companion object {
        public val OUTSIDE: CoordGrid = CoordGrid(1795, 3106, 0)
        private const val ARENA_ZONE_X = 28 * 8
        private const val ARENA_ZONE_Z = 148 * 8
        private const val NPC_LIFETIME = 150_000
        private const val REINFORCEMENT_DELAY = 67
        private val WARBAND =
            setOf(ColosseumMonster.Meleer, ColosseumMonster.Ranger, ColosseumMonster.Mager)
        private val WARBAND_SPAWNS = listOf(47 to 30, 48 to 34, 47 to 38)
        private val SPAWNS =
            listOf(31 to 21, 39 to 21, 46 to 26, 45 to 42, 38 to 48, 29 to 45, 34 to 34)
    }
}

public data class ColosseumReward(val obj: ObjType, val count: Int)

public data class ColosseumSession(
    val player: Player,
    val region: Region,
    val runStartClock: Int,
    var wave: Int = 1,
    var waveStartClock: Int = 0,
    var totalTicks: Int = 0,
    var glory: Int = 0,
    var damageThisWave: Int = 0,
    var doomStacks: Int = 0,
    var betweenWaves: Boolean = true,
    var reinforcementsSpawned: Boolean = false,
    var totemSpawned: Boolean = false,
    val modifiers: MutableMap<ColosseumModifier, Int> = linkedMapOf(),
    val rewards: MutableList<ColosseumReward> = mutableListOf(),
    val alive: MutableSet<Npc> = Collections.newSetFromMap(IdentityHashMap()),
    val support: MutableSet<Npc> = Collections.newSetFromMap(IdentityHashMap()),
) {
    public fun level(modifier: ColosseumModifier): Int = modifiers[modifier] ?: 0

    public fun arena(localX: Int, localZ: Int): CoordGrid =
        region.normal[0, 28, 148, localX, localZ]
}
