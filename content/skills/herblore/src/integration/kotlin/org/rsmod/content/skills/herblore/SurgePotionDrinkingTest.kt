package org.rsmod.content.skills.herblore

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.timers
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.events.PlayerDeathEvent
import org.rsmod.api.player.events.interact.HeldObjEvents
import org.rsmod.api.player.interact.HeldInteractions
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.realm.Realm
import org.rsmod.api.realm.WorldFlag
import org.rsmod.api.specials.SpecialAttackModule
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.game.interact.HeldOp
import org.rsmod.game.inv.InvObj
import org.rsmod.game.spot.EntitySpotanim
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.varbit.VarBitTypeList
import org.rsmod.game.type.varp.VarpTransmitLevel

@Execution(ExecutionMode.SAME_THREAD)
class SurgePotionDrinkingTest {
    @Test
    fun GameTestState.`cache 233 Surge doses timer graphic and Drink dispatch are pinned`() =
        runSurgeTest { deps ->
            assertEquals(
                listOf(30875, 30878, 30881, 30884),
                SurgePotionDoses.all.map { it.potion.id },
            )
            assertEquals(
                listOf(30878, 30881, 30884, 229),
                SurgePotionDoses.all.map { it.replacement?.id },
            )
            assertEquals(
                listOf(
                    "4 doses of surge potion.",
                    "3 doses of surge potion.",
                    "2 doses of surge potion.",
                    "1 dose of surge potion.",
                ),
                SurgePotionDoses.all.map { deps.objs[it.potion].desc },
            )
            assertEquals(829, seqs.human_eat.id)
            assertEquals(3232, spotanims.surge_potion.id)
            assertEquals(16270, varbits.surge_potion_timer.id)
            assertEquals(22, timers.surge_potion_cooldown.id)
            assertEquals(98, herblore_queues.surgePotionRestore.id)
            assertEquals(65501, varps.surge_potion_cooldown_cycles.id)
            assertEquals(VarpTransmitLevel.Never, varps.surge_potion_cooldown_cycles.transmit)
            for (dose in SurgePotionDoses.all) {
                val type = deps.objs[dose.potion]
                assertEquals("Drink", type.iop[0])
                assertTrue(type.members)
                assertFalse(type.stockmarket)
                assertTrue(deps.events.contains(HeldObjEvents.Op1::class.java, dose.potion.id))
            }
        }

    @Test
    fun GameTestState
        .`Surge potion consumes every dose and restores exactly twenty five percent`() =
        runSurgeTest { deps ->
            prepare(deps)
            repeat(player.inv.size) { player.inv[it] = InvObj(objs.coins) }
            player.inv[SLOT] = InvObj(objs.surge_potion_4)

            for (expected in SurgePotionDoses.all) {
                setSpecialEnergy(400)
                deps.cooldown.clear(player)
                client.clear()
                drink(deps)

                assertEquals(expected.replacement?.id, player.inv[SLOT]?.id)
                assertEquals(player.inv.size, player.inv.count { it != null })
                assertEquals(650, specialEnergy())
                assertEquals(seqs.human_eat.id, player.pendingSequence.id)
                assertEquals(spotanims.surge_potion.id, pendingSpotanim())
                assertEquals(
                    SurgePotionCooldownRepository.DURATION_CYCLES,
                    deps.cooldown.remainingCycles(player),
                )
                assertEquals(50, player.vars[varbits.surge_potion_timer])
                assertMessageSent(SurgePotionDrinking.DRINK_MESSAGE)
                if (expected.dosesRemaining == 0) {
                    assertMessageSent(SurgePotionDrinking.FINISHED_MESSAGE)
                } else {
                    val doseWord = if (expected.dosesRemaining == 1) "dose" else "doses"
                    assertMessageSent(
                        "You have ${expected.dosesRemaining} $doseWord of potion left."
                    )
                }
                if (expected.potion == objs.surge_potion_4) {
                    client.clear()
                    setSpecialEnergy(400)
                    drink(deps)
                    assertEquals(expected.replacement?.id, player.inv[SLOT]?.id)
                    assertEquals(400, specialEnergy())
                    assertTrue(client.outgoingMessages.isEmpty())
                }
                player.pendingSpotanims.clear()
                advance(SurgePotionDrinking.POTION_DELAY_CYCLES)
            }

            prepare(deps)
            repeat(player.inv.size) { player.inv[it] = InvObj(objs.coins) }
            player.inv[SLOT] = InvObj(objs.surge_potion_1)
            player.setVarBit(varbits.auto_smash_vials, 1)
            setSpecialEnergy(400)

            drink(deps)

            assertEquals(null, player.inv[SLOT])
            assertEquals(0, player.inv.count(objs.empty_vial))
            assertEquals(650, specialEnergy())
        }

