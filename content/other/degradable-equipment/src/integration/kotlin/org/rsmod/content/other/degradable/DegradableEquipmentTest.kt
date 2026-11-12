package org.rsmod.content.other.degradable

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.combat.manager.PlayerAttackManager
import org.rsmod.api.config.refs.npcs
import org.rsmod.api.config.refs.objs
import org.rsmod.api.player.events.interact.HeldObjEvents
import org.rsmod.api.player.events.interact.WornObjEvents
import org.rsmod.api.player.interact.HeldInteractions
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.events.EventBus
import org.rsmod.game.interact.HeldOp
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.obj.Wearpos

@Execution(ExecutionMode.SAME_THREAD)
class DegradableEquipmentTest {
    @Test
    fun GameTestState.`cache 233 degradation graph and check operations are pinned`() =
        runDegradationTest { deps ->
            assertEquals(
                28,
                DegradableEquipmentCatalog.definitions.count {
                    it.family == DegradableEquipmentFamily.Barrows
                },
            )
            assertEquals(
                9,
                DegradableEquipmentCatalog.definitions.count {
                    it.family == DegradableEquipmentFamily.Moon
                },
            )
            assertEquals(195, DegradableEquipmentCatalog.byType.size)
            assertEquals(4880, objs.dharoks_helm_100.id)
            assertEquals(4884, objs.dharoks_helm_0.id)
            assertEquals(29035, objs.eclipse_moon_helm_used.id)
            assertEquals(29055, objs.eclipse_moon_helm_broken.id)
            assertEquals("Check", deps.objs[objs.dharoks_helm_100].iop[2])
            assertEquals("Check", deps.objs[objs.eclipse_moon_helm_used].iop[2])
            assertFalse(deps.objs[objs.eclipse_moon_helm_broken].isEquipable)

            for (definition in DegradableEquipmentCatalog.definitions) {
                assertTrue(deps.objs[definition.repaired].tradeable)
                assertTrue(deps.objs[definition.broken].tradeable)
                for (active in definition.activeStages) {
                    assertTrue(
                        deps.events.contains(HeldObjEvents.Op3::class.java, active.id.toLong())
                    )
                    assertTrue(
                        deps.events.contains(WornObjEvents.Op2::class.java, active.id.toLong())
                    )
                }
            }
        }

    @Test
    fun GameTestState.`dynamic tradeability permits only repaired and broken states`() =
        runDegradationTest {
            player.withProtectedAccess {
                assertTrue(ocTradable(InvObj(objs.dharoks_helm)))
                assertFalse(ocTradable(InvObj(objs.dharoks_helm_100, vars = 999)))
                assertFalse(ocTradable(InvObj(objs.dharoks_helm_25, vars = 1)))
                assertTrue(ocTradable(InvObj(objs.dharoks_helm_0)))

                assertTrue(ocTradable(InvObj(objs.blood_moon_helm)))
                assertFalse(ocTradable(InvObj(objs.blood_moon_helm_used, vars = 2_999)))
                assertTrue(ocTradable(InvObj(objs.blood_moon_helm_broken)))
            }
        }

    @Test
    fun GameTestState.`combat entry degrades every eligible worn piece once`() =
        runDegradationTest { deps ->
            player.worn[Wearpos.Head.slot] = InvObj(objs.dharoks_helm)
            player.worn[Wearpos.Torso.slot] = InvObj(objs.eclipse_moon_chestplate)
            player.worn[Wearpos.RightHand.slot] = InvObj(objs.blue_moon_spear)

            deps.lifecycle.engage(player)

            assertEquals(InvObj(objs.dharoks_helm_100, vars = 999), player.worn[Wearpos.Head.slot])
            assertEquals(
                InvObj(objs.eclipse_moon_chestplate_used, vars = 2_999),
                player.worn[Wearpos.Torso.slot],
            )
            assertEquals(InvObj(objs.blue_moon_spear), player.worn[Wearpos.RightHand.slot])

            deps.lifecycle.engage(player)
            assertEquals(999, player.worn[Wearpos.Head.slot]?.vars)
            assertEquals(2_999, player.worn[Wearpos.Torso.slot]?.vars)
        }

