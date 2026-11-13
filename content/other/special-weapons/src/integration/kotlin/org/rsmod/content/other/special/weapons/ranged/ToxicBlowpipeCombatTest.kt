package org.rsmod.content.other.special.weapons.ranged

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.combat.commons.AreaAttackTargets
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.styles.RangedAttackStyle
import org.rsmod.api.combat.commons.types.RangedAttackType
import org.rsmod.api.combat.weapon.scripts.WeaponAttackStylesScript
import org.rsmod.api.combat.weapon.styles.AttackStyles
import org.rsmod.api.config.refs.areas
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.stats
import org.rsmod.api.hit.plugin.NpcHitScript
import org.rsmod.api.player.events.interact.BankedObjEvents
import org.rsmod.api.player.hat
import org.rsmod.api.player.quiver
import org.rsmod.api.player.righthand
import org.rsmod.api.specials.SpecialAttack
import org.rsmod.api.specials.SpecialAttackBootstrap
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackModule
import org.rsmod.api.specials.SpecialAttackRegistry
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.weapons.WeaponAttackManager
import org.rsmod.api.weapons.WeaponModule
import org.rsmod.api.weapons.WeaponRegistry
import org.rsmod.api.weapons.WeaponRepository
import org.rsmod.content.other.special.weapons.RangedWeaponsModule
import org.rsmod.content.skills.fletching.toxicblowpipe.ToxicBlowpipeDart
import org.rsmod.content.skills.fletching.toxicblowpipe.ToxicBlowpipeState
import org.rsmod.content.skills.magic.spell.attacks.ancient.AncientNpcPoisonRepository
import org.rsmod.game.area.AreaIndex
import org.rsmod.game.hit.HitType
import org.rsmod.game.inv.InvObj

@Execution(ExecutionMode.SAME_THREAD)
class ToxicBlowpipeCombatTest {
    @Test
    fun GameTestState
        .`all stored dart tiers have flight graphics and both weapons have the native 50 percent special`() =
        runInjectedGameTest(Deps::class, Modules) { d ->
            d.bootstrap.startup()
            with(d.pipes) { d.weaponRepo.register(d.weapons) }
            for (pipe in listOf(objs.toxic_blowpipe, objs.blazing_blowpipe)) {
                assertEquals(
                    500,
                    (d.specials[InvObj(pipe)] as SpecialAttack.Ranged).energyInHundreds,
                )
                assertNotNull(d.registry.getRanged(InvObj(pipe)))
            }
            for (dart in ToxicBlowpipeDart.entries) assertNotNull(
                objTypes[dart.obj].paramOrNull(params.proj_travel),
                dart.displayName,
            )
        }

    @Test
    fun GameTestState
        .`an ordinary shot consumes the stored amethyst dart leaves the quiver alone and uses the rapid NPC clock`() =
        runInjectedGameTest(Deps::class, Modules, WeaponAttackStylesScript::class) { d ->
            val target = spawnNpc(player.coords.translate(3, 0), npcTypes.getValue(100))
            val pipe =
                InvObj(
                    objs.blazing_blowpipe,
                    vars = ToxicBlowpipeState(ToxicBlowpipeDart.Amethyst, 1, 2).pack(),
                )
            player.observerUUID = player.uuid
            player.members = true
            player.righthand = pipe
            player.quiver = InvObj(objs.bronze_arrow, 10)
            player.withProtectedAccess {
                assertTrue(
                    d.pipes.fire(
                        this,
                        target,
                        CombatAttack.Ranged(pipe, RangedAttackType.Light, RangedAttackStyle.Rapid),
                        false,
                    )
                )
            }
            val state = ToxicBlowpipeState.unpack(player.righthand)
            assertEquals(0, state.dartCount)
            assertTrue(state.scales in 1..2)
            assertEquals(objs.blazing_blowpipe.id, player.righthand?.id)
            assertEquals(10, player.quiver?.count)
            assertEquals(player.currentMapClock + 2, player.actionDelay)
        }

