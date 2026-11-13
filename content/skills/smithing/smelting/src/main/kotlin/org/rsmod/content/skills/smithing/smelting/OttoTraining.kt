package org.rsmod.content.skills.smithing.smelting

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.diary.DiaryDifficulty
import org.rsmod.api.player.diary.DiaryRegion
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpcU
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.game.type.obj.ObjType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public object OttoNpcs : NpcReferences() {
    val otto = find("brut_otto")
}

public object OttoObjs : ObjReferences() {
    val rod = find("brut_fishing_rod")
    val spear = find("zamorak_spear")
    val hasta = find("zamorak_hasta")
    val broken = find("broken_dragon_hasta")
    val dragon = find("brut_dragon_spear")
}

public object OttoBits : VarBitReferences() {
    val planting = find("brut_farming_planting")
    val smashing = find("brut_farming_smashing")
    val autoSmash = find("brut_smash_pots_automatically")
}

internal object OttoSpawn : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<OttoTrainingScript>("otto.toml")
    }
}

public enum class OttoLesson {
    HeavyRod,
    Harpoon,
    Bow,
    Pyre,
    Smithing,
    Herblore,
    Farming,
}

/**
 * Original text presentation is shortened; skill and completed-action gates use native progress
 * bits.
 */
public class OttoTraining @Inject constructor() {
    public suspend fun talk(access: ProtectedAccess): Unit =
        with(access) {
            if (!player.members) return
            when (
                menu(
                    "What would you like Otto to teach you?",
                    "Fishing.",
                    "Firemaking.",
                    "Smithing spears and hastae.",
                    "Herblore.",
                    "Farming.",
                    "Convert or repair a weapon.",
                    "Leave.",
                )
            ) {
                1 ->
                    lesson(
                        this,
                        if (
                            menu(
                                "Which Fishing skill?",
                                "Heavy-rod fishing.",
                                "Barehanded fishing.",
                            ) == 1
                        )
                            OttoLesson.HeavyRod
                        else OttoLesson.Harpoon,
                    )
                2 ->
                    lesson(
                        this,
                        if (
                            menu(
                                "Which Firemaking skill?",
                                "Lighting fires with a bow.",
                                "Barbarian pyre ships.",
                            ) == 1
                        )
                            OttoLesson.Bow
                        else OttoLesson.Pyre,
                    )
                3 -> lesson(this, OttoLesson.Smithing)
                4 -> lesson(this, OttoLesson.Herblore)
                5 -> lesson(this, OttoLesson.Farming)
                6 -> {
                    val input =
                        when (
                            menu(
                                "What would you like Otto to do?",
                                "Turn a Zamorakian spear into a hasta.",
                                "Turn a Zamorakian hasta into a spear.",
                                "Repair a broken dragon hasta.",
                            )
                        ) {
                            1 -> OttoObjs.spear
                            2 -> OttoObjs.hasta
                            else -> OttoObjs.broken
                        }
                    convert(this, input)
                }
            }
        }

