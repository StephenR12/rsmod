package org.rsmod.content.skills.slayer

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.combat.commons.npc.SlayerLocationVars
import org.rsmod.api.combat.commons.npc.isAssignedSlayerTarget
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varps
import org.rsmod.api.hit.plugin.NpcHitScript
import org.rsmod.api.npc.events.NpcKillEvents
import org.rsmod.api.player.diary.DiaryDifficulty
import org.rsmod.api.player.diary.DiaryRegion
import org.rsmod.api.player.events.interact.NpcUEvents
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.hit.HitType
import org.rsmod.game.inv.InvObj
import org.rsmod.game.map.collision.get
import org.rsmod.game.type.interf.IfButtonOp
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class SlayerAssignmentsTest {
    @Test
    fun GameTestState
        .`boss assignments accept the solo alternative and pay completion experience once`() =
        runInjectedGameTest(Dependencies::class, null, SlayerMasterScript::class) { deps ->
            player.members = true
            player.slayerMaster = Master.Duradel.id
            player.slayerTarget = 98
            player.slayerCount = 1
            player.originalSlayerCount = 3
            player.setVarBit(SlayerVarBits.boss, 9)
            val bear = spawnNpc(player.coords.translate(1, 0), deps.types.getValue(11992))
            val spider = spawnNpc(player.coords.translate(4, 0), deps.types.getValue(11998))
            assertEquals(false, spider.isAssignedSlayerTarget(player))
            assertTrue(bear.isAssignedSlayerTarget(player))
            eventBus.publish(NpcKillEvents.Credited(player, spider))
            assertEquals(1, player.slayerCount)
            eventBus.publish(NpcKillEvents.Credited(player, bear))
            val xp = player.statMap.getFineXP(stats.slayer)
            assertTrue(xp >= 50000)
            assertEquals(0, player.vars[SlayerVarBits.boss])
            eventBus.publish(NpcKillEvents.Credited(player, bear))
            assertEquals(xp, player.statMap.getFineXP(stats.slayer))
        }

    @Test
    fun GameTestState.`regional chests consume the right key and grant a unique or noted payout`() =
        runInjectedGameTest(Dependencies::class, null, RegionalSlayerChests::class) {
            player.members = true
            val chest = placeMapLoc(player.coords.translate(1, 0), locTypes.getValue(34660))
            player.inv[0] = InvObj(objTypes.getValue(23490))
            player.opLoc1(chest)
            advance()
            assertEquals(23490, player.inv[0]?.id)
            player.inv[0] = InvObj(objTypes.getValue(23083))
            random.next = 0
            random.then = 0
            player.opLoc1(chest)
            advance()
            assertEquals(22963, player.inv[0]?.id)
            val larran = placeMapLoc(player.coords.translate(-1, 0), locTypes.getValue(33343))
            player.inv[1] = InvObj(objTypes.getValue(23490))
            random.next = 0
            random.then = 21
            player.withProtectedAccess {
                check(
                    eventBus.publish(
                        this,
                        org.rsmod.api.player.events.interact.LocEvents.Op1(
                            larran,
                            larran,
                            locTypes.getValue(33343),
                        ),
                    )
                )
            }
            assertEquals(1618, player.inv[1]?.id)
            assertEquals(21, player.inv[1]?.count)
        }

    @Test
    fun GameTestState
        .`an unlocked crafter can assemble and recover the original mask imbue source`() =
        runInjectedGameTest(Dependencies::class, null, SlayerHelmets::class) {
            player.members = true
            val parts = listOf(4168, 4164, 4166, 4551, 4155, 25266)
            parts.forEachIndexed { slot, id -> player.inv[slot] = InvObj(objTypes.getValue(id)) }
            fun org.rsmod.api.player.protect.ProtectedAccess.assemble() =
                org.rsmod.api.player.events.interact.HeldUEvents.Type(
                    objTypes.getValue(4168),
                    0,
                    objTypes.getValue(25266),
                    5,
                )
            player.withProtectedAccess { check(eventBus.publish(this, assemble())) }
            assertEquals(25266, player.inv[5]?.id)
            player.setVarBit(cacheTypes.varbits.getValue(3202), 1)
            player.stats[stats.crafting] = 55
            player.withProtectedAccess { check(eventBus.publish(this, assemble())) }
            val slot = player.inv.indexOfFirst { it?.id == 25177 }
            assertTrue(slot >= 0)
            player.withProtectedAccess {
                check(
                    eventBus.publish(
                        this,
                        org.rsmod.api.player.events.interact.HeldObjEvents.Op4(
                            slot,
                            inv[slot]!!,
                            objTypes.getValue(25177),
                            inv,
                        ),
                    )
                )
            }
            assertTrue(player.inv.any { it?.id == 25276 })
            assertTrue(parts.take(5).all { id -> player.inv.any { it?.id == id } })
        }

    @Test
    fun GameTestState
        .`Assignment op selects a weighted cache family and awards XP only for that family`() =
        runInjectedGameTest(Dependencies::class, null, SlayerMasterScript::class) { deps ->
            player.members = true
            val master = spawnNpc(player.coords.translate(1, 0), deps.types[SlayerNpcs.turael])
            // Rats and monkeys occupy the first 7 and 6 tickets. The next ticket selects cows.
            random.next = 13
            random.then = 15
            player.opNpc3(master)
            advance()
            assertEquals(6, player.vars[varps.slayer_target])
            assertEquals(15, player.vars[varps.slayer_count])
            assertEquals(15, player.vars[SlayerVarps.originalCount])
            val cow = spawnNpc(player.coords.translate(2, 0), deps.types.getValue(2791))
            val chicken = spawnNpc(player.coords.translate(3, 0), deps.types.getValue(1173))
            assertTrue(cow.visType.isAssignedSlayerTarget(player))
            assertFalse(chicken.visType.isAssignedSlayerTarget(player))
            eventBus.publish(NpcKillEvents.Credited(player, chicken))
            assertEquals(15, player.slayerCount)
            val xp = player.statMap.getFineXP(stats.slayer)
            eventBus.publish(NpcKillEvents.Credited(player, cow))
            assertEquals(14, player.slayerCount)
            assertEquals(xp + 80, player.statMap.getFineXP(stats.slayer))

            player.opNpc3(master)
            advance()
            assertEquals(6, player.slayerTarget)
            assertEquals(14, player.slayerCount)
        }

    @Test
    fun GameTestState
        .`Krystilia credits only Wilderness kills and advances a separate points streak`() =
        runInjectedGameTest(Dependencies::class, null, SlayerMasterScript::class) { deps ->
            player.members = true
            player.observerUUID = player.uuid
            player.setVarp(SlayerVarps.contacts, 1)
            val master = spawnNpc(player.coords.translate(1, 0), deps.types[SlayerNpcs.krystilia])
            random.next = 0
            random.then = 52
            player.opNpc3(master)
            advance()
            assertEquals(7, player.slayerMaster)
            assertTrue(player.slayerCount > 0)
            player.slayerTarget = 6
            player.slayerCount = 1
            player.slayerStreak = 17
            player.wildernessSlayerStreak = 4
            val safe = CoordGrid(3130, 3630)
            val wild = CoordGrid(3155, 3655)
            player.placeAt(safe)
            val cow = spawnNpc(wild, deps.types.getValue(2790))
            eventBus.publish(NpcKillEvents.Credited(player, cow))
            assertEquals(1, player.slayerCount)
            player.placeAt(wild.translate(1, 0))
            random.next = 1
            eventBus.publish(NpcKillEvents.Credited(player, cow))
            assertEquals(0, player.slayerCount)
            assertEquals(17, player.slayerStreak)
            assertEquals(5, player.wildernessSlayerStreak)
            assertEquals(25, player.slayerPoints)
            assertTrue(deps.ground.findAll(wild).any { it.type == 23490 })
        }

    @Test
    fun GameTestState
        .`Konar assigns a cache area and rejects the same monster from another dungeon`() =
        runInjectedGameTest(Dependencies::class, null, SlayerMasterScript::class) { deps ->
            player.members = true
            val master = spawnNpc(player.coords.translate(1, 0), deps.types[SlayerNpcs.konar])
            player.opNpc3(master)
            advance()
            assertEquals(0, player.slayerCount)
            player.appearance.combatLevel = 85
            player.opNpc3(master)
            advance()
            assertEquals(8, player.slayerMaster)
            assertTrue(player.vars[SlayerLocationVars.area] > 0)
            player.slayerTarget = 6
            player.setVarp(SlayerLocationVars.area, 9)
            player.slayerCount = 1
            player.slayerStreak = 4
            val outside = spawnNpc(CoordGrid(3124, 9912), deps.types.getValue(2790))
            eventBus.publish(NpcKillEvents.Credited(player, outside))
            assertEquals(1, player.slayerCount)
            val inside = spawnNpc(CoordGrid(3424, 3554), deps.types.getValue(2790))
            random.next = 1
            eventBus.publish(NpcKillEvents.Credited(player, inside))
            assertEquals(0, player.slayerCount)
            assertEquals(0, player.vars[SlayerLocationVars.area])
            assertEquals(18, player.slayerPoints)
        }

    @Test
    fun GameTestState.`last kill pays tenth Vannaka streak once and clears client counters`() =
        runInjectedGameTest(Dependencies::class, null, SlayerMasterScript::class) { deps ->
            player.members = true
            player.slayerMaster = 3
            player.slayerStreak = 9
            player.slayerPoints = 2
            player.slayerTarget = 6
            player.slayerCount = 1
            player.originalSlayerCount = 30
            val cow = spawnNpc(player.coords.translate(1, 0), deps.types.getValue(2790))
            eventBus.publish(NpcKillEvents.Credited(player, cow))
            eventBus.publish(NpcKillEvents.Credited(player, cow))
            assertEquals(10, player.slayerStreak)
            assertEquals(42, player.slayerPoints)
            assertEquals(0, player.slayerTarget)
            assertEquals(0, player.slayerCount)
            assertEquals(0, player.originalSlayerCount)
            assertEquals(80, player.statMap.getFineXP(stats.slayer))
        }

    @Test
    fun GameTestState.`Vannaka refuses low levels and free worlds before granting diary credit`() =
        runInjectedGameTest(Dependencies::class, null, SlayerMasterScript::class) { deps ->
            val master = spawnNpc(player.coords.translate(1, 0), deps.types[SlayerNpcs.vannaka])
            val diary = DiaryRegion.Varrock.tasks(DiaryDifficulty.Medium)[8]
            player.members = true
            player.opNpc3(master)
            advance()
            assertEquals(0, player.slayerCount)
            assertFalse(diary.isComplete(player))
            player.stats[stats.slayer] = 99
            player.members = false
            player.opNpc3(master)
            advance()
            assertEquals(0, player.slayerCount)
            assertFalse(diary.isComplete(player))
            player.members = true
            player.appearance.combatLevel = 90
            player.opNpc3(master)
            advance()
            assertTrue(player.slayerCount > 0)
            assertTrue(diary.isComplete(player))
        }

    @Test
    fun GameTestState
        .`Turael replacement must be eligible and confirmed before the streak changes`() =
        runInjectedGameTest(Dependencies::class, null, SlayerMasterScript::class) { deps ->
            player.members = true
            val master = spawnNpc(player.coords.translate(1, 0), deps.types[SlayerNpcs.turael])
            player.slayerStreak = 14
            player.slayerPoints = 90
            player.slayerMaster = 3
            player.slayerTarget = 6
            player.slayerCount = 8
            player.opNpc3(master)
            advance()
            assertEquals(6, player.slayerTarget)
            player.slayerTarget = 43
            player.opNpc3(master)
            advance()
            player.resumeActiveCoroutine(ResumePauseButtonInput(components.chatmenu_pbutton, 2))
            assertEquals(14, player.slayerStreak)
            assertEquals(43, player.slayerTarget)
            player.opNpc3(master)
            advance()
            player.resumeActiveCoroutine(ResumePauseButtonInput(components.chatmenu_pbutton, 1))
            assertEquals(1, player.slayerMaster)
            assertEquals(0, player.slayerStreak)
            assertEquals(90, player.slayerPoints)
            assertTrue(player.slayerTarget != 43 && player.slayerCount > 0)
        }

    @Test
    fun GameTestState.`Trade opens stocked Slayer Equipment and Porcine adds reinforced goggles`() =
        runInjectedGameTest(Dependencies::class, null, SlayerMasterScript::class) { deps ->
            player.members = true
            val master = spawnNpc(player.coords.translate(1, 0), deps.types[SlayerNpcs.vannaka])
            player.opNpc4(master)
            advance()
            val initial = checkNotNull(player.openedShop).inv
            assertEquals(SlayerShops.regular.id, initial.type.id)
            assertEquals(50, initial.first { it?.id == 4155 }?.count)
            assertFalse(objs.reinforced_goggles in initial)
            player.setVarBit(SlayerVarBits.porcine, 40)
            player.opNpc4(master)
            advance()
            val unlocked = checkNotNull(player.openedShop).inv
            assertEquals(SlayerShops.afterPorcine.id, unlocked.type.id)
            assertTrue(objs.reinforced_goggles in unlocked)
        }

    @Test
    fun GameTestState
        .`Rewards purchases validate points and tasks survive storage and cancellation`() =
        runInjectedGameTest(
            Dependencies::class,
            null,
            SlayerMasterScript::class,
            SlayerRewards::class,
        ) { deps ->
            player.members = true
            val master = spawnNpc(player.coords.translate(1, 0), deps.types[SlayerNpcs.vannaka])
            player.slayerPoints = 600
            player.slayerStreak = 8
            player.slayerMaster = 3
            player.slayerCount = 14
            player.originalSlayerCount = 35
            player.slayerTarget = 6
            player.opNpc5(master)
            advance()
            player.ifButton(RewardButtons.confirm, 51)
            advance()
            assertEquals(100, player.slayerPoints)
            player.ifButton(RewardButtons.confirm, 67)
            advance()
            assertEquals(0, player.slayerCount)
            player.ifButton(RewardButtons.confirm, 68)
            advance()
            assertEquals(14, player.slayerCount)
            assertEquals(35, player.originalSlayerCount)
            player.ifButton(RewardButtons.items, 1, IfButtonOp.Op2)
            advance()
            assertEquals(65, player.slayerPoints)
            assertEquals(250, player.inv.first { it?.id == objs.slayer_broad_bolts.id }?.count)
            player.ifButton(RewardButtons.confirm, 58)
            advance()
            assertEquals(35, player.slayerPoints)
            assertEquals(0, player.slayerTarget)
            assertEquals(8, player.slayerStreak)
        }

    @Test
    fun GameTestState.`Queued damage cannot kill a rockslug until the player salts it`() =
        runInjectedGameTest(
            Dependencies::class,
            null,
            SlayerFinishers::class,
            NpcHitScript::class,
            SlayerMasterScript::class,
        ) { deps ->
            player.members = true
            player.stats[stats.slayer] = 20
            player.slayerMaster = Master.Turael.id
            val slug = spawnNpc(player.coords.translate(1, 0), deps.types.getValue(421))
            player.slayerTarget = slug.visType.paramMap!!.primitiveMap[50] as Int
            player.slayerCount = 3
            slug.hitpoints = 8
            player.inv[0] = InvObj(objTypes.getValue(4161), 2)
            slug.queueHit(player, 1, HitType.Melee, 6)
            slug.queueHit(player, 1, HitType.Melee, 6)
            advance()
            assertEquals(1, slug.hitpoints)
            assertEquals(3, player.slayerCount)
            player.withProtectedAccess {
                check(
                    eventBus.publish(
                        this,
                        NpcUEvents.Op(slug, 0, objTypes.getValue(4161), slug.visType),
                    )
                )
            }
            advance()
            assertEquals(0, slug.hitpoints)
            assertEquals(2, player.slayerCount)
            assertEquals(1, player.inv[0]!!.count)
            val second = spawnNpc(player.coords.translate(2, 0), deps.types.getValue(422))
            second.hitpoints = 8
            player.setVarp(SlayerVarps.unlocks.first(), 1 shl 1)
            second.queueHit(player, 1, HitType.Melee, 5)
            advance()
            assertEquals(0, second.hitpoints)
            assertEquals(1, player.slayerCount)
            assertEquals(null, player.inv[0])
        }

    @Test
    fun GameTestState
        .`Last assignment kill can spawn an owned superior and its bonus XP survives task completion`() =
        runInjectedGameTest(
            Dependencies::class,
            null,
            SlayerMasterScript::class,
            SuperiorSlayer::class,
        ) { deps ->
            player.members = true
            player.slayerMaster = Master.Turael.id
            player.placeAt(CoordGrid(3411, 3560))
            val hand = spawnNpc(player.coords.translate(2, 0), deps.types.getValue(448))
            player.slayerTarget = hand.visType.paramMap!!.primitiveMap[50] as Int
            player.slayerCount = 1
            player.setVarp(SlayerVarps.unlocks[1], 1 shl 3)
            random.next = 0
            eventBus.publish(NpcKillEvents.Credited(player, hand))
            assertEquals(0, player.slayerCount)
            val superior =
                deps.repository.findAll(org.rsmod.map.zone.ZoneKey.from(hand.coords), 1).single {
                    it.id == 7388
                }
            val before = player.statMap.getFineXP(stats.slayer)
            random.next = 1000
            eventBus.publish(NpcKillEvents.Credited(player, superior))
            assertEquals(before + 5500, player.statMap.getFineXP(stats.slayer))
            assertEquals(0, player.slayerCount)
        }

    @Test
    fun GameTestState.`cape repeats the last Konar assignment and area even after cancellation`() =
        runInjectedGameTest(Dependencies::class, null, SlayerMasterScript::class) { deps ->
            player.members = true
            player.stats[stats.slayer] = 99
            player.appearance.combatLevel = 100
            val master = spawnNpc(player.coords.translate(1, 0), deps.types[SlayerNpcs.konar])
            fun request() {
                player.opNpc3(master)
                advance()
            }
            random.next = 0
            request()
            val target = player.slayerTarget
            val area = player.vars[SlayerLocationVars.area]
            assertTrue(area > 0)
            player.slayerCount = 0
            player.slayerTarget = 0
            player.setVarp(SlayerLocationVars.area, 0)
            player.inv[0] = InvObj(objs.slayer_skillcape_t)
            random.next = 0
            request()
            assertEquals(0, player.slayerCount)
            assertEquals(1, player.vars[SlayerVarps.capeHistory] ushr 20)
            player.resumeActiveCoroutine(ResumePauseButtonInput(components.chatmenu_pbutton, 1))
            assertEquals(target, player.slayerTarget)
            assertEquals(area, player.vars[SlayerLocationVars.area])
            assertTrue(player.slayerCount > 0)
            player.slayerCount = 0
            random.next = 1
            request()
            assertTrue(player.slayerCount > 0)
            player.slayerCount = 0
            player.inv[0] = null
            random.next = 0
            request()
            assertTrue(player.slayerCount > 0)
        }

    @Test
    fun GameTestState.`Fossil Island quest checks use the Bone Voyage row terminal stage`() =
        runInjectedGameTest(Dependencies::class, null, SlayerMasterScript::class) { deps ->
            player.setVarBit(cacheTypes.varbits.getValue(13718), 2)
            player.setVarBit(cacheTypes.varbits.getValue(5795), 49)
            assertFalse(deps.quests.canReceive(player, 106))
            player.setVarBit(cacheTypes.varbits.getValue(5795), 50)
            assertTrue(deps.quests.canReceive(player, 106))
        }

    private data class Dependencies
    @Inject
    constructor(
        val types: NpcTypeList,
        val quests: SlayerQuestAccess,
        val repository: NpcRepository,
        val ground: org.rsmod.api.repo.obj.ObjRepository,
    )
}
