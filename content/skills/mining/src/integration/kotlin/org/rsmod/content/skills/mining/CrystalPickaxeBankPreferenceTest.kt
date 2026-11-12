package org.rsmod.content.skills.mining

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.events.interact.BankedObjEvents
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.mining.configs.MiningObjs
import org.rsmod.content.skills.mining.scripts.Mining
import org.rsmod.events.EventBus
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.varbit.VarBitTypeList
import org.rsmod.game.type.varp.VarpLifetime

class CrystalPickaxeBankPreferenceTest {
    @Test
    fun GameTestState.`cache 233 pins crystal pickaxe bank preference contract`() =
        runPreferenceTest { deps ->
            assertEquals(23_680, MiningObjs.crystalPickaxe.id)
            assertEquals(23_682, MiningObjs.crystalPickaxeInactive.id)
            assertEquals(
                4_338,
                deps.objs[MiningObjs.crystalPickaxe].paramOrNull(params.bank_autocharge)?.id,
            )
            val quantity = deps.varbits[varbits.charges_crystal_tools_quantity]
            assertEquals(16_148, quantity.id)
            assertEquals(4_681, quantity.baseVar.id)
            assertEquals(0..15, quantity.bits)
            assertEquals(VarpLifetime.Perm, quantity.baseVar.scope)
            for (pickaxe in listOf(MiningObjs.crystalPickaxe, MiningObjs.crystalPickaxeInactive)) {
                assertTrue(
                    deps.events.contains(
                        BankedObjEvents.ConfigureCharges::class.java,
                        pickaxe.id.toLong(),
                    )
                )
            }
        }

    @Test
    fun GameTestState.`crystal pickaxe bank preference is capped permanent and members only`() =
        runPreferenceTest { deps ->
            player.members = true
            player.withProtectedAccess { deps.preference.configureCharges(this) }
            player.resumeCountDialog(25_000)
            advance()
            assertEquals(20_000, player.vars[varbits.charges_crystal_tools_quantity])

            player.members = false
            client.clear()
            player.withProtectedAccess { deps.preference.configureCharges(this) }
            assertEquals(20_000, player.vars[varbits.charges_crystal_tools_quantity])
            assertMessageSent("You need to be on a members' world to configure a crystal pickaxe.")
        }

    private fun GameTestState.runPreferenceTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(Dependencies::class, childModule = null, Mining::class, testBody = body)

    data class Dependencies
    @Inject
    constructor(
        val preference: CrystalPickaxeBankPreference,
        val events: EventBus,
        val objs: ObjTypeList,
        val varbits: VarBitTypeList,
    )
}
