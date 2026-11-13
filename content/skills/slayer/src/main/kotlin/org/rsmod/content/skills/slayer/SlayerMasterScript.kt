package org.rsmod.content.skills.slayer

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.npc.LeafBladed
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.npc.events.NpcKillEvents
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onModifyNpcHit
import org.rsmod.api.script.onOpHeld2
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpc4
import org.rsmod.api.script.onOpNpc5
import org.rsmod.api.shops.Shops
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.content.skills.skillcapes.SkillcapeSale
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.type.npc.NpcType
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

object SlayerNpcs : NpcReferences() {
    val turael = find("slayer_master_1_tureal")
    val burthorpeMaster = find("slayer_master_1")
    val aya = find("slayer_master_1_aya")
    val mazchna = find("slayer_master_2")
    val mazchnaVisible = find("slayer_master_2_mazchna")
    val achtryn = find("slayer_master_2_achtryn_vis")
    val vannaka = find("slayer_master_3")
    val chaeldar = find("slayer_master_4")
    val duradel = find("slayer_master_5")
    val duradelVisible = find("slayer_master_5_duradel")
    val kuradal = find("slayer_master_5_kuradal")
    val nieve = find("slayer_master_6")
    val nieveVisible = find("slayer_master_nieve")
    val steve = find("slayer_master_steve")
    val krystilia = find("slayer_master_7")
    val konar = find("slayer_master_8")
    val spria = find("slayer_master_9")
    val spriaVisible = find("slayer_master_9_active")
}

internal object SlayerObjs : ObjReferences() {
    val gem = find("slayer_gem")
    val eternalGem = find("slayer_eternal_gem")
}

class SlayerMasterScript
@Inject
internal constructor(
    private val assignments: SlayerAssignments,
    private val dialogue: SlayerMasterDialogue,
    private val npcs: NpcRepository,
    private val npcTypes: NpcTypeList,
    private val players: PlayerList,
    private val shops: Shops,
    private val rewards: SlayerRewards,
) : PluginScript() {
    override fun ScriptContext.startup() {
        // Assignment operations are resolved against the player's current NPC morph.
        with(SlayerNpcs) {
            register(Master.Turael, CoordGrid(2930, 3536), burthorpeMaster, turael, aya)
            register(Master.Mazchna, CoordGrid(3511, 3509), mazchna, mazchnaVisible, achtryn)
            register(Master.Vannaka, CoordGrid(3146, 9914), vannaka)
            register(Master.Chaeldar, CoordGrid(2444, 4432), chaeldar)
            register(Master.Duradel, CoordGrid(2869, 2982, 1), duradel, duradelVisible, kuradal)
            register(Master.Nieve, CoordGrid(2432, 3423), nieve, nieveVisible, steve)
            register(Master.Krystilia, CoordGrid(3109, 3516), krystilia)
            register(Master.Konar, CoordGrid(1309, 3785), konar)
            register(Master.Spria, CoordGrid(3091, 3267), spria, spriaVisible)
        }
        for (gem in listOf(SlayerObjs.gem, SlayerObjs.eternalGem)) {
            onOpHeld2(gem) { assignments.checkTask(player) }
        }
        for (type in npcTypes.values.filter(LeafBladed::required)) {
            onModifyNpcHit(type) { if (!LeafBladed.canDamage(hit)) hit.damage = 0 }
        }
        onEvent<NpcKillEvents.Credited> {
            val hero = npc.findHero(players)
            if (hero == null || hero == player) {
                assignments.credit(player, npc)
            } else {
                assignments.credit(hero, npc, 0.5)
                assignments.credit(player, npc, 0.5, rollDrops = false)
            }
        }
    }

    private fun ScriptContext.register(
        master: Master,
        coords: CoordGrid,
        npc: NpcType,
        vararg morphs: NpcType,
    ) {
        // Cache map icon loc 5118 marks each master's position.
        spawn(npc, coords)
        master(npc, master)
        for (morph in morphs) master(morph, master)
    }

    private fun ScriptContext.master(npc: NpcType, master: Master) {
        onOpNpc1(npc) {
            if (master == Master.Konar) player.slayerContacts = player.slayerContacts or 2
            if (
                master == Master.Duradel &&
                    master.accessible(player) &&
                    player.statBase(stats.slayer) >= 99
            ) {
                when (
                    menu(
                        "What would you like?",
                        true,
                        listOf(
                            "I need a Slayer assignment.",
                            "Buy a Slayer cape and hood.",
                            "Maybe later.",
                        ),
                    )
                ) {
                    0 -> dialogue.request(this, master)
                    1 -> SkillcapeSale.offer(this, stats.slayer, objs.slayer_hood)
                }
                return@onOpNpc1
            }
            val accept = choice2("I need a Slayer assignment.", true, "Maybe later.", false)
            if (accept) dialogue.request(this, master)
        }
        onOpNpc3(npc) { dialogue.request(this, master) }
        onOpNpc5(npc) { rewards.open(this, master) }
        onOpNpc4(npc) {
            if (!player.members) {
                mes("You need to be on a members' world to buy Slayer equipment.")
                return@onOpNpc4
            }
            val inventory =
                if (player.vars[SlayerVarBits.porcine] >= 40) SlayerShops.afterPorcine
                else SlayerShops.regular
            shops.open(
                player,
                "Slayer Equipment",
                inventory,
                buyPercentage = 60.0,
                sellPercentage = 100.0,
                changePercentage = 0.1,
            )
        }
    }

    private fun spawn(type: NpcType, coords: CoordGrid) {
        if (npcs.findAll(coords).none { it.id == type.id }) {
            npcs.add(Npc(npcTypes[type], coords), Int.MAX_VALUE)
        }
    }
}

