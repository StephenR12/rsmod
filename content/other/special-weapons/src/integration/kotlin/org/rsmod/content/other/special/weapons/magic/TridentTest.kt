package org.rsmod.content.other.special.weapons.magic

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.styles.MagicAttackStyle
import org.rsmod.api.combat.weapon.scripts.WeaponAttackStylesScript
import org.rsmod.api.combat.weapon.styles.AttackStyles
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.events.interact.BankedObjEvents
import org.rsmod.content.interfaces.bank.BankTab
import org.rsmod.api.player.righthand
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.weapons.*
import org.rsmod.game.inv.InvObj

@Execution(ExecutionMode.SAME_THREAD)
class TridentTest {
    @Test
    fun GameTestState
        .`charging requires exact materials and level and uncharging returns only recoverable ingredients`() =
        runInjectedGameTest(TridentCharging::class) { service ->
            for ((empty, active, toxic) in
                listOf(
                    Triple(11908, 11907, false),
                    Triple(12900, 12899, true),
                    Triple(22290, 22288, false),
                    Triple(22294, 22292, true),
                )) {
                for (i in player.inv.indices) player.inv[i] = null
                player.inv[0] = InvObj(objTypes.getValue(empty))
                player.inv[1] = InvObj(objs.death_rune, 20)
                player.inv[2] = InvObj(objs.chaos_rune, 15)
                player.inv[3] = InvObj(objs.fire_rune, 49)
                val last = if (toxic) objs.zulrahs_scales else objs.coins
                player.inv[4] = InvObj(last, 100)
                player.stats[stats.magic] = 74
                player.withProtectedAccess { assertEquals(0, service.charge(this, 0, 10)) }
                assertEquals(49, player.inv[3]?.count)
                player.stats[stats.magic] = 99
                player.withProtectedAccess { assertEquals(9, service.charge(this, 0, 10)) }
                assertEquals(active, player.inv[0]?.id)
                assertEquals(9, player.inv[0]?.vars)
                assertEquals(4, player.inv[3]?.count)
                assertEquals(if (toxic) 91 else 10, player.inv[4]?.count)
                player.withProtectedAccess { assertTrue(service.uncharge(this, 0)) }
                assertEquals(empty, player.inv[0]?.id)
                assertEquals(20, player.inv.count(objTypes[objs.death_rune]))
                assertEquals(49, player.inv.count(objTypes[objs.fire_rune]))
                assertEquals(if (toxic) 100 else 10, player.inv.count(objTypes[last]))
            }
        }

    @Test
    fun GameTestState
        .`full tradeable trident casts with native four tick cadence and last charge empties each variant`() =
        runInjectedGameTest(Weapons::class, Bindings, WeaponAttackStylesScript::class) { d ->
            with(d.map) { d.repo.register(d.manager) }
            player.stats[stats.magic] = 99
            val npc = spawnNpc(player.coords.translate(3, 0), npcTypes.getValue(100))
            for ((type, count, after) in
                listOf(
                    Triple(11905, 0, 11907),
                    Triple(11907, 1, 11908),
                    Triple(12899, 1, 12900),
                    Triple(22288, 1, 22290),
                    Triple(22292, 1, 22294),
                )) {
                val item = InvObj(objTypes.getValue(type), vars = count)
                player.righthand = item
                val weapon = checkNotNull(d.registry.getMagic(item))
                val attack = CombatAttack.Staff(item, MagicAttackStyle.Accurate)
                player.withProtectedAccess { with(weapon) { assertTrue(attack(npc, attack)) } }
                assertEquals(after, player.righthand?.id)
                assertEquals(if (type == 11905) 2499 else 0, player.righthand?.vars)
                assertEquals(player.currentMapClock + 4, player.actionDelay)
            }
            val item = InvObj(objTypes.getValue(11907), vars = 10)
            player.righthand = item
            val opponent = registerPlayer(player.coords.translate(1, 0))
            val weapon = checkNotNull(d.registry.getMagic(item))
            player.withProtectedAccess {
                with(weapon) {
                    attack(opponent, CombatAttack.Staff(item, MagicAttackStyle.Accurate))
                }
            }
            assertEquals(item, player.righthand)
        }

    @Test
    fun GameTestState
        .`bank target spends matched material sets and leaves an already charged trident untouched`() =
        runGameTest(TridentCharging::class) {
            player.setVarBit(cacheTypes.varbits.getValue(16126), 25)
            player.stats[stats.magic] = 99
            player.inv[0] = InvObj(objTypes.getValue(11907), vars = 30)
            player.withProtectedAccess {
                BankTab.Main.increaseSize(this, 4)
                bank[0] = InvObj(objs.death_rune, 100)
                bank[1] = InvObj(objs.chaos_rune, 100)
                bank[2] = InvObj(objs.fire_rune, 25)
                bank[3] = InvObj(objs.coins, 200)
                suspend fun withdraw() {
                    publish(
                        BankedObjEvents.Autocharge(0, inv[0]!!) { _, _ ->
                            error("the multi-material transaction must be atomic")
                        }
                    )
                }
                withdraw()
                assertEquals(200, bank[3]?.count)
                inv[0] = InvObj(objTypes.getValue(11907), vars = 10)
                withdraw()
                assertEquals(15, inv[0]?.vars)
                assertNull(bank[2])
                assertEquals(150, bank[3]?.count)
                withdraw()
                assertEquals(15, inv[0]?.vars)
                assertEquals(95, bank[0]?.count)
            }
        }

