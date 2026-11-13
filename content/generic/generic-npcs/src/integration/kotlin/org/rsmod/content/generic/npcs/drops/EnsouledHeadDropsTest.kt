package org.rsmod.content.generic.npcs.drops

import jakarta.inject.Inject
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.objs
import org.rsmod.api.death.*
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.inv.FreshDropOrigin

class EnsouledHeadDropsTest {
    @Test
    fun GameTestState
        .`all added head families roll with fresh provenance and compose with existing loot`() =
        runInjectedGameTest(Deps::class, null, EnsouledHeadDrops::class) { d ->
            player.members = true
            val representatives =
                mapOf(
                    2848 to 13450,
                    2481 to 13456,
                    3024 to 13459,
                    2839 to 13462,
                    114 to 13468,
                    520 to 13471,
                    2098 to 13474,
                    136 to 13477,
                    5293 to 13480,
                    936 to 13483,
                    1047 to 13486,
                    955 to 13489,
                    970 to 13492,
                    484 to 13495,
                    2173 to 13498,
                    2005 to 13501,
                    3169 to 13504,
                    415 to 13507,
                    260 to 13510,
                    104 to 26996,
                )
            for ((id, head) in representatives) {
                val type = npcTypes.getValue(id)
                d.tables.register(type, NpcDropTable(NpcDropRolls.item(objs.coins)))
                val npc = spawnNpc(player.coords, type)
                val ctx = NpcDropContext(npc, player, npc.coords, d.random)
                random.next = 0
                val drops = requireNotNull(d.tables[type]).roll(ctx)
                assertEquals(listOf(objs.coins.id, head), drops.map { it.obj.id })
                assertEquals(FreshDropOrigin.pack(npc.coords), drops.last().obj.vars)
                assertEquals(player, drops.last().receiver)
                random.next = 1
                assertEquals(
                    listOf(objs.coins.id),
                    requireNotNull(d.tables[type]).roll(ctx).map { it.obj.id },
                )
                player.members = false
                assertEquals(
                    listOf(objs.coins.id),
                    requireNotNull(d.tables[type]).roll(ctx).map { it.obj.id },
                )
                player.members = true
            }
        }

    private class Deps
    @Inject
    constructor(val tables: NpcDropTableRegistry, val random: org.rsmod.api.random.GameRandom)
}