public class SlayerMasterDialogue
@Inject
internal constructor(private val assignments: SlayerAssignments) {
    public fun canContact(player: org.rsmod.game.entity.Player, master: Master): Boolean =
        when (master) {
            Master.Krystilia -> player.vars[SlayerVarps.contacts] and 1 != 0
            Master.Konar -> player.vars[SlayerVarps.contacts] and 2 != 0
            else -> true
        }

    public suspend fun request(access: ProtectedAccess, master: Master): Unit =
        with(access) {
            if (
                master == Master.Krystilia &&
                    player.slayerCount <= 0 &&
                    player.slayerContacts and 1 == 0
            ) {
                mes(
                    "These assignments must be completed in the Wilderness, where other players can attack you."
                )
                if (!choice2("Give me a Wilderness assignment.", true, "Maybe later.", false))
                    return@with
            }
            val replace = assignments.canReplace(player, master)
            if (replace) {
                val streak =
                    if (player.slayerMaster == 7) "Wilderness task streak" else "task streak"
                val confirm =
                    choice2(
                        "Reset my $streak and give me an easier task.",
                        true,
                        "Keep my current task.",
                        false,
                    )
                if (!confirm) return@with
            }
            val previous = assignments.offerRepeat(player, master)
            val repeat =
                previous != null &&
                    choice2(
                        "Assign me $previous again.",
                        true,
                        "Give me a normal assignment.",
                        false,
                    )
            if (!assignments.request(player, master, replace, repeat)) return@with
            if (player.slayerTarget == 98) {
                val cap = assignments.maximumBossCount(player)
                val boss = player.slayerBoss
                val count =
                    countDialog("How many would you like to kill? (3-$cap)").coerceIn(3, cap)
                if (
                    player.slayerTarget == 98 &&
                        player.slayerBoss == boss &&
                        player.slayerCount == player.originalSlayerCount
                ) {
                    player.originalSlayerCount = count
                    player.slayerCount = count
                    assignments.checkTask(player)
                }
            }
            val diary =
                when (master) {
                    Master.Mazchna -> DiaryTask.MazchnaAssignment
                    Master.Vannaka -> DiaryTask.VannakaAssignment
                    Master.Chaeldar -> DiaryTask.ChaeldarAssignment
                    Master.Duradel -> DiaryTask.DuradelAssignment
                    else -> return@with
                }
            player.completeDiaryTask(diary)
        }
}
