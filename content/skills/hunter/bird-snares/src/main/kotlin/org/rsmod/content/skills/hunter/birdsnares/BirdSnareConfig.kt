package org.rsmod.content.skills.hunter.birdsnares

import org.rsmod.api.type.builders.controller.ControllerBuilder
import org.rsmod.api.type.builders.hunt.HuntModeBuilder
import org.rsmod.api.type.builders.varcon.VarConBuilder
import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.api.type.refs.hunt.HuntModeReferences
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.game.movement.MoveRestrict
import org.rsmod.game.type.hunt.HuntType
import org.rsmod.game.type.hunt.HuntVis
import org.rsmod.game.type.loc.LocType
import org.rsmod.game.type.npc.NpcType
import org.rsmod.game.type.obj.ObjType

public object BirdSnareLocs : LocReferences() {
    public val broken = find("hunting_ojibway_trap_broken")
    public val open = find("hunting_ojibway_trap")
    public val failing = find("hunting_ojibway_trap_failing")
    public val trappingCrimson = find("hunting_ojibway_trap_trapping_jungle")
    public val fullCrimson = find("hunting_ojibway_trap_full_jungle")
    public val trappingGolden = find("hunting_ojibway_trap_trapping_desert")
    public val fullGolden = find("hunting_ojibway_trap_full_desert")
    public val trappingCopper = find("hunting_ojibway_trap_trapping_woodland")
    public val fullCopper = find("hunting_ojibway_trap_full_woodland")
    public val trappingCerulean = find("hunting_ojibway_trap_trapping_polar")
    public val fullCerulean = find("hunting_ojibway_trap_full_polar")
    public val trappingTropical = find("hunting_ojibway_trap_trapping_coloured")
    public val fullTropical = find("hunting_ojibway_trap_full_coloured")
}

public object BirdSnareObjs : ObjReferences() {
    public val snare = find("hunting_ojibway_bird_snare")
    public val bones = find("bones")
    public val rawBirdMeat = find("spit_raw_bird_meat")
    public val redFeather = find("hunting_jungle_feather")
    public val yellowFeather = find("hunting_desert_feather")
    public val orangeFeather = find("hunting_woodland_feather")
    public val blueFeather = find("hunting_polar_feather")
    public val stripyFeather = find("hunting_stripy_bird_feather")
    public val litTorch = find("torch_lit")
    public val brumaTorch = find("wint_torch")
    public val guildHood = find("hg_hunter_hood")
    public val guildTop = find("hg_hunter_top")
    public val guildLegs = find("hg_hunter_legs")
    public val guildBoots = find("hg_hunter_boots")
}

public object BirdSnareNpcs : NpcReferences() {
    public val crimsonSwift = find("hunting_bird_jungle")
    public val goldenWarbler = find("hunting_bird_desert")
    public val copperLongtail = find("hunting_bird_woodland")
    public val ceruleanTwitch = find("hunting_bird_polar")
    public val tropicalWagtail = find("multicoloured_bird")
}

public object BirdSnareSeqs : SeqReferences() {
    public val lay = find("human_laytrap")
    public val check = find("human_hunting_dismantle_net")
    public val birdLand = find("bird_landontrap")
    public val birdLeave = find("bird_leavetrap")
    public val birdCaught = find("bird_caughtintrap")
}

public object BirdSnareHunts : HuntModeReferences() {
    public val openTrap = find("bird_snare_open_trap")
}

internal object BirdSnareHuntBuilder : HuntModeBuilder() {
    init {
        build("bird_snare_open_trap") {
            type = HuntType.Scenery
            checkLoc { loc = BirdSnareLocs.open }
            checkVis = HuntVis.LineOfSight
            findKeepHunting = true
            rate = 3
            findNewMode = NpcMode.OpLoc1
        }
    }
}

internal object BirdSnareNpcEditor : NpcEditor() {
    init {
        editBird(BirdSnareNpcs.crimsonSwift)
        editBird(BirdSnareNpcs.goldenWarbler)
        editBird(BirdSnareNpcs.copperLongtail)
        editBird(BirdSnareNpcs.ceruleanTwitch)
        editBird(BirdSnareNpcs.tropicalWagtail)
    }

    private fun editBird(type: NpcType) {
        edit(type) {
            defaultMode = NpcMode.Wander
            moveRestrict = MoveRestrict.Normal
            huntMode = BirdSnareHunts.openTrap
            huntRange = BirdSnareTrap.ATTRACTION_RADIUS
        }
    }
}

public object BirdSnareControllerBuilder : ControllerBuilder() {
    public val trap = build("bird_snare_trap")
}

internal object BirdSnareVarCons : VarConBuilder() {
    val ownerSlot = build("bird_snare_owner_slot")
    val phase = build("bird_snare_phase")
    val birdIndex = build("bird_snare_bird_index")
    val smoked = build("bird_snare_smoked")
}

public data class BirdSnareBird(
    val npc: NpcType,
    val level: Int,
    val xp: Double,
    val lowRate: Int,
    val highRate: Int,
    val transition: LocType,
    val full: LocType,
    val feather: ObjType,
)

public object BirdSnareBirds {
    public val all: List<BirdSnareBird> =
        listOf(
            BirdSnareBird(
                BirdSnareNpcs.crimsonSwift,
                1,
                34.0,
                100,
                420,
                BirdSnareLocs.trappingCrimson,
                BirdSnareLocs.fullCrimson,
                BirdSnareObjs.redFeather,
            ),
            BirdSnareBird(
                BirdSnareNpcs.goldenWarbler,
                5,
                47.0,
                92,
                400,
                BirdSnareLocs.trappingGolden,
                BirdSnareLocs.fullGolden,
                BirdSnareObjs.yellowFeather,
            ),
            BirdSnareBird(
                BirdSnareNpcs.copperLongtail,
                9,
                61.2,
                85,
                390,
                BirdSnareLocs.trappingCopper,
                BirdSnareLocs.fullCopper,
                BirdSnareObjs.orangeFeather,
            ),
            BirdSnareBird(
                BirdSnareNpcs.ceruleanTwitch,
                11,
                64.5,
                82,
                380,
                BirdSnareLocs.trappingCerulean,
                BirdSnareLocs.fullCerulean,
                BirdSnareObjs.blueFeather,
            ),
            BirdSnareBird(
                BirdSnareNpcs.tropicalWagtail,
                19,
                95.2,
                75,
                370,
                BirdSnareLocs.trappingTropical,
                BirdSnareLocs.fullTropical,
                BirdSnareObjs.stripyFeather,
            ),
        )

    public val byNpcId: Map<Int, BirdSnareBird> = all.associateBy { it.npc.id }
    public val byFullLocId: Map<Int, BirdSnareBird> = all.associateBy { it.full.id }
}
