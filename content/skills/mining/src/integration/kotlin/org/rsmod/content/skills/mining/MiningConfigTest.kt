package org.rsmod.content.skills.mining

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.content
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.skills.mining.configs.MiningLocs
import org.rsmod.content.skills.mining.configs.MiningObjs
import org.rsmod.content.skills.mining.configs.MiningParams
import org.rsmod.content.skills.mining.configs.MiningSeqs
import org.rsmod.content.skills.mining.scripts.Mining.Companion.depleted
import org.rsmod.content.skills.mining.scripts.Mining.Companion.gemRock
import org.rsmod.content.skills.mining.scripts.Mining.Companion.level
import org.rsmod.content.skills.mining.scripts.Mining.Companion.pickaxeAnim
import org.rsmod.content.skills.mining.scripts.Mining.Companion.pickaxeFastIntervalChance
import org.rsmod.content.skills.mining.scripts.Mining.Companion.pickaxeInterval
import org.rsmod.content.skills.mining.scripts.Mining.Companion.pickaxeSoteRequired
import org.rsmod.content.skills.mining.scripts.Mining.Companion.respawnTime
import org.rsmod.content.skills.mining.scripts.Mining.Companion.successHigh
import org.rsmod.content.skills.mining.scripts.Mining.Companion.successLow
import org.rsmod.content.skills.mining.scripts.Mining.Companion.xp
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.seq.SeqType

class MiningConfigTest {
    @Test
    fun GameTestState.`standard rocks have complete mining metadata`() = runBasicGameTest {
        val rocks = cacheTypes.locs.values.filter { it.isContentType(content.ore) && !it.gemRock }
        assertEquals(22, rocks.size)
        for (rock in rocks) {
            assertEquals("Mine", rock.op[0], "rock=${rock.id}, ops=${rock.op.contentToString()}")
            val map = checkNotNull(rock.paramMap)
            assertTrue(params.levelrequire in map)
            assertTrue(params.skill_productitem in map)
            assertTrue(params.skill_xp in map)
            assertTrue(params.next_loc_stage in map)
            assertTrue(params.respawn_time in map)
            assertTrue(MiningParams.successLow in map)
            assertTrue(MiningParams.successHigh in map)
            assertTrue(rock.respawnTime > 0)
            assertTrue(rock.successLow >= 0)
            assertTrue(rock.successHigh > rock.successLow)
            assertTrue(rock.depleted.id in 11390..11392)
        }
    }

    @Test
    fun GameTestState.`gem rocks have exact cache 233 extraction metadata`() = runBasicGameTest {
        val expected =
            mapOf(
                MiningLocs.gemRock1 to MiningLocs.empty1,
                MiningLocs.gemRock2 to MiningLocs.empty2,
            )
        assertEquals(setOf(11380, 11381), expected.keys.mapTo(mutableSetOf()) { it.id })
        for ((reference, depleted) in expected) {
            val rock = cacheTypes.locs[reference]
            assertEquals("Gem rocks", rock.name)
            assertEquals("Mine", rock.op[0])
            assertTrue(rock.isContentType(content.ore))
            assertTrue(rock.gemRock)
            assertEquals(40, rock.level)
            assertEquals(65.0, rock.xp)
            assertEquals(99, rock.respawnTime)
            assertEquals(28, rock.successLow)
            assertEquals(70, rock.successHigh)
            assertEquals(depleted, rock.depleted)
            assertFalse(params.skill_productitem in checkNotNull(rock.paramMap))
        }
    }

    @Test
    fun `gem reward table covers every exact weighted boundary`() {
        val expected =
            listOf(
                Triple(objs.uncut_opal, 0, 59),
                Triple(objs.uncut_jade, 60, 89),
                Triple(objs.uncut_red_topaz, 90, 104),
                Triple(objs.uncut_sapphire, 105, 113),
                Triple(objs.uncut_emerald, 114, 118),
                Triple(objs.uncut_ruby, 119, 123),
                Triple(objs.uncut_diamond, 124, 127),
            )
        assertEquals(128, GemRockRewards.totalWeight)
        for ((obj, first, last) in expected) {
            assertEquals(obj, GemRockRewards.at(first).obj)
            assertEquals(obj, GemRockRewards.at(last).obj)
        }
    }

    @Test
    fun `ordinary rock gem pre-table covers every exact weighted boundary`() {
        val expected =
            listOf(
                PreGemExpected(null, 0, 69, null, null),
                PreGemExpected(objs.uncut_sapphire, 70, 101, "sapphire", "a"),
                PreGemExpected(objs.uncut_emerald, 102, 117, "emerald", "an"),
                PreGemExpected(objs.uncut_ruby, 118, 125, "ruby", "a"),
                PreGemExpected(objs.uncut_diamond, 126, 127, "diamond", "a"),
            )
        assertEquals(128, MiningGemPreTable.TOTAL_WEIGHT)
        assertEquals(32_768, MiningGemPreTable.NORMAL_DENOMINATOR)
        assertEquals(8_216, MiningGemPreTable.CHARGED_GLORY_DENOMINATOR)
        assertEquals(128, MiningGemPreTable.all.sumOf { it.weight })
        for ((obj, first, last, displayName, article) in expected) {
            val firstEntry = MiningGemPreTable.at(first)
            val lastEntry = MiningGemPreTable.at(last)
            assertEquals(obj, firstEntry.obj)
            assertEquals(obj, lastEntry.obj)
            assertEquals(displayName, firstEntry.displayName)
            assertEquals(article, firstEntry.article)
        }
    }

