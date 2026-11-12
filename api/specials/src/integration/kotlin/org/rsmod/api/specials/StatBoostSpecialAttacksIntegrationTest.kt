package org.rsmod.api.specials

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.specials.energy.SpecialAttackEnergy
import org.rsmod.api.specials.weapon.SpecialAttackWeapons
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.other.special.attacks.SpecialAttackModule as ContentSpecialAttackModule
import org.rsmod.content.other.special.attacks.configs.special_seqs
import org.rsmod.content.other.special.attacks.configs.special_spots
import org.rsmod.game.inv.InvObj
import org.rsmod.game.spot.EntitySpotanim
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.seq.SeqType
import org.rsmod.game.type.spot.SpotanimType

@Execution(ExecutionMode.SAME_THREAD)
class StatBoostSpecialAttacksIntegrationTest {
    @Test
    fun GameTestState.`production startup registers every cache special attack weapon`() =
        runStatBoostTest { deps ->
            deps.bootstrap.startup()

            val expected = deps.weapons.definitions().mapTo(sortedSetOf()) { it.objId }
            val actual = deps.registry.registeredIds().toSortedSet()

            assertEquals(expected, actual)
        }

    @Test
    fun GameTestState.`production startup registers every cache 233 Lumber Up variant`() =
        runStatBoostTest { deps ->
            deps.bootstrap.startup()
            val variants =
                listOf(
                    LumberVariant(objs.dragon_axe, special_spots.lumber_up_red),
                    LumberVariant(objs.dragon_axe_or, special_spots.lumber_up_red),
                    LumberVariant(objs.dragon_axe_or_reloaded, special_spots.lumber_up_red),
                    LumberVariant(objs.third_age_axe, special_spots.lumber_up_silver),
                    LumberVariant(objs.infernal_axe, special_spots.lumber_up_red),
                    LumberVariant(objs.infernal_axe_uncharged, special_spots.lumber_up_red),
                    LumberVariant(objs.infernal_axe_or, special_spots.lumber_up_red),
                    LumberVariant(objs.infernal_axe_or_uncharged, special_spots.lumber_up_red),
                    LumberVariant(objs.infernal_axe_or_reloaded, special_spots.lumber_up_red),
                    LumberVariant(
                        objs.infernal_axe_or_reloaded_uncharged,
                        special_spots.lumber_up_red,
                    ),
                    LumberVariant(objs.crystal_axe, special_spots.lumber_up_silver),
                    LumberVariant(objs.crystal_axe_inactive, special_spots.lumber_up_silver),
                )
            assertEquals(
                listOf(
                    6739,
                    25378,
                    30352,
                    20011,
                    13241,
                    13242,
                    25066,
                    25371,
                    30347,
                    30348,
                    23673,
                    23675,
                ),
                variants.map { it.obj.id },
            )

            player.withProtectedAccess {
                for (variant in variants) {
                    val special = deps.instant(variant.obj)
                    assertEquals(SpecialAttackEnergy.MAX_ENERGY, special.energyInHundreds)
                    assertEquals(
                        SpecialAttackEnergy.MAX_ENERGY,
                        deps.weapons.getSpecialEnergy(variant.obj),
                    )

                    player.setBaseLevel(stats.woodcutting, 61)
                    player.setCurrentLevel(stats.woodcutting, 61)
                    player.pendingSay = null
                    player.pendingSpotanims.clear()
                    resetAnim()
                    deps.energy.restoreSpecialEnergy(player, SpecialAttackEnergy.MAX_ENERGY)

                    assertTrue(special.activate(this))
                    deps.energy.takeSpecialEnergy(player, special.energyInHundreds)

                    assertEquals(64, player.statMap.getCurrentLevel(stats.woodcutting).toInt())
                    assertEquals("Chop chop!", player.pendingSay)
                    assertEquals(special_seqs.lumber_up.id, player.pendingSequence.id)
                    val spot = EntitySpotanim(player.pendingSpotanims.getLong(0))
                    assertEquals(variant.spot.id, spot.id)
                    assertEquals(96, spot.height)
                    assertEquals(constants.spotanim_slot_combat, spot.slot)
                    assertEquals(false, deps.energy.hasSpecialEnergy(player, 1))
                }
            }
        }

