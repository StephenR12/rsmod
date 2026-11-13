package org.rsmod.content.skills.herblore.mixology

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.*
import org.rsmod.api.player.interact.HeldInteractions
import org.rsmod.api.specials.SpecialAttackModule
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.interfaces.bank.openBankWithoutEvents
import org.rsmod.content.interfaces.bank.scripts.BankInvScript
import org.rsmod.content.skills.herblore.*
import org.rsmod.content.skills.magic.spell.utility.standard.PotionProtectionScript
import org.rsmod.events.EventBus
import org.rsmod.game.interact.HeldOp
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.type.interf.IfButtonOp
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class MixologyContainersTest {
    @Test
    fun GameTestState
        .`all native storage families accept raw and noted doses without item arithmetic`() =
        game { d ->
            setup(d)
            assertEquals(104, d.store.kinds.size)
            for (kind in d.store.kinds) {
                assertTrue(kind.raw.isNotEmpty())
                for ((dose, id) in kind.raw + kind.noted.mapKeys { it.key + 10 }) {
                    player.inv[1] = InvObj(d.items.getValue(id), 1)
                    val expected = if (dose > 10) dose - 10 else dose
                    val before = player.vars[kind.counter]
                    assertEquals(1, d.store.deposit(player, player.inv, 1, 1), "$id")
                    assertEquals(before + expected, player.vars[kind.counter], "$id")
                }
            }
        }

    @Test
    fun GameTestState
        .`storage preserves selected dose preference and returns a final partial bottle`() =
        game { d ->
            setup(d)
            val k = d.store.byItem.getValue(objs.attack_potion_3.id).kind
            player.inv[1] = InvObj(objs.attack_potion_3)
            assertEquals(1, d.store.deposit(player, player.inv, 1, 1))
            assertEquals(3, d.store.preferred(player, k))
            player.inv[1] = InvObj(objs.attack_potion_4)
            d.store.deposit(player, player.inv, 1, 1)
            assertEquals(3, d.store.preferred(player, k))
            d.store.setDose(player, k, 4)
            d.access.launchLenient(player) {
                assertEquals(2, d.store.withdraw(this, k, Int.MAX_VALUE))
            }
            assertEquals(1, player.count(objs.attack_potion_4))
            assertEquals(1, player.count(objs.attack_potion_3))
            assertEquals(0, player.vars[k.counter])
        }

    @Test
    fun GameTestState.`storage withdrawal respects inventory space and noted output`() = game { d ->
        setup(d)
        val k = d.store.byItem.getValue(objs.attack_potion_4.id).kind
        player.setVarp(k.counter, 11)
        d.store.setDose(player, k, 4)
        for (i in player.inv.indices) player.inv[i] = InvObj(objs.coins)
        d.access.launchLenient(player) { assertEquals(0, d.store.withdraw(this, k, 99)) }
        assertEquals(11, player.vars[k.counter])
        player.inv[1] = null
        d.access.launchLenient(player) { assertEquals(1, d.store.withdraw(this, k, 99)) }
        assertEquals(7, player.vars[k.counter])
        player.clearInv()
        d.access.launchLenient(player) { assertEquals(2, d.store.withdraw(this, k, 99, true)) }
        assertEquals(1, player.count(d.items.getValue(d.items[objs.attack_potion_4].certlink)))
        assertEquals(1, player.count(d.items.getValue(d.items[objs.attack_potion_3].certlink)))
    }

    @Test
    fun GameTestState.`locked nonmember overflow and full bank new types preserve input`() =
        game { d ->
            setup(d)
            player.inv[1] = InvObj(objs.attack_potion_4)
            player.setVarBit(MixBits.potionStorage, 0)
            assertEquals(0, d.store.deposit(player, player.inv, 1, 1))
            player.setVarBit(MixBits.potionStorage, 1)
            player.members = false
            assertEquals(0, d.store.deposit(player, player.inv, 1, 1))
            player.members = true
            val k = d.store.byItem.getValue(objs.attack_potion_4.id).kind
            player.setVarp(k.counter, Int.MAX_VALUE - 3)
            assertEquals(0, d.store.deposit(player, player.inv, 1, 1))
            player.setVarp(k.counter, 0)
            player.setVarBit(varbits.bank_capacity, 1)
            bank(d)[0] = InvObj(objs.coins)
            assertEquals(0, d.store.deposit(player, player.inv, 1, 1))
            player.setVarp(k.counter, 1)
            assertEquals(1, d.store.deposit(player, player.inv, 1, 1))
            assertEquals(5, player.vars[k.counter])
        }

    @Test
    fun GameTestState.`native bank toggle and side deposit route eligible potions to storage`() =
        game { d ->
            setup(d)
            player.ifButton(PotionStoreComps.toggle)
            advance()
            assertEquals(15, player.vars[PotionStoreBits.currentTab])
            player.inv[1] = InvObj(objs.attack_potion_4)
            d.access.launchLenient(player) { assertTrue(d.bank.depositInventorySlot(this, 1, 1)) }
            assertEquals(
                4,
                player.vars[d.store.byItem.getValue(objs.attack_potion_4.id).kind.counter],
            )
            assertNull(player.inv[1])
            assertTrue(bank(d).isEmpty())
            player.inv[2] = InvObj(objs.coins, 17)
            d.access.launchLenient(player) { assertTrue(d.bank.depositInventorySlot(this, 2, 17)) }
            assertEquals(17, bank(d).count(d.items[objs.coins]))
            player.ifButton(PotionStoreComps.toggle)
            advance()
            assertEquals(0, player.vars[PotionStoreBits.currentTab])
        }

    @Test
    fun GameTestState.`ordinary bank deposit remains available before storage unlock`() =
        game { d ->
            setup(d)
            player.setVarBit(MixBits.potionStorage, 0)
            player.inv[1] = InvObj(objs.attack_potion_4)
            d.access.launchLenient(player) { assertTrue(d.bank.depositInventorySlot(this, 1, 1)) }
            assertEquals(1, bank(d).count(d.items[objs.attack_potion_4]))
            assertEquals(
                0,
                player.vars[d.store.byItem.getValue(objs.attack_potion_4.id).kind.counter],
            )
        }

    @Test
    fun GameTestState.`native storage item button withdraws its referenced potion`() = game { d ->
        setup(d)
        player.setVarBit(PotionStoreBits.currentTab, 15)
        val k = d.store.byItem.getValue(objs.attack_potion_4.id).kind
        player.setVarp(k.counter, 8)
        d.store.setDose(player, k, 4)
        player.ifButton(PotionStoreComps.items, d.store.nativeRows.indexOf(k) * 5, IfButtonOp.Op2)
        advance()
        assertEquals(1, player.count(objs.attack_potion_4))
        assertEquals(4, player.vars[k.counter])
    }

    @Test
    fun GameTestState.`native kit assembles and barrel open transmits five persistent slots`() =
        game { d ->
            setup(d)
            player.inv[0] = InvObj(BarrelObjs.kit)
            d.access.launchLenient(player) { d.held.interact(this, player.inv, 0, HeldOp.Op1) }
            assertEquals(BarrelObjs.assembled.id, player.inv[0]?.id)
            d.access.launchLenient(player) { d.held.interact(this, player.inv, 0, HeldOp.Op2) }
            assertTrue(BarrelUi.main in player.ui)
            assertEquals(5, d.barrel.contents(player).size)
        }

    @Test
    fun GameTestState
        .`barrel partial loading conserves the hundred dose cap and replacement bottle`() =
        game { d ->
            setup(d)
            val bag = d.barrel.contents(player)
            bag[0] = InvObj(objs.attack_potion_4, 98)
            player.inv[1] = InvObj(objs.attack_potion_4)
            d.access.launchLenient(player) { assertEquals(2, d.barrel.load(this, 1)) }
            assertEquals(100, bag[0]?.count)
            assertEquals(objs.attack_potion_2.id, player.inv[1]?.id)
            d.access.launchLenient(player) { assertEquals(0, d.barrel.load(this, 1)) }
            assertEquals(objs.attack_potion_2.id, player.inv[1]?.id)
            d.access.launchLenient(player) { assertEquals(7, d.barrel.unload(this, 0, 7)) }
            assertEquals(93, bag[0]?.count)
            assertEquals(1, player.count(objs.attack_potion_4))
            assertEquals(1, player.count(objs.attack_potion_3))
        }

    @Test
    fun GameTestState.`barrel rejects sixth type notes oils and distant use`() = game { d ->
        setup(d)
        val fluids = d.barrel.fluids.take(6)
        val bag = d.barrel.contents(player)
        for (i in 0..4) bag[i] = InvObj(fluids[i].doses.last(), 4)
        player.inv[1] = InvObj(fluids[5].doses.last())
        d.access.launchLenient(player) { assertEquals(0, d.barrel.load(this, 1)) }
        player.inv[1] = InvObj(d.items.getValue(d.items[objs.attack_potion_4].certlink))
        d.access.launchLenient(player) { assertEquals(0, d.barrel.load(this, 1)) }
        player.inv[1] = InvObj(objs.sacred_oil_4)
        d.access.launchLenient(player) { assertEquals(0, d.barrel.load(this, 1)) }
        player.placeAt(CoordGrid(2200, 2200))
        d.access.launchLenient(player) {
            assertFalse(d.barrel.open(this))
            assertEquals(0, d.barrel.drink(this))
        }
    }

    @Test
    fun GameTestState
        .`barrel drinks real potion effects in stored order and preserves refill targets`() =
        game { d ->
            setup(d)
            player.stats[stats.attack] = 50
            player.stats[stats.strength] = 50
            player.inv[1] = InvObj(objs.attack_potion_4)
            player.inv[2] = InvObj(objs.strength_potion_4)
            d.access.launchLenient(player) {
                assertEquals(4, d.barrel.load(this, 1))
                assertEquals(4, d.barrel.load(this, 2))
                assertEquals(2, d.barrel.drink(this))
            }
            assertEquals(58, player.stats[stats.attack])
            assertEquals(58, player.stats[stats.strength])
            assertEquals(3, d.barrel.contents(player)[0]?.count)
            assertEquals(4, player.vars[BarrelBits.caps[0][0]])
            d.access.launchLenient(player) { assertEquals(0, d.barrel.drink(this)) }
            advance(3)
            d.access.launchLenient(player) { assertEquals(2, d.barrel.drink(this)) }
        }

    @Test
    fun GameTestState.`surge barrel dose observes full energy and shared cooldown gates`() =
        game { d ->
            setup(d)
            val bag = d.barrel.contents(player)
            bag[0] = InvObj(objs.surge_potion_4, 4)
            player.setVarp(varps.sa_energy, 1000)
            d.access.launchLenient(player) { assertEquals(0, d.barrel.drink(this)) }
            assertEquals(4, bag[0]?.count)
            player.setVarp(varps.sa_energy, 400)
            d.access.launchLenient(player) { assertEquals(1, d.barrel.drink(this)) }
            assertEquals(650, player.vars[varps.sa_energy])
            assertEquals(3, bag[0]?.count)
            advance(4)
            d.access.launchLenient(player) { assertEquals(0, d.barrel.drink(this)) }
            assertEquals(3, bag[0]?.count)
        }

    @Test
    fun GameTestState
        .`saved bank preset consumes multiple stacked bottles and retains unavailable target`() =
        game { d ->
            setup(d)
            player.inv[1] = InvObj(objs.attack_potion_4)
            d.access.launchLenient(player) {
                d.barrel.load(this, 1)
                assertTrue(d.barrel.savePreset(this, 1))
                assertEquals(4, d.barrel.emptyToBank(this))
            }
            bank(d)[0] = InvObj(objs.attack_potion_3, 2)
            player.setVarBit(BarrelBits.caps[1][0], 10)
            d.access.launchLenient(player) { assertEquals(6, d.barrel.loadPreset(this, 1)) }
            assertEquals(6, d.barrel.contents(player)[0]?.count)
            assertEquals(10, player.vars[BarrelBits.caps[0][0]])
            bank(d)[0] = InvObj(objs.attack_potion_4)
            d.access.launchLenient(player) { assertEquals(4, d.barrel.loadPreset(this, 0)) }
            assertEquals(10, d.barrel.contents(player)[0]?.count)
        }

    @Test
    fun GameTestState
        .`barrel preset loads direct doses from unlocked potion storage without duplicating bottles`() =
        game { d ->
            setup(d)
            val k = d.store.byItem.getValue(objs.attack_potion_4.id).kind
            player.setVarp(BarrelVars.layout[1][0], k.doseEnum)
            player.setVarBit(BarrelBits.caps[1][0], 9)
            player.setVarp(k.counter, 7)
            player.setVarBit(PotionStoreBits.currentTab, 15)
            d.access.launchLenient(player) { assertEquals(7, d.barrel.loadPreset(this, 1)) }
            assertEquals(0, player.vars[k.counter])
            assertEquals(7, d.barrel.contents(player)[0]?.count)
            assertTrue(bank(d).isEmpty())
        }

    @Test
    fun GameTestState
        .`barrel bank empty and partial refill enforce unlocked capacity atomically`() = game { d ->
        setup(d)
        val bag = d.barrel.contents(player)
        bag[0] = InvObj(objs.attack_potion_4, 7)
        bank(d)[0] = InvObj(objs.coins)
        player.setVarBit(varbits.bank_capacity, 1)
        d.access.launchLenient(player) { assertEquals(0, d.barrel.emptyToBank(this)) }
        assertEquals(7, bag[0]?.count)
        assertEquals(1, bank(d).occupiedSpace())
        bag[0] = null
        bank(d)[0] = InvObj(objs.attack_potion_4, 2)
        val k = d.store.byItem.getValue(objs.attack_potion_4.id).kind
        player.setVarp(BarrelVars.layout[0][0], k.doseEnum)
        player.setVarBit(BarrelBits.caps[0][0], 2)
        d.access.launchLenient(player) { assertEquals(0, d.barrel.loadPreset(this, 0)) }
        assertEquals(2, bank(d)[0]?.count)
        assertTrue(bag.isEmpty())
        bank(d)[0] = InvObj(objs.attack_potion_4)
        d.access.launchLenient(player) { assertEquals(2, d.barrel.loadPreset(this, 0)) }
        assertEquals(objs.attack_potion_2.id, bank(d)[0]?.id)
    }

    @Test
    fun GameTestState.`dismantle returns kit only after transaction and clears stored doses`() =
        game { d ->
            setup(d)
            d.barrel.contents(player)[0] = InvObj(objs.attack_potion_4, 8)
            d.access.launchLenient(player) {
                assertFalse(d.barrel.dismantle(this, 1))
                assertTrue(d.barrel.dismantle(this, 0))
            }
            assertTrue(d.barrel.contents(player).isEmpty())
            assertEquals(BarrelObjs.kit.id, player.inv[0]?.id)
        }

    @Test
    fun GameTestState
        .`native storage dose image favourite star and vial header use their real children`() =
        game { d ->
            setup(d)
            player.setVarBit(PotionStoreBits.currentTab, 15)
            val k = d.store.byItem.getValue(objs.attack_potion_4.id).kind
            player.setVarp(k.counter, 8)
            val row = d.store.nativeRows.indexOf(k) * 5
            player.ifButton(
                PotionStoreComps.items,
                row + 1,
                IfButtonOp.Op8,
                objs.attack_potion_1.id,
            )
            advance()
            assertEquals(3, d.store.preferred(player, k))
            player.ifButton(PotionStoreComps.items, row + 4)
            advance()
            assertEquals(objs.attack_potion_1.id, player.vars[PotionStoreVars.favourites[0]])
            player.setVarp(PotionStoreVars.vials, 11)
            val control = d.store.vialControls(player).first()
            player.ifButton(PotionStoreComps.items, control, IfButtonOp.Op3)
            advance()
            assertEquals(5, player.count(objs.empty_vial))
            assertEquals(6, player.vars[PotionStoreVars.vials])
            player.ifButton(PotionStoreComps.items, row + 4)
            advance()
            assertEquals(-1, player.vars[PotionStoreVars.favourites[0]])
        }

    @Test
    fun GameTestState.`native barrel Store5 Fill1 and WithdrawAll preserve bottle doses`() =
        game { d ->
            setup(d)
            d.access.launchLenient(player) { assertTrue(d.barrel.open(this)) }
            for (i in 1..5) player.inv[i] = InvObj(objs.attack_potion_4)
            player.ifButton(BarrelComps.inventory, 1, IfButtonOp.Op2, objs.attack_potion_4.id)
            advance()
            assertEquals(20, d.barrel.contents(player)[0]?.count)
            assertEquals(5, player.count(objs.empty_vial))
            player.inv[1] = InvObj(objs.attack_potion_4)
            player.ifButton(BarrelComps.inventory, 1, IfButtonOp.Op5, objs.attack_potion_4.id)
            advance()
            assertEquals(21, d.barrel.contents(player)[0]?.count)
            assertEquals(objs.attack_potion_3.id, player.inv[1]?.id)
            player.ifButton(BarrelComps.contents, 0, IfButtonOp.Op3, objs.attack_potion_4.id)
            advance()
            assertTrue(d.barrel.contents(player).isEmpty())
            assertEquals(5, player.count(objs.attack_potion_4))
            assertEquals(1, player.count(objs.attack_potion_1))
        }

    @Test
    fun GameTestState.`native barrel drag reorders doses and the corresponding refill targets`() =
        game { d ->
            setup(d)
            player.inv[1] = InvObj(objs.attack_potion_4)
            player.inv[2] = InvObj(objs.strength_potion_3)
            d.access.launchLenient(player) {
                d.barrel.load(this, 1)
                d.barrel.load(this, 2)
                assertTrue(d.barrel.open(this))
            }
            player.ifButtonD(
                BarrelComps.contents,
                0,
                objs.attack_potion_4,
                BarrelComps.contents,
                1,
                objs.strength_potion_4,
            )
            advance()
            assertEquals(objs.strength_potion_4.id, d.barrel.contents(player)[0]?.id)
            assertEquals(3, player.vars[BarrelBits.caps[0][0]])
            assertEquals(4, player.vars[BarrelBits.caps[0][1]])
        }

    private fun GameTestScope.setup(d: Deps) {
        player.members = true
        player.setVarBit(MixBits.potionStorage, 1)
        player.setVarBit(varbits.bank_capacity, 800)
        val chest =
            placeMapLoc(
                CoordGrid(2000, 2000),
                locTypes.values.first {
                    it.name.contains("Bank chest", true) && it.op.any { it.equals("Use", true) }
                },
            )
        player.placeAt(chest.coords.translateX(-1))
        player.inv[0] = InvObj(BarrelObjs.assembled)
        bank(d)
        player.openBankWithoutEvents(d.events)
    }

    private fun GameTestScope.bank(d: Deps): Inventory = player.invMap.getOrPut(d.invs[invs.bank])

    private fun GameTestState.game(body: GameTestScope.(Deps) -> Unit) =
        runInjectedGameTest(
            Deps::class,
            Module,
            PotionStorage::class,
            ChuggingBarrel::class,
            BankInvScript::class,
            org.rsmod.content.interfaces.bank.scripts.BankOpenScript::class,
            PotionProtectionScript::class,
            testBody = body,
        )

    private object Module : AbstractModule() {
        override fun configure() {
            install(HerbloreModule())
            install(SpecialAttackModule())
        }
    }

    data class Deps
    @Inject
    constructor(
        val store: PotionStorage,
        val barrel: ChuggingBarrel,
        val bank: BankInvScript,
        val items: ObjTypeList,
        val invs: org.rsmod.game.type.inv.InvTypeList,
        val held: HeldInteractions,
        val events: EventBus,
        val access: org.rsmod.api.player.protect.ProtectedAccessLauncher,
    )
}
