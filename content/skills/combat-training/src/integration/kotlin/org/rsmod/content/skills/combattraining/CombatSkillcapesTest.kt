package org.rsmod.content.skills.combattraining

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.combat.commons.ranged.RangedAmmunition
import org.rsmod.api.config.refs.*
import org.rsmod.api.hit.plugin.PlayerHitScript
import org.rsmod.api.player.back
import org.rsmod.api.player.diary.DiaryDifficulty
import org.rsmod.api.player.diary.DiaryRegion
import org.rsmod.api.player.events.interact.HeldUEvents
import org.rsmod.api.player.front
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.ring
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.torso
import org.rsmod.api.realm.Realm
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.hit.HitType
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class CombatSkillcapesTest {
    @Test
    fun GameTestState.`worn Magic cape swaps only unlocked books up to five times a UTC day`() =
        runInjectedGameTest(Deps::class, null, MagicAndPrayerCapes::class) { d ->
            player.members = true
            player.stats[stats.magic] = 99
            player.back = InvObj(objs.magic_skillcape)
            val uses = cacheTypes.varps.getValue(65719)
            val slot = org.rsmod.game.type.obj.Wearpos.Back.slot
            fun swap() {
                player.ifClose()
                player.withProtectedAccess {
                    d.wear.interact(this, worn, slot, org.rsmod.game.type.interf.IfButtonOp.Op3)
                }
            }
            swap()
            assertEquals(0, player.vars[uses])
            player.setVarBit(varbits.desert_treasure_main, 15)
            repeat(5) { i ->
                swap()
                player.resumeActiveCoroutine(
                    org.rsmod.api.player.input.ResumePauseButtonInput(
                        org.rsmod.api.config.refs.components.chatmenu_pbutton,
                        0,
                    )
                )
                assertEquals((i + 1) % 2, player.vars[varbits.spellbook])
                assertEquals(i + 1, player.vars[uses] % 8)
            }
            swap()
            assertEquals(1, player.vars[varbits.spellbook])
            assertEquals(5, player.vars[uses] % 8)
            player.ifClose()
            player.withProtectedAccess {
                d.wear.interact(this, worn, slot, org.rsmod.game.type.interf.IfButtonOp.Op1)
            }
            assertEquals(null, player.back)
        }

    @Test
    fun GameTestState
        .`Redemption survives below ten percent even on a zero and yields to the phoenix necklace`() =
        runInjectedGameTest(
            Deps::class,
            null,
            DefenceCapeProtection::class,
            PlayerHitScript::class,
        ) {
            player.members = true
            player.stats[stats.hitpoints] = 90
            player.stats[stats.prayer] = 80
            player.setCurrentLevel(stats.hitpoints, 10)
            player.setVarBit(varbits.redemption, 1)
            fun hit(n: Int) {
                player.queueHit(1, HitType.Typeless, n)
                advance()
            }
            hit(1)
            assertEquals(9, player.hitpoints)
            assertEquals(80, player.stats[stats.prayer])
            player.front = InvObj(objs.phoenix_necklace)
            hit(1)
            assertEquals(35, player.hitpoints)
            assertEquals(1, player.vars[varbits.redemption])
            player.setCurrentLevel(stats.hitpoints, 8)
            hit(0)
            assertEquals(28, player.hitpoints)
            assertEquals(0, player.stats[stats.prayer])
            assertEquals(0, player.vars[varbits.redemption])
        }

    @Test
    fun GameTestState.`Smite rounds each direct player hit down and disables prayers at zero`() =
        runInjectedGameTest(Deps::class, null, CombatPrayers::class, PlayerHitScript::class) {
            val attacker = registerPlayer(player.coords.translate(1, 0))
            attacker.setVarBit(varbits.smite, 1)
            player.stats[stats.hitpoints] = 90
            player.stats[stats.prayer] = 52
            player.setCurrentLevel(stats.prayer, 2)
            player.setVarBit(varbits.protect_item, 1)
            fun hit(n: Int, type: HitType) {
                player.queueHit(attacker, 1, type, n)
                advance()
            }
            hit(7, HitType.Typeless)
            hit(3, HitType.Ranged)
            assertEquals(2, player.stats[stats.prayer])
            hit(7, HitType.Melee)
            assertEquals(1, player.stats[stats.prayer])
            hit(8, HitType.Magic)
            assertEquals(0, player.stats[stats.prayer])
            assertEquals(0, player.vars[varbits.protect_item])
        }

    @Test
    fun GameTestState.`Retribution strikes the adjacent killer on lethal impact`() =
        runInjectedGameTest(Deps::class, null, CombatPrayers::class, PlayerHitScript::class) {
            player.stats[stats.hitpoints] = 10
            player.stats[stats.prayer] = 80
            player.setVarBit(varbits.retribution, 1)
            val attacker = registerPlayer(player.coords.translate(1, 0))
            attacker.stats[stats.hitpoints] = 90
            val observer = registerPlayer(player.coords.translate(0, 2))
            observer.stats[stats.hitpoints] = 90
            repeat(4) { random.next = 1 }
            player.queueHit(attacker, 1, HitType.Magic, 10)
            advance(2)
            assertEquals(0, player.hitpoints)
            assertEquals(89, attacker.hitpoints)
            assertEquals(90, observer.hitpoints)
        }

    @Test
    fun GameTestState.`Harlan refuses boosts and sells a cape and hood atomically`() =
        runInjectedGameTest(Deps::class, null, CombatSkillcapes::class) {
            player.members = true
            val seller = spawnNpc(player.coords.translate(1, 0), npcTypes.getValue(3216))
            fun speak() {
                player.opNpc1(seller)
                advance()
            }
            fun accept() =
                player.resumeActiveCoroutine(ResumePauseButtonInput(components.chatmenu_pbutton, 1))
            player.inv[0] = InvObj(objs.coins, 99_000)
            player.stats[stats.defence] = 98
            player.setCurrentLevel(stats.defence, 99)
            speak()
            assertMessageSent("You must master this skill before you can buy its cape.")
            player.stats[stats.defence] = 99
            for (slot in 1 until player.inv.size) player.inv[slot] = InvObj(objs.knife)
            speak()
            accept()
            assertEquals(99_000, player.inv.count(objs.coins))
            player.inv[1] = null
            speak()
            accept()
            assertEquals(1, player.inv.count(objs.defence_skillcape))
            assertEquals(1, player.inv.count(objs.defence_hood))
            player.inv[0] = InvObj(objs.coins, 99_000)
            player.inv[1] = null
            player.stats[stats.strength] = 99
            speak()
            accept()
            assertEquals(1, player.inv.count(objs.defence_skillcape_t))
            assertEquals(0, player.inv.count(objs.coins))
        }

    @Test
    fun GameTestState
        .`Ranging cape upgrade survives replacement and metallic armour still interferes`() =
        runInjectedGameTest(Deps::class, null, CombatSkillcapes::class) {
            player.members = true
            player.back = InvObj(objs.ranged_skillcape_t)
            assertEquals(72, RangedAmmunition.recoveryRate(player, objTypes))
            player.inv[0] = InvObj(objs.ranged_skillcape)
            player.inv[1] = InvObj(objTypes.getValue(21907))
            fun upgrade() =
                player.withProtectedAccess {
                    check(
                        eventBus.publish(
                            this,
                            HeldUEvents.Type(
                                objTypes[objs.ranged_skillcape],
                                0,
                                objTypes.getValue(21907),
                                1,
                            ),
                        )
                    )
                }
            upgrade()
            assertNull(player.inv[1])
            assertEquals(80, RangedAmmunition.recoveryRate(player, objTypes))
            player.back = InvObj(objs.max_cape_worn)
            assertEquals(80, RangedAmmunition.recoveryRate(player, objTypes))
            player.inv[1] = InvObj(objTypes.getValue(21907))
            upgrade()
            assertNotNull(player.inv[1])
            player.torso = InvObj(objs.rune_platebody)
            assertEquals(0, RangedAmmunition.recoveryRate(player, objTypes))
            player.torso = null
            player.members = false
            assertEquals(0, RangedAmmunition.recoveryRate(player, objTypes))
        }

    @Test
    fun GameTestState.`Strength cape teleports inside the guild and honours blocked activities`() =
        runInjectedGameTest(Deps::class, null, CombatSkillcapes::class) {
            player.members = true
            val destination = CoordGrid(2865, 3546)
            allocZoneCollision(destination)
            player.inv[0] = InvObj(objs.strength_skillcape)
            val start = player.coords
            player.activityBlocksTeleport = true
            player.withProtectedAccess { opHeld3(0) }
            advance(5)
            assertEquals(start, player.coords)
            player.activityBlocksTeleport = false
            player.withProtectedAccess { opHeld3(0) }
            advance(5)
            assertEquals(destination, player.coords)
        }

    @Test
    fun GameTestState
        .`Defence cape and ring trigger on surviving damage after phoenix necklace precedence`() =
        runInjectedGameTest(
            Deps::class,
            null,
            DefenceCapeProtection::class,
            PlayerHitScript::class,
        ) { deps ->
            player.members = true
            player.stats[stats.hitpoints] = 90
            player.inv[0] = InvObj(objs.defence_skillcape)
            player.back = InvObj(objs.defence_skillcape)
            player.withProtectedAccess { opHeld3(0) }
            assertEquals(1, player.vars[cacheTypes.varbits.getValue(4815)])
            val start = CoordGrid(3200, 3200)
            val respawn = deps.realm.config.respawnCoord
            allocZoneCollision(respawn)
            player.placeAt(start)
            fun hit(amount: Int) {
                player.queueHit(1, HitType.Melee, amount)
                advance()
            }
            hit(80)
            assertEquals(10, player.hitpoints)
            assertEquals(start, player.coords)
            player.setVarBit(varbits.teleblock_cycles, 105)
            hit(1)
            assertEquals(start, player.coords)
            player.setVarBit(varbits.teleblock_cycles, 0)
            hit(1)
            assertEquals(respawn, player.coords)
            player.placeAt(start)
            player.setCurrentLevel(stats.hitpoints, 90)
            player.front = InvObj(objs.phoenix_necklace)
            player.ring = InvObj(objs.ring_of_life)
            player.queueHit(1, HitType.Melee, 82)
            player.queueHit(1, HitType.Melee, 80)
            advance()
            assertEquals(35, player.hitpoints)
            assertNull(player.front)
            assertNotNull(player.ring)
            assertEquals(start, player.coords)
            player.withProtectedAccess {
                opHeld3(0)
                opHeld4(0)
            }
            assertEquals(0, player.vars[cacheTypes.varbits.getValue(4546)])
            player.setVarBit(DiaryRegion.Ardougne.tier(DiaryDifficulty.Medium).reward, 1)
            player.withProtectedAccess { opHeld4(0) }
            val market = CoordGrid(2662, 3306)
            allocZoneCollision(market)
            hit(26)
            assertEquals(market, player.coords)
            assertNull(player.ring)
            player.placeAt(start)
            player.ring = InvObj(objs.ring_of_life)
            hit(9)
            assertEquals(0, player.hitpoints)
            assertEquals(start, player.coords)
            assertNotNull(player.ring)
        }

    @Test
    fun GameTestState
        .`metal attraction needs movement and allows communing without changing recovery`() =
        runInjectedGameTest(Deps::class, null, AvaMetalAttraction::class) {
            player.members = true
            player.back = InvObj(objs.ranged_skillcape)
            player.inv[0] = InvObj(objs.ranged_skillcape)
            eventBus.publish(
                org.rsmod.api.player.events.interact.HeldEquipEvents.WearposChange(
                    player,
                    org.rsmod.game.type.obj.Wearpos.Back,
                    objTypes[objs.ranged_skillcape],
                )
            )
            advance(350)
            assertEquals(0, player.worn.count(objs.steel_arrow))
            player.placeAt(player.coords.translate(3, 0))
            random.next = 0
            advance(350)
            assertEquals(1, player.worn.count(objs.steel_arrow))
            player.withProtectedAccess { opHeld3(0) }
            player.placeAt(player.coords.translate(3, 0))
            advance(350)
            assertEquals(1, player.worn.count(objs.steel_arrow))
            assertEquals(72, RangedAmmunition.recoveryRate(player, objTypes))
        }

    private class Deps
    @Inject
    constructor(val realm: Realm, val wear: org.rsmod.api.player.interact.WornInteractions)
}
