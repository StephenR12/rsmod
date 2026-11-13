package org.rsmod.content.skills.combattraining

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.combat.weapon.styles.AttackStyles
import org.rsmod.api.combat.weapon.types.AttackTypes
import org.rsmod.api.config.refs.stats
import org.rsmod.api.hit.plugin.NpcHitScript
import org.rsmod.api.player.back
import org.rsmod.api.player.hat
import org.rsmod.api.player.lefthand
import org.rsmod.api.player.righthand
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.hit.HitType
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey

@Execution(ExecutionMode.SAME_THREAD)
class WarriorsGuildTest {
    @Test
    fun GameTestState
        .`animate armour with physical pieces recover unclaimed gear and earn melee-only tokens`() =
        runInjectedGameTest(Deps::class, null, WarriorsGuildTraining::class, NpcHitScript::class) {
            deps ->
            player.members = true
            player.stats[stats.attack] = 70
            player.stats[stats.strength] = 70
            player.observerUUID = player.uuid
            val spot = CoordGrid(2851, 3536)
            player.placeAt(spot.translate(0, 1))
            val animator = placeMapLoc(spot, locTypes.getValue(23955))
            val set = listOf(1165, 1125, 1077)
            fun activate() {
                player.opLoc1(animator)
                advance()
            }
            player.inv[0] = InvObj(objTypes.getValue(1165))
            activate()
            assertNotNull(player.inv[0])
            assertMessageSent("Bring a full helm, platebody and platelegs of the same metal.")
            player.inv[1] = InvObj(objTypes.getValue(1125))
            player.inv[2] = InvObj(objTypes.getValue(1077))
            activate()
            val armour = npcRepo.findAll(ZoneKey.from(spot), 1).first { it.id == 2453 }
            assertEquals(60, armour.hitpoints)
            assertTrue(player.inv.isEmpty())
            val other = registerPlayer(spot.translate(-1, 0))
            armour.queueHit(other, 1, HitType.Melee, 50)
            advance()
            assertEquals(60, armour.hitpoints)
            assertTrue(armour.heroPoints.toMutableList().isEmpty())
            armour.queueHit(player, 1, HitType.Magic, 60)
            advance()
            assertEquals(60, armour.hitpoints)
            player.setVarBit(cacheTypes.varbits.getValue(12864), 2)
            armour.queueHit(player, 1, HitType.Melee, 80)
            advance()
            assertEquals(60, armour.heroPoints.toMutableList().single().points)
            val drops = deps.ground.findAll(armour.coords).toList()
            assertEquals(60, drops.single { it.type == 8851 }.count)
            assertTrue(set.all { id -> drops.any { it.type == id } })
            for ((slot, id) in set.withIndex()) player.inv[slot] = InvObj(objTypes.getValue(id))
            activate()
            advance(501)
            val master = spawnNpc(player.coords.translate(1, 0), npcTypes.getValue(2462))
            player.opNpc1(master)
            advance()
            for (id in set) assertEquals(1, player.inv.count(objTypes.getValue(id)))
        }