    @Test
    fun GameTestState
        .`Surge potion rejects full special cooldown free worlds and stale slots atomically`() =
        runSurgeTest { deps ->
            prepare(deps)
            player.inv[SLOT] = InvObj(objs.surge_potion_4)
            setSpecialEnergy(1000)
            drink(deps)
            assertEquals(objs.surge_potion_4.id, player.inv[SLOT]?.id)
            assertEquals(0, deps.cooldown.remainingCycles(player))
            assertMessageSent(SurgePotionDrinking.NO_EFFECT_MESSAGE)

            setSpecialEnergy(500)
            client.clear()
            deps.cooldown.start(player)
            drink(deps)
            assertEquals(objs.surge_potion_4.id, player.inv[SLOT]?.id)
            assertEquals(500, specialEnergy())
            assertMessageSent(
                "You are still full of adrenaline. You'll be able to drink in 5 minutes."
            )

            prepare(deps, members = false)
            player.inv[SLOT] = InvObj(objs.surge_potion_4)
            drink(deps)
            assertEquals(objs.surge_potion_4.id, player.inv[SLOT]?.id)
            assertMessageSent(SurgePotionDrinking.MEMBERS_MESSAGE)

            prepare(deps)
            player.inv[SLOT] = InvObj(objs.logs)
            player.withProtectedAccess {
                deps.drinking.drink(this, SLOT, SurgePotionDoses.all.first())
            }
            assertEquals(objs.logs.id, player.inv[SLOT]?.id)
            assertEquals(0, deps.cooldown.remainingCycles(player))
        }

    @Test
    fun GameTestState
        .`Surge cooldown is exactly five hundred ticks pauses offline and drives client timer`() =
        runSurgeTest { deps ->
            prepare(deps)
            deps.cooldown.start(player)

            advance(9)
            assertEquals(491, deps.cooldown.remainingCycles(player))
            assertEquals(50, player.vars[varbits.surge_potion_timer])
            advance()
            assertEquals(490, deps.cooldown.remainingCycles(player))
            assertEquals(49, player.vars[varbits.surge_potion_timer])

            deps.events.publish(SessionStateEvent.Logout(player))
            advance(100)
            assertEquals(490, deps.cooldown.remainingCycles(player))
            deps.events.publish(SessionStateEvent.Login(player))
            advance(489)
            assertEquals(1, deps.cooldown.remainingCycles(player))
            assertEquals(1, player.vars[varbits.surge_potion_timer])
            advance()
            assertEquals(0, deps.cooldown.remainingCycles(player))
            assertEquals(0, player.vars[varbits.surge_potion_timer])
            assertMessageSent("<col=e00a19>${SurgePotionCooldownRepository.EXPIRED_MESSAGE}</col>")
        }

    @Test
    fun GameTestState.`PvP Surge restore lands after six ticks at fifteen tiles but not beyond`() =
        runSurgeTest { deps ->
            prepare(deps)
            player.setVarBit(varbits.inside_wilderness, 1)
            player.inv[SLOT] = InvObj(objs.surge_potion_4)
            setSpecialEnergy(400)
            val origin = player.coords

            drink(deps)

            assertEquals(400, specialEnergy())
            assertTrue(herblore_queues.surgePotionRestore in player.queueList)
            advance(5)
            assertEquals(400, specialEnergy())
            player.coords = origin.translateX(15)
            advance()
            assertEquals(650, specialEnergy())

            prepare(deps)
            player.setVarBit(varbits.pvp_area_client, 1)
            player.inv[SLOT] = InvObj(objs.surge_potion_4)
            setSpecialEnergy(400)
            val secondOrigin = player.coords
            drink(deps)
            player.coords = secondOrigin.translateX(16)
            advance(6)
            assertEquals(400, specialEnergy())
            assertTrue(player.pendingSpotanims.isEmpty)
        }

