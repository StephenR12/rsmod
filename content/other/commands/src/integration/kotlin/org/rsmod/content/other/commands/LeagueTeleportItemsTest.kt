package org.rsmod.content.other.commands

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.player.events.interact.HeldObjEvents
import org.rsmod.api.player.events.interact.WornObjEvents
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.testing.GameTestState
import org.rsmod.events.EventBus
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.varp.VarpTypeList
import org.rsmod.map.CoordGrid

class LeagueTeleportItemsTest {
    @Test
    fun GameTestState.`native last destination and worn teleport operations are registered`() {
        runInjectedGameTest(Dependencies::class, null, LeagueTeleportItems::class) { deps ->
            for (id in listOf(30361, 33227, 33233)) {
                assertEquals("Last-destination", deps.objects.getValue(id).iop[2])
                assertTrue(deps.events.contains(HeldObjEvents.Op3::class.java, id.toLong()))
            }
            for (id in listOf(30361, 33227)) {
                assertTrue(deps.events.contains(WornObjEvents.Op2::class.java, id.toLong()))
                assertTrue(deps.events.contains(WornObjEvents.Op3::class.java, id.toLong()))
            }
        }
    }

    @Test
    fun GameTestState.`map last destination uses its saved arrival without requiring traversal level`() {
        runInjectedGameTest(Dependencies::class, null, LeagueTeleportItems::class) { deps ->
            val fields =
                requireNotNull(LeagueTeleportItemsTest::class.java.getResourceAsStream("league-agility.tsv"))
                    .bufferedReader().useLines { lines ->
                        lines.first { it.startsWith("Burthorpe: Tight gap\t") }.split('\t')
                    }
            val destination = CoordGrid(fields[1].toInt(), fields[2].toInt(), fields[3].toInt())
            val last = deps.varps.values.first { it.internalName == "league_map_last_destination" }
            player.inv[0] = InvObj(deps.objects.getValue(33233))
            VarPlayerIntMapSetter.set(player, last, destination.packed)

            player.withProtectedAccess { opHeld3(0) }
            advance(4)

            assertEquals(destination, player.coords)
            assertEquals(destination.packed, player.vars[last])
            assertEquals(33233, player.inv[0]?.id)
        }
    }

    private data class Dependencies
    @Inject
    constructor(val events: EventBus, val objects: ObjTypeList, val varps: VarpTypeList)
}