    @Test
    fun GameTestState
        .`cyclops doors charge tokens and require showing the next defender before advancing its drop`() =
        runInjectedGameTest(
            Deps::class,
            null,
            WarriorsGuildTraining::class,
            GuildCyclopsDrops::class,
            NpcHitScript::class,
            org.rsmod.api.death.plugin.NpcDeathScript::class,
        ) { deps ->
            player.members = true
            player.stats[stats.attack] = 70
            player.stats[stats.strength] = 70
            player.observerUUID = player.uuid
            val door =
                placeMapLoc(
                    CoordGrid(2847, 3540, 2),
                    locTypes.getValue(24306),
                    org.rsmod.game.loc.LocShape.WallStraight,
                )
            val outside = CoordGrid(2846, 3540, 2)
            val inside = CoordGrid(2848, 3540, 2)
            player.placeAt(outside)
            fun toggle() {
                player.opLoc1(door)
                advance()
                if (player.interaction != null) advance()
            }
            val tokens = objTypes.getValue(8851)
            player.inv[0] = InvObj(tokens, 99)
            toggle()
            assertEquals(outside, player.coords)
            assertMessageSent("You need at least 100 Warrior Guild tokens to enter.")
            player.inv[0] = InvObj(tokens, 130)
            toggle()
            assertEquals(inside, player.coords)
            assertEquals(120, player.inv.count(tokens))
            val cyclops = spawnNpc(inside.translate(2, 0), npcTypes.getValue(2463))
            val hp = cyclops.hitpoints
            cyclops.queueHit(player, 1, HitType.Ranged, 100)
            advance()
            assertEquals(hp, cyclops.hitpoints)
            fun kill(id: Int, c: CoordGrid, defender: Int) {
                val foe = spawnNpc(c, npcTypes.getValue(id))
                foe.queueHit(player, 1, HitType.Melee, 500)
                advance(7)
                assertTrue(deps.ground.findAll(c).any { it.type == defender })
            }
            kill(2463, inside.translate(0, 2), 8844)
            player.inv[1] = InvObj(objTypes.getValue(8844))
            kill(2463, inside.translate(0, 3), 8844)
            toggle()
            toggle()
            kill(2463, inside.translate(0, 4), 8845)
            advance(100)
            assertEquals(100, player.inv.count(tokens))
            toggle()
            val basement =
                placeMapLoc(
                    CoordGrid(2911, 9968),
                    locTypes.getValue(10043),
                    org.rsmod.game.loc.LocShape.WallStraight,
                    org.rsmod.game.loc.LocAngle.East,
                )
            val below = CoordGrid(2910, 9968)
            player.placeAt(below)
            player.opLoc1(basement)
            advance()
            if (player.interaction != null) advance()
            assertEquals(0, player.vars[cacheTypes.varbits.getValue(994)])
            assertMessageSent("Show Lorelai a rune defender before entering.")
            player.inv[1] = InvObj(objTypes.getValue(8850))
            player.opLoc1(basement)
            advance()
            assertEquals(1, player.vars[cacheTypes.varbits.getValue(994)])
            assertEquals(90, player.inv.count(tokens))
            kill(2137, player.coords.translate(2, 0), 12954)
            player.inv[0] = null
            advance(201)
            assertEquals(below, player.coords)
            assertEquals(1, player.vars[cacheTypes.varbits.getValue(994)])
            player.inv[1] = null
            player.back = InvObj(objTypes.getValue(9747))
            player.opLoc1(basement)
            advance(205)
            assertEquals(CoordGrid(2912, 9968), player.coords)
            player.back = null
            advance()
            assertEquals(below, player.coords)
        }