    @Test
    fun GameTestState
        .`continuous combat degrades after 90 ticks and re-entry degrades immediately`() =
        runDegradationTest { deps ->
            player.currentMapClock = 0
            player.worn[Wearpos.Head.slot] = InvObj(objs.karils_coif)
            deps.lifecycle.engage(player)

            for (tick in 8..88 step 8) {
                player.currentMapClock = tick
                deps.lifecycle.engage(player)
            }
            player.currentMapClock = 90
            deps.lifecycle.combatTimer(player)
            assertEquals(998, player.worn[Wearpos.Head.slot]?.vars)

            player.currentMapClock = 100
            deps.lifecycle.combatTimer(player)
            assertEquals(998, player.worn[Wearpos.Head.slot]?.vars)

            player.currentMapClock = 110
            deps.lifecycle.engage(player)
            assertEquals(997, player.worn[Wearpos.Head.slot]?.vars)
        }

    @Test
    fun GameTestState.`quarter boundaries and zero remain in the worn slot`() =
        runDegradationTest { deps ->
            player.worn[Wearpos.Head.slot] = InvObj(objs.dharoks_helm_100, vars = 751)
            deps.lifecycle.engage(player)
            assertEquals(InvObj(objs.dharoks_helm_75, vars = 750), player.worn[Wearpos.Head.slot])

            deps.lifecycle.clear(player)
            player.currentMapClock = 20
            player.worn[Wearpos.Head.slot] = InvObj(objs.dharoks_helm_25, vars = 1)
            deps.lifecycle.engage(player)
            assertEquals(InvObj(objs.dharoks_helm_0, vars = 0), player.worn[Wearpos.Head.slot])
        }

    @Test
    fun GameTestState.`standard player attack publishes combat entry before impact`() =
        runDegradationTest { deps ->
            player.worn[Wearpos.Head.slot] = InvObj(objs.ahrims_hood)
            val target = spawnNpc(player.coords.translate(1, 0), deps.npcs[npcs.cow])

            deps.attacks.queueMeleeHit(player, target, damage = 0)

            assertEquals(InvObj(objs.ahrims_hood_100, vars = 999), player.worn[Wearpos.Head.slot])
        }

    @Test
    fun GameTestState.`confirmed ground drop breaks Barrows and Moon armour`() =
        runDegradationTest { deps ->
            player.observerUUID = player.uuid
            player.inv[0] = InvObj(objs.veracs_brassard_50, vars = 444)
            player.withProtectedAccess { deps.held.interact(this, player.inv, 0, HeldOp.Op5) }

            assertEquals(null, player.inv[0])
            var ground = deps.objRepo.findAll(player.coords).single()
            assertEquals(objs.veracs_brassard_0.id, ground.type)
            assertEquals(0, ground.vars)

            deps.objRepo.del(ground)
            player.inv[0] = InvObj(objs.blood_moon_helm)
            player.withProtectedAccess { deps.held.interact(this, player.inv, 0, HeldOp.Op5) }
            ground = deps.objRepo.findAll(player.coords).single()
            assertEquals(objs.blood_moon_helm_broken.id, ground.type)
            assertEquals(0, ground.vars)
        }

    private fun GameTestState.runDegradationTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            DegradableEquipmentScript::class,
            testBody = body,
        )

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(DegradableEquipmentModule())
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val lifecycle: DegradableEquipmentLifecycle,
        val held: HeldInteractions,
        val objRepo: ObjRepository,
        val attacks: PlayerAttackManager,
        val objs: ObjTypeList,
        val npcs: NpcTypeList,
        val events: EventBus,
    )
}
