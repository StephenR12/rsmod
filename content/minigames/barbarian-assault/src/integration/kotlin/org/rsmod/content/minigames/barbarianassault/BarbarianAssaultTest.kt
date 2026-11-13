package org.rsmod.content.minigames.barbarianassault

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.combat.weapon.scripts.WeaponAttackStylesScript
import org.rsmod.api.combat.weapon.styles.AttackStyles
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.hit.plugin.NpcHitScript
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.hasCompletedDiaryTask
import org.rsmod.api.player.events.interact.*
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.minigames.completedBarbarianAssaultTutorial
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey

@Execution(ExecutionMode.SAME_THREAD)
class BarbarianAssaultTest {
    private class Dependencies @Inject constructor(val ground: ObjRepository)

    private object Weapons : AbstractModule() {
        override fun configure() {
            bind(AttackStyles::class.java).`in`(Scopes.SINGLETON)
        }
    }

    @Test
    fun GameTestState
        .`the five player wave requires an attacker defenders traps and healer poison`() =
        assault(1)

    @Test
    fun GameTestState.`wave ten needs the full egg chain for eight shots and pays every role`() =
        assault(10)

    private fun GameTestState.assault(wave: Int) =
        runInjectedGameTest(
            Dependencies::class,
            Weapons,
            BarbarianAssault::class,
            WeaponAttackStylesScript::class,
            NpcHitScript::class,
        ) { deps ->
            val doorTile = if (wave == 1) CoordGrid(2579, 5298) else CoordGrid(2587, 5278)
            val door = placeMapLoc(doorTile, locTypes.getValue(20198 + wave))
            val team = listOf(player) + List(4) { registerPlayer(doorTile.translate(0, -1)) }
            val attacker = team[0]
            val collector = team[1]
            val defender = team[2]
            val healer = team[3]
            for ((i, p) in team.withIndex()) {
                p.members = true
                p.placeAt(doorTile.translate(0, -1))
                p.setVarBit(cacheTypes.varbits.getValue(3264), 11)
                p.setVarp(cacheTypes.varps.getValue(65612), wave)
                p.opLoc1(door)
                advance()
                if (i == 4) {
                    random.next = 1
                    repeat(3) { random.then = 0 }
                    repeat(if (wave == 1) 4 else 11) { random.then = 0 }
                }
                p.resumeActiveCoroutine(ResumePauseButtonInput(components.menu_list, i % 4))
                advance()
            }
            val arrival = player.coords
            assertNotEquals(doorTile.translate(0, 1), arrival)
            fun tile(x: Int, z: Int) = arrival.translate(x - 1886, z - 5456)
            fun mobs() =
                npcRepo
                    .findAll(ZoneKey.from(arrival), 8)
                    .filter { it.hitpoints > 0 && it.id in 5739..5775 }
                    .distinct()
                    .toList()
            val rank = if (wave == 10) 6 else 0
            val first = mobs().first { it.id == 5739 + rank }
            val hp = first.hitpoints
            first.queueHit(collector, 1, HitType.Melee, 1000)
            advance()
            assertEquals(hp, first.hitpoints)
            val expected = listOf(5739 + rank, 5748 + rank, 5757 + rank, 5766 + rank)
            var handled = 0
            while (mobs().any { it.id in expected } && handled++ < 60) {
                // Keep the first call deterministic when the wave lasts beyond thirty seconds.
                for (npc in mobs()) {
                    when (npc.id) {
                        5739 + rank,
                        5757 + rank -> npc.queueHit(attacker, 1, HitType.Melee, 1000)
                        5748 + rank -> {
                            val pt = listOf(tile(1871, 5473), tile(1901, 5474))[handled % 2]
                            val t = locTypes.getValue(20135)
                            val loc = placeMapLoc(pt, t)
                            val food = objTypes.getValue(10513)
                            defender.inv[15] = InvObj(food)
                            npc.telejump(pt)
                            defender.withProtectedAccess {
                                eventBus.publish(this, LocUEvents.Op(loc, loc, t, food, 15))
                            }
                            if (handled > 1) {
                                // Repair the already used trap with the two real tools.
                                val broken = locTypes.getValue(20231)
                                val target = placeMapLoc(pt, broken)
                                defender.inv[20] = InvObj(objs.hammer)
                                defender.inv[21] = InvObj(objs.logs)
                                defender.withProtectedAccess {
                                    eventBus.publish(this, LocEvents.Op1(target, target, broken))
                                }
                            }
                        }
                        5766 + rank -> {
                            healer.inv[15] = InvObj(objTypes.getValue(10539))
                            healer.withProtectedAccess {
                                eventBus.publish(
                                    this,
                                    NpcUEvents.Op(
                                        npc,
                                        15,
                                        objTypes.getValue(10539),
                                        npcTypes.getValue(npc.id),
                                    ),
                                )
                            }
                        }
                    }
                }
                if (handled % 25 == 24) {
                    random.next = 1
                    repeat(3) { random.then = 0 }
                }
                advance(2)
            }
            assertFalse(
                mobs().any { it.id in expected },
                "Unfinished penance ${mobs().map{it.id to it.hitpoints}}",
            )
            if (wave == 10) {
                advance(35)
                val queen = mobs().single { it.id == 5775 }
                queen.queueHit(attacker, 1, HitType.Melee, 1000)
                advance()
                assertEquals(250, queen.hitpoints)
                val eggTile = queen.coords.translate(2, 0)
                fun pass(from: Player, to: Player, id: Int) {
                    val slot = from.inv.indices.first { from.inv[it]?.id == id }
                    from.withProtectedAccess {
                        eventBus.publish(this, PlayerUEvents.Op(to, slot, objTypes.getValue(id)))
                    }
                }
                fun station(p: Player, id: Int, pt: CoordGrid) {
                    val t = locTypes.getValue(id)
                    val loc = placeMapLoc(pt, t)
                    p.withProtectedAccess { eventBus.publish(this, LocEvents.Op1(loc, loc, t)) }
                }
                repeat(8) {
                    advance(10)
                    val egg = deps.ground.findAll(eggTile).first { it.type == 10534 }
                    collector.withProtectedAccess { eventBus.publish(this, ObjEvents.Op3(egg)) }
                    pass(collector, healer, 10534)
                    station(healer, 20233, tile(1886, 5460))
                    pass(healer, attacker, 10535)
                    station(attacker, 20250, tile(1888, 5460))
                    val spikeSlot = attacker.inv.indices.first { attacker.inv[it]?.id == 10561 }
                    val eggSlot = attacker.inv.indices.first { attacker.inv[it]?.id == 10535 }
                    attacker.withProtectedAccess {
                        eventBus.publish(
                            this,
                            HeldUEvents.Type(
                                objTypes.getValue(10561),
                                spikeSlot,
                                objTypes.getValue(10535),
                                eggSlot,
                            ),
                        )
                    }
                    pass(attacker, defender, 10536)
                    station(defender, 20232, tile(1889, 5460))
                    pass(defender, collector, 10537)
                    station(collector, 20267, tile(1878, 5474))
                    station(attacker, 20133, tile(1877, 5474))
                    attacker.resumeActiveCoroutine(ResumePauseButtonInput(components.menu_list, 3))
                    advance(2)
                }
            }
            for (p in team) {
                assertTrue(p.hasCompletedDiaryTask(DiaryTask.FinishAssaultWave))
                assertEquals(CoordGrid(2593, 5261), p.coords)
                assertEquals(if (wave == 10) 1 else 2, p.vars[cacheTypes.varps.getValue(65612)])
                assertEquals(if (wave == 10) 1 else 0, p.vars[cacheTypes.varps.getValue(65614)])
            }
            if (wave == 10) {
                assertEquals(80, attacker.vars[cacheTypes.varbits.getValue(4759)])
                assertEquals(5, attacker.vars[cacheTypes.varbits.getValue(4760)])
            }
        }