    @Test
    fun GameTestState.`standard pickaxe intervals match cache 233 contract`() = runBasicGameTest {
        val expected =
            mapOf(
                MiningObjs.bronzePickaxe to 8,
                MiningObjs.ironPickaxe to 7,
                MiningObjs.steelPickaxe to 6,
                MiningObjs.blackPickaxe to 5,
                MiningObjs.mithrilPickaxe to 5,
                MiningObjs.adamantPickaxe to 4,
                MiningObjs.runePickaxe to 3,
                MiningObjs.gildedPickaxe to 3,
            )
        for ((type, interval) in expected) {
            val pickaxe = cacheTypes.objs[type]
            assertEquals(interval, pickaxe.pickaxeInterval)
            assertEquals(0, pickaxe.pickaxeFastIntervalChance)
            assertFalse(pickaxe.pickaxeSoteRequired)
            val map = checkNotNull(pickaxe.paramMap)
            assertTrue(params.levelrequire in map)
            assertTrue(params.skill_anim in map)
            assertTrue(MiningParams.pickaxeInterval in map)
            assertTrue(MiningParams.pickaxeFastIntervalChance in map)
            assertTrue(MiningParams.pickaxeSoteRequired in map)
        }
    }

    @Test
    fun GameTestState.`special pickaxe families match cache 233 rate and animation contracts`() =
        runBasicGameTest {
            val expected =
                listOf(
                    SpecialPickaxe(MiningObjs.dragonPickaxe, 6, MiningSeqs.dragon),
                    SpecialPickaxe(MiningObjs.dragonPickaxeUpgraded, 6, MiningSeqs.dragonUpgraded),
                    SpecialPickaxe(MiningObjs.dragonPickaxeZalcano, 6, MiningSeqs.dragonZalcano),
                    SpecialPickaxe(
                        MiningObjs.dragonPickaxeTrailblazer,
                        6,
                        MiningSeqs.dragonTrailblazer,
                    ),
                    SpecialPickaxe(
                        MiningObjs.dragonPickaxeTrailblazerReloaded,
                        6,
                        MiningSeqs.dragonTrailblazerReloaded,
                    ),
                    SpecialPickaxe(MiningObjs.infernalPickaxe, 6, MiningSeqs.infernal),
                    SpecialPickaxe(MiningObjs.infernalPickaxeUncharged, 6, MiningSeqs.infernal),
                    SpecialPickaxe(
                        MiningObjs.infernalPickaxeTrailblazer,
                        6,
                        MiningSeqs.infernalTrailblazer,
                    ),
                    SpecialPickaxe(
                        MiningObjs.infernalPickaxeTrailblazerUncharged,
                        6,
                        MiningSeqs.infernalTrailblazer,
                    ),
                    SpecialPickaxe(
                        MiningObjs.infernalPickaxeTrailblazerReloaded,
                        6,
                        MiningSeqs.infernalTrailblazerReloaded,
                    ),
                    SpecialPickaxe(
                        MiningObjs.infernalPickaxeTrailblazerReloadedUncharged,
                        6,
                        MiningSeqs.infernalTrailblazerReloaded,
                    ),
                    SpecialPickaxe(MiningObjs.thirdAgePickaxe, 6, MiningSeqs.thirdAge),
                    SpecialPickaxe(
                        MiningObjs.crystalPickaxe,
                        4,
                        MiningSeqs.crystal,
                        soteRequired = true,
                    ),
                    SpecialPickaxe(
                        MiningObjs.crystalPickaxeInactive,
                        6,
                        MiningSeqs.dragon,
                        soteRequired = true,
                    ),
                )
            val contentPickaxes =
                cacheTypes.objs.values.filter { it.isContentType(content.mining_pickaxe) }
            assertEquals(22, contentPickaxes.size)
            for (definition in expected) {
                val pickaxe = cacheTypes.objs[definition.type]
                assertEquals(3, pickaxe.pickaxeInterval, pickaxe.name)
                assertEquals(definition.fastChance, pickaxe.pickaxeFastIntervalChance, pickaxe.name)
                assertEquals(definition.animation, pickaxe.pickaxeAnim, pickaxe.name)
                assertEquals(definition.soteRequired, pickaxe.pickaxeSoteRequired, pickaxe.name)
                assertTrue(pickaxe.members, pickaxe.name)
            }
        }

    private data class SpecialPickaxe(
        val type: ObjType,
        val fastChance: Int,
        val animation: SeqType,
        val soteRequired: Boolean = false,
    )

    private data class PreGemExpected(
        val obj: ObjType?,
        val first: Int,
        val last: Int,
        val displayName: String?,
        val article: String?,
    )
}
