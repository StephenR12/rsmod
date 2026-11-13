package org.rsmod.content.skills.thieving

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.npc.queueCombatRetaliate
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.npc.isValidTarget
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.stat.thievingLvl
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.*
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.game.hit.HitType
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.loc.LocType
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object CityChestLocs : LocReferences() {
    val trapped = (1..5).map { find("trapchest$it") }
    val arrows = find("pickchest3")
    val empty = find("emptytrapchest")
    val rich = find("dorgesh_rich_chest_closed")
    val average = find("dorgesh_average_chest_closed")
    val richEmpty = find("dorgesh_rich_chest_closed_empty")
    val averageEmpty = find("dorgesh_average_chest_closed_empty")
    val castle = find("wilderness_rogue_chest")
    val castleOpen = find("wilderness_rogue_chest_open")
}
internal object CityChestSeqs : SeqReferences() {
    val pick = find("human_picklock_chest")
    val open = find("human_openchest")
}
internal object CityChestGuards : NpcReferences() { val rogue = find("wilderness_rogue") }
internal object CastleGuardStats : NpcEditor() {
    init { edit(CityChestGuards.rogue) {
        hitpoints = 125
        attack = 140
        strength = 140
        defence = 150
        param[params.attackrate] = 4
    } }
}
internal object CastleGuardSpawns : MapNpcSpawnBuilder() {
    override fun onPackMapTask() { resourceFile<CityChestTheft>("rogue-chest-guards.toml") }
}

internal enum class ThievableChest(val full:LocType, val empty:LocType, val level:Int, val xp:Double, val ticks:Int, val diary:DiaryTask? = null) {
    Coins(CityChestLocs.trapped[0],CityChestLocs.empty,13,7.8,12),
    Nature(CityChestLocs.trapped[1],CityChestLocs.empty,28,25.0,25),
    LargeCoins(CityChestLocs.trapped[2],CityChestLocs.empty,43,125.0,84),
    Blood(CityChestLocs.trapped[3],CityChestLocs.empty,59,250.0,225),
    Ardougne(CityChestLocs.trapped[4],CityChestLocs.empty,72,500.0,834,DiaryTask.LootArdougneCastleChest),
    Arrowtips(CityChestLocs.arrows,CityChestLocs.empty,47,150.0,150,DiaryTask.LootHemensterChest),
    Dorgesh(CityChestLocs.average,CityChestLocs.averageEmpty,52,200.0,150),
    Rich(CityChestLocs.rich,CityChestLocs.richEmpty,78,650.0,500,DiaryTask.LootDorgeshRichChest),
    Wilderness(CityChestLocs.castle,CityChestLocs.castleOpen,84,701.7,34,DiaryTask.LootRoguesCastleChest);

    val isDorgesh get() = this == Dorgesh || this == Rich
    val needsPick get() = isDorgesh || this == Arrowtips
}

public class CityChestTheft @Inject internal constructor(private val locs: LocRepository, private val npcs:NpcRepository, private val loot:CityChestLoot) : PluginScript() {
    override fun ScriptContext.startup() {
        for (chest in ThievableChest.entries) {
            onOpLoc1(chest.full) { openWithoutSearching(chest) }
            onOpLoc2(chest.full) { search(it.loc,chest) }
            if (chest.needsPick) onOpLocU(chest.full,SpecialTheftObjs.lockpick) { search(it.loc,chest) }
        }
        for (empty in listOf(CityChestLocs.empty,CityChestLocs.averageEmpty,CityChestLocs.richEmpty)) {
            onOpLoc1(empty) { mes("The chest is empty.") }
            onOpLoc2(empty) { mes("You find nothing in the chest.") }
        }
    }

