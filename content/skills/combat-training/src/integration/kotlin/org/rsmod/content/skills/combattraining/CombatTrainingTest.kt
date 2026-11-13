package org.rsmod.content.skills.combattraining

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.CombatStance
import org.rsmod.api.combat.commons.styles.MeleeAttackStyle
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.combat.manager.PlayerAttackManager
import org.rsmod.api.combat.manager.RangedAmmoManager
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.timers
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.hit.plugin.NpcHitScript
import org.rsmod.api.npc.events.NpcKillEvents
import org.rsmod.api.player.back
import org.rsmod.api.player.diary.DiaryDifficulty
import org.rsmod.api.player.diary.DiaryRegion
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.hasCompletedDiaryTask
import org.rsmod.api.player.events.PlayerTimerEvent
import org.rsmod.api.player.events.interact.LocUEvents
import org.rsmod.api.player.hands
import org.rsmod.api.player.hat
import org.rsmod.api.player.legs
import org.rsmod.api.player.quiver
import org.rsmod.api.player.righthand
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.torso
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.stats.plugin.StatRegenScript
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.hit.HitType
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class CombatTrainingTest {
    @Test
    fun GameTestState
        .`guild entry checks a boosted Ranged level and Biohazard and equips team capes in the Wilderness`() =
        runInjectedGameTest(Dependencies::class, null, CombatDiaryEquipment::class) {
            player.members = true
            val cape = objTypes.getValue(4315)
            player.worn[org.rsmod.game.type.obj.Wearpos.Back.slot] = InvObj(cape)
            fun equip() {
                eventBus.publish(
                    org.rsmod.api.player.events.interact.HeldEquipEvents.WearposChange(
                        player,
                        org.rsmod.game.type.obj.Wearpos.Back,
                        cape,
                    )
                )
            }
            player.placeAt(CoordGrid(3102, 3519))
            equip()
            assertEquals(false, player.hasCompletedDiaryTask(DiaryTask.WildernessTeamCape))
            player.placeAt(CoordGrid(3102, 3535))
            assertEquals(false, player.hasCompletedDiaryTask(DiaryTask.WildernessTeamCape))
            equip()
            assertEquals(true, player.hasCompletedDiaryTask(DiaryTask.WildernessTeamCape))
            val range =
                placeMapLoc(
                    CoordGrid(2658, 3438),
                    locTypes.getValue(11665),
                    org.rsmod.game.loc.LocShape.WallDiagonal,
                )
            player.placeAt(CoordGrid(2657, 3438))
            player.opLoc1(range)
            advance()
            assertEquals(false, player.hasCompletedDiaryTask(DiaryTask.EnterRangingGuild))
            player.stats[stats.ranged] = 40
            player.opLoc1(range)
            advance()
            assertEquals(CoordGrid(2658, 3437), player.coords)
            assertEquals(true, player.hasCompletedDiaryTask(DiaryTask.EnterRangingGuild))
            val gate =
                placeMapLoc(
                    CoordGrid(2517, 3356),
                    locTypes.getValue(2039),
                    org.rsmod.game.loc.LocShape.WallStraight,
                    angle = org.rsmod.game.loc.LocAngle.North,
                )
            player.placeAt(CoordGrid(2517, 3356))
            player.opLoc1(gate)
            advance()
            assertEquals(false, player.hasCompletedDiaryTask(DiaryTask.EnterCombatTrainingCamp))
            player.setVarp(cacheTypes.varps.getValue(68), 16)
            player.opLoc1(gate)
            advance()
            assertEquals(CoordGrid(2517, 3357), player.coords)
            assertEquals(true, player.hasCompletedDiaryTask(DiaryTask.EnterCombatTrainingCamp))
        }

    @Test
    fun GameTestState
        .`Wilderness God Wars entrance gates the inner chamber behind a boostable shortcut`() =
        runInjectedGameTest(Dependencies::class, null, WildernessGodWars::class) {
            player.members = true
            player.placeAt(CoordGrid(3017, 3740))
            val entrance =
                placeMapLoc(
                    CoordGrid(3016, 3738),
                    locTypes.getValue(26766),
                    angle = org.rsmod.game.loc.LocAngle.East,
                )
            player.opLoc1(entrance)
            advance(2)
            assertEquals(CoordGrid(3065, 10159, 3), player.coords)
            assertEquals(false, player.hasCompletedDiaryTask(DiaryTask.EnterWildernessGodWars))
            val wall = placeMapLoc(CoordGrid(3066, 10148, 3), locTypes.getValue(26768))
            player.placeAt(CoordGrid(3066, 10149, 3))
            player.opLoc1(wall)
            advance()
            assertEquals(CoordGrid(3066, 10149, 3), player.coords)
            player.stats[stats.agility] = 60
            player.opLoc1(wall)
            advance()
            assertEquals(CoordGrid(3066, 10147, 3), player.coords)
            val crevice =
                placeMapLoc(
                    CoordGrid(3066, 10142, 3),
                    locTypes.getValue(26767),
                    org.rsmod.game.loc.LocShape.WallStraight,
                    angle = org.rsmod.game.loc.LocAngle.North,
                )
            player.placeAt(CoordGrid(3066, 10143, 3))
            player.opLoc1(crevice)
            advance()
            assertEquals(CoordGrid(3062, 10130), player.coords)
            assertEquals(true, player.hasCompletedDiaryTask(DiaryTask.EnterWildernessGodWars))
        }

    @Test
    fun GameTestState.`dummy trains Attack without Hitpoints and stops at eight`() =
        runInjectedGameTest(Dependencies::class, null, TrainingDummies::class) {
            val dummy = placeMapLoc(player.coords.translate(1, 0), DummyLocs.varrockDummy)
            val hpXp = player.statMap.getFineXP(stats.hitpoints)
            val attackXp = player.statMap.getFineXP(stats.attack)
            player.opLoc1(dummy)
            advance()
            assertEquals(attackXp + 50, player.statMap.getFineXP(stats.attack))
            assertEquals(hpXp, player.statMap.getFineXP(stats.hitpoints))
            assertEquals(0, player.statMap.getFineXP(stats.strength))
            assertEquals(0, player.statMap.getFineXP(stats.defence))
            advance(5)
            player.stats[stats.attack] = 8
            val cappedXp = player.statMap.getFineXP(stats.attack)
            player.opLoc1(dummy)
            advance()
            assertEquals(cappedXp, player.statMap.getFineXP(stats.attack))
            assertMessageSent("There is nothing more you can learn from hitting a dummy.")
        }

    @Test
    fun GameTestState.`accumulator recovers drops or breaks each arrow independently`() =
        runInjectedGameTest(Dependencies::class) { deps ->
            player.members = true
            player.observerUUID = player.uuid
            player.back = InvObj(objs.avas_accumulator)
            player.quiver = InvObj(objs.bronze_arrow, 3)
            val coords = player.coords.translate(1, 0)
            allocZoneCollision(coords)
            random.next = 71
            random.then = 72
            random.then = 80
            deps.ammo.useQuiverAmmo(
                player,
                deps.types[objs.bronze_arrow],
                coords,
                dropDelay = 1,
                useCount = 3,
            )
            assertEquals(1, player.quiver?.count)
            advance(2)
            assertEquals(
                1,
                deps.repo.findAll(coords).single { it.type == objs.bronze_arrow.id }.count,
            )
        }

    @Test
    fun GameTestState.`only credited deaths in each Lumbridge dungeon advance its task`() =
        runInjectedGameTest(Dependencies::class, null, CombatDiaryKills::class) { deps ->
            player.members = true
            fun kill(id: Int, coords: CoordGrid) {
                val npc = spawnNpc(coords, deps.npcs.getValue(id))
                eventBus.publish(NpcKillEvents.Credited(player, npc))
            }
            val cave = CoordGrid(3149, 9578)
            val sewer = CoordGrid(3119, 9647)
            kill(481, sewer)
            kill(55, cave)
            assertEquals(false, player.hasCompletedDiaryTask(DiaryTask.SwampCaveBug))
            assertEquals(false, player.hasCompletedDiaryTask(DiaryTask.DraynorSewerZombie))
            kill(481, cave)
            kill(55, sewer)
            assertEquals(true, player.hasCompletedDiaryTask(DiaryTask.SwampCaveBug))
            assertEquals(true, player.hasCompletedDiaryTask(DiaryTask.DraynorSewerZombie))
        }

    @Test
    fun GameTestState
        .`Hitpoints cape and Rapid Heal share their bonus and boosted health decays`() =
        runInjectedGameTest(Dependencies::class, null, StatRegenScript::class) {
            player.stats[stats.hitpoints] = 10
            player.back = InvObj(objs.hitpoints_skillcape)
            player.hands = InvObj(objs.regen_bracelet)
            player.setCurrentLevel(stats.hitpoints, 2)
            eventBus.publish(PlayerTimerEvent.Soft(player, timers.health_regen.id))
            eventBus.publish(PlayerTimerEvent.Soft(player, timers.health_cape_regen.id))
            assertEquals(6, player.hitpoints)
            player.setVarBit(varbits.rapid_heal, 1)
            eventBus.publish(PlayerTimerEvent.Soft(player, timers.health_regen.id))
            eventBus.publish(PlayerTimerEvent.Soft(player, timers.health_cape_regen.id))
            assertEquals(8, player.hitpoints)
            player.setCurrentLevel(stats.hitpoints, 12)
            eventBus.publish(PlayerTimerEvent.Soft(player, timers.health_regen.id))
            assertEquals(12, player.hitpoints)
            eventBus.publish(PlayerTimerEvent.Soft(player, timers.health_boost_decay.id))
            assertEquals(11, player.hitpoints)
        }

    @Test
    fun GameTestState
        .`melee styles train the selected skill and Hitpoints only up to damage dealt`() =
        runInjectedGameTest(Dependencies::class) { deps ->
            val target = spawnNpc(player.coords.translate(1, 0), deps.npcs.getValue(2791))
            target.combatXpMultiplier = 1000
            target.hitpoints = 3
            val hp = player.statMap.getFineXP(stats.hitpoints)
            for ((style, stat) in
                listOf(
                    MeleeAttackStyle.Accurate to stats.attack,
                    MeleeAttackStyle.Aggressive to stats.strength,
                    MeleeAttackStyle.Defensive to stats.defence,
                )) {
                val before = player.statMap.getFineXP(stat)
                val attack =
                    CombatAttack.Melee(null, MeleeAttackType.Crush, style, CombatStance.Stance1)
                deps.attacks.giveCombatXp(player, target, attack, damage = 15)
                assertEquals(before + 120, player.statMap.getFineXP(stat))
            }
            assertEquals(hp + 117, player.statMap.getFineXP(stats.hitpoints))
        }

    @Test
    fun GameTestState.`surface rope and a lit candle give access to the swamp cave bugs`() =
        runInjectedGameTest(Dependencies::class, null, LumbridgeDungeons::class) {
            player.members = true
            val surface = CoordGrid(3169, 3171)
            val underground = CoordGrid(3169, 9571)
            player.placeAt(surface)
            val entrance = placeMapLoc(CoordGrid(3169, 3172), locTypes.getValue(5947))
            player.opLoc1(entrance)
            advance()
            assertEquals(surface, player.coords)
            player.inv[0] = InvObj(objs.rope)
            player.withProtectedAccess {
                check(
                    eventBus.publish(
                        this,
                        LocUEvents.Op(
                            entrance,
                            entrance,
                            locTypes.getValue(5947),
                            objTypes[objs.rope],
                            0,
                        ),
                    )
                )
            }
            assertEquals(null, player.inv[0])
            player.opLoc1(entrance)
            advance()
            assertEquals(surface, player.coords)
            player.inv[0] = InvObj(objs.lit_candle)
            player.opLoc1(entrance)
            advance()
            assertEquals(underground, player.coords)
            val exit = placeMapLoc(CoordGrid(3169, 9572), locTypes.getValue(5946))
            player.opLoc1(exit)
            advance()
            assertEquals(surface, player.coords)
        }

    @Test
    fun GameTestState
        .`Ava device purchase checks quest, level, arrow count and diary completion`() =
        runInjectedGameTest(Dependencies::class, null, AvaDevices::class) { deps ->
            player.members = true
            val ava = spawnNpc(player.coords.translate(1, 0), deps.npcs.getValue(4407))
            val diary = DiaryRegion.Lumbridge.tasks(DiaryDifficulty.Medium)[2]
            player.setVarBit(AvaQuests.animalMagnetism, 240)
            player.inv[0] = InvObj(objs.coins, 1000)
            player.inv[1] = InvObj(objs.steel_arrow, 74)
            player.opNpc4(ava)
            advance()
            assertModalOpen(AvaInterface.devices)
            player.ifButton(AvaButtons.accumulator)
            advance()
            assertMessageSent("You need a Ranged level of 50 for an accumulator.")
            assertEquals(false, diary.isComplete(player))
            player.stats[stats.ranged] = 50
            player.ifButton(AvaButtons.accumulator)
            advance()
            assertMessageSent("You need the materials and enough inventory space for the device.")
            assertEquals(1000, player.inv[0]!!.count)
            player.inv[1] = InvObj(objs.steel_arrow, 75)
            player.ifButton(AvaButtons.accumulator)
            advance()
            assertEquals(1, player.inv[0]!!.count)
            assertEquals(true, objs.avas_accumulator in player.inv)
            assertEquals(true, diary.isComplete(player))
        }

    @Test
    fun GameTestState
        .`Wolfbane keeps the human form and an ordinary weapon transforms the villager`() =
        runInjectedGameTest(
            Dependencies::class,
            null,
            NpcHitScript::class,
            CanifisWerewolves::class,
        ) { deps ->
            player.members = true
            player.observerUUID = player.uuid
            val first = spawnNpc(player.coords.translate(1, 0), deps.npcs.getValue(2613))
            val dagger = deps.types.getValue(2952)
            player.righthand = InvObj(dagger)
            val diary = DiaryRegion.Morytania.tasks(DiaryDifficulty.Easy)[9]
            first.queueHit(player, 1, HitType.Melee, first.hitpoints)
            advance()
            assertEquals(2613, first.visType.id)
            assertEquals(true, diary.isComplete(player))
            val second = spawnNpc(player.coords.translate(2, 0), deps.npcs.getValue(2614))
            player.righthand = null
            second.queueHit(player, 1, HitType.Melee, 1)
            advance()
            assertEquals(2594, second.visType.id)
        }

    @Test
    fun GameTestState
        .`repeated boss kills cannot fill another slot and unrelated progress only interrupts Wilderness`() =
        runInjectedGameTest(Dependencies::class, null, CombatDiaryKills::class) { deps ->
            player.members = true
            fun kill(id: Int, x: Int = 2700, z: Int = 9880) {
                val npc = spawnNpc(CoordGrid(x, z), deps.npcs.getValue(id))
                eventBus.publish(NpcKillEvents.Credited(player, npc))
            }
            kill(6618)
            kill(2265)
            kill(2265)
            kill(6619)
            kill(6615)
            assertEquals(false, player.hasCompletedDiaryTask(DiaryTask.WildernessHardBosses))
            kill(6618)
            assertEquals(true, player.hasCompletedDiaryTask(DiaryTask.WildernessHardBosses))
            kill(2266)
            kill(2267)
            assertEquals(true, player.hasCompletedDiaryTask(DiaryTask.DagannothKings))
            kill(6609)
            kill(11992)
            kill(11998)
            assertEquals(false, player.hasCompletedDiaryTask(DiaryTask.WildernessEliteBosses))
            kill(11993)
            assertEquals(true, player.hasCompletedDiaryTask(DiaryTask.WildernessEliteBosses))
            kill(1367)
            kill(1365, 3205, 9348)
            kill(1366)
            kill(1369)
            kill(1370)
            assertEquals(false, player.hasCompletedDiaryTask(DiaryTask.WorkshopElementals))
            kill(1365)
            assertEquals(true, player.hasCompletedDiaryTask(DiaryTask.WorkshopElementals))
            repeat(4) { kill(100, 2686, 3713) }
            assertEquals(false, player.hasCompletedDiaryTask(DiaryTask.FremennikRockCrabs))
            kill(102, 2512, 3765)
            assertEquals(true, player.hasCompletedDiaryTask(DiaryTask.FremennikRockCrabs))
        }

    @Test
    fun GameTestState
        .`rock crabs reveal on approach and a ranged killing blow uses the firing weapon`() =
        runInjectedGameTest(
            Dependencies::class,
            null,
            BeachCrabs::class,
            CombatDiaryKills::class,
            NpcHitScript::class,
        ) { deps ->
            player.members = true
            player.observerUUID = player.uuid
            advance(12)
            val crab = spawnNpc(player.coords.translate(1, 0), deps.npcs.getValue(101))
            eventBus.publish(org.rsmod.api.npc.events.AiTimerEvents.Type(crab))
            assertEquals(100, crab.visType.id)
            assertEquals(50, crab.hitpoints)
            val elf = spawnNpc(player.coords.translate(2, 0), deps.npcs.getValue(5293))
            player.righthand = InvObj(deps.types.getValue(23983))
            elf.queueHit(player, 1, HitType.Ranged, elf.hitpoints)
            player.righthand = null
            advance()
            assertEquals(true, player.hasCompletedDiaryTask(DiaryTask.CrystalBowElf))
        }

    @Test
    fun GameTestState.`Warriors Guild uses base levels and full Void requires four pieces`() =
        runInjectedGameTest(Dependencies::class, null, CombatDiaryEquipment::class) { deps ->
            player.members = true
            player.placeAt(CoordGrid(2877, 3546))
            val door =
                placeMapLoc(
                    CoordGrid(2877, 3546),
                    locTypes.getValue(24318),
                    org.rsmod.game.loc.LocShape.WallStraight,
                )
            player.opLoc1(door)
            advance()
            assertEquals(false, player.hasCompletedDiaryTask(DiaryTask.EnterWarriorsGuild))
            player.stats[stats.attack] = 65
            player.stats[stats.strength] = 65
            player.opLoc1(door)
            advance()
            assertEquals(true, player.hasCompletedDiaryTask(DiaryTask.EnterWarriorsGuild))
            player.hat = InvObj(objs.void_ranger_helm)
            player.torso = InvObj(objs.void_top)
            player.legs = InvObj(objs.void_robe)
            val wear = org.rsmod.game.type.obj.Wearpos.Hands
            val gloves = deps.types[objs.void_gloves]
            fun notifyEquip() {
                eventBus.publish(
                    org.rsmod.api.player.events.interact.HeldEquipEvents.WearposChange(
                        player,
                        wear,
                        gloves,
                    )
                )
            }
            notifyEquip()
            assertEquals(false, player.hasCompletedDiaryTask(DiaryTask.FullVoid))
            player.hands = InvObj(gloves)
            notifyEquip()
            assertEquals(true, player.hasCompletedDiaryTask(DiaryTask.FullVoid))
        }

    @Test
    fun GameTestState
        .`cannon consumes four matching parts and can load fire unload and return them`() =
        runInjectedGameTest(
            Dependencies::class,
            CannonWeapons,
            DwarfMulticannon::class,
            NpcHitScript::class,
            org.rsmod.api.combat.weapon.scripts.WeaponAttackTypesScript::class,
            org.rsmod.api.combat.weapon.scripts.WeaponAttackStylesScript::class,
        ) { deps ->
            player.members = true
            val centre = CoordGrid(3235, 3216)
            val anchor = centre.translate(-1, -1)
            player.placeAt(centre)
            for (x in -4..4) for (z in -4..4) allocZoneCollision(centre.translate(x, z))
            val parts = listOf(6, 8, 10, 12)
            for ((slot, part) in parts.withIndex()) player.inv[slot] =
                InvObj(objTypes.getValue(part))
            fun setup() =
                player.withProtectedAccess {
                    val base = checkNotNull(inv[0])
                    eventBus.publish(
                        this,
                        org.rsmod.api.player.events.interact.HeldObjEvents.Op1(
                            0,
                            base,
                            objTypes.getValue(6),
                            inv,
                        ),
                    )
                }
            setup()
            assertEquals(0, player.vars[cacheTypes.varps.getValue(65604)])
            player.setVarp(org.rsmod.api.config.refs.varps.mcannon, 11)
            setup()
            assertEquals(anchor.packed, player.vars[cacheTypes.varps.getValue(65604)])
            assertEquals(null, player.inv[0])
            advance(4)
            val active = checkNotNull(findLoc(anchor, locTypes.getValue(6)))
            val type = locTypes.getValue(active.id)
            val bound = org.rsmod.game.loc.BoundLocInfo(active, type)
            player.placeAt(centre.translate(-2, 0))
            player.inv[0] = InvObj(objTypes.getValue(2), 40)
            player.inv[1] = InvObj(objTypes.getValue(21728), 35)
            advance(20)
            val ranged = player.statMap.getFineXP(stats.ranged)
            val hp = player.statMap.getFineXP(stats.hitpoints)
            val npc = spawnNpc(centre.translate(0, 4), deps.npcs.getValue(2791))
            npc.baseHitpointsLvl = 3
            npc.combatXpMultiplier = 1000
            npc.hitpoints = 3
            player.withProtectedAccess {
                eventBus.publish(
                    this,
                    org.rsmod.api.player.events.interact.LocEvents.Op1(bound, bound, type),
                )
            }
            assertEquals(30, player.vars[cacheTypes.varps.getValue(65607)])
            assertEquals(5, player.inv[1]?.count)
            random.next = 0
            random.then = 12
            advance(4)
            assertEquals(29, player.vars[cacheTypes.varps.getValue(65607)])
            assertEquals(ranged + 60, player.statMap.getFineXP(stats.ranged))
            assertEquals(hp, player.statMap.getFineXP(stats.hitpoints))
            player.withProtectedAccess {
                eventBus.publish(
                    this,
                    org.rsmod.api.player.events.interact.LocEvents.Op3(bound, bound, type),
                )
            }
            assertEquals(34, player.inv[1]?.count)
            assertEquals(0, player.vars[cacheTypes.varps.getValue(65607)])
            player.withProtectedAccess {
                eventBus.publish(
                    this,
                    org.rsmod.api.player.events.interact.LocEvents.Op2(bound, bound, type),
                )
            }
            assertEquals(null, findLoc(anchor, locTypes.getValue(6)))
            assertEquals(0, player.vars[cacheTypes.varps.getValue(65604)])
            for (part in parts) assertEquals(true, player.inv.any { it?.id == part })
        }

    @Test
    fun `cannon firing sectors expose adjacent double hits without hitting the centre or corners`() {
        fun hits(x: Int, z: Int) = (0..7).count { CannonCombat.covers(it, x, z) }
        assertEquals(0, hits(0, 0))
        assertEquals(0, hits(1, -1))
        assertEquals(2, hits(2, -1))
        assertEquals(2, hits(-1, 2))
        assertEquals(1, hits(0, 8))
        assertEquals(0, hits(0, 9))
    }

    @Test
    fun GameTestState
        .`runite golem heads require a usable pickaxe and only credit the killer after ore delivery`() =
        runInjectedGameTest(Dependencies::class, null, ResourceAreaGolems::class) { deps ->
            player.members = true
            val site = CoordGrid(3178, 3938)
            val other = registerPlayer(site.translate(1, 0))
            other.members = true
            player.placeAt(site.translate(-1, 0))
            other.placeAt(site.translate(1, 0))
            val golem = spawnNpc(site, deps.npcs.getValue(6600))
            fun kill() = eventBus.publish(NpcKillEvents.Credited(player, golem))
            fun head() = deps.npcRepo.findAll(site).single { it.visType.id == 6601 }
            fun oreCount(p: org.rsmod.game.entity.Player) = p.inv.count(objTypes.getValue(451))
            kill()
            player.opNpc1(head())
            advance(2)
            assertEquals(0, oreCount(player))
            player.stats.setCurrentLevel(stats.mining, 85)
            player.opNpc1(head())
            advance(2)
            assertEquals(0, oreCount(player))
            other.stats.setCurrentLevel(stats.mining, 85)
            other.inv[0] = InvObj(objTypes.getValue(1275))
            random.next = 0
            other.opNpc1(head())
            advance(8)
            assertEquals(1, oreCount(other))
            val progress = cacheTypes.varps.getValue(65654)
            assertEquals(0, other.vars[progress])
            assertEquals(0, player.vars[progress])
            kill()
            player.inv[0] = InvObj(objTypes.getValue(1275))
            for (slot in 1 until player.inv.size) player.inv[slot] = InvObj(objTypes.getValue(1275))
            player.opNpc1(head())
            advance(5)
            assertEquals(0, player.vars[progress])
            player.inv[1] = null
            random.next = 0
            player.opNpc1(head())
            advance(8)
            assertEquals(1, oreCount(player))
            assertEquals(16, player.vars[progress] and 48)
        }

    @Test
    fun GameTestState
        .`god wars entrance remembers rope and boss doors prefer earned essence to a key`() =
        runInjectedGameTest(Dependencies::class, null, GodWarsDungeonAccess::class) { deps ->
            player.members = true
            player.placeAt(CoordGrid(2916, 3745))
            val mouth = placeMapLoc(CoordGrid(2917, 3745), locTypes.getValue(26419))
            player.inv[0] = InvObj(objs.rope)
            player.opLoc1(mouth)
            advance()
            assertEquals(objs.rope.id, player.inv[0]?.id)
            val knight = spawnNpc(CoordGrid(2915, 3745), deps.npcs.getValue(5929))
            player.opNpc1(knight)
            advance()
            player.opLoc1(mouth)
            advance(4)
            assertEquals(1, player.vars[cacheTypes.varbits.getValue(3966)])
            player.opLoc1(mouth)
            advance()
            assertEquals(CoordGrid(2882, 5311, 2), player.coords)
            val bandos = cacheTypes.varbits.getValue(3975)
            val goblin = spawnNpc(player.coords.translate(1, 0), deps.npcs.getValue(2245))
            eventBus.publish(NpcKillEvents.Credited(player, goblin))
            assertEquals(1, player.vars[bandos])
            val outside = CoordGrid(2862, 5354, 2)
            val inside = CoordGrid(2864, 5354, 2)
            val door = placeMapLoc(CoordGrid(2863, 5354, 2), locTypes.getValue(26503))
            player.placeAt(outside)
            player.opLoc1(door)
            advance()
            assertEquals(outside, player.coords)
            player.inv[0] = InvObj(objTypes.getValue(11942))
            player.setVarBit(bandos, 40)
            player.opLoc1(door)
            advance()
            assertEquals(inside, player.coords)
            assertEquals(0, player.vars[bandos])
            assertEquals(11942, player.inv[0]?.id)
            player.placeAt(outside)
            player.opLoc1(door)
            advance()
            assertEquals(inside, player.coords)
            assertEquals(null, player.inv[0])
            eventBus.publish(NpcKillEvents.Credited(player, goblin))
            player.placeAt(CoordGrid(2916, 3745))
            advance(2)
            assertEquals(0, player.vars[bandos])
        }

    @Test
    fun GameTestState
        .`lizardman families award fangs or the talisman with Kourend easy modifier`() =
        runInjectedGameTest(Dependencies::class, null, LizardmanDrops::class) { deps ->
            player.members = true
            player.observerUUID = player.uuid
            val first = spawnNpc(player.coords.translate(0, 1), deps.npcs.getValue(6914))
            random.next = 87
            eventBus.publish(NpcKillEvents.Credited(player, first))
            assertEquals(1, deps.repo.findAll(first.coords).single { it.type == 13391 }.count)
            val boss = spawnNpc(player.coords.translate(1, 0), deps.npcs.getValue(6766))
            random.next = 510
            random.then = 13
            eventBus.publish(NpcKillEvents.Credited(player, boss))
            assertEquals(13, deps.repo.findAll(boss.coords).single { it.type == 13391 }.count)
            player.setVarBit(DiaryRegion.Kourend.tier(DiaryDifficulty.Easy).complete, 1)
            random.next = 499
            eventBus.publish(NpcKillEvents.Credited(player, boss))
            assertEquals(1, deps.repo.findAll(boss.coords).single { it.type == 13392 }.count)
        }

    @Test
    fun GameTestState
        .`Wilderness ankou supply sixty dark bait while ordinary skeletons supply none`() =
        runInjectedGameTest(Dependencies::class, null, WildernessFishingBait::class) { deps ->
            player.members = true
            player.observerUUID = player.uuid
            fun defeat(id: Int, tile: CoordGrid, roll: Int): Int {
                val npc = spawnNpc(tile, deps.npcs.getValue(id))
                random.next = roll
                eventBus.publish(NpcKillEvents.Credited(player, npc))
                return deps.repo.findAll(tile).filter { it.type == 11940 }.sumOf { it.count }
            }
            assertEquals(60, defeat(6608, CoordGrid(2960, 3744), 32))
            assertEquals(0, defeat(6608, CoordGrid(2961, 3762), 33))
            assertEquals(60, defeat(7864, CoordGrid(3353, 10080), 56))
            assertEquals(0, defeat(2514, CoordGrid(2340, 5210), 0))
        }

    private object CannonWeapons : com.google.inject.AbstractModule() {
        override fun configure() {
            bind(org.rsmod.api.combat.weapon.types.AttackTypes::class.java)
                .`in`(com.google.inject.Scopes.SINGLETON)
            bind(org.rsmod.api.combat.weapon.styles.AttackStyles::class.java)
                .`in`(com.google.inject.Scopes.SINGLETON)
        }
    }

    private data class Dependencies
    @Inject
    constructor(
        val ammo: RangedAmmoManager,
        val types: ObjTypeList,
        val repo: ObjRepository,
        val npcRepo: org.rsmod.api.repo.npc.NpcRepository,
        val npcs: NpcTypeList,
        val attacks: PlayerAttackManager,
    )
}
