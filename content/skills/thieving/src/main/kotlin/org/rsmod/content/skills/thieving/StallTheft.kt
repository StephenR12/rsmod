package org.rsmod.content.skills.thieving

import org.rsmod.api.player.diary.recordDiaryStall
import org.rsmod.api.player.events.PlayerStallTheftEvent
import jakarta.inject.Inject
import org.rsmod.content.skills.pets.SkillPetRates
import jakarta.inject.Singleton
import org.rsmod.api.combat.commons.npc.queueCombatRetaliate
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.npc.isValidTarget
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.thievingLvl
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.entity.Npc
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.loc.LocType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public object StallLocs : LocReferences() {
    val rellekkaFish = find("viking_fish_market")
    val rellekkaEmpty = find("viking_market")
    val empty = find("market")
    val dwarfEmpty = find("dwarf_market_empty_stall")
    val hosidiusEmpty = find("hos_stall_empty")
    val prifddinasEmpty = find("prif_marketstall_empty")
    val fortisEmpty = find("fortis_market_stall")
    val tea = find("tea_stall")
    val fruit = find("hos_fruit_stall_02")
    val fruitEmpty = find("hos_fruit_stall")
    val bakery = find("cakethiefstall")
    val dwarfBakery = find("dwarf_market_bakery")
    val hosidiusBakery = find("hos_stall_bread")
    val fortisBakery = find("fortis_market_stall_bakers")
    val silk = find("silkthiefstall")
    val prifddinasSilk = find("prif_marketstall_silk")
    val fortisSilk = find("fortis_market_stall_silk")
    val silver = find("silverthiefstall")
    val dwarfSilver = find("dwarf_market_silver")
    val prifddinasSilver = find("prif_marketstall_silver")
    val spice = find("spicethiefstall")
    val prifddinasSpice = find("prif_marketstall_spice")
    val fortisSpice = find("fortis_market_stall_spice")
    val gems = find("gemthiefstall")
    val dwarfGems = find("dwarf_market_gems")
    val prifddinasGems = find("prif_marketstall_gem")
    val fortisGems = find("fortis_market_stall_gems")
}

public object StallQuestVars : VarpReferences() { val trials = find("viking") }

public object StallNpcs : NpcReferences() {
    val teaSeller = find("tea_seller")
    val baker = find("baker_merchant")
    val ardougneBaker = find("baker_merchant_ardougne")
    val ardougneBaker2 = find("baker_merchant_ardougne2")
    val dwarfBaker = find("dwarf_city_shop_bakery")
    val fortisBaker = find("fortis_shop_baker")
    val silkMerchant = find("silk_merchant")
    val gemMerchant = find("gem_merchant")
    val spiceMerchant = find("spice_merchant")
}

public object StallObjs : ObjReferences() {
    val chocolateSlice = find("chocolate_slice")
    val silk = find("silk")
    val lemon = find("lemon")
    val lime = find("lime")
    val pineapple = find("pineapple")
    val strangeFruit = find("macro_triffidfruit")
    val papaya = find("papaya")
}

public data class StallLoot(val obj: ObjType, val weight: Int)

public data class StallDefinition(
    val full: LocType,
    val empty: LocType,
    val level: Int,
    val xp: Double,
    val restockTicks: Int,
    val loot: List<StallLoot>,
    val ownerIds: Set<Int> = emptySet(),
    val operation: Int = 2,
) {
    val totalWeight: Int = loot.sumOf { it.weight }

    public fun reward(roll: Int): ObjType {
        require(roll in 0 until totalWeight)
        var remaining = roll
        for (entry in loot) {
            if (remaining < entry.weight) return entry.obj
            remaining -= entry.weight
        }
        error("Stall loot weight mismatch")
    }
}