    public fun lesson(access: ProtectedAccess, lesson: OttoLesson): Unit =
        with(access) {
            if (!player.members) return
            fun fishingDone() =
                vars[varbits.brut_fishing_r] >= 3 && vars[varbits.brut_fishing_s] >= 3
            when (lesson) {
                OttoLesson.HeavyRod -> {
                    if (
                        player.stat(stats.fishing) < 48 ||
                            player.stat(stats.agility) < 15 ||
                            player.stat(stats.strength) < 15
                    ) {
                        mes(
                            "You need 48 Fishing, 15 Agility and 15 Strength for heavy-rod fishing."
                        )
                        return
                    }
                    when (vars[varbits.brut_fishing_r]) {
                        0 -> {
                            vars[varbits.brut_fishing_r] = 1
                            mes("Search beneath my bed for a heavy rod, then catch a leaping fish.")
                        }
                        2 -> {
                            vars[varbits.brut_fishing_r] = 3
                            mes("You have learned barbarian heavy-rod fishing.")
                        }
                        else -> mes("Use a barbarian rod and bait to catch leaping fish.")
                    }
                }
                OttoLesson.Harpoon -> {
                    if (player.stat(stats.fishing) < 55 || player.stat(stats.strength) < 35) {
                        mes("You need 55 Fishing and 35 Strength to catch tuna with your hands.")
                        return
                    }
                    when (vars[varbits.brut_fishing_s]) {
                        0 -> {
                            vars[varbits.brut_fishing_s] = 1
                            mes("Catch a tuna, swordfish or shark without using a harpoon.")
                        }
                        2 -> {
                            vars[varbits.brut_fishing_s] = 3
                            mes("You have learned barbarian barehanded fishing.")
                        }
                        else -> mes("Put your harpoon away and fish with your hands.")
                    }
                }
                OttoLesson.Bow -> {
                    if (player.stat(stats.firemaking) < 35 || player.stat(stats.crafting) < 11) {
                        mes("You need 35 Firemaking and 11 Crafting to learn this skill.")
                        return
                    }
                    when (vars[varbits.brut_fire]) {
                        0 -> {
                            vars[varbits.brut_fire] = 1
                            mes("Use a wooden bow to light some oak logs.")
                        }
                        2 -> {
                            vars[varbits.brut_fire] = 3
                            mes("You have learned barbarian bow Firemaking.")
                        }
                        else -> mes("Light oak logs with a wooden bow and return to me.")
                    }
                }
                OttoLesson.Pyre -> {
                    if (vars[varbits.brut_fire] < 3) {
                        mes("Complete my bow Firemaking training first.")
                        return
                    }
                    when (vars[varbits.brut_craft_ship]) {
                        0 -> {
                            vars[varbits.brut_craft_ship] = 1
                            mes(
                                "Build a pyre ship and lay the remains of an ancient barbarian to rest."
                            )
                        }
                        2 -> {
                            vars[varbits.brut_craft_ship] = 3
                            mes("You have learned to make barbarian pyre ships.")
                        }
                        else ->
                            mes(
                                "Use the pyre sites beside the lake to put ancient barbarians to rest."
                            )
                    }
                }
                OttoLesson.Smithing -> {
                    if (
                        !fishingDone() ||
                            vars[varps.tbwt_main] < 6 ||
                            player.stat(stats.smithing) < 5
                    ) {
                        mes(
                            "Complete Barbarian Fishing and Tai Bwo Wannai Trio, then return with at least 5 Smithing."
                        )
                        return
                    }
                    when {
                        vars[BarbarianSmithingBits.spear] == 0 -> {
                            vars[BarbarianSmithingBits.spear] = 1
                            mes(
                                "Smith a spear on the anvil outside my hut, using a metal bar and corresponding logs."
                            )
                        }
                        vars[BarbarianSmithingBits.spear] == 2 -> {
                            vars[BarbarianSmithingBits.spear] = 3
                            vars[BarbarianSmithingBits.hasta] = 1
                            mes("Now make a hasta with a metal bar and corresponding logs.")
                        }
                        vars[BarbarianSmithingBits.hasta] == 2 -> {
                            vars[BarbarianSmithingBits.hasta] = 3
                            mes("You have completed Barbarian Smithing training.")
                        }
                        vars[BarbarianSmithingBits.hasta] >= 3 ->
                            mes("You have already mastered spears and hastae.")
                        else -> mes("Make the weapon I requested at the anvil outside my hut.")
                    }
                }
                OttoLesson.Herblore -> {
                    if (
                        !fishingDone() ||
                            vars[varbits.brut_fire] < 3 ||
                            vars[varps.druidic_ritual] < 4 ||
                            player.stat(stats.herblore) < 4
                    ) {
                        mes(
                            "Complete Barbarian Fishing, bow Firemaking and Druidic Ritual, then return with at least 4 Herblore."
                        )
                        return
                    }
                    when (vars[varbits.brut_herb_potion]) {
                        0 -> {
                            vars[varbits.brut_herb_potion] = 1
                            mes("Mix roe or caviar into a two-dose attack potion.")
                        }
                        2 -> {
                            val result =
                                player.invTransaction(player.inv) {
                                    delete(select(player.inv), objs.attack_mix_2.id, 1)
                                }
                            if (result.success) {
                                vars[varbits.brut_herb_potion] = 3
                                mes("You have learned barbarian Herblore.")
                            } else mes("Bring me the two-dose attack mix you made.")
                        }
                        else -> mes("Make a two-dose attack mix and bring it to me.")
                    }
                }
                OttoLesson.Farming -> {
                    if (player.stat(stats.farming) < 15) {
                        mes("You need 15 Farming to learn this skill.")
                        return
                    }
                    when {
                        vars[OttoBits.planting] == 0 -> {
                            vars[OttoBits.planting] = 1
                            mes(
                                "Plant a seed in a patch using your hands instead of a seed dibber."
                            )
                        }
                        vars[OttoBits.planting] == 2 -> {
                            vars[OttoBits.planting] = 3
                            vars[OttoBits.smashing] = 1
                            vars[OttoBits.autoSmash] = 1
                            mes("Now plant a sapling and smash its empty pot.")
                        }
                        vars[OttoBits.smashing] == 2 -> {
                            vars[OttoBits.smashing] = 3
                            mes("You have completed Barbarian Farming training.")
                        }
                        vars[OttoBits.smashing] >= 3 ->
                            mes("You have already mastered barbarian planting and pot smashing.")
                        else -> mes("Complete the planting task I gave you, then return to me.")
                    }
                }
            }
        }

