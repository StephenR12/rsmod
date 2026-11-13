package org.rsmod.content.travel.diaryroutes

import jakarta.inject.Inject
import org.rsmod.api.config.refs.*
import org.rsmod.api.death.*
import org.rsmod.api.invtx.*
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.travelWithItem
import org.rsmod.api.script.*
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory
import org.rsmod.game.type.dbrow.DbRowTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public object RegionalDungeonLocs : LocReferences() {
    val bottom = find("deal_stairs_bottom")
    val top = find("deal_stairs_top")
    val ladder = find("deal_ladder_up")
    val ladderTop = find("deal_laddertop")
    val tarnDoor = find("lotr_ruins_door_30")
    val tarnExit = find("lotr_ruins_door_boss_exit_2")
    val dwellings = (listOf("", "2", "3", "4")).map { find("molch_temple_lizard_dwelling$it") }
    val templeExit = find("molch_temple_exit")
    val barriers = (1..5).map { find("molch_temple_barrier$it") }
    val barrierForms =
        listOf(
            find("molch_temple_barrier_green"),
            find("molch_temple_barrier_orange"),
            find("molch_temple_barrier_red"),
        )
}

public object RegionalDungeonNpcs : NpcReferences() {
    val pete = find("deal_pete")
    val islandPete = find("deal_island_pete")
    val fever = find("deal_fever_spiders1")
    val dogs = listOf(find("lotr_terror_dog_small"), find("lotr_terror_dog_large"))
    val shaman = find("molch_lizardshaman_1")
}

public object RegionalDungeonVars : VarpReferences() {
    val rum = find("deal_quest")
    val mine = find("hauntedmine")
    val priest = find("priestperil")
    val legends = find("legendsquest")
}

public object RegionalDungeonBits : VarBitReferences() {
    val tarn = find("lotr_instance_entered")
    val mourning = find("mourning_quest_main")
    val barriers = (1..5).map { find("molch_barrier_$it") }
}

public object RegionalDungeonItems : ObjReferences() {
    val rings = (1..8).map { find("slayer_ring_$it") }
    val eternal = find("slayer_ring_eternal")
    val gem = find("slayer_gem")
}

internal object RegionalDungeonSeqs : SeqReferences() {
    val spider = find("giantspider_attack")
    val spiderDeath = find("giantspider_death")
    val dog = find("lotr_terror_dog_attack")
    val largeDog = find("lotr_terror_dog_attack_large")
    val dogDefend = find("lotr_terror_dog_defend")
    val dogDeath = find("lotr_terror_dog_death")
    val shaman = find("shayzien_lizard_boss_tail_swipe")
    val shamanDeath = find("shayzien_lizard_boss_hands_to_head")
}

internal object RegionalDungeonPopulation : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<RegionalSlayerDungeons>("regional-slayer-dungeons.toml")
    }
}

