package org.rsmod.content.minigames.nightmarezone

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.queues
import org.rsmod.api.npc.events.NpcKillEvents
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.opPlayer2
import org.rsmod.api.player.events.PlayerCombatPermission
import org.rsmod.api.player.events.PlayerDeathEvent
import org.rsmod.api.player.events.PlayerTeleportEvent
import org.rsmod.api.player.minigames.canEnterNightmareZone
import org.rsmod.api.player.minigames.completedNightmareZoneQuests
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.ui.ifCloseOverlay
import org.rsmod.api.player.ui.ifOpenOverlay
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.region.RegionRepository
import org.rsmod.api.repo.region.RegionTemplate
import org.rsmod.api.script.*
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.refs.interf.InterfaceReferences
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity.Companion.INVALID_SLOT
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.game.region.Region
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

internal object DreamLocs : LocReferences() {
    val coffer =
        listOf(find("nzone_lobby_moneypouch_locked"), find("nzone_lobby_moneypouch_unlocked"))
    val vial = listOf(find("nzone_lobby_vial_empty"), find("nzone_lobby_vial_full"))
    val chest = find("nzone_lobby_chest")
    val escape = find("nzone_arena_vial")
}

internal object DreamNpcs : NpcReferences() {
    val host = find("nzone_host")
}

internal object DreamBits : VarBitReferences() {
    val purchased = find("nzone_purchaseddream")
    val previous = find("nzone_previousdream")
    val cash = find("nzone_cash")
    val unlocked = find("nzone_moneypouch_unlocked")
    val current = find("nzone_currentpoints")
    val hardCA = find("ca_tier_status_hard")
}

internal object DreamVarps : VarpReferences() {
    val total = find("nzone_rewardpoints")
}

internal object DreamInterface : InterfaceReferences() {
    val game = find("nzone_game")
    val shop = find("nzone_rewards")
}

internal object DreamPopulation : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<NightmareZone>("lobby.toml")
    }
}

internal var Player.dreamPoints by intVarp(DreamVarps.total)
private var Player.purchase by intVarBit(DreamBits.purchased)
private var Player.coinsThousands by intVarBit(DreamBits.cash)
private var Player.earned by intVarBit(DreamBits.current)
private var Player.previous by intVarBit(DreamBits.previous)

private enum class Mode(val label: String, val fee: Int, val hard: Boolean, val rumble: Boolean) {
    Endurance("Normal Endurance", 1, false, false),
    HardEndurance("Hard Endurance", 5, true, false),
    Rumble("Normal Rumble", 2, false, true),
    HardRumble("Hard Rumble", 6, true, true),
}

private class Dream(
    val player: Player,
    val region: Region,
    val mode: Mode,
    val available: List<DreamBoss>,
    val vial: LocInfo,
) {
    var next = 0
    val foes = linkedMapOf<Npc, Pair<DreamBoss, Int>>()

    fun tile(x: Int, z: Int): CoordGrid = region.normal[0, 35, 73, x - 2240, z - 4672]
}

