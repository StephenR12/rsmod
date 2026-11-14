package org.rsmod.content.other.commands

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.invs
import org.rsmod.api.config.refs.modlevels
import org.rsmod.api.config.refs.objs
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.inv.InvTypeList
import org.rsmod.game.type.mod.ModLevelTypeList
import org.rsmod.game.type.spot.SpotanimTypeList

class DemonicPactItemsTest {
    @Test
    fun GameTestState.`league items fill inventory and overflow into the bank`() {
        giveItems(occupiedSlots = 0)
    }

    @Test
    fun GameTestState.`a full inventory sends every league item to the bank`() {
        giveItems(occupiedSlots = 28)
    }

    private fun GameTestState.giveItems(occupiedSlots: Int) {
        val module =
            object : AbstractModule() {
                override fun configure() {
                    bind(SpotanimTypeList::class.java).toInstance(cacheTypes.spotanims)
                }
            }
        runInjectedGameTest(Dependencies::class, module, AdminCommands::class) { deps ->
            player.modLevel = deps.modLevels[modlevels.admin]
            repeat(occupiedSlots) { player.inv[it] = InvObj(objs.spade) }
            val bank = player.invMap.getOrPut(deps.invTypes[invs.bank])

            assertTrue(deps.commands.execute(player, "giveleagueitems", emptyList()))

            val inventoryItems = minOf(EXPECTED_ITEMS.size, 28 - occupiedSlots)
            assertEquals(occupiedSlots + inventoryItems, player.inv.occupiedSpace())
            assertEquals(EXPECTED_ITEMS.size - inventoryItems, bank.occupiedSpace())
            EXPECTED_ITEMS.forEach { id ->
                val count =
                    player.inv.filterNotNull().filter { it.id == id }.sumOf { it.count } +
                        bank.filterNotNull().filter { it.id == id }.sumOf { it.count }
                assertEquals(1, count, "item $id")
            }
            assertEquals(occupiedSlots, player.inv.filterNotNull().count { it.id == objs.spade.id })
        }
    }

    private data class Dependencies
    @Inject
    constructor(
        val commands: CheatCommandMap,
        val modLevels: ModLevelTypeList,
        val invTypes: InvTypeList,
    )
}

private val EXPECTED_ITEMS =
    listOf(
        33218, // barbarian_knapsack
        28771, // league_relic_agility_boots
        30357, // foragers_pouch
        30361, // league_bank_heist_teleport
        33227, // league_evil_eye_necklace
        33233, // league_agility_map
        33229, // league_transmute_book
        33235, // league_clue_contract
        33237, // league_butlers_bell
        25102, // league_trailblazer_fairys_flight_teleport
        33231, // league_soul_shard
        26551, // league_3_magic_book
        28773, // league_executioner_weapon
        28769, // league_guardian_summon
        33239, // league_flask_of_fervour
        30367, // echo_godsword
        30371, // devils_element
        33243, // infernal_tecpatl
        33245, // natures_recurve
        33247, // v_helm
        33249, // fang_of_the_hound
        33251, // kings_barrage
        33253, // shadowflame_quadrant
        33257, // lithic_sceptre_uncharged
        30393, // infernal_echoing_orb
        30394, // queenly_echoing_orb
        30395, // kingly_echoing_orb
        30396, // explosive_echoing_orb
        30397, // stonebound_echoing_orb
        30398, // bestial_echoing_orb
        30399, // draconic_echoing_orb
        30400, // tangled_echoing_orb
        30401, // solar_echoing_orb
        30402, // leagues_megarare_voucher
    )
