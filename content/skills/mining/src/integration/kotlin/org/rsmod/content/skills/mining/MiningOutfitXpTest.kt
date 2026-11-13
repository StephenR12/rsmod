package org.rsmod.content.skills.mining

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.feet
import org.rsmod.api.player.hat
import org.rsmod.api.player.legs
import org.rsmod.api.player.torso
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.inv.InvObj

@Execution(ExecutionMode.SAME_THREAD)
class MiningOutfitXpTest {
    @Test
    fun GameTestState.`every normal and golden piece grants only its documented fraction of Mining xp`() = runInjectedGameTest(Dependencies::class) { deps ->
        player.members = true
        for ((piece, expected) in listOf(ProspectorOutfits.hat to 0.004, ProspectorOutfits.goldenHat to 0.004, ProspectorOutfits.top to 0.008, ProspectorOutfits.goldenTop to 0.008, ProspectorOutfits.legs to 0.006, ProspectorOutfits.goldenLegs to 0.006, ProspectorOutfits.boots to 0.002, ProspectorOutfits.goldenBoots to 0.002)) {
            player.worn.fillNulls()
            player.worn[0] = InvObj(piece)
            assertEquals(1.0 + expected, deps.mods.get(player, stats.mining), 0.0000001)
            assertEquals(1.0, deps.mods.get(player, stats.crafting), 0.0000001)
        }
    }

    @Test
    fun GameTestState.`mixed full set and Varrock four substitute grant two point five percent`() = runInjectedGameTest(Dependencies::class) { deps ->
        player.members = true
        player.hat = InvObj(ProspectorOutfits.goldenHat)
        player.torso = InvObj(ProspectorOutfits.top)
        player.legs = InvObj(ProspectorOutfits.goldenLegs)
        player.feet = InvObj(ProspectorOutfits.boots)
        assertEquals(1.025, deps.mods.get(player, stats.mining), 0.0000001)
        player.torso = InvObj(objs.diary_varrock_armour_4)
        assertEquals(1.025, deps.mods.get(player, stats.mining), 0.0000001)
        player.torso = InvObj(objs.diary_varrock_armour_3)
        assertEquals(1.012, deps.mods.get(player, stats.mining), 0.0000001)
    }

    @Test
    fun GameTestState.`carried pieces and free worlds grant no Mining outfit experience`() = runInjectedGameTest(Dependencies::class) { deps ->
        player.members = true
        player.inv[0] = InvObj(ProspectorOutfits.hat)
        assertEquals(1.0, deps.mods.get(player, stats.mining))
        player.hat = InvObj(ProspectorOutfits.hat)
        player.members = false
        assertEquals(1.0, deps.mods.get(player, stats.mining))
    }

    data class Dependencies @Inject constructor(val outfit: MiningOutfitXp) { val mods: XpModifiers = XpModifiers(setOf(outfit)) }
}
