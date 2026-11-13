package org.rsmod.content.skills.fishing

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public object HighLevelFishingNpcs : NpcReferences() {
    val monkfish = find("swan_fishingspot")
    val angler = find("piscariliusfish")
    val darkCoast = find("52_59_crabs")
    val darkResource = find("49_61_crabs")
}

public object HighLevelFishingObjs : ObjReferences() {
    val sandworms = find("piscarilius_sandworms")
    val diabolic = find("diabolic_worms")
    val darkBait = find("wilderness_fishing_bait")
}

public object HighLevelFishingBits : VarBitReferences() {
    val swan = find("swansong")
}

internal object HighLevelFishingNpcEditor : NpcEditor() {
    init {
        for (type in
            listOf(
                HighLevelFishingNpcs.monkfish,
                HighLevelFishingNpcs.angler,
                HighLevelFishingNpcs.darkCoast,
                HighLevelFishingNpcs.darkResource,
            )) {
            edit(type) {
                moveRestrict = nomove
                wanderRange = 0
            }
        }
    }
}

/** Dated charts: Raw monkfish14791518, Raw anglerfish14903409, Raw dark crab14980753. */
public object HighLevelFishingMethods {
    fun selectAngler(player: Player): FishingMethod =
        if (HighLevelFishingObjs.diabolic in player.inv) diabolicAngler else angler

    private val swan =
        FishingVarbitRequirement(
            HighLevelFishingBits.swan,
            200,
            "You need to complete Swan Song before fishing for monkfish.",
        )
    val monkfish =
        StandardFishingMethods.smallNet.copy(
            membersOnly = true,
            varbitRequirement = swan,
            catches =
                listOf(FishingCatch(objs.raw_monkfish, 62, 120.0, 48, 90, "You catch a monkfish.")),
        )
    val colonyHarpoon =
        StandardFishingMethods.harpoon.copy(membersOnly = true, varbitRequirement = swan)
    val angler =
        StandardFishingMethods.bait.copy(
            membersOnly = true,
            bait = HighLevelFishingObjs.sandworms,
            missingBaitMessage = "You need sandworms or diabolic worms to fish for anglerfish.",
            catches =
                listOf(
                    FishingCatch(objs.raw_anglerfish, 82, 120.0, 3, 36, "You catch an anglerfish.")
                ),
        )
    val diabolicAngler =
        angler.copy(
            bait = HighLevelFishingObjs.diabolic,
            catches =
                listOf(
                    FishingCatch(objs.raw_anglerfish, 82, 79.2, 13, 100, "You catch an anglerfish.")
                ),
        )
    val darkCrab =
        StandardFishingMethods.lobster.copy(
            membersOnly = true,
            bait = HighLevelFishingObjs.darkBait,
            missingBaitMessage = "You need dark fishing bait to catch dark crabs.",
            catches =
                listOf(FishingCatch(objs.raw_dark_crab, 85, 130.0, 3, 40, "You catch a dark crab.")),
        )
}

public class HighLevelFishingScript @Inject constructor(private val fishing: StandardFishing) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(HighLevelFishingNpcs.monkfish) {
            fishing.fish(this, it.npc, HighLevelFishingMethods.monkfish)
        }
        onOpNpc3(HighLevelFishingNpcs.monkfish) {
            fishing.fish(this, it.npc, HighLevelFishingMethods.colonyHarpoon)
        }
        onOpNpc1(HighLevelFishingNpcs.angler) {
            val method = HighLevelFishingMethods.selectAngler(player)
            fishing.fish(this, it.npc, method)
        }
        for (npc in
            listOf(HighLevelFishingNpcs.darkCoast, HighLevelFishingNpcs.darkResource)) onOpNpc1(
            npc
        ) {
            fishing.fish(this, it.npc, HighLevelFishingMethods.darkCrab)
        }
    }
}