    @Test
    fun GameTestState.`production startup registers every cache 233 Fishstabber variant`() =
        runStatBoostTest { deps ->
            deps.bootstrap.startup()
            val variants =
                listOf(
                    FishVariant(
                        objs.dragon_harpoon,
                        special_seqs.fishstabber_dragon_harpoon,
                        spotanims.sp_attackglow_red,
                    ),
                    FishVariant(
                        objs.dragon_harpoon_or,
                        special_seqs.fishstabber_infernal_harpoon_or,
                        spotanims.sp_attackglow_red,
                    ),
                    FishVariant(
                        objs.dragon_harpoon_or_reloaded,
                        special_seqs.fishstabber_infernal_harpoon_or_reloaded,
                        spotanims.sp_attackglow_red,
                    ),
                    FishVariant(
                        objs.infernal_harpoon,
                        special_seqs.fishstabber_infernal_harpoon,
                        spotanims.sp_attackglow_red,
                    ),
                    FishVariant(
                        objs.infernal_harpoon_uncharged,
                        special_seqs.fishstabber_infernal_harpoon,
                        spotanims.sp_attackglow_red,
                    ),
                    FishVariant(
                        objs.infernal_harpoon_or,
                        special_seqs.fishstabber_infernal_harpoon_or,
                        spotanims.sp_attackglow_red,
                    ),
                    FishVariant(
                        objs.infernal_harpoon_or_uncharged,
                        special_seqs.fishstabber_infernal_harpoon_or,
                        spotanims.sp_attackglow_red,
                    ),
                    FishVariant(
                        objs.infernal_harpoon_or_reloaded,
                        special_seqs.fishstabber_infernal_harpoon_or_reloaded,
                        spotanims.sp_attackglow_red,
                    ),
                    FishVariant(
                        objs.infernal_harpoon_or_reloaded_uncharged,
                        special_seqs.fishstabber_infernal_harpoon_or_reloaded,
                        spotanims.sp_attackglow_red,
                    ),
                    FishVariant(
                        objs.crystal_harpoon,
                        special_seqs.fishstabber_crystal_harpoon,
                        special_spots.fishstabber_silver,
                    ),
                    FishVariant(
                        objs.crystal_harpoon_inactive,
                        special_seqs.fishstabber_crystal_harpoon,
                        special_spots.fishstabber_silver,
                    ),
                )
            assertEquals(
                listOf(21028, 25373, 30349, 21031, 21033, 25059, 25367, 30342, 30343, 23762, 23764),
                variants.map { it.obj.id },
            )

            player.withProtectedAccess {
                for (variant in variants) {
                    val special = deps.instant(variant.obj)
                    assertEquals(SpecialAttackEnergy.MAX_ENERGY, special.energyInHundreds)
                    assertEquals(
                        SpecialAttackEnergy.MAX_ENERGY,
                        deps.weapons.getSpecialEnergy(variant.obj),
                    )

                    player.setBaseLevel(stats.fishing, 61)
                    player.setCurrentLevel(stats.fishing, 61)
                    player.pendingSay = null
                    player.pendingSpotanims.clear()
                    resetAnim()
                    deps.energy.restoreSpecialEnergy(player, SpecialAttackEnergy.MAX_ENERGY)

                    assertTrue(special.activate(this))
                    deps.energy.takeSpecialEnergy(player, special.energyInHundreds)

                    assertEquals(64, player.statMap.getCurrentLevel(stats.fishing).toInt())
                    assertEquals("Here fishy fishies!", player.pendingSay)
                    assertEquals(variant.seq.id, player.pendingSequence.id)
                    val spot = EntitySpotanim(player.pendingSpotanims.getLong(0))
                    assertEquals(variant.spot.id, spot.id)
                    assertEquals(false, deps.energy.hasSpecialEnergy(player, 1))
                }
            }
        }