    @Test
    fun GameTestState.`shot put uses strength energy style dust and claimable tokens`() =
        runInjectedGameTest(
            Deps::class,
            null,
            WarriorsGuildTraining::class,
            GuildTrainingRooms::class,
        ) {
            player.members = true
            player.stats[stats.attack] = 99
            player.stats[stats.strength] = 49
            val door =
                placeMapLoc(
                    CoordGrid(2852, 3552, 1),
                    locTypes.getValue(15658),
                    org.rsmod.game.loc.LocShape.WallStraight,
                    org.rsmod.game.loc.LocAngle.East,
                )
            player.placeAt(door.coords)
            player.setCurrentLevel(stats.strength, 99)
            player.opLoc1(door)
            advance()
            assertMessageSent("You need a Strength level of 50 to open the heavy door.")
            player.stats[stats.strength] = 99
            random.next = 0
            player.opLoc1(door)
            advance()
            assertEquals(CoordGrid(2853, 3552, 1), player.coords)
            val shot = placeMapLoc(CoordGrid(2861, 3554, 1), locTypes.getValue(15664))
            player.placeAt(shot.coords.translate(-1, 0))
            player.runEnergy = 410
            player.opLoc1(shot)
            advance()
            assertMessageSent("You are too tired to put a shot. Take a rest.")
            val xp = player.statMap.getFineXP(stats.strength)
            player.runEnergy = 10000
            player.inv[0] = InvObj(objTypes.getValue(8865))
            player.withProtectedAccess {
                eventBus.publish(
                    this,
                    org.rsmod.api.player.events.interact.HeldObjEvents.Op1(
                        0,
                        inv[0]!!,
                        objTypes.getValue(8865),
                        inv,
                    ),
                )
            }
            assertNull(player.inv[0])
            player.opLoc1(shot)
            advance()
            random.next = 0
            player.resumeActiveCoroutine(
                org.rsmod.api.player.input.ResumePauseButtonInput(
                    org.rsmod.api.config.refs.components.menu_list,
                    0,
                )
            )
            assertEquals(xp + 1512, player.statMap.getFineXP(stats.strength))
            assertEquals(7840, player.runEnergy)
            assertEquals(15, player.vars[cacheTypes.varps.getValue(65622)])
            player.opLoc1(shot)
            advance()
            random.next = 249
            player.resumeActiveCoroutine(
                org.rsmod.api.player.input.ResumePauseButtonInput(
                    org.rsmod.api.config.refs.components.menu_list,
                    2,
                )
            )
            assertEquals(xp + 1512, player.statMap.getFineXP(stats.strength))
            val ref = spawnNpc(player.coords.translate(-1, 0), npcTypes.getValue(6073))
            player.opNpc3(ref)
            advance()
            assertEquals(15, player.inv.count(objTypes.getValue(8851)))
        }

    @Test
    fun GameTestState
        .`catapult checks target equipping guards incoming missiles and safely removes shield on exit`() =
        runInjectedGameTest(
            Deps::class,
            null,
            WarriorsGuildTraining::class,
            GuildTrainingRooms::class,
        ) { deps ->
            player.members = true
            player.stats[stats.attack] = 70
            player.stats[stats.strength] = 70
            player.observerUUID = player.uuid
            player.placeAt(CoordGrid(2841, 3541, 1))
            val coach = spawnNpc(player.coords.translate(1, 0), npcTypes.getValue(2459))
            player.opNpc4(coach)
            advance()
            val shield = objTypes.getValue(8856)
            assertEquals(1, player.inv.count(shield))
            assertTrue(
                deps.equip.equip(player, 0, player.inv)
                    is org.rsmod.api.player.worn.HeldEquipResult.Fail
            )
            player.placeAt(CoordGrid(2842, 3545, 1))
            player.righthand = InvObj(objTypes.getValue(1205))
            assertTrue(
                deps.equip.equip(player, 0, player.inv)
                    is org.rsmod.api.player.worn.HeldEquipResult.Success
            )
            assertNull(player.righthand)
            assertEquals(8856, player.lefthand?.id)
            val def = player.statMap.getFineXP(stats.defence)
            random.next = 2
            advance(4)
            player.ifButton(cacheTypes.components.getValue((411 shl 16) or 10))
            advance(4)
            assertEquals(def + 100, player.statMap.getFineXP(stats.defence))
            assertEquals(1, player.vars[cacheTypes.varps.getValue(65622)])
            random.next = 1
            random.then = 2
            advance(8)
            assertEquals(def + 100, player.statMap.getFineXP(stats.defence))
            player.placeAt(CoordGrid(2842, 3544, 1))
            advance()
            assertNull(player.lefthand)
            assertEquals(1, player.inv.count(shield))
        }