public object OrdinaryStalls {
    val all: List<StallDefinition> by lazy {
        val bakeryLoot =
            listOf(
                StallLoot(objs.bread, 5),
                StallLoot(objs.cake, 13),
                StallLoot(StallObjs.chocolateSlice, 2),
            )
        val gemLoot =
            listOf(
                StallLoot(objs.uncut_sapphire, 105),
                StallLoot(objs.uncut_emerald, 17),
                StallLoot(objs.uncut_ruby, 5),
                StallLoot(objs.uncut_diamond, 1),
            )
        val silverLoot =
            listOf(
                StallLoot(objs.silver_ore, 16),
                StallLoot(objs.silver_bar, 3),
                StallLoot(objs.tiara, 1),
            )
        val bakers =
            setOf(
                StallNpcs.baker.id,
                StallNpcs.ardougneBaker.id,
                StallNpcs.ardougneBaker2.id,
                StallNpcs.dwarfBaker.id,
                StallNpcs.fortisBaker.id,
            )
        buildList {
            // Fish stall oldid14907500; exact 14:5:1 loot and twelve-cycle restock.
            add(StallDefinition(StallLocs.rellekkaFish, StallLocs.rellekkaEmpty, 42, 42.0, 12, listOf(StallLoot(objs.raw_salmon,14),StallLoot(objs.raw_tuna,5),StallLoot(objs.raw_lobster,1)), operation=2))
            // Fruit Stall oldid14906888 (2025-05-23); Kourend favour was removed in 2024.
            add(StallDefinition(StallLocs.fruit, StallLocs.fruitEmpty, 25, 28.5, 4, listOf(
                StallLoot(objs.cooking_apple, 40), StallLoot(objs.banana, 20), StallLoot(objs.jangerberries, 5), StallLoot(StallObjs.lemon, 5), StallLoot(objs.redberries, 5), StallLoot(StallObjs.pineapple, 5), StallLoot(StallObjs.lime, 5), StallLoot(objs.strawberry, 7), StallLoot(StallObjs.strangeFruit, 5), StallLoot(objs.golovanova_fruit_top, 2), StallLoot(StallObjs.papaya, 1)), operation = 2))
            add(
                StallDefinition(
                    StallLocs.tea,
                    StallLocs.empty,
                    5,
                    16.0,
                    4,
                    listOf(StallLoot(objs.cup_of_tea, 1)),
                    setOf(StallNpcs.teaSeller.id),
                )
            )
            for ((full, empty, ticks) in
                listOf(
                    Triple(StallLocs.bakery, StallLocs.empty, 4),
                    Triple(StallLocs.dwarfBakery, StallLocs.dwarfEmpty, 16),
                    Triple(StallLocs.hosidiusBakery, StallLocs.hosidiusEmpty, 4),
                    Triple(StallLocs.fortisBakery, StallLocs.fortisEmpty, 4),
                )) {
                add(StallDefinition(full, empty, 5, 16.0, ticks, bakeryLoot, bakers))
            }
            for ((full, empty) in
                listOf(
                    StallLocs.silk to StallLocs.empty,
                    StallLocs.prifddinasSilk to StallLocs.prifddinasEmpty,
                    StallLocs.fortisSilk to StallLocs.fortisEmpty,
                )) {
                add(
                    StallDefinition(
                        full,
                        empty,
                        20,
                        24.0,
                        8,
                        listOf(StallLoot(StallObjs.silk, 1)),
                        setOf(StallNpcs.silkMerchant.id),
                    )
                )
            }
            for ((full, empty) in
                listOf(
                    StallLocs.silver to StallLocs.empty,
                    StallLocs.dwarfSilver to StallLocs.dwarfEmpty,
                    StallLocs.prifddinasSilver to StallLocs.prifddinasEmpty,
                )) {
                add(
                    StallDefinition(
                        full,
                        empty,
                        50,
                        205.0,
                        32,
                        if (full == StallLocs.dwarfSilver) listOf(StallLoot(objs.silver_ore, 1))
                        else silverLoot,
                    )
                )
            }
            for ((full, empty) in
                listOf(
                    StallLocs.spice to StallLocs.empty,
                    StallLocs.prifddinasSpice to StallLocs.prifddinasEmpty,
                    StallLocs.fortisSpice to StallLocs.fortisEmpty,
                )) {
                add(
                    StallDefinition(
                        full,
                        empty,
                        65,
                        92.0,
                        10,
                        listOf(StallLoot(objs.spice, 1)),
                        setOf(StallNpcs.spiceMerchant.id),
                    )
                )
            }
            for ((full, empty) in
                listOf(
                    StallLocs.gems to StallLocs.empty,
                    StallLocs.dwarfGems to StallLocs.dwarfEmpty,
                    StallLocs.prifddinasGems to StallLocs.prifddinasEmpty,
                    StallLocs.fortisGems to StallLocs.fortisEmpty,
                )) {
                add(
                    StallDefinition(
                        full,
                        empty,
                        75,
                        408.0,
                        100,
                        gemLoot,
                        setOf(StallNpcs.gemMerchant.id),
                    )
                )
            }
        }
    }
}

