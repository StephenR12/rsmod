package org.rsmod.content.travel.diaryroutes

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.*
import org.rsmod.api.npc.events.NpcKillEvents
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.events.KingdomContribution
import org.rsmod.api.player.events.KingdomWork
import org.rsmod.api.player.events.PlayerStallTheftEvent
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.*
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.entity.Player
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object MiscNpcs : NpcReferences() {
    val advisor = find("misc_advisor_ghrim")
    val florist = find("misc_flowergirl")
    val miner = find("misc_miner")
    val lumberjack = find("misc_lumberjack")
    val fisherman = find("misc_fisherman")
    val gardener = find("misc_gardener")
    val farmer = find("misc_etc_farmer")
    val citizens = ((1..7).map { find("misc_man_$it") } + (1..6).map { find("misc_woman_$it") })
}

internal object MiscItems : ObjReferences() {
    val flowers = find("flowers_waterfall_quest_mixed")
    val weeds = find("weeds")
    val gems = listOf("sapphire", "emerald", "ruby", "diamond").map { find("uncut_$it") }
}

internal object MiscGardens : LocReferences() {
    val weeds =
        listOf("misc_flax", "misc_herb", "misc_weedingpatch").map { prefix ->
            listOf("heavyweeds", "medweeds", "lightweeds", "multiloc").map {
                find("${prefix}_${it}")
            }
        }
    val dry = find("misc_wateringpatch_multiloc")
    val bare = find("misc_compostpatch_multiloc")
}

internal object MiscSeqs : SeqReferences() {
    val rake = find("farming_raking")
    val water = find("farming_watering")
}

internal object MiscBits : VarBitReferences() {
    val approval = find("misc_approval")
    val royal = find("royal_quest")
    val medium = find("fremennik_diary_medium_complete")
    val elite = find("fremennik_diary_elite_complete")
    val weeds =
        listOf("misc_flax_multivar", "misc_herb_multivar", "misc_weedingpatch_multivar").map {
            find(it)
        }
    val water = find("misc_wateringpatch_multivar")
    val compost = find("misc_compostpatch_multivar")
}

internal object MiscSaves : VarpReferences() {
    val throne = find("misc_quest")
    val day = find("misc_last_approval_day")
    val garden = find("misc_last_garden_minute")
}

internal object MiscSaveBuilder : VarpBuilder() {
    init {
        build("misc_last_approval_day") { transmitNever = true }
        build("misc_last_garden_minute") { transmitNever = true }
    }
}