    @Test
    fun GameTestState
        .`keg balancing uses individual scenery empty slots energy and resets on leaving`() =
        runGameTest(GuildTrainingRooms::class) {
            player.members = true
            player.stats[stats.attack] = 70
            player.stats[stats.strength] = 70
            val keg1 = placeMapLoc(CoordGrid(2870, 3542, 1), locTypes.getValue(15673))
            val keg2 = placeMapLoc(CoordGrid(2867, 3536, 1), locTypes.getValue(15669))
            player.placeAt(keg1.coords.translate(-1, 0))
            player.righthand = InvObj(objTypes.getValue(1205))
            player.opLoc1(keg1)
            advance()
            assertNull(player.hat)
            player.righthand = null
            player.opLoc1(keg1)
            advance()
            assertEquals(8860, player.hat?.id)
            assertEquals(1, player.vars[cacheTypes.varbits.getValue(2252)])
            player.placeAt(keg2.coords.translate(-1, 0))
            player.opLoc1(keg2)
            advance()
            assertEquals(8861, player.hat?.id)
            val xp = player.statMap.getFineXP(stats.strength)
            player.runEnergy = 10000
            random.next = 999
            advance(3)
            assertEquals(xp + 20, player.statMap.getFineXP(stats.strength))
            assertEquals(2, player.vars[cacheTypes.varps.getValue(65622)])
            player.placeAt(CoordGrid(2864, 3544, 1))
            advance(4)
            assertNull(player.hat)
            assertEquals(0, player.vars[cacheTypes.varbits.getValue(2252)])
            assertEquals(0, player.vars[cacheTypes.varbits.getValue(2253)])
        }

    @Test
    fun GameTestState.`training dummies require the active target and the correct combat style`() =
        runInjectedGameTest(
            Deps::class,
            WeaponModule,
            WarriorsGuildTraining::class,
            org.rsmod.api.combat.weapon.scripts.WeaponAttackStylesScript::class,
            org.rsmod.api.combat.weapon.scripts.WeaponAttackTypesScript::class,
        ) { deps ->
            player.members = true
            player.stats[stats.attack] = 70
            player.stats[stats.strength] = 70
            val door =
                placeMapLoc(
                    CoordGrid(2853, 3554),
                    locTypes.getValue(24315),
                    org.rsmod.game.loc.LocShape.WallStraight,
                    org.rsmod.game.loc.LocAngle.East,
                )
            player.placeAt(door.coords)
            val tile = CoordGrid(2856, 3554)
            placeMapLoc(tile, locTypes.getValue(23965))
            random.next = 0
            player.opLoc1(door)
            advance()
            assertEquals(CoordGrid(2854, 3554), player.coords)
            player.placeAt(tile.translate(-1, 0))
            val loc = deps.locs.findAll(tile).single { it.id == 23958 }
            val active = org.rsmod.game.loc.BoundLocInfo(loc, locTypes.getValue(23958))
            val xp = player.statMap.getFineXP(stats.attack)
            val mode = org.rsmod.api.config.refs.varps.com_mode
            player.setVarp(mode, 1)
            player.opLoc1(active)
            advance()
            assertMessageSent("You need a different attack style for that dummy.")
            assertEquals(xp, player.statMap.getFineXP(stats.attack))
            advance(3)
            player.setVarp(mode, 0)
            player.opLoc1(active)
            advance()
            assertEquals(xp + 150, player.statMap.getFineXP(stats.attack))
            assertEquals(2, player.vars[cacheTypes.varps.getValue(65622)])
            player.opLoc1(active)
            advance()
            assertEquals(xp + 150, player.statMap.getFineXP(stats.attack))
        }

    private object WeaponModule : com.google.inject.AbstractModule() {
        override fun configure() {
            bind(AttackTypes::class.java).`in`(com.google.inject.Scopes.SINGLETON)
            bind(AttackStyles::class.java).`in`(com.google.inject.Scopes.SINGLETON)
        }
    }

    private class Deps
    @Inject
    constructor(
        val ground: ObjRepository,
        val locs: org.rsmod.api.repo.loc.LocRepository,
        val equip: org.rsmod.api.player.worn.HeldEquipOp,
    )
}