internal object RegionalDungeonStats : NpcEditor() {
    init {
        edit(RegionalDungeonNpcs.fever) {
            hitpoints = 40
            attack = 60
            strength = 30
            defence = 40
            magic = 1
            ranged = 1
            respawnRate = 9
            param[params.attackrate] = 4
            param[params.npc_attack_type] = categories.attacktype_stab
            param[params.attack_anim] = RegionalDungeonSeqs.spider
            param[params.death_anim] = RegionalDungeonSeqs.spiderDeath
            param[params.slayer_levelrequire] = 42
            param[params.slayer_experience] = 400
            param[params.defence_stab] = 20
            param[params.defence_slash] = 15
            param[params.defence_crush] = 10
            param[params.defence_magic] = 15
            for (p in
                listOf(params.defence_light, params.defence_standard, params.defence_heavy)) param[
                p] = 15
        }
        for ((i, npc) in RegionalDungeonNpcs.dogs.withIndex()) edit(npc) {
            hitpoints = if (i == 0) 82 else 87
            attack = if (i == 0) 100 else 110
            strength = if (i == 0) 90 else 104
            defence = if (i == 0) 74 else 78
            magic = 1
            ranged = 1
            respawnRate = 84
            huntMode = huntmodes.aggressive_melee
            huntRange = 5
            param[params.attackrate] = 4
            param[params.npc_attack_type] = categories.attacktype_crush
            param[params.melee_strength] = if (i == 0) 10 else 20
            param[params.attack_anim] =
                if (i == 0) RegionalDungeonSeqs.dog else RegionalDungeonSeqs.largeDog
            param[params.defend_anim] = RegionalDungeonSeqs.dogDefend
            param[params.death_anim] = RegionalDungeonSeqs.dogDeath
            param[params.slayer_levelrequire] = 40
            param[params.slayer_experience] = if (i == 0) 820 else 870
        }
        edit(RegionalDungeonNpcs.shaman) {
            hitpoints = 150
            attack = 120
            strength = 120
            defence = 140
            magic = 130
            ranged = 120
            respawnRate = 45
            huntMode = huntmodes.aggressive_melee
            huntRange = 5
            param[params.attackrate] = 4
            param[params.npc_attack_type] = categories.attacktype_crush
            param[params.attack_melee] = 45
            param[params.melee_strength] = 38
            param[params.defence_stab] = -20
            param[params.defence_slash] = 40
            param[params.defence_crush] = 30
            param[params.defence_magic] = 50
            param[params.defence_light] = -10
            param[params.defence_standard] = 0
            param[params.defence_heavy] = 0
            param[params.slayer_experience] = 1575
            param[params.poison_immunity] = 1
            param[params.venom_immunity] = 1
            param[params.attack_anim] = RegionalDungeonSeqs.shaman
            param[params.death_anim] = RegionalDungeonSeqs.shamanDeath
        }
    }
}

