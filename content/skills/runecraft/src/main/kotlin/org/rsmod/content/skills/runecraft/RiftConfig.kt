package org.rsmod.content.skills.runecraft

import org.rsmod.api.type.builders.controller.ControllerBuilder
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.refs.comp.ComponentReferences
import org.rsmod.api.type.refs.interf.InterfaceReferences
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.map.CoordGrid

internal val riftTemples = RuneTemple.entries.filter { it != RuneTemple.Wrath }

internal object RiftLocs : LocReferences() {
    val entrance = find("tote_portal_to_gotr_child")
    val exit = find("gotr_entry")
    val barrier =
        listOf("gotr_barrier", "gotr_barrier_closed", "gotr_barrier_noentry").map { find(it) }
    val portals = riftTemples.associateWith { find("gotr_portal_${it.key}") }
    val remains =
        listOf(
                "essence_tier_1",
                "essence_tier_1b",
                "essence_tier_1_large",
                "essence_tier_1_large_b",
                "essence_tier_2",
                "essence_tier_3",
                "fallen_guardian",
            )
            .map { find("gotr_$it") }
    val cells = find("gotr_uncharged_cells")
    val weakCells = find("gotr_weak_cells")
    val bank = find("gotr_bankchest")
    val deposit = find("gotr_depositchest")
    val bench = find("gotr_workbench")
    val crafting = listOf(find("gotr_guardian_spawn_east"), find("gotr_guardian_spawn_west"))
    val shortcuts = listOf(find("gotr_agility_shortcut_top"), find("gotr_agility_shortcut_bottom"))
    val portalTop = find("gotr_agility_portal_top")
    val portalBottom = find("gotr_agility_portal_bottom_child")
    val tiles =
        listOf("broken_repairable", "inactive", "tier1", "tier2", "tier3", "tier4").map {
            find("gotr_cell_tile_$it")
        }
    val walls =
        (1..4).map { listOf(find("gotr_barrier_tier$it"), find("gotr_barrier_tier${it}_3x3")) }
    val reward = find("gotr_reward_rift")
}

internal object RiftItems : ObjReferences() {
    val fragment = find("gotr_guardian_fragment")
    val essence
        get() = RunecraftObjs.guardianEssence

    val stones =
        listOf("elemental", "catalytic", "polyelemental", "polycatalytic").map {
            find("gotr_guardian_stone_$it")
        }
    val cells =
        listOf("uncharged", "tier1", "tier2", "tier3", "tier4").map { find("gotr_cell_$it") }
    val talismans = riftTemples.associateWith { find("gotr_portal_talisman_${it.key}") }
    val pearls = find("abyssal_pearl")
    val needle = find("abyssal_needle")
    val diary = find("gotr_book")
    val pouch = find("gotr_intricate_pouch")
    val lantern = find("abyssal_lantern")
    val lanterns =
        listOf(
                "normal",
                "normal_red",
                "normal_green",
                "normal_blue",
                "normal_white",
                "normal_purple",
                "oak",
                "willow",
                "maple",
                "yew",
                "blisterwood",
                "magic",
                "redwood",
            )
            .associateWith { find("abyssal_lantern_$it") }
    val roundItems
        get() = listOf(fragment, essence) + stones + cells + talismans.values
}

internal object RiftNpcs : NpcReferences() {
    val guardian = find("gotr_great_guardian")
    val felix = find("gotr_reward_trader")
    val tamara = listOf(find("gotr_tamara_1op"), find("gotr_tamara_2op"))
    val defenders =
        listOf("air", "water", "earth", "fire", "mind", "chaos", "death", "blood").map {
            find("gotr_guardian_$it")
        }
    val walls =
        (1..4).map {
            listOf(find("gotr_barrier_npc_tier$it"), find("gotr_barrier_npc_tier${it}_large"))
        }
    val attackers = listOf("leech", "pyramid", "walker").map { find("gotr_abyssal_$it") }
}

internal object RiftMap : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<RiftScript>("rift-guardians.toml")
    }
}