    @Test
    fun GameTestState
        .`Lieve combines deposited and noted tentacles and the enhanced swamp fang is reversible`() =
        runInjectedGameTest(TridentCharging::class) { svc ->
            player.inv[0] = InvObj(objTypes.getValue(11908))
            player.inv[1] = InvObj(objTypes.getValue(12005), 4)
            player.withProtectedAccess { assertFalse(svc.enhance(this, 0)) }
            assertEquals(4, player.inv[1]?.count)
            player.setVarBit(cacheTypes.varbits.getValue(6259), 6)
            player.withProtectedAccess { assertTrue(svc.enhance(this, 0)) }
            assertEquals(22290, player.inv[0]?.id)
            assertNull(player.inv[1])
            player.inv[1] = InvObj(objTypes.getValue(12932))
            player.inv[2] = InvObj(objs.chisel)
            player.stats[stats.crafting] = 58
            player.withProtectedAccess { assertFalse(svc.attachFang(this, 0, 1)) }
            player.stats[stats.crafting] = 59
            player.withProtectedAccess {
                assertTrue(svc.attachFang(this, 0, 1))
                assertEquals(22294, inv[0]?.id)
                assertTrue(svc.dismantle(this, 0))
            }
            assertEquals(22290, player.inv[0]?.id)
            assertEquals(1, player.inv.count(objTypes.getValue(12932)))
        }

    @Test
    fun GameTestState
        .`warped sceptre and both Sanguinesti forms charge fire their last shot and return unused runes`() =
        runInjectedGameTest(
            Weapons::class,
            Bindings,
            WeaponAttackStylesScript::class,
            PoweredStaffCharging::class,
        ) { d ->
            with(d.other) { d.repo.register(d.manager) }
            player.stats[stats.magic] = 99
            val npc = spawnNpc(player.coords.translate(3, 0), npcTypes.getValue(100))
            for ((empty, active) in listOf(28583 to 28585, 22481 to 22323, 25733 to 25731)) {
                for (i in player.inv.indices) player.inv[i] = null
                val warped = active == 28585
                player.inv[0] = InvObj(objTypes.getValue(empty))
                player.inv[1] =
                    InvObj(if (warped) objs.chaos_rune else objs.blood_rune, if (warped) 8 else 12)
                if (warped) player.inv[2] = InvObj(objs.earth_rune, 20)
                player.withProtectedAccess {
                    assertEquals(3, d.charging.charge(this, 0, 3))
                    assertTrue(d.charging.uncharge(this, 0))
                    assertEquals(if (warped) 8 else 12, inv[1]?.count)
                    assertEquals(1, d.charging.charge(this, 0, 1))
                }
                val item = player.inv[0]!!
                assertEquals(active, item.id)
                assertEquals(1, item.vars)
                player.righthand = item
                player.inv[0] = null
                val weapon = checkNotNull(d.registry.getMagic(item))
                player.withProtectedAccess {
                    with(weapon) {
                        attack(npc, CombatAttack.Staff(item, MagicAttackStyle.Accurate))
                    }
                }
                assertEquals(empty, player.righthand?.id)
                assertEquals(player.currentMapClock + 4, player.actionDelay)
            }
        }

    @Test
    fun GameTestState
        .`shadow withdraw target spends complete rune sets and preserves excess existing charges`() =
        runGameTest(PoweredStaffCharging::class) {
            player.setVarBit(cacheTypes.varbits.getValue(16156), 10)
            player.inv[0] = InvObj(objs.tumekens_shadow_uncharged)
            player.withProtectedAccess {
                BankTab.Main.increaseSize(this, 2)
                bank[0] = InvObj(objs.chaos_rune, 54)
                bank[1] = InvObj(objs.soul_rune, 19)
                suspend fun withdraw() {
                    publish(
                        BankedObjEvents.Autocharge(0, inv[0]!!) { _, _ ->
                            error("material transaction must stay atomic")
                        }
                    )
                }
                withdraw()
                assertEquals(objs.tumekens_shadow.id, inv[0]?.id)
                assertEquals(9, inv[0]?.vars)
                assertEquals(9, bank[0]?.count)
                assertEquals(1, bank[1]?.count)
                vars[cacheTypes.varbits.getValue(16156)] = 5
                withdraw()
                assertEquals(9, inv[0]?.vars)
                assertEquals(9, bank[0]?.count)
            }
        }

    private object Bindings : AbstractModule() {
        override fun configure() {
            bind(AttackStyles::class.java).`in`(Scopes.SINGLETON)
            install(WeaponModule())
        }
    }

    private class Weapons
    @Inject
    constructor(
        val other: OtherPoweredStaves,
        val charging: PoweredStaffCharging,
        val map: KrakenTridents,
        val repo: WeaponRepository,
        val registry: WeaponRegistry,
        val manager: WeaponAttackManager,
    )
}