public class NightmareZone
@Inject
constructor(
    private val regions: RegionRepository,
    private val npcs: NpcRepository,
    private val types: NpcTypeList,
    private val locations: LocRepository,
    private val interactions: AiPlayerInteractions,
    private val random: GameRandom,
    private val events: EventBus,
    private val collision: CollisionFlagMap,
) : PluginScript() {
    private val dreams = mutableMapOf<Player, Dream>()
    private val owners = mutableMapOf<Npc, Dream>()

    override fun ScriptContext.startup() {
        onOpNpc1(DreamNpcs.host) { shopDream() }
        onOpNpc3(DreamNpcs.host) { shopDream() }
        for (coffer in DreamLocs.coffer) {
            onOpLoc1(coffer) { coffer() }
        }
        for (vial in DreamLocs.vial) onOpLoc1(vial) { enter() }
        onOpLoc1(DreamLocs.escape) {
            if (player in dreams && choice2("Yes, leave the dream.", true, "Stay here.", false)) {
                end(player)
                telejump(LOBBY)
            }
        }
        for (type in DreamCombatTypes.profiles.map { it.type }) onNpcQueue(type, queues.death) {
            noneMode()
            hideAllOps()
            delay(2)
            if (npc.slotId != INVALID_SLOT) npcs.del(npc, Int.MAX_VALUE)
        }
        onEvent<NpcKillEvents.Credited> {
            val dream = owners.remove(npc) ?: return@onEvent
            val (boss, phase) = dream.foes.remove(npc) ?: return@onEvent
            if (player !== dream.player) return@onEvent
            val forms = if (dream.mode.hard) boss.hard else boss.normal
            if (phase + 1 < forms.size) {
                spawn(dream, boss, phase + 1)
                return@onEvent
            }
            val full = if (dream.mode.hard) boss.hardPoints else boss.normalPoints
            val award =
                if (dream.mode.rumble)
                    (full * dream.available.size / dreamBosses.size).coerceAtLeast(1)
                else full
            player.earned = (player.earned + award).coerceAtMost(1_048_575)
            if (dream.mode.rumble) spawn(dream, dream.available[random.of(dream.available.size)], 0)
            else if (dream.next < dream.available.size)
                spawn(dream, dream.available[dream.next++], 0)
            else {
                player.mes("You have defeated every opponent in your Endurance dream.")
                end(player)
                move(player, LOBBY)
            }
        }
        onEvent<PlayerDeathEvent.ResolveRespawn> {
            if (player in dreams) {
                end(player)
                safeDestination = LOBBY
            }
        }
        onEvent<PlayerTeleportEvent.Start> { if (player in dreams) end(player) }
        onEvent<PlayerCombatPermission> {
            if (attacker in dreams || target in dreams) allowed = false
        }
        onPlayerLogout {
            if (player in dreams) {
                end(player)
                move(player, LOBBY)
            }
        }
        onPlayerLogin {
            if (player.earned > 0) {
                depositPoints(player)
                move(player, LOBBY)
            }
        }
    }

    private suspend fun ProtectedAccess.shopDream() {
        if (!player.canEnterNightmareZone()) {
            mes("Complete at least five quests with Nightmare Zone bosses to enter a dream.")
            return
        }
        vars[DreamBits.unlocked] = 1
        if (player.purchase > 0) {
            val mode = Mode.entries.getOrNull(player.purchase - 1)
            if (mode != null)
                mes("Your ${mode.label} dream is ready. Drink from the potion to begin.")
            return
        }
        val options = Mode.entries.map { "${it.label} (${it.fee},000 coins)" }
        val mode = Mode.entries.getOrNull(menu("Choose a dream", true, options)) ?: return
        if (player.coinsThousands < mode.fee) {
            mes("Deposit at least ${mode.fee},000 coins in Dominic's coffer.")
            return
        }
        player.coinsThousands -= mode.fee
        player.purchase = mode.ordinal + 1
        player.previous = player.purchase
        mes("Drink from the dream potion when you are ready.")
    }

    private suspend fun ProtectedAccess.coffer() {
        if (!player.members) return
        vars[DreamBits.unlocked] = 1
        when (choice3("Deposit coins.", 1, "Withdraw coins.", 2, "Check balance.", 3)) {
            1 -> deposit(countDialog("How many coins would you like to deposit?"))
            2 -> withdraw(countDialog("How many coins would you like to withdraw?"))
            else -> mes("There are ${player.coinsThousands*1000} coins in the coffer.")
        }
    }

    private fun ProtectedAccess.deposit(amount: Int) {
        if (!player.members) return
        val stacks =
            minOf(
                amount.coerceAtLeast(0) / 1000,
                invTotal(inv, objs.coins) / 1000,
                262143 - player.coinsThousands,
            )
        if (stacks < 1) {
            mes("Coins must be deposited in multiples of 1,000.")
            return
        }
        if (invDel(inv, objs.coins, stacks * 1000).success) {
            player.coinsThousands += stacks
            vars[DreamBits.unlocked] = 1
        }
    }

    private fun ProtectedAccess.withdraw(amount: Int) {
        if (!player.members) return
        val stacks = minOf(amount.coerceAtLeast(0) / 1000, player.coinsThousands)
        if (stacks < 1) return
        if (invAdd(inv, objs.coins, stacks * 1000).success) player.coinsThousands -= stacks
    }

    private fun ProtectedAccess.enter() {
        val mode = Mode.entries.getOrNull(player.purchase - 1)
        if (mode == null) {
            mes("Arrange a dream with Dominic Onion first.")
            return
        }
        if (player in dreams || !player.canEnterNightmareZone()) return
        val quests = player.completedNightmareZoneQuests()
        val available = dreamBosses.filter { it.quest in quests }
        if (available.isEmpty()) return
        val region =
            regions.add(
                RegionTemplate.create {
                    copy(280, 584, 0) {
                        zoneWidth = 8
                        zoneLength = 8
                        regionZoneX = 4
                        regionZoneZ = 4
                    }
                }
            ) ?: return
        val exit =
            locations.add(
                region.normal[0, 35, 73, 31, 9],
                DreamLocs.escape,
                Int.MAX_VALUE,
                LocAngle.West,
                LocShape.CentrepieceStraight,
            )
        val dream = Dream(player, region, mode, available, exit)
        dreams[player] = dream
        player.purchase = 0
        player.earned = 0
        telejump(dream.tile(2272, 4684))
        player.ifOpenOverlay(DreamInterface.game, events)
        if (mode.rumble) repeat(4) { spawn(dream, available[random.of(available.size)], 0) }
        else spawn(dream, available[dream.next++], 0)
    }

    private fun spawn(dream: Dream, boss: DreamBoss, phase: Int) {
        val id = (if (dream.mode.hard) boss.hard else boss.normal)[phase]
        val base = dream.tile(2270, 4687)
        val npc =
            Npc(
                types.getValue(id),
                base.translate(dream.foes.size % 2 * 3, dream.foes.size / 2 * 3),
            )
        npcs.add(npc, 36000)
        owners[npc] = dream
        dream.foes[npc] = boss to phase
        npc.opPlayer2(dream.player, interactions)
    }

    private fun end(player: Player) {
        val dream = dreams.remove(player) ?: return
        for (npc in dream.foes.keys) {
            owners.remove(npc)
            if (npc.slotId != INVALID_SLOT) npcs.del(npc, Int.MAX_VALUE)
        }
        locations.del(dream.vial, Int.MAX_VALUE)
        player.ifCloseOverlay(DreamInterface.game, events)
        depositPoints(player)
    }

    private fun depositPoints(player: Player) {
        val earned = player.earned
        player.dreamPoints =
            (player.dreamPoints.toLong() + earned).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        player.earned = 0
        if (earned > 0) player.mes("You earned $earned Nightmare Zone points.")
    }

    private fun move(player: Player, coords: CoordGrid) =
        PathingEntityCommon.telejump(player, collision, coords)

    private companion object {
        val LOBBY = CoordGrid(2606, 3115)
    }
}