internal object RiftBits : VarBitReferences() {
    val quest = find("tote")
    val energy = listOf("elemental", "catalytic").map { find("gotr_${it}_earned_this_game") }
    val points = listOf("elemental", "catalytic").map { find("gotr_${it}_points") }
    val tallies = riftTemples.associateWith { find("gotr_talisman_toggle_${it.key}") }
    val finished = find("gotr_player_has_finished_game")
    val mineCount = find("gotr_mining_count")
    val minePortal = find("gotr_portal_bottom_visible")
    val effectsOff = find("gotr_colourful_fade_disabled")
    val needle = find("gotr_unlocked_needle")
    val diary = find("gotr_book_obtained")
}

internal object RiftVars : VarpReferences() {
    val completions = find("total_gotr_kills")
    val gameId = find("gotr_game_id_on_player")
}

internal object RiftGame : ControllerBuilder() {
    val controller = build("guardians_of_the_rift")
}

internal object RiftInterface : InterfaceReferences() {
    val hud = find("gotr_hud")
    val deposit = find("bank_depositbox")
}

internal object RiftButtons : ComponentReferences() {
    val phase = find("gotr_hud:status")
}

internal object RiftAnims : SeqReferences() {
    val attacks =
        listOf("leech_attack", "pyramid_attack", "walker_attack").map { find("gotr_abyssal_$it") }
    val make = find("gotr_chisel_essence")
    val guardian = find("gotr_chisel_guardian")
    val lantern = find("gotr_light_lantern")
}

internal object RiftMapTiles {
    val lobby = CoordGrid(3615, 9482)
    val inside = CoordGrid(3615, 9485)
    val center = CoordGrid(3615, 9503)
    val tower = CoordGrid(3104, 9573)
    val entry = CoordGrid(3615, 9470)
    val portalCentres =
        mapOf(
            RuneTemple.Air to CoordGrid(3618, 9495),
            RuneTemple.Water to CoordGrid(3624, 9501),
            RuneTemple.Earth to CoordGrid(3624, 9506),
            RuneTemple.Fire to CoordGrid(3618, 9512),
            RuneTemple.Mind to CoordGrid(3613, 9495),
            RuneTemple.Chaos to CoordGrid(3607, 9501),
            RuneTemple.Death to CoordGrid(3607, 9506),
            RuneTemple.Blood to CoordGrid(3613, 9512),
            RuneTemple.Body to CoordGrid(3609, 9497),
            RuneTemple.Cosmic to CoordGrid(3622, 9497),
            RuneTemple.Nature to CoordGrid(3622, 9510),
            RuneTemple.Law to CoordGrid(3609, 9510),
        )
    val barriers =
        listOf(
                3608 to 9503,
                3609 to 9507,
                3611 to 9509,
                3615 to 9510,
                3619 to 9509,
                3621 to 9507,
                3622 to 9503,
            )
            .map { (x, z) -> CoordGrid(x, z) }
    // South-west corners of the seven barrier footprints on the native temple map.
    val wallCorners =
        listOf(
                3605 to 9502,
                3607 to 9507,
                3610 to 9510,
                3614 to 9511,
                3619 to 9510,
                3622 to 9507,
                3623 to 9502,
            )
            .map { (x, z) -> CoordGrid(x, z) }
    val shortcuts = listOf(CoordGrid(3601, 9495), CoordGrid(3626, 9500), CoordGrid(3617, 9491))

    fun inBattle(c: CoordGrid) = c.level == 0 && c.x in 3584..3647 && c.z in 9484..9535
}

internal val RuneTemple.cellTier: Int
    get() =
        when (this) {
            RuneTemple.Air,
            RuneTemple.Mind,
            RuneTemple.Body -> 1
            RuneTemple.Water,
            RuneTemple.Cosmic,
            RuneTemple.Chaos -> 2
            RuneTemple.Earth,
            RuneTemple.Nature,
            RuneTemple.Law -> 3
            else -> 4
        }

internal object RiftRewardSave : org.rsmod.api.type.builders.varp.VarpBuilder() {
    init {
        build("gotr_search_count") { transmitNever = true }
    }
}

internal object RiftUnbankableItems : org.rsmod.api.type.editors.obj.ObjEditor() {
    init {
        for (item in RiftItems.roundItems) edit(item) {
            param[org.rsmod.api.config.refs.params.no_bank] = 1
        }
    }
}

internal object RiftEffects : org.rsmod.api.type.refs.spot.SpotanimReferences() {
    val beams = riftTemples.associateWith { find("gotr_portal_beam_${it.key}") }
}