    @Test
    fun GameTestState
        .`Captain Cain unlocks recruitment and Connad charges for role mastery and granite`() =
        runGameTest(AssaultRewards::class, BarbarianAssault::class) {
            player.members = true
            val cain = spawnNpc(player.coords.translate(1, 0), npcTypes.getValue(1657))
            player.opNpc1(cain)
            advance()
            player.resumeActiveCoroutine(ResumePauseButtonInput(components.chatmenu_pbutton, 2))
            advance()
            assertTrue(player.completedBarbarianAssaultTutorial())
            val entrance = placeMapLoc(player.coords.translate(-1, 0), locTypes.getValue(20226))
            player.opLoc1(entrance)
            advance(3)
            assertEquals(CoordGrid(2593, 5261), player.coords)
            val commander = spawnNpc(player.coords.translate(1, 0), npcTypes.getValue(1656))
            fun buy(index: Int) {
                player.opNpc1(commander)
                advance()
                player.resumeActiveCoroutine(ResumePauseButtonInput(components.menu_list, index))
                advance()
            }
            player.setVarBit(cacheTypes.varbits.getValue(4759), 500)
            player.setVarBit(cacheTypes.varbits.getValue(3251), 4)
            buy(0)
            assertEquals(5, player.vars[cacheTypes.varbits.getValue(3251)])
            assertEquals(0, player.vars[cacheTypes.varbits.getValue(4759)])
            val board = placeMapLoc(player.coords.translate(-1, 0), locTypes.getValue(20134))
            player.opLoc1(board)
            advance(5)
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.ReadAssaultBlackboard))
            for (id in 3253..3255) player.setVarBit(cacheTypes.varbits.getValue(id), 5)
            player.opLoc1(board)
            advance(5)
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.ReadAssaultBlackboard))
            player.inv[10] = InvObj(objs.coins, 95000)
            buy(12)
            assertEquals(95000, player.inv.count(objs.coins))
            player.setVarp(cacheTypes.varps.getValue(65614), 1)
            buy(12)
            assertEquals(1, player.inv.count(objTypes.getValue(10564)))
            assertEquals(0, player.inv.count(objs.coins))
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.EquipAssaultGraniteBody))
        }
}