    public fun rod(access: ProtectedAccess): Unit =
        with(access) {
            if (!player.members || vars[varbits.brut_fishing_r] < 1) {
                mes("Ask Otto about heavy-rod fishing first.")
                return
            }
            if (player.invMap.values.any { OttoObjs.rod in it }) {
                mes("You already have a barbarian rod.")
                return
            }
            if (invAdd(inv, OttoObjs.rod, 1).success)
                mes("You find a heavy barbarian fishing rod beneath the bed.")
        }

    public fun convert(access: ProtectedAccess, input: ObjType): Unit =
        with(access) {
            if (!player.members) return
            if (input != OttoObjs.broken && vars[BarbarianSmithingBits.hasta] < 3) {
                mes(
                    "Complete Barbarian Smithing training before asking Otto to work on these weapons."
                )
                return
            }
            val output =
                when (input) {
                    OttoObjs.spear -> OttoObjs.hasta
                    OttoObjs.hasta -> OttoObjs.spear
                    OttoObjs.broken -> OttoObjs.dragon
                    else -> return
                }
            val price =
                if (input == OttoObjs.spear)
                    if (vars[DiaryRegion.Kandarin.tier(DiaryDifficulty.Elite).complete] != 0) 150000
                    else 300000
                else 0
            val result =
                player.invTransaction(player.inv) {
                    val inv = select(player.inv)
                    delete(inv, input.id, 1)
                    if (price > 0) delete(inv, objs.coins.id, price)
                    add(inv, output.id, 1)
                }
            if (result.success) mes("Otto returns your weapon.")
            else mes("You don't have the required weapon and coins.")
        }
}

public class OttoTrainingScript @Inject constructor(private val otto: OttoTraining) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(OttoNpcs.otto) { otto.talk(this) }
        onOpLoc1(BarbarianAnvilLocs.bed) { otto.rod(this) }
        for (obj in listOf(OttoObjs.spear, OttoObjs.hasta, OttoObjs.broken)) onOpNpcU(
            OttoNpcs.otto,
            obj,
        ) {
            otto.convert(this, obj)
        }
    }
}