    @Test
    fun GameTestState.`rapid PvP is one tick slower and a dry pipe declines Toxic Siphon`() =
        runInjectedGameTest(Deps::class, Modules, WeaponAttackStylesScript::class) { d ->
            val target = registerPlayer(player.coords.translate(2, 0))
            player.observerUUID = player.uuid
            val pipe =
                InvObj(
                    objs.toxic_blowpipe,
                    vars = ToxicBlowpipeState(ToxicBlowpipeDart.Dragon, 2, 2).pack(),
                )
            player.righthand = pipe
            player.withProtectedAccess {
                assertTrue(
                    d.pipes.fire(
                        this,
                        target,
                        CombatAttack.Ranged(pipe, RangedAttackType.Light, RangedAttackStyle.Rapid),
                        true,
                    )
                )
            }
            assertEquals(player.currentMapClock + 3, player.actionDelay)
            val dry =
                InvObj(
                    objs.toxic_blowpipe,
                    vars = ToxicBlowpipeState(ToxicBlowpipeDart.Dragon, 2, 0).pack(),
                )
            player.righthand = dry
            player.withProtectedAccess {
                assertFalse(
                    d.pipes.fire(
                        this,
                        target,
                        CombatAttack.Ranged(dry, RangedAttackType.Light, RangedAttackStyle.Rapid),
                        true,
                    )
                )
            }
            assertEquals(dry, player.righthand)
        }

    @Test
    fun GameTestState.`bank scales top up to the saved target preserving the ornament and darts`() =
        runInjectedGameTest(Deps::class, Modules) { d ->
            val stored = ToxicBlowpipeState(ToxicBlowpipeDart.Rune, 88, 10)
            val pipe = InvObj(objs.blazing_blowpipe, vars = stored.pack())
            player.inv[0] = pipe
            player.setVarBit(cacheTypes.varbits.getValue(16147), 7)
            player.withProtectedAccess {
                d.equipment.autocharge(
                    this,
                    BankedObjEvents.Autocharge(0, pipe) { _, _ -> error("above target") },
                )
            }
            assertEquals(pipe, player.inv[0])
            player.setVarBit(cacheTypes.varbits.getValue(16147), 30)
            player.withProtectedAccess {
                d.equipment.autocharge(
                    this,
                    BankedObjEvents.Autocharge(0, pipe) { item, amount ->
                        assertEquals(objs.zulrahs_scales, item)
                        assertEquals(20, amount)
                        9
                    },
                )
            }
            assertEquals(
                InvObj(objs.blazing_blowpipe, vars = stored.copy(scales = 19).pack()),
                player.inv[0],
            )
        }

    @Test
    fun GameTestState
        .`real envenoming impacts upgrade existing poison and progress from six to eight damage`() =
        runInjectedGameTest(Deps::class, Modules, NpcHitScript::class, BlowpipeEquipment::class) { d
            ->
            val target = spawnNpc(player.coords.translate(2, 0), npcTypes.getValue(2098))
            target.hitpoints = 80
            player.hat = InvObj(objTypes.getValue(12931))
            player.righthand = InvObj(objs.toxic_blowpipe)
            assertTrue(d.poison.inflict(target, 10))
            target.queueHit(player, 1, HitType.Ranged, 1)
            advance()
            assertEquals(0, d.poison.severity(target))
            assertFalse(d.poison.envenom(target))
            assertFalse(d.poison.inflict(target, 10))
            val beforeVenom = target.hitpoints
            d.poison.tick(target)
            advance()
            assertEquals(beforeVenom - 6, target.hitpoints)
            d.poison.tick(target)
            advance()
            assertEquals(beforeVenom - 14, target.hitpoints)
            d.poison.clear(target)
            assertTrue(d.poison.inflict(target, 10))
        }

