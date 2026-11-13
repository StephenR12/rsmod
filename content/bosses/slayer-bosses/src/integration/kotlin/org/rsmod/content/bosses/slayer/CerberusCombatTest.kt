package org.rsmod.content.bosses.slayer

import jakarta.inject.Inject
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.hit.plugin.PlayerHitScript
import org.rsmod.api.npc.events.NpcKillEvents
import org.rsmod.api.player.magic.WardOfArceuusStateRepository
import org.rsmod.api.player.stat.stat
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.Wearpos
import org.rsmod.map.CoordGrid

class CerberusCombatTest {
    class Deps
    @Inject
    constructor(
        val combat: CerberusCombat,
        val npcs: NpcRepository,
        val ward: WardOfArceuusStateRepository,
    )

    @Test
    fun GameTestState
        .`souls launch west to east sample each overhead and drain the protected target`() =
        runInjectedGameTest(Deps::class, null, CerberusCombat::class, PlayerHitScript::class) { deps
            ->
            val lair = CoordGrid(1238, 1250)
            val boss = spawnNpc(lair, npcTypes.getValue(5862))
            boss.mode = NpcMode.None
            boss.hitpoints = 300
            player.placeAt(lair.translate(4, -3))
            player.setBaseLevel(stats.hitpoints, 99)
            player.setCurrentLevel(stats.hitpoints, 99)
            player.setBaseLevel(stats.prayer, 99)
            player.setCurrentLevel(stats.prayer, 99)
            player.withProtectedAccess {
                vars[varbits.protect_from_magic] = 1
                vars[varbits.protect_from_missiles] = 1
                vars[varbits.protect_from_melee] = 1
            }
            repeat(6) {
                deps.combat.attack(boss, player)
                advance(6)
            }
            random.next = 1
            repeat(3) { random.then = 0 }
            deps.combat.attack(boss, player)
            val souls =
                (-3..3 step 3).map { dx -> deps.npcs.findAll(lair.translate(dx, 7)).single() }
            assertEquals(listOf(5869, 5867, 5868), souls.map { it.id })
            player.worn[Wearpos.LeftHand.slot] = InvObj(objTypes.getValue(12821))
            deps.ward.activate(player, 40)
            advance(8)
            assertEquals(89, player.stat(stats.prayer))
            player.withProtectedAccess { vars[varbits.protect_from_missiles] = 0 }
            advance(2)
            assertEquals(89, player.stat(stats.prayer))
            advance(2)
            assertEquals(79, player.stat(stats.prayer))
            assertEquals(69, player.stat(stats.hitpoints))
            assertTrue(souls.none { it.isSlotAssigned })
        }

    @Test
    fun GameTestState
        .`chip damage from lava stops after moving and a dead Cerberus releases souls`() =
        runInjectedGameTest(Deps::class, null, CerberusCombat::class, PlayerHitScript::class) { deps
            ->
            val lair = CoordGrid(1238, 1250)
            val boss = spawnNpc(lair, npcTypes.getValue(5862))
            boss.mode = NpcMode.None
            boss.hitpoints = 190
            player.placeAt(lair.translate(4, -3))
            player.setBaseLevel(stats.hitpoints, 99)
            player.setCurrentLevel(stats.hitpoints, 99)
            player.setCurrentLevel(stats.prayer, 50)
            player.withProtectedAccess {
                vars[varbits.protect_from_missiles] = 1
                vars[varbits.protect_from_magic] = 1
                vars[varbits.protect_from_melee] = 1
                vars[varbits.super_antifire_potion] = 10
            }
            repeat(4) {
                deps.combat.attack(boss, player)
                advance(6)
            }
            random.next = 1
            random.then = 0
            random.then = 0
            deps.combat.attack(boss, player)
            advance(6)
            assertEquals(78, player.stat(stats.hitpoints))
            player.placeAt(lair.translate(8, -2))
            deps.combat.attack(boss, player)
            advance(6)
            assertEquals(78, player.stat(stats.hitpoints))
            random.next = 1
            deps.combat.attack(boss, player)
            val spirits = (-3..3 step 3).flatMap { deps.npcs.findAll(lair.translate(it, 7)) }
            assertEquals(3, spirits.size)
            eventBus.publish(NpcKillEvents.Credited(player, boss))
            advance(12)
            assertTrue(spirits.none { it.isSlotAssigned })
            assertEquals(78, player.stat(stats.hitpoints))
        }
}