    private fun ProtectedAccess.openWithoutSearching(chest:ThievableChest) {
        if (!player.members || actionDelay > player.currentMapClock) return
        if (chest.isDorgesh) { mes("The chest is locked."); return }
        anim(CityChestSeqs.open)
        actionDelay = player.currentMapClock + 3
        val hp = player.hitpoints
        val damage = when (chest) {
            ThievableChest.Coins -> hp * 10 / 100 + 2
            ThievableChest.Nature -> hp * 12 / 100 + 3
            ThievableChest.LargeCoins -> hp * 15 / 100 + 4
            ThievableChest.Arrowtips -> hp * 16 / 100 + 4
            ThievableChest.Blood -> (9 * hp + 225) / 50
            else -> hp / 5 + 5
        }
        queueHit(1,HitType.Typeless,damage)
        mes("You have triggered a trap!")
    }

    private suspend fun ProtectedAccess.search(loc:BoundLocInfo,chest:ThievableChest) {
        if (!player.members || actionDelay > player.currentMapClock) return
        fun qualified() = (if (chest == ThievableChest.Rich) player.statBase(stats.thieving) else player.thievingLvl) >= chest.level
        if (!qualified()) { mes("You need a Thieving level of ${chest.level} to loot this chest."); return }
        if (chest.needsPick && SpecialTheftObjs.lockpick !in inv) { mes("You need a lockpick to pick this lock."); return }
        anim(if(chest.needsPick) CityChestSeqs.pick else CityChestSeqs.open)
        delay(2)
        if (!qualified() || !isWithinDistance(loc,1) || locs.findExact(loc.coords,chest.full) == null ||
            (chest.needsPick && SpecialTheftObjs.lockpick !in inv)) return
        val items = when (chest) {
            ThievableChest.Coins -> listOf(InvObj(objs.coins,10))
            ThievableChest.Nature -> listOf(InvObj(objs.coins,3),InvObj(objs.nature_rune))
            ThievableChest.LargeCoins -> listOf(InvObj(objs.coins,50))
            ThievableChest.Arrowtips -> listOf(InvObj(objs.coins,20),InvObj(objs.steel_arrowheads,5))
            ThievableChest.Blood -> listOf(InvObj(objs.coins,500),InvObj(objs.blood_rune,2))
            ThievableChest.Ardougne -> listOf(InvObj(objs.coins,1000),InvObj(objs.raw_shark),InvObj(objs.adamantite_ore),InvObj(objs.uncut_sapphire))
            ThievableChest.Dorgesh,ThievableChest.Rich -> listOf(loot.dorgesh(this,chest == ThievableChest.Rich,loc.coords))
            ThievableChest.Wilderness -> listOf(loot.wilderness(this))
        }
        val bag = if (chest == ThievableChest.Wilderness && objs.looting_bag_open in inv && CityChestGoods.castle.any { it.item.id == items.single().id }) inv(LootingBagInv.storage) else null
        if ((bag == null || !store(bag,items)) && !store(inv,items)) {
            mes("You don't have enough inventory space.")
            return
        }
        locs.change(loc,chest.empty,chest.ticks)
        statAdvance(stats.thieving,chest.xp)
        chest.diary?.let { player.completeDiaryTask(it) }
        actionDelay = player.currentMapClock + 1
        mes("You take the loot from the chest.")
        when (chest) {
            ThievableChest.Blood -> telejump(CoordGrid(2567,3330))
            ThievableChest.Ardougne -> telejump(CoordGrid(2695,3283))
            ThievableChest.Wilderness -> {
                for (guard in npcs.findAll(ZoneKey.from(loc.coords),2)) {
                    if (guard.id == CityChestGuards.rogue.id && guard.isValidTarget() && guard.isWithinDistance(player,10)) guard.queueCombatRetaliate(player)
                }
            }
            else -> Unit
        }
    }

    private fun ProtectedAccess.store(destination:Inventory,items:List<InvObj>):Boolean =
        player.invTransaction(destination) {
            val target = select(destination)
            for (item in items) add(target,item.id,item.count)
        }.success
}