    @OptIn(org.rsmod.annotations.InternalApi::class)
    @Test
    fun GameTestState
        .`one chinchompa can roll damage on nine opponents and a failed primary roll misses them all`() =
        runInjectedGameTest(
            Deps::class,
            Modules,
            WeaponAttackStylesScript::class,
            NpcHitScript::class,
        ) { d ->
            player.members = true
            player.observerUUID = player.uuid
            player.stats[stats.ranged] = 99
            d.areas.registerAll(
                org.rsmod.map.square.MapSquareKey.from(player.coords),
                listOf(areas.multiway.id.toShort()).iterator(),
            )
            val dummy = npcTypes.getValue(2098)
            val primary = spawnNpc(player.coords.translate(4, 0), dummy)
            val group =
                listOf(primary) + (1..9).map { spawnNpc(primary.coords.translate(1, 0), dummy) }
            val outside = spawnNpc(primary.coords.translate(3, 0), dummy)
            val start = primary.hitpoints
            player.righthand = InvObj(objs.red_chinchoma, 3)
            suspend fun org.rsmod.api.player.protect.ProtectedAccess.shoot() {
                val attack =
                    CombatAttack.Ranged(
                        requireNotNull(player.righthand),
                        RangedAttackType.Heavy,
                        RangedAttackStyle.Rapid,
                    )
                with(d.chins) { attack(primary, attack) }
            }
            random.next = 1000000
            player.withProtectedAccess { shoot() }
            advance(5)
            assertEquals(2, player.righthand?.count)
            assertTrue(group.all { it.hitpoints == start })
            player.withProtectedAccess {
                assertTrue(mapMultiway(), "The test throw must occur in a multiway area")
                assertEquals(
                    9,
                    d.targets.targets(player, primary, true, true).size,
                    "The group must still be attackable before the throw",
                )
            }
            random.next = 0
            repeat(9) { random.then = 1 }
            player.withProtectedAccess { shoot() }
            advance(5)
            assertEquals(1, player.righthand?.count)
            assertEquals(9, group.count { it.hitpoints < start })
            assertEquals(start, outside.hitpoints)
        }

    @Test
    fun GameTestState
        .`charged crystal bows shoot without ammunition and degrade while corrupted colours do not`() =
        runInjectedGameTest(Deps::class, Modules, WeaponAttackStylesScript::class) { d ->
            player.members = true
            player.observerUUID = player.uuid
            val target = spawnNpc(player.coords.translate(3, 0), npcTypes.getValue(2098))
            for (id in
                listOf(
                    23983,
                    24123,
                    25865,
                    25867,
                    25884,
                    25886,
                    25888,
                    25890,
                    25892,
                    25894,
                    25896,
                )) {
                val bow =
                    InvObj(objTypes.getValue(id), vars = if (id == 23983 || id == 25865) 1 else 0)
                player.righthand = bow
                val assumed =
                    CombatAttack.Ranged(bow, RangedAttackType.Standard, RangedAttackStyle.Rapid)
                player.withProtectedAccess { with(d.crystal) { attack(target, assumed) } }
                when (id) {
                    23983 -> assertEquals(23985, player.righthand?.id)
                    24123 -> assertEquals(InvObj(objs.crystal_bow, vars = 2499), player.righthand)
                    25865 -> assertEquals(25862, player.righthand?.id)
                    else -> assertEquals(bow, player.righthand)
                }
            }
            assertNull(player.quiver)
        }

    @Test
    fun GameTestState
        .`the final paid charge automatically corrupts an enhanced bow without consuming a quiver arrow`() =
        runInjectedGameTest(Deps::class, Modules, WeaponAttackStylesScript::class) { d ->
            player.observerUUID = player.uuid
            val bow = InvObj(objs.bow_of_faerdhinen, vars = (2000 shl 15) or 1)
            player.righthand = bow
            val target = spawnNpc(player.coords.translate(1, 0), npcTypes.getValue(2098))
            val shot = CombatAttack.Ranged(bow, RangedAttackType.Standard, RangedAttackStyle.Rapid)
            player.withProtectedAccess { with(d.crystal) { attack(target, shot) } }
            assertEquals(InvObj(objs.bow_of_faerdhinen_c_hefin), player.righthand)
        }

