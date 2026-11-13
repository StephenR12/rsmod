package org.rsmod.content.skills.thieving

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.WeakHashMap
import org.rsmod.api.combat.commons.npc.queueCombatRetaliate
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.queues
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.*
import org.rsmod.api.npc.isValidTarget
import org.rsmod.api.player.clearInteractionRoute
import org.rsmod.api.player.isInCombat
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.player.stat.thievingLvl
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.*
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public object BlackjackBits : VarBitReferences() {
    val unlocked = find("feud_npc_multi")
    val quest = find("feud_var")
}

public object BlackjackNpcs : NpcReferences() {
    val seller = find("feud_bjseller")
    val sword = find("feud_arabian_guard_multi")
    val club = find("feud_arabian_guard2_multi")
    val thug = find("feud_egyptian_doorman_multi")
}

public object BlackjackObjs : ObjReferences() {
    val willow = find("blackjack_willow")
    val all =
        listOf(
                "blackjack_oak",
                "blackjack_willow",
                "roguetrader_bj_assault_oak",
                "roguetrader_bj_defend_oak",
                "roguetrader_bj_assault_willow",
                "roguetrader_bj_defend_willow",
                "roguetrader_bj_maple",
                "roguetrader_bj_assault_maple",
                "roguetrader_bj_defend_maple",
            )
            .map(::find)
}

public object BlackjackSeqs : SeqReferences() {
    val strike = find("human_blunt_pound")
    val unconscious = find("stunned_blackjack")
}

/** Both bandits have guaranteed knockout loot; awake pickpocket rates remain unpublished. */
public object BlackjackTargets {
    val bearded =
        StandardPickpockets.menaphiteThug.copy(
            name = "bandit",
            npcs = ThievingNpcs.beardedPollnivneachBandits,
            level = 45,
            xp = 65.0,
            rewards =
                listOf(
                    PickpocketReward(
                        ThievingObjs.bandit2Pouch,
                        coinPouch = CoinPouchDefinition(ThievingObjs.bandit2Pouch, coins = 40),
                    )
                ),
        )
    val sword =
        StandardPickpockets.menaphiteThug.copy(
            name = "bandit",
            npcs = ThievingNpcs.pollnivneachBandits,
            level = 55,
            xp = 84.3,
            rewards =
                listOf(
                    PickpocketReward(
                        ThievingObjs.banditPouch,
                        coinPouch = CoinPouchDefinition(ThievingObjs.banditPouch, coins = 50),
                    )
                ),
        )
    val all = listOf(bearded, sword, StandardPickpockets.menaphiteThug)
}

@Singleton
public class BlackjackingState @Inject constructor() {
    private data class Knockout(val player: Player, val expires: Int, var picks: Int = 2)

    private val knocked = WeakHashMap<Npc, Knockout>()
    private val anger = WeakHashMap<Npc, Player>()

    public fun active(npc: Npc, clock: Int): Boolean {
        val state = knocked[npc] ?: return false
        if (clock >= state.expires || !npc.isValidTarget()) {
            knocked.remove(npc)
            return false
        }
        return true
    }

    public fun owns(player: Player, npc: Npc): Boolean =
        active(npc, player.currentMapClock) && knocked[npc]?.player === player

    public fun knock(player: Player, npc: Npc) {
        knocked[npc] = Knockout(player, player.currentMapClock + 5)
    }

    public fun picked(npc: Npc) {
        val state = knocked[npc] ?: return
        if (--state.picks == 0) knocked.remove(npc)
    }

    public fun angry(player: Player, npc: Npc) {
        anger[npc] = player
    }

    public fun interrupt(player: Player, npc: Npc) {
        if (anger[npc] !== player) return
        npc.clearQueue(queues.com_retaliate_player)
        npc.resetMode()
        anger.remove(npc)
    }

    public fun clear(npc: Npc) {
        knocked.remove(npc)
        anger.remove(npc)
    }

    public fun clear(player: Player) {
        knocked.entries.removeIf { it.value.player === player }
        anger.entries.removeIf { it.value === player }
    }
}

internal object BlackjackPeople : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<Blackjacking>("blackjacking.toml")
    }
}