/**
 * Ordinary guaranteed-success stall core; detection range/facing remains a client-capture boundary.
 */
@Singleton
public class StallTheft
@Inject
constructor(private val petRates: SkillPetRates, private val locRepo: LocRepository, private val npcRepo: NpcRepository) {
    public suspend fun steal(
        access: ProtectedAccess,
        loc: BoundLocInfo,
        definition: StallDefinition,
    ) {
        with(access) {
            if (!player.members) {
                mes("You need to be on a members' world to steal from stalls.")
                return
            }
            if (definition.full == StallLocs.rellekkaFish && vars[StallQuestVars.trials] < 10) {
                mes("You must complete The Fremennik Trials before stealing from this stall.")
                return
            }
            if (player.thievingLvl < definition.level) {
                mes("You need a Thieving level of ${definition.level} to steal from this stall.")
                return
            }
            if (player.inv.isFull()) {
                mes("You don't have enough inventory space.")
                return
            }
            if (locRepo.findExact(loc.coords, definition.full) == null) return
            faceLoc(loc)
            anim(seqs.human_pickpocket)
            delay(STEAL_TICKS)
            if (
                !isWithinDistance(loc, distance = 1) ||
                    locRepo.findExact(loc.coords, definition.full) == null ||
                    player.thievingLvl < definition.level ||
                    !player.members
            ) {
                resetAnim()
                return
            }
            val nearby =
                npcRepo
                    .findAll(ZoneKey.from(loc.coords), zoneRadius = 1)
                    .filter { it.isValidTarget() && it.isWithinDistance(player, DETECTION_RANGE) }
                    .toList()
            val witness =
                nearby.firstOrNull {
                    (it.id in definition.ownerIds || it.isStallGuard(definition)) &&
                        lineOfSight(it.coords, player.coords)
                }
            if (witness != null) {
                publish(PlayerStallTheftEvent(player,loc,reward=null,caught=true))
                witness.say("Guards! Guards!")
                val guard =
                    if (witness.isStallGuard(definition)) witness
                    else nearby.firstOrNull { it.isStallGuard(definition) }
                guard?.queueCombatRetaliate(player)
                mes("You've been spotted trying to steal from the stall.")
                resetAnim()
                return
            }
            val output = definition.reward(random.of(definition.totalWeight))
            val result = player.invTransaction(player.inv) { add(select(player.inv), output.id, 1) }
            if (!result.success) {
                mes("You don't have enough inventory space.")
                resetAnim()
                return
            }
            locRepo.change(loc, definition.empty, definition.restockTicks)
            val rate = when (definition.full) {
                StallLocs.bakery, StallLocs.dwarfBakery, StallLocs.hosidiusBakery, StallLocs.fortisBakery, StallLocs.fruit -> 124_066
                StallLocs.tea, StallLocs.silk, StallLocs.prifddinasSilk, StallLocs.fortisSilk -> 68_926
                else -> 36_490
            }
            petRates.roll(player,stats.thieving,20663,rate)
            statAdvance(stats.thieving, definition.xp)
            player.recordDiaryStall(output.id, definition.full.id, loc.coords)
            publish(PlayerStallTheftEvent(player,loc,output,caught=false))
            spam("You steal from the stall.")
            resetAnim()
        }
    }

    private fun Npc.isStallGuard(definition: StallDefinition): Boolean =
        visType.name == "Guard" || visType.name == "Knight of Ardougne" || (definition.full == StallLocs.fruit && visType.name == "Guard dog")

    public companion object {
        public const val STEAL_TICKS: Int = 2
        public const val DETECTION_RANGE: Int = 5
    }
}

public class StallTheftScript @Inject constructor(private val theft: StallTheft) : PluginScript() {
    override fun ScriptContext.startup() {
        for (definition in OrdinaryStalls.all) {
            when (definition.operation) {
                1 -> onOpLoc1(definition.full) { theft.steal(this, it.loc, definition) }
                2 -> onOpLoc2(definition.full) { theft.steal(this, it.loc, definition) }
                else -> error("Unsupported stall operation: ${definition.operation}")
            }
        }
    }
}