    @Test
    fun GameTestState
        .`one shard revives a bow and bank charging preserves progress and honours the target`() =
        runInjectedGameTest(Deps::class, Modules) { d ->
            player.inv[0] = InvObj(objTypes.getValue(25862))
            player.inv[1] = InvObj(objs.crystal_shard)
            player.withProtectedAccess { d.charging.addShards(this, 0, 1) }
            val first = requireNotNull(player.inv[0])
            assertEquals(25865, first.id)
            assertEquals(100, first.vars and 32767)
            assertEquals(1, first.vars ushr 15)
            assertNull(player.inv[1])
            player.setVarBit(cacheTypes.varbits.getValue(16130), 90)
            player.withProtectedAccess {
                d.charging.autocharge(
                    this,
                    BankedObjEvents.Autocharge(0, first) { _, _ -> error("above target") },
                )
            }
            player.setVarBit(cacheTypes.varbits.getValue(16130), 250)
            player.withProtectedAccess {
                d.charging.autocharge(
                    this,
                    BankedObjEvents.Autocharge(0, first) { item, amount ->
                        assertEquals(objs.crystal_shard, item)
                        assertEquals(2, amount)
                        amount
                    },
                )
            }
            assertEquals(300, requireNotNull(player.inv[0]).vars and 32767)
            assertEquals(3, requireNotNull(player.inv[0]).vars ushr 15)
        }

    @Test
    fun GameTestState
        .`elven purchases require Roving Elves and only paid enchanting advances Ilfeen's discount`() =
        runInjectedGameTest(Deps::class, Modules) { d ->
            player.members = true
            player.inv[0] = InvObj(objs.coins, 2000000)
            player.inv[1] = InvObj(objTypes.getValue(4207))
            player.withProtectedAccess { assertFalse(d.singers.buy(this, false)) }
            player.setVarp(cacheTypes.varps.getValue(402), 6)
            player.withProtectedAccess { assertTrue(d.singers.buy(this, false)) }
            assertEquals(1100000, player.inv.count(objs.coins))
            assertEquals(
                2500,
                player.inv.filterNotNull { it.id == objs.crystal_bow.id }.single().vars,
            )
            player.inv[3] = InvObj(objTypes.getValue(23985))
            player.withProtectedAccess {
                assertTrue(d.singers.buy(this, false))
                assertFalse(d.singers.buy(this, true))
            }
            assertEquals(380000, player.inv.count(objs.coins))
        }

    @Test
    fun GameTestState
        .`bow singing enforces membership quest skills and genuinely exchanges seed and shards for the bow`() =
        runInjectedGameTest(Deps::class, Modules) { d ->
            player.members = true
            player.inv[0] = InvObj(objTypes.getValue(25859))
            player.inv[1] = InvObj(objs.crystal_shard, 100)
            player.withProtectedAccess { d.singing.create(this, false, true) }
            assertEquals(100, player.inv.count(objs.crystal_shard))
            player.setVarBit(org.rsmod.api.config.refs.varbits.song_of_the_elves, 194)
            player.withProtectedAccess { d.singing.create(this, false, true) }
            assertEquals(100, player.inv.count(objs.crystal_shard))
            player.stats[stats.smithing] = 82
            player.stats[stats.crafting] = 82
            player.withProtectedAccess { d.singing.create(this, false, true) }
            advance(4)
            assertEquals(0, player.inv.count(objs.crystal_shard))
            assertEquals(
                10000,
                player.inv.filterNotNull { it.id == objs.bow_of_faerdhinen.id }.single().vars,
            )
        }

    private object Modules : AbstractModule() {
        override fun configure() {
            bind(AttackStyles::class.java).`in`(Scopes.SINGLETON)
            install(WeaponModule())
            install(SpecialAttackModule())
            install(RangedWeaponsModule())
        }
    }

    private class Deps
    @Inject
    constructor(
        val crystal: CrystalBows,
        val charging: CrystalBowCharging,
        val singing: ElvenBowSinging,
        val singers: ElvenBowServices,
        val targets: AreaAttackTargets,
        val areas: AreaIndex,
        val chins: Chinchompas,
        val pipes: ToxicBlowpipes,
        val equipment: BlowpipeEquipment,
        val poison: AncientNpcPoisonRepository,
        val weapons: WeaponAttackManager,
        val bootstrap: SpecialAttackBootstrap,
        val specials: SpecialAttackRegistry,
        val registry: WeaponRegistry,
        val specialManager: SpecialAttackManager,
        val specialsRepo: SpecialAttackRepository,
        val weaponRepo: WeaponRepository,
    )
}