/** Repeatable post-quest access; this does not advance any quest or miniquest. */
public class RegionalSlayerDungeons
@Inject
constructor(
    private val types: ObjTypeList,
    private val rows: DbRowTypeList,
    private val drops: NpcDropTableRegistry,
) : PluginScript() {
    private val barrierPasses = java.util.WeakHashMap<Player, MutableMap<Int, Pair<Int, Int>>>()

    private fun complete(player: Player, row: Int, value: Int) =
        value >= rows.getValue(row).data.getValue(19).single() as Int

    override fun ScriptContext.startup() {
        onOpNpc1(RegionalDungeonNpcs.pete) { sail(this, false) }
        onOpNpc1(RegionalDungeonNpcs.islandPete) { sail(this, true) }
        onOpLoc1(RegionalDungeonLocs.ladderTop) {
            if (player.members) {
                stopAction()
                telejump(it.loc.coords.translate(0, -1, -1))
            }
        }
        onApLoc1(RegionalDungeonLocs.ladderTop) {
            if (player.members && isWithinApRange(it.loc, 1)) {
                stopAction()
                telejump(it.loc.coords.translate(0, -1, -1))
            }
        }
        onOpLoc1(RegionalDungeonLocs.ladder) {
            if (player.members) {
                stopAction()
                telejump(it.loc.coords.translate(0, -1, 1))
            }
        }
        onApLoc1(RegionalDungeonLocs.ladder) {
            if (player.members && isWithinApRange(it.loc, 1)) {
                stopAction()
                telejump(it.loc.coords.translate(0, -1, 1))
            }
        }
        onOpLoc1(RegionalDungeonLocs.bottom) { stairs(this, it.loc.coords, true) }
        onOpLoc1(RegionalDungeonLocs.top) { stairs(this, it.loc.coords, false) }
        onOpLoc1(RegionalDungeonLocs.tarnDoor) { enterTarn(this) }
        onOpLoc1(RegionalDungeonLocs.tarnExit) {
            if (player.members) telejump(CoordGrid(3185, 4601))
        }
        for ((i, dwelling) in RegionalDungeonLocs.dwellings.withIndex()) onOpLoc1(dwelling) {
            enterMolch(this, i)
        }
        onOpLoc1(RegionalDungeonLocs.templeExit) {
            if (player.members) telejump(CoordGrid(1305, 3666))
        }
        for (barrier in RegionalDungeonLocs.barriers + RegionalDungeonLocs.barrierForms) onOpLoc1(
            barrier
        ) {
            passBarrier(this, it.loc.coords)
        }
        for (ring in RegionalDungeonItems.rings + RegionalDungeonItems.eternal) {
            onOpHeld3(ring) { ringMenu(this, inv, it.slot) }
            onOpHeld4(ring) { ringMenu(this, inv, it.slot) }
            onOpHeld5(ring) { checkRing(this, inv, it.slot) }
            onOpWorn2(ring) { checkRing(this, worn, it.slot) }
            onOpWorn3(ring) { ringMenu(this, worn, it.slot) }
        }
        registerDrops()
    }

    public suspend fun sail(access: ProtectedAccess, back: Boolean): Boolean =
        with(access) {
            if (!player.members || !back && vars[RegionalDungeonVars.rum] == 0) {
                mes("Pirate Pete cannot take you there until you have started Rum Deal.")
                return false
            }
            stopAction()
            delay(2)
            teleport(if (back) CoordGrid(3680, 3534) else CoordGrid(2156, 5111))
            true
        }

    private fun stairs(access: ProtectedAccess, c: CoordGrid, up: Boolean) =
        with(access) {
            if (!player.members) return@with
            val destination =
                when {
                    c.x == 2151 || c.x == 2152 ->
                        if (up) CoordGrid(2152, 5107, 1) else CoordGrid(2150, 5109)
                    c.x == 2129 -> if (up) CoordGrid(2130, 5096, 1) else CoordGrid(2128, 5094)
                    else -> CoordGrid(c.x, c.z + if (up) 2 else -1, if (up) 1 else 0)
                }
            stopAction()
            telejump(destination)
        }

    public fun enterTarn(access: ProtectedAccess): Boolean =
        with(access) {
            if (
                !player.members ||
                    !complete(player, 70, vars[RegionalDungeonVars.mine]) ||
                    !complete(player, 84, vars[RegionalDungeonBits.tarn])
            ) {
                mes(
                    "You must have completed the Haunted Mine and Tarn's miniquest to return to this chamber."
                )
                return false
            }
            stopAction()
            telejump(CoordGrid(3149, 4644))
            true
        }

    public fun enterMolch(access: ProtectedAccess, index: Int): Boolean =
        with(access) {
            if (!player.members || index !in 0..3) return false
            stopAction()
            telejump(
                listOf(
                    CoordGrid(1292, 10062, 1),
                    CoordGrid(1314, 10066, 1),
                    CoordGrid(1330, 10072, 1),
                    CoordGrid(1312, 10090, 1),
                )[index]
            )
            true
        }

    public fun passBarrier(access: ProtectedAccess, c: CoordGrid): Boolean =
        with(access) {
            if (!player.members || c.level != 1) return false
            val index =
                listOf(1292 to 10092, 1297 to 10096, 1308 to 10096, 1317 to 10096, 1325 to 10096)
                    .indexOf(c.x to c.z)
            if (index < 0) return false
            val state = barrierPasses.getOrPut(player) { mutableMapOf() }
            val old = state[index]
            val elapsed = if (old == null) 0 else ((mapClock - old.second).coerceAtLeast(0) / 100)
            val count = ((old?.first ?: 0) - elapsed).coerceAtLeast(0) + 1
            state[index] = count to mapClock
            val stage = if (count >= 6) 2 else if (count >= 3) 1 else 0
            vars[RegionalDungeonBits.barriers[index]] = stage
            val destination =
                if (index == 0) c.translate(0, if (coords.z < c.z) 2 else -1)
                else c.translate(if (coords.x < c.x) 2 else -1, 0)
            stopAction()
            telejump(destination)
            takeInstantHit(
                org.rsmod.game.hit.HitType.Typeless,
                if (stage == 2) 20 else if (stage == 1) 10 else 1,
            )
            true
        }

    private suspend fun ringMenu(access: ProtectedAccess, bag: Inventory, slot: Int) =
        with(access) {
            val choice =
                menu(
                    "Slayer ring teleport",
                    true,
                    listOf(
                        "Stronghold Slayer Cave",
                        "Slayer Tower",
                        "Fremennik Slayer Dungeon",
                        "Tarn's Lair",
                        "Dark Beasts",
                        "Cancel",
                    ),
                )
            if (choice in 0..4) ringTeleport(this, bag, slot, choice)
        }

    private fun checkRing(access: ProtectedAccess, bag: Inventory, slot: Int) =
        with(access) {
            val id = bag[slot]?.id ?: return@with
            val index = RegionalDungeonItems.rings.indexOfFirst { it.id == id }
            mes(
                if (id == RegionalDungeonItems.eternal.id)
                    "Your Slayer ring has unlimited teleport charges."
                else "Your Slayer ring has ${index+1} teleport charges."
            )
        }

    public suspend fun ringTeleport(
        access: ProtectedAccess,
        bag: Inventory,
        slot: Int,
        choice: Int,
    ): Boolean =
        with(access) {
            val id = bag[slot]?.id ?: return false
            val index = RegionalDungeonItems.rings.indexOfFirst { it.id == id }
            if (index < 0 && id != RegionalDungeonItems.eternal.id || choice !in 0..4) return false
            if (
                choice == 1 && !complete(player, 111, vars[RegionalDungeonVars.priest]) ||
                    choice == 3 && !complete(player, 70, vars[RegionalDungeonVars.mine]) ||
                    choice == 4 && !complete(player, 100, vars[RegionalDungeonBits.mourning])
            ) {
                mes("You have not completed the quest required for that destination.")
                return false
            }
            travelWithItem(
                listOf(
                    CoordGrid(2431, 3422),
                    CoordGrid(3421, 3537),
                    CoordGrid(2802, 9999),
                    CoordGrid(3185, 4601),
                    CoordGrid(2028, 4636),
                )[choice],
                maxWilderness = 30,
            ) {
                if (bag[slot]?.id != id) return@travelWithItem false
                if (index < 0) return@travelWithItem true
                val replacement =
                    if (index == 0) RegionalDungeonItems.gem
                    else RegionalDungeonItems.rings[index - 1]
                if (bag === worn && index == 0) {
                    if (!invAdd(inv, replacement).success) return@travelWithItem false
                    worn[slot] = null
                    true
                } else
                    player
                        .invTransaction(bag) {
                            val selected = select(bag)
                            delete(selected, id, 1, slot = slot)
                            add(selected, replacement.id, 1, slot = slot)
                        }
                        .success
            }
        }

    private fun registerDrops() {
        fun item(id: Int, count: IntRange = 1..1) = NpcDropRolls.item(types.getValue(id), count)
        fun gem() =
            NpcGemDropRolls.standard(types.getValue(1462), NpcGemDropRolls::wearingWealth) {
                complete(it.hero, 85, it.hero.vars[RegionalDungeonVars.legends])
            }
        drops.register(
            RegionalDungeonNpcs.fever,
            NpcDropTable(
                NpcDropRolls.weighted(
                    listOf(
                        NpcWeightedDrop(2, item(1357)),
                        NpcWeightedDrop(3, item(2495)),
                        NpcWeightedDrop(1, item(1371)),
                        NpcWeightedDrop(2, item(1145)),
                        NpcWeightedDrop(6, item(213)),
                        NpcWeightedDrop(7, item(365, 1..2)),
                        NpcWeightedDrop(8, item(995, 200..600)),
                        NpcWeightedDrop(2, item(types.getValue(7936).certlink, 100..200)),
                        NpcWeightedDrop(4, item(225)),
                        NpcWeightedDrop(1, gem()),
                    )
                )
            ),
        )
        val main =
            NpcDropRolls.weighted(
                listOf(
                        4 to item(1069),
                        3 to item(1355),
                        3 to item(1365),
                        2 to item(1243),
                        1 to item(1161),
                        1 to item(10589),
                        1 to item(1197),
                        1 to item(1213),
                        8 to item(555, 75..75),
                        5 to item(561, 15..15),
                        3 to item(563, 3..3),
                        1 to item(554, 37..37),
                        15 to NpcHerbDropRolls.standard(),
                        29 to item(995, 44..44),
                        25 to item(995, 132..132),
                        10 to item(995, 200..200),
                        5 to item(995, 11..11),
                        1 to item(995, 440..440),
                        3 to item(449),
                        2 to item(365),
                        5 to gem(),
                    )
                    .map { NpcWeightedDrop(it.first, it.second) }
            )
        for ((i, npc) in RegionalDungeonNpcs.dogs.withIndex()) drops.register(
            npc,
            NpcDropTable(NpcDropRolls.sequence(main, item(if (i == 0) 526 else 532))),
        )
        drops.register(RegionalDungeonNpcs.shaman, NpcDropTable(item(532)))
    }
}