public class KingdomApproval
@Inject
constructor(private val rng: GameRandom, private val npcs: NpcRepository) : PluginScript() {
    private val cans =
        listOf(
            objs.watering_can_0,
            objs.watering_can_1,
            objs.watering_can_2,
            objs.watering_can_3,
            objs.watering_can_4,
            objs.watering_can_5,
            objs.watering_can_6,
            objs.watering_can_7,
            objs.watering_can_8,
        )

    override fun ScriptContext.startup() {
        onPlayerLogin { refresh(player) }
        onEvent<KingdomContribution> { credit(player, work) }
        onEvent<PlayerStallTheftEvent> {
            if (
                !caught &&
                    reward != null &&
                    stall.level == 0 &&
                    stall.x in 2507..2528 &&
                    stall.z in 3858..3872 &&
                    player.vars[MiscSaves.throne] >= 90
            ) {
                refresh(player)
                VarPlayerIntMapSetter.set(
                    player,
                    MiscBits.approval,
                    (player.vars[MiscBits.approval] - 4).coerceAtLeast(0),
                )
            }
        }
        onEvent<NpcKillEvents.Credited> {
            if (
                npc.coords.level == 0 &&
                    npc.coords.x in 2490..2560 &&
                    npc.coords.z in 3835..3900 &&
                    MiscNpcs.citizens.any { it.id == npc.id } &&
                    player.vars[MiscSaves.throne] >= 90
            ) {
                refresh(player)
                VarPlayerIntMapSetter.set(
                    player,
                    MiscBits.approval,
                    (player.vars[MiscBits.approval] - 6).coerceAtLeast(0),
                )
            }
        }
        for (n in
            listOf(
                MiscNpcs.advisor,
                MiscNpcs.gardener,
                MiscNpcs.miner,
                MiscNpcs.lumberjack,
                MiscNpcs.fisherman,
                MiscNpcs.farmer,
            )) {
            onOpNpc1(n) { report() }
        }
        onOpNpc1(MiscNpcs.florist) {
            val buy =
                choice2(
                    "Buy mixed flowers for 15 coins.",
                    true,
                    "Ask about my approval rating.",
                    false,
                )
            if (!buy) {
                report()
                return@onOpNpc1
            }
            if (
                player
                    .invTransaction(inv) {
                        val bag = select(inv)
                        delete(bag, objs.coins.id, 15)
                        add(bag, MiscItems.flowers.id, 1)
                    }
                    .success
            ) {
                if (vars[MiscSaves.throne] >= 90) credit(player, KingdomWork.Flowers)
            } else mes("You need 15 coins and room for the flowers.")
        }
        for (gem in MiscItems.gems) onOpNpcU(MiscNpcs.miner, gem) {
            refresh(player)
            if (KingdomWork.Coal.permitted(player) && invDel(inv, gem, slot = it.invSlot).success)
                credit(player, KingdomWork.Coal, 2)
        }
        for ((index, stages) in MiscGardens.weeds.withIndex()) for (patch in stages) {
            onOpLoc1(patch) { rake(index) }
            onApLoc1(patch) { if (isWithinApRange(it.loc, 1)) rake(index) }
            onOpLocU(patch, objs.rake) { rake(index) }
        }
        for (i in 1..8) onOpLocU(MiscGardens.dry, cans[i]) {
            refresh(player)
            if (!gardenAccess(true) || vars[MiscBits.water] >= 2) return@onOpLocU
            if (
                !player
                    .invTransaction(inv) {
                        val bag = select(inv)
                        delete(bag, cans[i].id, 1)
                        add(bag, cans[i - 1].id, 1)
                    }
                    .success
            )
                return@onOpLocU
            anim(MiscSeqs.water)
            vars[MiscBits.water]++
            credit(player, KingdomWork.EasternGardens)
        }
        onOpLocU(MiscGardens.bare, objs.bucket_of_compost) {
            refresh(player)
            if (!gardenAccess(true) || vars[MiscBits.compost] != 0) return@onOpLocU
            if (
                !player
                    .invTransaction(inv) {
                        val bag = select(inv)
                        delete(bag, objs.bucket_of_compost.id, 1)
                        add(bag, objs.bucket_empty.id, 1)
                    }
                    .success
            )
                return@onOpLocU
            vars[MiscBits.compost] = 1
            credit(player, KingdomWork.EasternGardens)
        }
    }

    private fun ProtectedAccess.report() {
        refresh(player)
        if (vars[MiscSaves.throne] < 90) {
            mes("The king has not asked you to gain the support of his people.")
            return
        }
        mes("Your subjects' approval rating is ${percent(player)}%.")
    }

    private fun ProtectedAccess.gardenAccess(east: Boolean): Boolean {
        if (stat(stats.farming) < 10) {
            mes("You need a Farming level of 10 to tend this patch.")
            return false
        }
        return (if (east) KingdomWork.EasternGardens else KingdomWork.Gardens).permitted(player)
    }

    private suspend fun ProtectedAccess.rake(index: Int) {
        refresh(player)
        if (!gardenAccess(index == 2)) return
        if (objs.rake !in inv && objs.rake !in worn) {
            mes("You need a rake to remove the weeds.")
            return
        }
        val patch = MiscBits.weeds[index]
        while (vars[patch] < 3 && vars[MiscBits.approval] < 127) {
            if (inv.isFull()) {
                mes("You need space for the weeds.")
                return
            }
            anim(MiscSeqs.rake)
            delay(3)
            if (vars[patch] >= 3 || !invAdd(inv, MiscItems.weeds).success) break
            vars[patch]++
            credit(player, if (index == 2) KingdomWork.EasternGardens else KingdomWork.Gardens)
        }
        resetAnim()
    }

    private fun credit(player: Player, work: KingdomWork, amount: Int = 1) {
        refresh(player)
        if (!work.permitted(player, false)) return
        val bonus =
            when {
                player.vars[MiscBits.elite] != 0 -> 15
                player.vars[MiscBits.medium] != 0 -> 10
                else -> 0
            }
        if (work.chance < 100 && rng.of(100) >= work.chance + bonus) return
        val approval = (player.vars[MiscBits.approval] + amount).coerceAtMost(127)
        VarPlayerIntMapSetter.set(player, MiscBits.approval, approval)
        val worker =
            when (work) {
                KingdomWork.Coal -> MiscNpcs.miner
                KingdomWork.Maple,
                KingdomWork.Hardwood -> MiscNpcs.lumberjack
                KingdomWork.Fish -> MiscNpcs.fisherman
                KingdomWork.Gardens -> MiscNpcs.gardener
                KingdomWork.EasternGardens -> MiscNpcs.farmer
                KingdomWork.Flowers -> MiscNpcs.florist
            }
        npcs
            .findAll(ZoneKey.from(player.coords), 2)
            .firstOrNull { it.id == worker.id }
            ?.say("Thanks!")
        if (approval == 127 && player.vars[MiscSaves.throne] >= 100)
            player.completeDiaryTask(DiaryTask.SupportMiscellania)
    }

    private fun percent(p: Player): Int = (p.vars[MiscBits.approval] * 100 / 127).coerceAtLeast(0)

    private fun refresh(p: Player) {
        if (!p.members || p.vars[MiscSaves.throne] < 90) return
        val minutes = (System.currentTimeMillis() / 60_000).toInt()
        val today = minutes / 1440
        val old = p.vars[MiscSaves.day]
        if (old > 0 && old < today && p.vars[MiscSaves.throne] >= 100) {
            val elapsed = (today - old).coerceAtMost(128)
            val decay = if (p.vars[MiscBits.royal] >= 30) elapsed else (elapsed * 127 + 39) / 40
            VarPlayerIntMapSetter.set(
                p,
                MiscBits.approval,
                (p.vars[MiscBits.approval] - decay).coerceAtLeast(32),
            )
        }
        VarPlayerIntMapSetter.set(p, MiscSaves.day, today)
        val garden = p.vars[MiscSaves.garden]
        val growth = if (garden > 0) (minutes - garden) / 5 else 0
        if (growth > 0) {
            for (bit in
                MiscBits.weeds + listOf(MiscBits.water, MiscBits.compost)) VarPlayerIntMapSetter
                .set(p, bit, (p.vars[bit] - growth).coerceAtLeast(0))
        }
        if (garden == 0 || growth > 0) VarPlayerIntMapSetter.set(p, MiscSaves.garden, minutes)
    }
}
