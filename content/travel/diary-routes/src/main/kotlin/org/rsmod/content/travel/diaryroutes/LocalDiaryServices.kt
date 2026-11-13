package org.rsmod.content.travel.diaryroutes

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpcU
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.content.skills.prayer.remains.PrayerRemainsKind
import org.rsmod.content.skills.prayer.remains.PrayerRemainsRecipes
import org.rsmod.game.type.obj.ObjType
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object LocalPeople : NpcReferences() {
    val benny = find("qip_soa_newspaperseller")
    val bartender = find("foresters_bartender")
    val galahad = find("brother_galahad")
    val sherlock = find("sherlock")
    val curator = find("curator")
    val orlando = find("vm_nathis_apprentice")
    val dogs = listOf(find("dog_stray"), find("dog_stray2"))
}

internal object LocalItems : ObjReferences() {
    val newspaper = find("qip_soa_newspaper2")
    val tea = find("cup_of_tea")
    val beer = find("beer")
    val pie = find("meat_pie")
    val stew = find("stew")
}

internal object WarrensLocs : LocReferences() {
    val closed = find("piscquest_manhole_closed")
    val open = find("piscquest_manhole_open")
    val ladder = find("piscquest_manhole_ladder")
}

internal object WarrensBits : VarBitReferences() {
    val quest = find("piscquest")
}

internal object MuseumBits : VarBitReferences() {
    val kudos = find("vm_kudos")
}

internal object LocalPlacements : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<LocalDiaryServices>("regional-services.toml")
    }
}

public class LocalDiaryServices @Inject constructor(private val locations: LocRepository) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(WarrensLocs.closed) {
            if (vars[WarrensBits.quest] >= 4) locations.change(it.loc, WarrensLocs.open, 100)
            else mes("You have no reason to go down there.")
        }
        onOpLoc1(WarrensLocs.open) {
            if (player.members && vars[WarrensBits.quest] >= 4) teleport(CoordGrid(1813, 10145))
        }
        onOpLoc2(WarrensLocs.open) { locations.change(it.loc, WarrensLocs.closed, 100) }
        onOpLoc1(WarrensLocs.ladder) { teleport(CoordGrid(1813, 3744)) }
        onOpNpc1(LocalPeople.benny) {
            if (
                choice2("Buy a newspaper for 50 coins.", true, "No thanks.", false) &&
                    buy(LocalItems.newspaper, 50)
            )
                player.completeDiaryTask(DiaryTask.BuyNewspaper)
        }
        onOpNpc1(LocalPeople.bartender) {
            if (!player.members) return@onOpNpc1
            when (
                menu(
                    "What would you like?",
                    true,
                    listOf(
                        "Beer (2 coins)",
                        "Meat pie (16 coins)",
                        "Stew (20 coins)",
                        "Nothing, thanks.",
                    ),
                )
            ) {
                0 -> buy(LocalItems.beer, 2)
                1 -> buy(LocalItems.pie, 16)
                2 -> if (buy(LocalItems.stew, 20)) player.completeDiaryTask(DiaryTask.BuySeersStew)
            }
        }
        onOpNpc1(LocalPeople.galahad) {
            if (
                player.members &&
                    choice2("Yes, I would like a cup of tea.", true, "No thanks.", false) &&
                    invAdd(inv, LocalItems.tea).success
            )
                player.completeDiaryTask(DiaryTask.ReceiveGalahadTea)
        }
        onOpNpc1(LocalPeople.sherlock) {
            if (player.members) {
                mes("Sherlock looks up from his clues and greets you.")
                player.completeDiaryTask(DiaryTask.SpeakToSherlock)
            }
        }
        onOpNpc1(LocalPeople.curator) {
            mes("You have earned ${vars[MuseumBits.kudos]} Kudos at the museum.")
            if (vars[MuseumBits.kudos] >= 50) player.completeDiaryTask(DiaryTask.SpeakToCurator)
        }
        onOpNpc1(LocalPeople.orlando) {
            mes("Orlando is studying the museum's natural history exhibits.")
            if (vars[MuseumBits.kudos] >= 153) player.completeDiaryTask(DiaryTask.SpeakToOrlando)
        }
        val food =
            PrayerRemainsRecipes.all.filter {
                it.kind == PrayerRemainsKind.Bone && it.obj != objs.shaikahan_bones
            }
        for (dog in LocalPeople.dogs) for (bone in food) onOpNpcU(dog, bone.obj) {
            if (invDel(inv, bone.obj, slot = it.invSlot).success) {
                mes("The dog happily gnaws on the bone.")
                player.completeDiaryTask(DiaryTask.FeedVarrockDog)
            }
        }
    }

    private fun ProtectedAccess.buy(item: ObjType, price: Int): Boolean {
        if (invTotal(inv, objs.coins) < price) {
            mes("You need ${price} coins.")
            return false
        }
        val result =
            player.invTransaction(inv) {
                val bag = select(inv)
                delete(bag, objs.coins.id, price)
                add(bag, item.id, 1)
            }
        if (!result.success) mes("You need more inventory space.")
        return result.success
    }
}
