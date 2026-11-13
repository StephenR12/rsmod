package org.rsmod.content.skills.combattraining

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.npc.inWildernessGodWars
import org.rsmod.api.combat.commons.npc.queueCombatRetaliate
import org.rsmod.api.config.constants
import org.rsmod.api.hunt.Hunt
import org.rsmod.api.npc.events.NpcKillEvents
import org.rsmod.api.npc.isOutOfCombat
import org.rsmod.api.player.diary.DiaryDifficulty
import org.rsmod.api.player.diary.DiaryRegion
import org.rsmod.api.player.isValidTarget
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onNpcTimer
import org.rsmod.api.script.onOpObj3
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.timer.TimerReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.content.skills.prayer.remains.PrayerGodWarsAlignment
import org.rsmod.content.skills.prayer.remains.PrayerGodWarsGod
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.game.type.hunt.HuntVis
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object WildernessFollowerTimer : TimerReferences() {
    val hunt = find("wilderness_godwars_hunt")
}

internal object WildernessFollowerKeys : ObjReferences() {
    val key = find("ecumenical_key")
}

internal object WildernessFollowerRewards : VarBitReferences() {
    val tiers = listOf("hard", "elite", "master", "grandmaster").map { find("ca_tier_status_$it") }
}

class WildernessGodWarsFollowers
@Inject
constructor(
    private val hunt: Hunt,
    private val alignment: PrayerGodWarsAlignment,
    private val objs: ObjRepository,
    private val random: GameRandom,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onEvent<NpcStateEvents.Create> { startHunting(npc) }
        onEvent<NpcStateEvents.Respawn> { startHunting(npc) }
        onNpcTimer(WildernessFollowerTimer.hunt) {
            val god = followerAllegiance(npc.id) ?: return@onNpcTimer
            if (npc.hitpoints <= 0 || !npc.isOutOfCombat()) return@onNpcTimer
            val player =
                hunt.findPlayers(npc.coords, 5, HuntVis.LineOfSight).firstOrNull {
                    it.members &&
                        (it.coords.inWildernessGodWars() || inMainDungeon(it.coords)) &&
                        it.isValidTarget() &&
                        alignment.count(it, god) == 0
                } ?: return@onNpcTimer
            npc.queueCombatRetaliate(player)
        }
        onEvent<NpcKillEvents.Credited> {
            if (!npc.spawnCoords.inWildernessGodWars() || followerAllegiance(npc.id) == null)
                return@onEvent
            val rate = 60 - 5 * WildernessFollowerRewards.tiers.count { player.vars[it] >= 2 }
            if (random.of(rate) == 0)
                objs.add(
                    WildernessFollowerKeys.key,
                    npc.coords,
                    constants.lootdrop_duration,
                    player,
                )
        }
        onOpObj3(WildernessFollowerKeys.key) {
            val key = WildernessFollowerKeys.key
            val cap =
                when {
                    player.vars[DiaryRegion.Wilderness.tier(DiaryDifficulty.Hard).complete] != 0 ->
                        5
                    player.vars[DiaryRegion.Wilderness.tier(DiaryDifficulty.Medium).complete] !=
                        0 -> 4
                    else -> 3
                }
            if (invTotal(inv, key) + invTotal(bank, key) >= cap) {
                if (objs.del(it.obj)) mes("You already have enough of these.")
                return@onOpObj3
            }
            val take = invAdd(inv, key, 1, autoCommit = false)
            if (take.failure) mes("You do not have enough inventory space.")
            else if (objs.del(it.obj)) take.commitAll()
        }
    }

    private fun followerAllegiance(id: Int): PrayerGodWarsGod? =
        if (id in 2205..2208 || id in 2215..2218 || id in 3129..3132 || id in 3162..3165) null
        else godWarsAllegiance(id)

    private fun startHunting(npc: Npc) {
        if (
            (!npc.spawnCoords.inWildernessGodWars() && !inMainDungeon(npc.spawnCoords)) ||
                followerAllegiance(npc.id) == null
        )
            return
        npc.setHunt(0)
        npc.timer(WildernessFollowerTimer.hunt, 3)
    }
}

internal fun godWarsAllegiance(id: Int): PrayerGodWarsGod? =
    when (id) {
        in 2205..2208 -> PrayerGodWarsGod.Saradomin
        in 2215..2218 -> PrayerGodWarsGod.Bandos
        in 3129..3132 -> PrayerGodWarsGod.Zamorak
        in 3162..3165 -> PrayerGodWarsGod.Armadyl
        in 2209..2214 -> PrayerGodWarsGod.Saradomin
        in 2233..2249 -> PrayerGodWarsGod.Bandos
        in 3133..3141,
        in 3159..3161 -> PrayerGodWarsGod.Zamorak
        in 3166..3183 -> PrayerGodWarsGod.Armadyl
        else -> null
    }