    @Test
    fun GameTestState
        .`PvP-world delay caps restoration and death cancels cooldown and pending restore`() =
        runSurgeTest { deps ->
            prepare(deps)
            deps.realm.updateConfig(deps.realm.config.copy(worldFlags = WorldFlag.Pvp.mask))
            player.inv[SLOT] = InvObj(objs.surge_potion_4)
            setSpecialEnergy(900)

            drink(deps)
            advance(6)
            assertEquals(1000, specialEnergy())

            prepare(deps)
            deps.realm.updateConfig(deps.realm.config.copy(worldFlags = WorldFlag.Pvp.mask))
            player.inv[SLOT] = InvObj(objs.surge_potion_4)
            setSpecialEnergy(400)
            drink(deps)
            deps.events.publish(PlayerDeathEvent.Completed(player))
            assertEquals(0, deps.cooldown.remainingCycles(player))
            assertEquals(0, player.vars[varbits.surge_potion_timer])
            assertFalse(herblore_queues.surgePotionRestore in player.queueList)
            advance(6)
            assertEquals(400, specialEnergy())
        }

    @Test
    fun `cooldown warning formats cycle boundaries without losing partial seconds`() {
        val player = Player()
        val cooldown = SurgePotionCooldownRepository()
        player.vars.backing[varps.surge_potion_cooldown_cycles.id] = 101
        assertEquals(
            "You are still full of adrenaline. You'll be able to drink in 1 minute and 1 second.",
            cooldown.rejectionMessage(player),
        )
        player.vars.backing[varps.surge_potion_cooldown_cycles.id] = 10
        assertEquals(
            "You are still full of adrenaline. You'll be able to drink in 6 seconds.",
            cooldown.rejectionMessage(player),
        )
    }

    private fun GameTestScope.prepare(deps: Dependencies, members: Boolean = true) {
        player.clearAllInvs()
        player.members = members
        player.actionDelay = player.currentMapClock
        player.setVarBit(varbits.auto_smash_vials, 0)
        player.setVarBit(varbits.inside_wilderness, 0)
        player.setVarBit(varbits.pvp_area_client, 0)
        deps.realm.updateConfig(deps.realm.config.copy(worldFlags = 0))
        deps.drinking.cancelPendingRestore(player)
        deps.cooldown.clear(player)
        player.pendingSpotanims.clear()
        client.clear()
    }

    private fun GameTestScope.drink(deps: Dependencies, slot: Int = SLOT) {
        player.withProtectedAccess { deps.held.interact(this, player.inv, slot, HeldOp.Op1) }
    }

    private fun GameTestScope.pendingSpotanim(): Int =
        EntitySpotanim(player.pendingSpotanims.getLong(0)).id

    private fun GameTestScope.setSpecialEnergy(value: Int) {
        player.vars.backing[varps.sa_energy.id] = value
    }

    private fun GameTestScope.specialEnergy(): Int = player.specialEnergy

    private fun GameTestState.runSurgeTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(Dependencies::class, TestModule, HerbloreScript::class, testBody = body)

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(HerbloreModule())
            install(SpecialAttackModule())
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val drinking: SurgePotionDrinking,
        val cooldown: SurgePotionCooldownRepository,
        val held: HeldInteractions,
        val events: EventBus,
        val objs: ObjTypeList,
        val varbits: VarBitTypeList,
        val realm: Realm,
    )

    private companion object {
        const val SLOT = 6
    }
}

private var Player.specialEnergy by intVarp(varps.sa_energy)