    @Test
    fun GameTestState.`production startup registers every cache 233 Rock Knocker variant`() =
        runStatBoostTest { deps ->
            deps.bootstrap.startup()
            val variants =
                listOf(
                    RockVariant(objs.dragon_pickaxe, special_seqs.rock_knocker_dragon_pickaxe),
                    RockVariant(
                        objs.dragon_pickaxe_or_zalcano,
                        special_seqs.rock_knocker_dragon_pickaxe_or_zalcano,
                    ),
                    RockVariant(
                        objs.dragon_pickaxe_or_trailblazer,
                        special_seqs.rock_knocker_dragon_pickaxe_or_trailblazer,
                    ),
                    RockVariant(
                        objs.dragon_pickaxe_or_trailblazer_reloaded,
                        special_seqs.rock_knocker_dragon_pickaxe_or_trailblazer_reloaded,
                    ),
                    RockVariant(
                        objs.dragon_pickaxe_upgraded,
                        special_seqs.rock_knocker_dragon_pickaxe_upgraded,
                    ),
                    RockVariant(objs.infernal_pickaxe, special_seqs.rock_knocker_infernal_pickaxe),
                    RockVariant(
                        objs.infernal_pickaxe_uncharged,
                        special_seqs.rock_knocker_infernal_pickaxe,
                    ),
                    RockVariant(
                        objs.infernal_pickaxe_or,
                        special_seqs.rock_knocker_dragon_pickaxe_or_trailblazer,
                    ),
                    RockVariant(
                        objs.infernal_pickaxe_or_uncharged,
                        special_seqs.rock_knocker_dragon_pickaxe_or_trailblazer,
                    ),
                    RockVariant(
                        objs.infernal_pickaxe_or_reloaded,
                        special_seqs.rock_knocker_dragon_pickaxe_or_trailblazer_reloaded,
                    ),
                    RockVariant(
                        objs.infernal_pickaxe_or_reloaded_uncharged,
                        special_seqs.rock_knocker_dragon_pickaxe_or_trailblazer_reloaded,
                    ),
                    RockVariant(objs.third_age_pickaxe, special_seqs.rock_knocker_3rd_age_pickaxe),
                    RockVariant(objs.crystal_pickaxe, special_seqs.rock_knocker_crystal_pickaxe),
                    RockVariant(
                        objs.crystal_pickaxe_inactive,
                        special_seqs.rock_knocker_crystal_pickaxe,
                    ),
                )
            assertEquals(
                listOf(
                    11920,
                    23677,
                    25376,
                    30351,
                    12797,
                    13243,
                    13244,
                    25063,
                    25369,
                    30345,
                    30346,
                    20014,
                    23680,
                    23682,
                ),
                variants.map { it.obj.id },
            )

            player.withProtectedAccess {
                for (variant in variants) {
                    val special = deps.instant(variant.obj)
                    assertEquals(SpecialAttackEnergy.MAX_ENERGY, special.energyInHundreds)
                    assertEquals(
                        SpecialAttackEnergy.MAX_ENERGY,
                        deps.weapons.getSpecialEnergy(variant.obj),
                    )

                    player.setBaseLevel(stats.mining, 61)
                    player.setCurrentLevel(stats.mining, 61)
                    player.pendingSay = null
                    resetAnim()
                    deps.energy.restoreSpecialEnergy(player, SpecialAttackEnergy.MAX_ENERGY)

                    assertTrue(special.activate(this))
                    deps.energy.takeSpecialEnergy(player, special.energyInHundreds)

                    assertEquals(64, player.statMap.getCurrentLevel(stats.mining).toInt())
                    assertEquals("Smashing!", player.pendingSay)
                    assertEquals(variant.seq.id, player.pendingSequence.id)
                    assertEquals(false, deps.energy.hasSpecialEnergy(player, 1))
                }
            }
        }

    @Test
    fun GameTestState.`stat boost specials preserve a larger existing boost`() =
        runStatBoostTest { deps ->
            deps.bootstrap.startup()
            val special = deps.instant(objs.dragon_pickaxe)
            player.setBaseLevel(stats.mining, 61)
            player.setCurrentLevel(stats.mining, 70)

            player.withProtectedAccess { assertTrue(special.activate(this)) }

            assertEquals(70, player.statMap.getCurrentLevel(stats.mining).toInt())
        }

    private fun GameTestState.runStatBoostTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(Dependencies::class, TestModule, testBody = body)

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(SpecialAttackModule())
            install(ContentSpecialAttackModule())
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val bootstrap: SpecialAttackBootstrap,
        val registry: SpecialAttackRegistry,
        val weapons: SpecialAttackWeapons,
        val energy: SpecialAttackEnergy,
    ) {
        fun instant(obj: ObjType): SpecialAttack.Instant =
            assertInstanceOf(SpecialAttack.Instant::class.java, registry[InvObj(obj)])
    }

    private data class LumberVariant(val obj: ObjType, val spot: SpotanimType)

    private data class FishVariant(val obj: ObjType, val seq: SeqType, val spot: SpotanimType)

    private data class RockVariant(val obj: ObjType, val seq: SeqType)
}
