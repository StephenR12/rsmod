package org.rsmod.content.skills.agility

import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onApNpc1
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpHeld4
import org.rsmod.api.script.onOpHeld5
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object MonkeyKeepers : NpcReferences() {
    val minder = find("mm_monkey_minder")
    val bobawu = find("hundred_agility_monkey")
}

internal object MonkeyCrate : LocReferences() {
    val crate = find("mm2_crashsite_monkeycrate")
}

internal object MonkeyQuest : VarpReferences() {
    val madness = find("mm_main")
}

internal object MonkeyPacks : ObjReferences() {
    val forms =
        listOf(
                "mm2_monkey",
                "mm2_monkey_karamja",
                "mm2_monkey_zombie",
                "mm2_monkey_maniacal",
                "mm2_monkey_skeleton",
                "mm2_monkey_kruk",
                "mm2_monkey_awowogei",
            )
            .map { find(it) }
}

public class MonkeyRewards : PluginScript() {
    private val laps = listOf(0, 100, 250, 500, 1000, 1500, 2000)
    private val names =
        listOf(
            "Monkey",
            "Karamjan monkey",
            "Zombie monkey",
            "Maniacal monkey",
            "Skeleton monkey",
            "Kruk jr",
            "Princely monkey",
        )

    override fun ScriptContext.startup() {
        onOpNpc1(MonkeyKeepers.minder) { minder() }
        onApNpc1(MonkeyKeepers.minder) {
            if (isWithinApRange(it.npc, if (inPen(coords)) 4 else 1)) minder()
        }
        onOpNpc1(MonkeyKeepers.bobawu) {
            mes("Finish laps after Monkey Madness II to unlock new forms for your monkey backpack.")
            count()
        }
        onOpNpc3(MonkeyKeepers.bobawu) { count() }
        onOpLoc1(MonkeyCrate.crate) {
            if (!player.members || vars[ApeCourseBits.mm2] < 195) return@onOpLoc1
            val monkey = MonkeyPacks.forms.first()
            if (monkey in inv || monkey in bank || monkey in worn) mes("The crate is empty.")
            else if (invAdd(inv, monkey).success)
                mes("You gently lift a monkey out of the warm crate.")
        }
        for ((index, monkey) in MonkeyPacks.forms.withIndex()) {
            onOpHeld3(monkey) {
                val selection =
                    menu(
                        "Choose a monkey",
                        true,
                        names.mapIndexed { i, name ->
                            if (i == 0) name else "$name (${laps[i]} laps)"
                        },
                    )
                if (selection !in names.indices || index == selection) return@onOpHeld3
                if (vars[ApeCourseVars.laps] < laps[selection]) {
                    mes("Complete ${laps[selection]} laps to unlock this monkey.")
                    return@onOpHeld3
                }
                if (inv[it.slot]?.id == monkey.id)
                    invReplaceSlot(inv, it.slot, 1, MonkeyPacks.forms[selection])
            }
            onOpHeld4(monkey) { count() }
            onOpHeld5(monkey) {
                if (
                    choice2("Release the monkey?", true, "Keep the monkey.", false) &&
                        inv[it.slot]?.id == monkey.id
                ) {
                    invDel(inv, monkey, 1, it.slot)
                    mes("The monkey scampers away.")
                }
            }
        }
    }

    private fun ProtectedAccess.count() {
        mes(
            "You have completed ${vars[ApeCourseVars.laps]} laps of the Ape Atoll Agility Course since Monkey Madness II."
        )
    }

    private fun inPen(tile: CoordGrid): Boolean {
        val range =
            when (tile.z) {
                3274 -> 2598..2600
                3275 -> 2598..2601
                3276 -> 2598..2605
                3277,
                3278 -> 2598..2606
                3279 -> 2599..2606
                3280,
                3281 -> 2600..2606
                3282 -> 2600..2605
                else -> return false
            }
        return tile.level == 0 && tile.x in range
    }

    private suspend fun ProtectedAccess.minder() {
        if (!player.members) return
        val monkey = ApeGreegrees.items.indexOfFirst { it in worn }
        if (inPen(coords)) {
            if (monkey >= 0) {
                mes("The minder tells you to be quiet.")
                return
            }
            mes("The monkey minder lets you out of the enclosure.")
            delay(2)
            telejump(CoordGrid(2603, 3275))
            return
        }
        if (
            monkey != 3 ||
                player.transmog?.id != ApeForms.forms[3].id ||
                vars[MonkeyQuest.madness] == 0
        ) {
            mes(
                if (monkey < 0) "The minder is keeping an eye on the monkeys."
                else "The minder does not recognise you as one of his monkeys."
            )
            return
        }
        mes("The monkey minder returns you to the enclosure.")
        delay(2)
        telejump(CoordGrid(2604, 3276))
        player.completeDiaryTask(DiaryTask.EnterArdougneMonkeyCage)
    }
}