public class Blackjacking
@Inject
constructor(
    private val state: BlackjackingState,
    private val pickpocket: Pickpocketing,
    private val npcs: NpcRepository,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (def in BlackjackTargets.all) for (npc in
            def.npcs.filter { it.id == 735 || it.id == 737 || it.id == 3550 }) {
            onOpNpc5(npc) { knock(this, it.npc, def) }
            onApNpc4(npc) {
                if (isWithinApRange(it.npc, 8) && lineOfSight(player.coords, it.npc.coords)) {
                    lure(this, it.npc, def)
                    player.clearInteractionRoute()
                    player.clearInteraction()
                }
            }
            onOpNpc4(npc) { lure(this, it.npc, def) }
            if (def !== StandardPickpockets.menaphiteThug)
                onOpNpc3(npc) {
                    state.interrupt(player, it.npc)
                    if (state.owns(player, it.npc)) pickpocket.attempt(this, it.npc, def)
                    else mes("Knock out the bandit before attempting to steal from him.")
                }
        }
        onOpNpc1(BlackjackNpcs.seller) { seller() }
        onOpNpc3(BlackjackNpcs.seller) { seller() }
        onEvent<NpcStateEvents.Delete> { state.clear(npc) }
        onPlayerLogout { state.clear(player) }
    }

    public fun eligible(access: ProtectedAccess, def: PickpocketDefinition): Boolean =
        with(access) {
            if (!player.members) return false
            // Native multi340 index2 unlocks the Knock-Out/Lure morphs for all three targets.
            if (vars[BlackjackBits.unlocked] < 2) {
                mes("You must learn how to use a blackjack during The Feud first.")
                return false
            }
            if (player.thievingLvl < def.level) {
                mes("You need a Thieving level of ${def.level} to do that.")
                return false
            }
            if (BlackjackObjs.all.none { it.id == player.righthand?.id }) {
                mes("You need to wield a blackjack to do that.")
                return false
            }
            true
        }

    public fun knock(access: ProtectedAccess, npc: Npc, def: PickpocketDefinition): Unit =
        with(access) {
            if (
                !eligible(this, def) ||
                    !npc.isValidTarget() ||
                    !isWithinDistance(npc, 1) ||
                    actionDelay > mapClock ||
                    state.active(npc, mapClock)
            )
                return
            if (player.isInCombat()) {
                mes("You can't knock-out the ${def.name.lowercase()} during combat.")
                return
            }
            val banditIds = setOf(734, 735, 736, 737, 1975, 1976)
            if (
                def !== StandardPickpockets.menaphiteThug &&
                    npcs.findAll(ZoneKey.from(npc.coords), 1).any {
                        it !== npc &&
                            it.id in banditIds &&
                            it.isValidTarget() &&
                            it.isWithinDistance(player, 10) &&
                            lineOfSight(player.coords, it.coords)
                    }
            ) {
                mes("Perhaps I shouldn't do this here, I think another bandit will see me.")
                return
            }
            faceEntitySquare(npc)
            anim(BlackjackSeqs.strike)
            actionDelay = mapClock + 1
            // Shared Bandit14990770/Blackjack14898817 chart; Menaphite14978160 has a conflicting78
            // low.
            if (statRandom(stats.thieving, 80, 240, 0)) {
                state.interrupt(player, npc)
                npc.resetMovement()
                npc.wanderIdleCycles = 5
                npc.actionDelay = mapClock + 5
                npc.anim(BlackjackSeqs.unconscious)
                state.knock(player, npc)
                statAdvance(stats.thieving, 10.0)
                mes("You knock the ${def.name.lowercase()} unconscious.")
            } else {
                npc.say("I'll kill you for that!")
                // Unpublished aggression probability: half of failed attempts retaliate in this
                // playable model.
                if (random.of(2) == 0) {
                    state.angry(player, npc)
                    npc.queueCombatRetaliate(player)
                }
            }
        }

    public fun lure(access: ProtectedAccess, npc: Npc, def: PickpocketDefinition): Unit =
        with(access) {
            if (
                !eligible(this, def) ||
                    !npc.isValidTarget() ||
                    state.active(npc, mapClock) ||
                    !isWithinApRange(npc, 8) ||
                    !lineOfSight(player.coords, npc.coords)
            )
                return
            state.interrupt(player, npc)
            npc.say("What do you want?")
            npc.walk(player.coords)
        }

    private suspend fun ProtectedAccess.seller() {
        if (!player.members || vars[BlackjackBits.quest] < 28) {
            mes("The seller will deal with you after The Feud.")
            return
        }
        if (choice2("Buy a willow blackjack for 600 coins.", true, "No, thank you.", false))
            buy(this)
    }

    public fun buy(access: ProtectedAccess): Boolean =
        with(access) {
            if (!player.members || vars[BlackjackBits.quest] < 28) return false
            val tx =
                player.invTransaction(inv) {
                    val i = select(inv)
                    delete(i, objs.coins.id, 600)
                    add(i, BlackjackObjs.willow.id, 1)
                }
            if (!tx.success) mes("You need 600 coins and enough inventory space.")
            tx.success
        }
}
