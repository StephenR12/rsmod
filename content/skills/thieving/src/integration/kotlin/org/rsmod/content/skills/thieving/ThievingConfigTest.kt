package org.rsmod.content.skills.thieving

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.synths
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.testing.GameTestState
import org.rsmod.map.CoordGrid

class ThievingConfigTest {
    @Test
    fun GameTestState.`historical man and woman families have exact pickpocket contracts`() =
        runBasicGameTest {
            val expectedMen =
                setOf(
                    3014,
                    3106,
                    3107,
                    3108,
                    3109,
                    3110,
                    3261,
                    3264,
                    3265,
                    3652,
                    6815,
                    6818,
                    6987,
                    6988,
                    6989,
                    11057,
                    11058,
                )
            val expectedWomen = setOf(3015, 3111, 3112, 3113, 6990, 6991, 6992, 10728, 11053, 11054)
            assertEquals(expectedMen, StandardPickpockets.man.npcs.mapTo(hashSetOf()) { it.id })
            assertEquals(expectedWomen, StandardPickpockets.woman.npcs.mapTo(hashSetOf()) { it.id })
            assertEquals(209, StandardPickpockets.allNpcs.size)
            for (npc in StandardPickpockets.allNpcs) {
                val type = cacheTypes.npcs[npc]
                assertEquals(
                    "Pickpocket",
                    type.op[2],
                    "npc=${type.id}, ops=${type.op.contentToString()}",
                )
            }
        }

    @Test
    fun GameTestState.`ham member source boundary identities and outputs match cache 233`() =
        runBasicGameTest {
            assertEquals(setOf(2540, 2541), ThievingNpcs.hamMembers.mapTo(hashSetOf()) { it.id })
            for (npc in ThievingNpcs.hamMembers) {
                val type = cacheTypes.npcs[npc]
                assertEquals(
                    "Pickpocket",
                    type.op[0],
                    "npc=${npc.id}, ops=${type.op.contentToString()}",
                )
                assertFalse("Pickpocket" in type.op.drop(1), "npc=${npc.id}")
            }

            val outputs =
                listOf(
                    ThievingObjs.hamShirt,
                    ThievingObjs.hamRobe,
                    ThievingObjs.hamHood,
                    ThievingObjs.hamCloak,
                    ThievingObjs.hamLogo,
                    ThievingObjs.hamGloves,
                    ThievingObjs.hamBoots,
                    ThievingObjs.bronzeArrow,
                    ThievingObjs.bronzeAxe,
                    ThievingObjs.bronzeDagger,
                    ThievingObjs.bronzePickaxe,
                    ThievingObjs.ironAxe,
                    ThievingObjs.ironDagger,
                    ThievingObjs.ironPickaxe,
                    ThievingObjs.leatherBody,
                    ThievingObjs.steelArrow,
                    ThievingObjs.steelAxe,
                    ThievingObjs.steelDagger,
                    ThievingObjs.steelPickaxe,
                    ThievingObjs.buttons,
                    ThievingObjs.hamPouch,
                    ThievingObjs.feather,
                    ThievingObjs.knife,
                    ThievingObjs.logs,
                    ThievingObjs.needle,
                    ThievingObjs.rawAnchovies,
                    ThievingObjs.rawChicken,
                    ThievingObjs.thread,
                    ThievingObjs.tinderbox,
                    ThievingObjs.uncutOpal,
                    ThievingObjs.coal,
                    ThievingObjs.cowhide,
                    ThievingObjs.damagedArmour,
                    ThievingObjs.grimyGuamLeaf,
                    ThievingObjs.grimyMarrentill,
                    ThievingObjs.grimyTarromin,
                    ThievingObjs.ironOre,
                    ThievingObjs.rustySword,
                    ThievingObjs.uncutJade,
                )
            assertEquals(
                listOf(
                    4298,
                    4300,
                    4302,
                    4304,
                    4306,
                    4308,
                    4310,
                    882,
                    1351,
                    1205,
                    1265,
                    1349,
                    1203,
                    1267,
                    1129,
                    886,
                    1353,
                    1207,
                    1269,
                    688,
                    22523,
                    314,
                    946,
                    1511,
                    1733,
                    321,
                    2138,
                    1734,
                    590,
                    1609,
                    453,
                    1739,
                    697,
                    199,
                    201,
                    203,
                    440,
                    686,
                    1611,
                ),
                outputs.map { it.id },
            )

            val pouch = cacheTypes.objs[ThievingObjs.hamPouch]
            assertEquals("Coin pouch", pouch.name)
            assertTrue(pouch.isStackable)
            assertFalse(pouch.tradeable)
            assertEquals("Open-all", pouch.iop[0])
            assertEquals("Open", pouch.iop[1])
            assertTrue(ThievingNpcs.hamMembers.none { it in StandardPickpockets.allNpcs })
            assertEquals(209, StandardPickpockets.allNpcs.size)
        }

    @Test
    fun GameTestState.`ham storeroom guard source boundary matches cache 233`() = runBasicGameTest {
        assertEquals(
            setOf(4522, 4523, 4524, 4525, 4526),
            ThievingNpcs.hamStoreroomGuards.mapTo(hashSetOf()) { it.id },
        )
        for (npc in ThievingNpcs.hamStoreroomGuards) {
            val type = cacheTypes.npcs[npc]
            assertEquals(
                "Pickpocket",
                type.op[0],
                "npc=${npc.id}, ops=${type.op.contentToString()}",
            )
            assertFalse("Pickpocket" in type.op.drop(1), "npc=${npc.id}")
            assertTrue(npc !in StandardPickpockets.allNpcs, "npc=${npc.id}")
        }
        assertEquals(
            setOf(4516, 4517, 4518, 4519, 4520),
            ThievingNpcs.hamStoreroomQuestPatrolGuards.mapTo(hashSetOf()) { it.id },
        )
        assertEquals(4521, ThievingNpcs.hamStoreroomQuestArrestGuard.id)
        val questGuards =
            ThievingNpcs.hamStoreroomQuestPatrolGuards + ThievingNpcs.hamStoreroomQuestArrestGuard
        for (npc in questGuards) {
            assertFalse("Pickpocket" in cacheTypes.npcs[npc].op, "npc=${npc.id}")
            assertTrue(npc !in StandardPickpockets.allNpcs, "npc=${npc.id}")
        }

        val keys =
            listOf(
                ThievingObjs.hamBronzeKey,
                ThievingObjs.hamIronKey,
                ThievingObjs.hamSilverKey,
                ThievingObjs.hamSteelKey,
            )
        assertEquals(listOf(8867, 8869, 8868, 8866), keys.map { it.id })
        assertEquals(
            listOf("Bronze key", "Iron key", "Silver key", "Steel key"),
            keys.map { cacheTypes.objs[it].name },
        )
        for (key in keys) {
            val type = cacheTypes.objs[key]
            assertFalse(type.isStackable, "obj=${key.id}")
            assertTrue(type.tradeable, "obj=${key.id}")
            assertFalse(type.stockmarket, "obj=${key.id}")
            assertTrue(type.hasPlaceholder, "obj=${key.id}")
        }
        assertTrue(CoinPouchDefinitions.active.none { it.obj == ThievingObjs.hamPouch })
        assertEquals(209, StandardPickpockets.allNpcs.size)
    }

    @Test
    fun GameTestState.`ham storeroom chest source boundary matches cache 233`() =
        runAdvancedGameTest { advanced ->
            val closed =
                listOf(
                    ThievingLocs.hamSteelChest,
                    ThievingLocs.hamBronzeChest,
                    ThievingLocs.hamSilverChest,
                    ThievingLocs.hamIronChest,
                )
            assertEquals(listOf(15722, 15723, 15724, 15726), closed.map { it.id })
            for (chest in closed) {
                val type = cacheTypes.locs[chest]
                assertEquals("Small chest", type.name)
                assertEquals("Open", type.op[0], "loc=${chest.id}")
                assertEquals("I don't think it contains ham.", type.desc)
            }
            val opened = cacheTypes.locs[ThievingLocs.hamOpenChest]
            assertEquals(15725, opened.id)
            assertEquals("Small chest", opened.name)
            assertFalse(opened.op.any { it != null })

            val placements =
                mapOf(
                    ThievingLocs.hamSteelChest to
                        listOf(CoordGrid(2573, 5193), CoordGrid(2577, 5208), CoordGrid(2577, 5209)),
                    ThievingLocs.hamBronzeChest to listOf(CoordGrid(2570, 5192)),
                    ThievingLocs.hamSilverChest to listOf(CoordGrid(2569, 5199)),
                    ThievingLocs.hamIronChest to listOf(CoordGrid(2573, 5198)),
                )
            for ((type, coords) in placements) {
                for (coord in coords) {
                    assertNotNull(
                        advanced.readOnly.locRegistry.findType(coord, type.id),
                        "loc=${type.id}, coords=$coord",
                    )
                }
            }

            val rewards =
                listOf(
                    ThievingObjs.coins,
                    ThievingObjs.sapphire,
                    ThievingObjs.emerald,
                    ThievingObjs.ruby,
                    ThievingObjs.diamond,
                    ThievingObjs.goldRing,
                    ThievingObjs.sapphireRing,
                    ThievingObjs.emeraldRing,
                    ThievingObjs.rubyRing,
                    ThievingObjs.diamondRing,
                    ThievingObjs.goldNecklace,
                    ThievingObjs.sapphireNecklace,
                    ThievingObjs.emeraldNecklace,
                    ThievingObjs.rubyNecklace,
                    ThievingObjs.diamondNecklace,
                    ThievingObjs.goldAmulet,
                    ThievingObjs.sapphireAmulet,
                    ThievingObjs.emeraldAmulet,
                    ThievingObjs.rubyAmulet,
                    ThievingObjs.diamondAmulet,
                )
            assertEquals(
                listOf(
                    995,
                    1607,
                    1605,
                    1603,
                    1601,
                    1635,
                    1637,
                    1639,
                    1641,
                    1643,
                    1654,
                    1656,
                    1658,
                    1660,
                    1662,
                    1692,
                    1694,
                    1696,
                    1698,
                    1700,
                ),
                rewards.map { it.id },
            )
            assertEquals(
                listOf(
                    "Coins",
                    "Sapphire",
                    "Emerald",
                    "Ruby",
                    "Diamond",
                    "Gold ring",
                    "Sapphire ring",
                    "Emerald ring",
                    "Ruby ring",
                    "Diamond ring",
                    "Gold necklace",
                    "Sapphire necklace",
                    "Emerald necklace",
                    "Ruby necklace",
                    "Diamond necklace",
                    "Gold amulet",
                    "Sapphire amulet",
                    "Emerald amulet",
                    "Ruby amulet",
                    "Diamond amulet",
                ),
                rewards.map { cacheTypes.objs[it].name },
            )
        }

    @Test
    fun GameTestState.`ham storeroom door source boundary matches cache 233`() =
        runAdvancedGameTest { advanced ->
            val door = cacheTypes.locs[ThievingLocs.hamLockedDoor]
            assertEquals(15759, door.id)
            assertEquals("Door", door.name)
            assertEquals(listOf("Open", null, null, "Pick-lock", null), door.op.toList())
            assertEquals("The door is closed.", door.desc)

            val placements =
                listOf(
                    CoordGrid(2567, 5198),
                    CoordGrid(2576, 5198),
                    CoordGrid(2567, 5192),
                    CoordGrid(2576, 5192),
                )
            for (coord in placements) {
                assertNotNull(
                    advanced.readOnly.locRegistry.findType(coord, door.id),
                    "loc=${door.id}, coords=$coord",
                )
            }

            assertEquals(1523, ThievingObjs.lockpick.id)
            assertEquals("Lockpick", cacheTypes.objs[ThievingObjs.lockpick].name)
        }

    @Test
    fun GameTestState.`wealthy citizen source boundary matches cache 233`() = runBasicGameTest {
        assertEquals(
            setOf(13302, 13303, 13304, 13305),
            ThievingNpcs.wealthyCitizens.mapTo(hashSetOf()) { it.id },
        )
        for (npc in ThievingNpcs.wealthyCitizens) {
            val type = cacheTypes.npcs[npc]
            assertEquals(
                "Pickpocket",
                type.op[0],
                "npc=${npc.id}, ops=${type.op.contentToString()}",
            )
            assertFalse("Pickpocket" in type.op.drop(1), "npc=${npc.id}")
        }

        val pouch = cacheTypes.objs[ThievingObjs.wealthyCitizenPouch]
        assertEquals(28822, pouch.id)
        assertEquals("Coin pouch", pouch.name)
        assertTrue(pouch.isStackable)
        assertFalse(pouch.tradeable)
        assertEquals("Open-all", pouch.iop[0])
        assertEquals("Open", pouch.iop[1])

        val houseKey = cacheTypes.objs[ThievingObjs.wealthyCitizenHouseKey]
        assertEquals(29325, houseKey.id)
        assertEquals("House keys", houseKey.name)
        assertTrue(houseKey.isStackable)

        val dryStreak = cacheTypes.varbits[ThievingVarbits.wealthyCitizenHouseKeyDryStreak]
        assertEquals(9942, dryStreak.id)
        assertEquals(4175, dryStreak.baseVar.id)
        assertEquals(2..9, dryStreak.bits)
        assertTrue(ThievingNpcs.wealthyCitizens.none { it in StandardPickpockets.allNpcs })
        assertTrue(CoinPouchDefinitions.active.none { it.obj == ThievingObjs.wealthyCitizenPouch })
        assertEquals(209, StandardPickpockets.allNpcs.size)
    }

    @Test
    fun GameTestState.`pollnivneach bandit source boundary matches cache 233`() = runBasicGameTest {
        assertEquals(setOf(734, 735), ThievingNpcs.pollnivneachBandits.mapTo(hashSetOf()) { it.id })
        assertEquals(
            setOf(736, 737),
            ThievingNpcs.beardedPollnivneachBandits.mapTo(hashSetOf()) { it.id },
        )
        val bandits = ThievingNpcs.pollnivneachBandits + ThievingNpcs.beardedPollnivneachBandits
        for (npc in bandits) {
            val type = cacheTypes.npcs[npc]
            val expectedOps =
                if (npc.id == 735 || npc.id == 737) {
                    listOf("Talk-to", "Attack", "Pickpocket", "Lure", "Knock-Out")
                } else {
                    listOf("Talk-to", "Attack", "Pickpocket", null, null)
                }
            assertEquals(
                expectedOps,
                type.op.toList(),
                "npc=${npc.id}, ops=${type.op.contentToString()}",
            )
        }

        val beardedPouch = cacheTypes.objs[ThievingObjs.bandit2Pouch]
        assertEquals(22529, beardedPouch.id)
        assertEquals("Coin pouch", beardedPouch.name)
        assertTrue(beardedPouch.isStackable)
        assertFalse(beardedPouch.tradeable)
        assertEquals("Open-all", beardedPouch.iop[0])
        assertEquals("Open", beardedPouch.iop[1])

        val nonBeardedPouch = cacheTypes.objs[ThievingObjs.banditPouch]
        assertEquals(22532, nonBeardedPouch.id)
        assertEquals("Coin pouch", nonBeardedPouch.name)
        assertTrue(nonBeardedPouch.isStackable)
        assertFalse(nonBeardedPouch.tradeable)
        assertEquals("Open-all", nonBeardedPouch.iop[0])
        assertEquals("Open", nonBeardedPouch.iop[1])

        assertTrue(bandits.none { it in StandardPickpockets.allNpcs })
        assertTrue(CoinPouchDefinitions.active.none { it.obj == ThievingObjs.bandit2Pouch })
        assertTrue(CoinPouchDefinitions.active.none { it.obj == ThievingObjs.banditPouch })
        assertEquals(209, StandardPickpockets.allNpcs.size)
    }

    @Test
    fun GameTestState.`historical farmer family and weighted table match cache 233`() =
        runBasicGameTest {
            val expectedFarmers =
                setOf(
                    3114,
                    3243,
                    3244,
                    11918,
                    11919,
                    11920,
                    11921,
                    13228,
                    13229,
                    13230,
                    13231,
                    13232,
                    13233,
                    13234,
                    13235,
                    14751,
                    14752,
                    14753,
                    14754,
                    14773,
                )
            assertEquals(
                expectedFarmers,
                StandardPickpockets.farmer.npcs.mapTo(hashSetOf()) { it.id },
            )
            assertEquals(10, StandardPickpockets.farmer.level)
            assertEquals(14.5, StandardPickpockets.farmer.xp)
            assertEquals(150, StandardPickpockets.farmer.chanceLow)
            assertEquals(240, StandardPickpockets.farmer.chanceHigh)
            assertEquals(128, StandardPickpockets.farmer.totalRewardWeight)
            assertEquals(ThievingObjs.farmerPouch, StandardPickpockets.farmer.rewardAt(0).obj)
            assertEquals(ThievingObjs.farmerPouch, StandardPickpockets.farmer.rewardAt(122).obj)
            assertEquals(ThievingObjs.potatoSeed, StandardPickpockets.farmer.rewardAt(123).obj)
            assertEquals(ThievingObjs.potatoSeed, StandardPickpockets.farmer.rewardAt(127).obj)

            val pouch = cacheTypes.objs[ThievingObjs.farmerPouch]
            assertEquals(22522, pouch.id)
            assertEquals("Coin pouch", pouch.name)
            assertTrue(pouch.isStackable)
            assertFalse(pouch.tradeable)
            assertEquals("Open-all", pouch.iop[0])
            assertEquals("Open", pouch.iop[1])
            assertEquals(5318, ThievingObjs.potatoSeed.id)
        }

    @Test
    fun GameTestState.`rogue outfit references match cache 233`() = runBasicGameTest {
        assertEquals(5553, ThievingObjs.rogueTop.id)
        assertEquals(5554, ThievingObjs.rogueMask.id)
        assertEquals(5555, ThievingObjs.rogueTrousers.id)
        assertEquals(5556, ThievingObjs.rogueGloves.id)
        assertEquals(5557, ThievingObjs.rogueBoots.id)
    }

    @Test
    fun GameTestState.`historical warrior family and pouch match cache 233`() = runBasicGameTest {
        val expectedWarriors = setOf(3260, 3292, 11925, 11926, 11927, 11928, 11929)
        assertEquals(
            expectedWarriors,
            StandardPickpockets.warrior.npcs.mapTo(hashSetOf()) { it.id },
        )
        assertEquals(25, StandardPickpockets.warrior.level)
        assertEquals(26.0, StandardPickpockets.warrior.xp)
        assertEquals(100, StandardPickpockets.warrior.chanceLow)
        assertEquals(240, StandardPickpockets.warrior.chanceHigh)
        assertEquals(2, StandardPickpockets.warrior.damage)

        val pouch = cacheTypes.objs[ThievingObjs.warriorPouch]
        assertEquals(22524, pouch.id)
        assertEquals("Coin pouch", pouch.name)
        assertTrue(pouch.isStackable)
        assertFalse(pouch.tradeable)
        assertEquals("Open-all", pouch.iop[0])
        assertEquals("Open", pouch.iop[1])
    }

    @Test
    fun GameTestState.`historical rogue table and pouch match cache 233`() = runBasicGameTest {
        assertEquals(setOf(526), StandardPickpockets.rogue.npcs.mapTo(hashSetOf()) { it.id })
        assertEquals("Pickpocket", cacheTypes.npcs[ThievingNpcs.rogue].op[2])
        assertFalse("Pickpocket" in cacheTypes.npcs[ThievingNpcs.wildernessRogue].op)
        assertEquals(32, StandardPickpockets.rogue.level)
        assertEquals(36.5, StandardPickpockets.rogue.xp)
        assertEquals(75, StandardPickpockets.rogue.chanceLow)
        assertEquals(240, StandardPickpockets.rogue.chanceHigh)
        assertEquals(2, StandardPickpockets.rogue.damage)
        assertEquals(144, StandardPickpockets.rogue.totalRewardWeight)
        assertEquals(ThievingObjs.roguePouch, StandardPickpockets.rogue.rewardAt(0).obj)
        assertEquals(ThievingObjs.roguePouch, StandardPickpockets.rogue.rewardAt(122).obj)
        assertEquals(ThievingObjs.airRune, StandardPickpockets.rogue.rewardAt(123).obj)
        assertEquals(8, StandardPickpockets.rogue.rewardAt(131).count)
        assertEquals(ThievingObjs.jugWine, StandardPickpockets.rogue.rewardAt(132).obj)
        assertEquals(ThievingObjs.lockpick, StandardPickpockets.rogue.rewardAt(138).obj)
        assertEquals(ThievingObjs.ironDaggerP, StandardPickpockets.rogue.rewardAt(143).obj)
        assertEquals(25..40, CoinPouchDefinitions.rogue.coins)

        val pouch = cacheTypes.objs[ThievingObjs.roguePouch]
        assertEquals(22525, pouch.id)
        assertEquals("Coin pouch", pouch.name)
        assertTrue(pouch.isStackable)
        assertFalse(pouch.tradeable)
        assertEquals("Open-all", pouch.iop[0])
        assertEquals("Open", pouch.iop[1])
    }

    @Test
    fun GameTestState
        .`ordinary guard family excludes ham storeroom guards and matches cache 233`() =
        runBasicGameTest {
            val expectedGuards = buildSet {
                addAll(397..400)
                addAll(1546..1550)
                addAll(3010..3011)
                add(3254)
                addAll(3269..3274)
                add(3283)
                add(5418)
                addAll(11092..11107)
                addAll(11911..11917)
                addAll(11922..11924)
                addAll(11937..11939)
                addAll(11942..11947)
                addAll(13100..13109)
                addAll(13986..13995)
                addAll(14663..14670)
                addAll(14716..14723)
            }
            assertEquals(91, expectedGuards.size)
            assertEquals(
                expectedGuards,
                StandardPickpockets.guard.npcs.mapTo(hashSetOf()) { it.id },
            )
            assertTrue((4522..4526).none { it in expectedGuards })
            assertEquals(40, StandardPickpockets.guard.level)
            assertEquals(46.8, StandardPickpockets.guard.xp)
            assertEquals(50, StandardPickpockets.guard.chanceLow)
            assertEquals(240, StandardPickpockets.guard.chanceHigh)
            assertEquals(2, StandardPickpockets.guard.damage)

            val pouch = cacheTypes.objs[ThievingObjs.guardPouch]
            assertEquals(22527, pouch.id)
            assertEquals("Coin pouch", pouch.name)
            assertTrue(pouch.isStackable)
            assertFalse(pouch.tradeable)
            assertEquals("Open-all", pouch.iop[0])
            assertEquals("Open", pouch.iop[1])
        }

    @Test
    fun GameTestState.`knight families and pouch match cache 233`() = runBasicGameTest {
        val expectedArdougne = setOf(3297, 3300, 8854, 11902, 11936)
        val expectedVarlamore = (13114..13119).toSet()
        assertEquals(
            expectedArdougne,
            StandardPickpockets.knightOfArdougne.npcs.mapTo(hashSetOf()) { it.id },
        )
        assertEquals(
            expectedVarlamore,
            StandardPickpockets.knightOfVarlamore.npcs.mapTo(hashSetOf()) { it.id },
        )
        assertTrue(StandardPickpockets.knightOfArdougne.mediumArdougneDiaryApplies)
        assertFalse(StandardPickpockets.knightOfVarlamore.mediumArdougneDiaryApplies)
        for (definition in
            listOf(StandardPickpockets.knightOfArdougne, StandardPickpockets.knightOfVarlamore)) {
            assertEquals(55, definition.level)
            assertEquals(84.3, definition.xp)
            assertEquals(50, definition.chanceLow)
            assertEquals(240, definition.chanceHigh)
            assertEquals(3, definition.damage)
            assertEquals(10, definition.pickpocketBlockCycles)
            assertEquals(11, definition.movementBlockCycles)
            for (npc in definition.npcs) {
                assertEquals("Pickpocket", cacheTypes.npcs[npc].op[2], "npc=${npc.id}")
            }
        }

        val pouch = cacheTypes.objs[ThievingObjs.knightPouch]
        assertEquals(22531, pouch.id)
        assertEquals("Coin pouch", pouch.name)
        assertTrue(pouch.isStackable)
        assertFalse(pouch.tradeable)
        assertEquals("Open-all", pouch.iop[0])
        assertEquals("Open", pouch.iop[1])
        assertEquals(50..50, CoinPouchDefinitions.knight.coins)
    }

    @Test
    fun GameTestState.`watchman bundle and pouch match cache 233`() = runBasicGameTest {
        assertEquals(setOf(5420), StandardPickpockets.watchman.npcs.mapTo(hashSetOf()) { it.id })
        assertEquals("Pickpocket", cacheTypes.npcs[ThievingNpcs.watchman].op[2])
        assertEquals(65, StandardPickpockets.watchman.level)
        assertEquals(137.5, StandardPickpockets.watchman.xp)
        assertEquals(15, StandardPickpockets.watchman.chanceLow)
        assertEquals(160, StandardPickpockets.watchman.chanceHigh)
        assertEquals(3, StandardPickpockets.watchman.damage)
        assertEquals(ThievingObjs.watchmanPouch, StandardPickpockets.watchman.rewardAt(0).obj)
        assertEquals(
            listOf(ThievingObjs.bread),
            StandardPickpockets.watchman.guaranteedRewards.map { it.obj },
        )
        assertEquals(2309, ThievingObjs.bread.id)

        val pouch = cacheTypes.objs[ThievingObjs.watchmanPouch]
        assertEquals(22533, pouch.id)
        assertEquals("Coin pouch", pouch.name)
        assertTrue(pouch.isStackable)
        assertFalse(pouch.tradeable)
        assertEquals("Open-all", pouch.iop[0])
        assertEquals("Open", pouch.iop[1])
        assertEquals(60..60, CoinPouchDefinitions.watchman.coins)
    }

    @Test
    fun GameTestState.`menaphite thug quest forms and ordinary pouch match cache 233`() =
        runBasicGameTest {
            assertEquals(
                setOf(3549, 3550),
                ThievingNpcs.menaphiteThugs.mapTo(hashSetOf()) { it.id },
            )
            val beforeQuest = cacheTypes.npcs[ThievingNpcs.menaphiteThugBeforeQuest]
            assertEquals(listOf("Talk-to", "Attack"), beforeQuest.op.filterNotNull())
            assertFalse("Pickpocket" in beforeQuest.op)
            assertFalse(ThievingNpcs.menaphiteThugBeforeQuest in StandardPickpockets.allNpcs)

            val afterQuest = cacheTypes.npcs[ThievingNpcs.menaphiteThugAfterQuest]
            assertEquals(
                listOf("Talk-to", "Attack", "Pickpocket", "Lure", "Knock-Out"),
                afterQuest.op.filterNotNull(),
            )
            assertEquals("Pickpocket", afterQuest.op[2])
            assertEquals(
                setOf(3550),
                StandardPickpockets.menaphiteThug.npcs.mapTo(hashSetOf()) { it.id },
            )
            assertEquals(65, StandardPickpockets.menaphiteThug.level)
            assertEquals(137.5, StandardPickpockets.menaphiteThug.xp)
            assertEquals(50, StandardPickpockets.menaphiteThug.chanceLow)
            assertEquals(160, StandardPickpockets.menaphiteThug.chanceHigh)
            assertEquals(5, StandardPickpockets.menaphiteThug.damage)
            assertEquals(8, StandardPickpockets.menaphiteThug.pickpocketBlockCycles)
            assertEquals(9, StandardPickpockets.menaphiteThug.movementBlockCycles)
            assertEquals(
                ThievingObjs.menaphitePouch,
                StandardPickpockets.menaphiteThug.rewardAt(0).obj,
            )

            val pouch = cacheTypes.objs[ThievingObjs.menaphitePouch]
            assertEquals(22534, pouch.id)
            assertEquals("Coin pouch", pouch.name)
            assertTrue(pouch.isStackable)
            assertFalse(pouch.tradeable)
            assertEquals("Open-all", pouch.iop[0])
            assertEquals("Open", pouch.iop[1])
            assertEquals(60..60, CoinPouchDefinitions.menaphite.coins)
            assertEquals(209, StandardPickpockets.allNpcs.size)
        }

    @Test
    fun GameTestState.`paladin family bundle and pouch match cache 233`() = runBasicGameTest {
        val expected = setOf(3293, 3294, 8853, 11901, 11930, 11931, 11932, 11933)
        assertEquals(expected, StandardPickpockets.paladin.npcs.mapTo(hashSetOf()) { it.id })
        for (npc in StandardPickpockets.paladin.npcs) {
            assertEquals("Pickpocket", cacheTypes.npcs[npc].op[2], "npc=${npc.id}")
        }
        assertEquals(70, StandardPickpockets.paladin.level)
        assertEquals(131.8, StandardPickpockets.paladin.xp)
        assertEquals(40, StandardPickpockets.paladin.chanceLow)
        assertEquals(170, StandardPickpockets.paladin.chanceHigh)
        assertEquals(3, StandardPickpockets.paladin.damage)
        assertTrue(StandardPickpockets.paladin.mediumArdougneDiaryApplies)
        assertEquals(ThievingObjs.paladinPouch, StandardPickpockets.paladin.rewardAt(0).obj)
        assertEquals(1, StandardPickpockets.paladin.guaranteedRewards.size)
        assertEquals(ThievingObjs.chaosRune, StandardPickpockets.paladin.guaranteedRewards[0].obj)
        assertEquals(2, StandardPickpockets.paladin.guaranteedRewards[0].count)
        assertEquals(562, ThievingObjs.chaosRune.id)

        val pouch = cacheTypes.objs[ThievingObjs.paladinPouch]
        assertEquals(22535, pouch.id)
        assertEquals("Coin pouch", pouch.name)
        assertTrue(pouch.isStackable)
        assertFalse(pouch.tradeable)
        assertEquals("Open-all", pouch.iop[0])
        assertEquals("Open", pouch.iop[1])
        assertEquals(80..80, CoinPouchDefinitions.paladin.coins)
    }

    @Test
    fun GameTestState.`desert bandit family table and pouch match cache 233`() = runBasicGameTest {
        assertEquals(
            setOf(690, 695),
            StandardPickpockets.desertBandit.npcs.mapTo(hashSetOf()) { it.id },
        )
        for (npc in StandardPickpockets.desertBandit.npcs) {
            assertEquals("Pickpocket", cacheTypes.npcs[npc].op[2], "npc=${npc.id}")
        }
        assertEquals(53, StandardPickpockets.desertBandit.level)
        assertEquals(79.4, StandardPickpockets.desertBandit.xp)
        assertEquals(50, StandardPickpockets.desertBandit.chanceLow)
        assertEquals(240, StandardPickpockets.desertBandit.chanceHigh)
        assertEquals(3, StandardPickpockets.desertBandit.damage)
        assertFalse(StandardPickpockets.desertBandit.mediumArdougneDiaryApplies)
        assertEquals(7, StandardPickpockets.desertBandit.totalRewardWeight)
        assertEquals(
            ThievingObjs.desertBanditPouch,
            StandardPickpockets.desertBandit.rewardAt(0).obj,
        )
        assertEquals(
            ThievingObjs.desertBanditPouch,
            StandardPickpockets.desertBandit.rewardAt(4).obj,
        )
        assertEquals(ThievingObjs.antipoison1, StandardPickpockets.desertBandit.rewardAt(5).obj)
        assertEquals(ThievingObjs.lockpick, StandardPickpockets.desertBandit.rewardAt(6).obj)
        assertEquals(179, ThievingObjs.antipoison1.id)
        assertEquals(1523, ThievingObjs.lockpick.id)

        val pouch = cacheTypes.objs[ThievingObjs.desertBanditPouch]
        assertEquals(22530, pouch.id)
        assertEquals("Coin pouch", pouch.name)
        assertTrue(pouch.isStackable)
        assertFalse(pouch.tradeable)
        assertEquals("Open-all", pouch.iop[0])
        assertEquals("Open", pouch.iop[1])
        assertEquals(30..30, CoinPouchDefinitions.desertBandit.coins)
    }

    @Test
    fun GameTestState.`hero family weighted table and pouch match cache 233`() = runBasicGameTest {
        assertEquals(
            setOf(3295, 11934, 11935),
            StandardPickpockets.hero.npcs.mapTo(hashSetOf()) { it.id },
        )
        for (npc in StandardPickpockets.hero.npcs) {
            assertEquals("Pickpocket", cacheTypes.npcs[npc].op[2], "npc=${npc.id}")
        }
        assertEquals(80, StandardPickpockets.hero.level)
        assertEquals(163.3, StandardPickpockets.hero.xp)
        assertEquals(39, StandardPickpockets.hero.chanceLow)
        assertEquals(160, StandardPickpockets.hero.chanceHigh)
        assertEquals(3, StandardPickpockets.hero.damage)
        assertEquals(10, StandardPickpockets.hero.pickpocketBlockCycles)
        assertEquals(11, StandardPickpockets.hero.movementBlockCycles)
        assertTrue(StandardPickpockets.hero.mediumArdougneDiaryApplies)
        assertEquals(128, StandardPickpockets.hero.totalRewardWeight)
        assertEquals(ThievingObjs.heroPouch, StandardPickpockets.hero.rewardAt(0).obj)
        assertEquals(ThievingObjs.heroPouch, StandardPickpockets.hero.rewardAt(104).obj)
        assertEquals(ThievingObjs.deathRune, StandardPickpockets.hero.rewardAt(105).obj)
        assertEquals(2, StandardPickpockets.hero.rewardAt(112).count)
        assertEquals(ThievingObjs.jugWine, StandardPickpockets.hero.rewardAt(113).obj)
        assertEquals(ThievingObjs.bloodRune, StandardPickpockets.hero.rewardAt(119).obj)
        assertEquals(ThievingObjs.diamond, StandardPickpockets.hero.rewardAt(124).obj)
        assertEquals(ThievingObjs.fireOrb, StandardPickpockets.hero.rewardAt(125).obj)
        assertEquals(ThievingObjs.goldOre, StandardPickpockets.hero.rewardAt(127).obj)
        assertEquals(560, ThievingObjs.deathRune.id)
        assertEquals(565, ThievingObjs.bloodRune.id)
        assertEquals(1601, ThievingObjs.diamond.id)
        assertEquals(569, ThievingObjs.fireOrb.id)
        assertEquals(444, ThievingObjs.goldOre.id)

        val pouch = cacheTypes.objs[ThievingObjs.heroPouch]
        assertEquals(22537, pouch.id)
        assertEquals("Coin pouch", pouch.name)
        assertTrue(pouch.isStackable)
        assertFalse(pouch.tradeable)
        assertEquals("Open-all", pouch.iop[0])
        assertEquals("Open", pouch.iop[1])
        assertEquals(200..300, CoinPouchDefinitions.hero.coins)
        assertEquals(200..200, CoinPouchDefinitions.hero.rogueBonusCoins)
    }

    @Test
    fun GameTestState.`gnome family weighted table and pouch match cache 233`() = runBasicGameTest {
        assertEquals(
            setOf(6094, 6095, 6096),
            StandardPickpockets.gnome.npcs.mapTo(hashSetOf()) { it.id },
        )
        for (npc in StandardPickpockets.gnome.npcs) {
            assertEquals("Pickpocket", cacheTypes.npcs[npc].op[2], "npc=${npc.id}")
        }
        val battlefieldCombatIds =
            listOf(
                ThievingNpcs.battlefieldGnome,
                ThievingNpcs.battlefieldDarkGnomeArcher,
                ThievingNpcs.battlefieldGnomeArcher,
            )
        assertEquals(setOf(5968, 5969, 5970), battlefieldCombatIds.mapTo(hashSetOf()) { it.id })
        for (npc in battlefieldCombatIds) {
            assertFalse("Pickpocket" in cacheTypes.npcs[npc].op, "npc=${npc.id}")
        }
        assertEquals(75, StandardPickpockets.gnome.level)
        assertEquals(133.5, StandardPickpockets.gnome.xp)
        assertEquals(43, StandardPickpockets.gnome.chanceLow)
        assertEquals(175, StandardPickpockets.gnome.chanceHigh)
        assertEquals(1, StandardPickpockets.gnome.damage)
        assertEquals(8, StandardPickpockets.gnome.pickpocketBlockCycles)
        assertEquals(9, StandardPickpockets.gnome.movementBlockCycles)
        assertFalse(StandardPickpockets.gnome.mediumArdougneDiaryApplies)
        assertEquals(128, StandardPickpockets.gnome.totalRewardWeight)
        assertEquals(ThievingObjs.arrowShaft, StandardPickpockets.gnome.rewardAt(0).obj)
        assertEquals(2..4, StandardPickpockets.gnome.rewardAt(55).countRange)
        assertEquals(ThievingObjs.gnomePouch, StandardPickpockets.gnome.rewardAt(56).obj)
        assertEquals(ThievingObjs.gnomePouch, StandardPickpockets.gnome.rewardAt(85).obj)
        assertEquals(ThievingObjs.swampToad, StandardPickpockets.gnome.rewardAt(86).obj)
        assertEquals(ThievingObjs.goldOre, StandardPickpockets.gnome.rewardAt(110).obj)
        assertEquals(ThievingObjs.earthRune, StandardPickpockets.gnome.rewardAt(118).obj)
        assertEquals(ThievingObjs.kingWorm, StandardPickpockets.gnome.rewardAt(123).obj)
        assertEquals(ThievingObjs.fireOrb, StandardPickpockets.gnome.rewardAt(126).obj)
        assertEquals(ThievingObjs.fireOrb, StandardPickpockets.gnome.rewardAt(127).obj)
        assertEquals(52, ThievingObjs.arrowShaft.id)
        assertEquals(2150, ThievingObjs.swampToad.id)
        assertEquals(557, ThievingObjs.earthRune.id)
        assertEquals(2162, ThievingObjs.kingWorm.id)

        val pouch = cacheTypes.objs[ThievingObjs.gnomePouch]
        assertEquals(22536, pouch.id)
        assertEquals("Coin pouch", pouch.name)
        assertTrue(pouch.isStackable)
        assertFalse(pouch.tradeable)
        assertEquals("Open-all", pouch.iop[0])
        assertEquals("Open", pouch.iop[1])
        assertEquals(300..300, CoinPouchDefinitions.gnome.coins)
    }

    @Test
    fun GameTestState.`Lletya elf family and cache exclusions match cache 233`() =
        runBasicGameTest {
            val expected = setOf(5297, 5299, 5300, 9015)
            assertEquals(expected, StandardPickpockets.lletyaElf.npcs.mapTo(hashSetOf()) { it.id })
            for (npc in StandardPickpockets.lletyaElf.npcs) {
                assertEquals("Pickpocket", cacheTypes.npcs[npc].op[2], "npc=${npc.id}")
            }
            val cacheExclusions =
                listOf(
                    ThievingNpcs.kelyn,
                    ThievingNpcs.mawrthSongOfTheElves,
                    ThievingNpcs.mawrthCutscene,
                )
            assertEquals(setOf(8914, 8927, 8928), cacheExclusions.mapTo(hashSetOf()) { it.id })
            for (npc in cacheExclusions) {
                assertFalse("Pickpocket" in cacheTypes.npcs[npc].op, "npc=${npc.id}")
            }

            val definition = StandardPickpockets.lletyaElf
            assertEquals(85, definition.level)
            assertEquals(353.3, definition.xp)
            assertEquals(6, definition.chanceLow)
            assertEquals(100, definition.chanceHigh)
            assertEquals(5, definition.damage)
            assertEquals(10, definition.pickpocketBlockCycles)
            assertEquals(11, definition.movementBlockCycles)
            assertFalse(definition.mediumArdougneDiaryApplies)
            assertEquals(128, definition.totalRewardWeight)
            assertEquals(ThievingObjs.elfPouch, definition.rewardAt(0).obj)
            assertEquals(ThievingObjs.elfPouch, definition.rewardAt(104).obj)
            assertEquals(ThievingObjs.deathRune, definition.rewardAt(105).obj)
            assertEquals(2, definition.rewardAt(112).count)
            assertEquals(ThievingObjs.natureRune, definition.rewardAt(113).obj)
            assertEquals(3, definition.rewardAt(117).count)
            assertEquals(ThievingObjs.jugWine, definition.rewardAt(118).obj)
            assertEquals(ThievingObjs.diamond, definition.rewardAt(124).obj)
            assertEquals(ThievingObjs.fireOrb, definition.rewardAt(125).obj)
            assertEquals(ThievingObjs.goldOre, definition.rewardAt(127).obj)
            assertEquals(561, ThievingObjs.natureRune.id)

            val pouch = cacheTypes.objs[ThievingObjs.elfPouch]
            assertEquals(22538, pouch.id)
            assertEquals("Coin pouch", pouch.name)
            assertTrue(pouch.isStackable)
            assertFalse(pouch.tradeable)
            assertEquals("Open-all", pouch.iop[0])
            assertEquals("Open", pouch.iop[1])
            assertEquals(280..350, CoinPouchDefinitions.elf.coins)
            assertEquals(280..350, CoinPouchDefinitions.elf.rogueBonusCoins)
        }

    @Test
    fun GameTestState.`Prifddinas elf source boundary matches cache 233`() = runBasicGameTest {
        val expected = (9054..9090).toSet() + (9106..9117).toSet()
        assertEquals(49, expected.size)
        assertEquals(expected, ThievingNpcs.prifddinasElves.mapTo(hashSetOf()) { it.id })
        for (npc in ThievingNpcs.prifddinasElves) {
            val type = cacheTypes.npcs[npc]
            assertEquals(
                "Pickpocket",
                type.op[2],
                "npc=${npc.id}, ops=${type.op.contentToString()}",
            )
            assertFalse("Pickpocket" in type.op.take(2) + type.op.drop(3), "npc=${npc.id}")
        }

        val excluded = setOf(9053, 9101, 9102, 9103, 9104, 9105)
        assertEquals(excluded, ThievingNpcs.prifddinasElfExclusions.mapTo(hashSetOf()) { it.id })
        for (npc in ThievingNpcs.prifddinasElfExclusions) {
            assertFalse("Pickpocket" in cacheTypes.npcs[npc].op, "npc=${npc.id}")
        }

        val shard = cacheTypes.objs[ThievingObjs.crystalShard]
        assertEquals(23962, shard.id)
        assertEquals("Crystal shard", shard.name)
        val seed = cacheTypes.objs[ThievingObjs.enhancedCrystalTeleportSeed]
        assertEquals(23959, seed.id)
        assertEquals("Enhanced crystal teleport seed", seed.name)

        assertTrue(ThievingNpcs.prifddinasElves.none { it in StandardPickpockets.allNpcs })
        assertEquals(209, StandardPickpockets.allNpcs.size)
    }

    @Test
    fun GameTestState.`Vyre family weighted table and pouch match cache 233`() = runBasicGameTest {
        val expected = (9685..9714).toSet()
        assertEquals(expected, StandardPickpockets.vyre.npcs.mapTo(hashSetOf()) { it.id })
        for (npc in StandardPickpockets.vyre.npcs) {
            assertEquals("Pickpocket", cacheTypes.npcs[npc].op[2], "npc=${npc.id}")
        }

        val definition = StandardPickpockets.vyre
        assertEquals(82, definition.level)
        assertEquals(306.9, definition.xp)
        assertEquals(8, definition.chanceLow)
        assertEquals(128, definition.chanceHigh)
        assertEquals(5, definition.damage)
        assertEquals(10, definition.pickpocketBlockCycles)
        assertEquals(11, definition.movementBlockCycles)
        assertFalse(definition.mediumArdougneDiaryApplies)
        assertEquals(132, definition.totalRewardWeight)
        assertEquals(ThievingObjs.vyrePouch, definition.rewardAt(0).obj)
        assertEquals(ThievingObjs.vyrePouch, definition.rewardAt(108).obj)
        assertEquals(ThievingObjs.deathRune, definition.rewardAt(109).obj)
        assertEquals(2, definition.rewardAt(116).count)
        assertEquals(ThievingObjs.bloodRune, definition.rewardAt(117).obj)
        assertEquals(4, definition.rewardAt(118).count)
        assertEquals(ThievingObjs.bloodPint, definition.rewardAt(119).obj)
        assertEquals(ThievingObjs.uncutRuby, definition.rewardAt(125).obj)
        assertEquals(ThievingObjs.diamond, definition.rewardAt(130).obj)
        assertEquals(ThievingObjs.cookedMysteryMeat, definition.rewardAt(131).obj)
        assertEquals(24774, ThievingObjs.bloodPint.id)
        assertEquals(1619, ThievingObjs.uncutRuby.id)
        assertEquals(24785, ThievingObjs.cookedMysteryMeat.id)

        val pouch = cacheTypes.objs[ThievingObjs.vyrePouch]
        assertEquals(24703, pouch.id)
        assertEquals("Coin pouch", pouch.name)
        assertTrue(pouch.isStackable)
        assertFalse(pouch.tradeable)
        assertEquals("Open-all", pouch.iop[0])
        assertEquals("Open", pouch.iop[1])
        assertEquals(230..315, CoinPouchDefinitions.vyre.coins)
        assertEquals(230..230, CoinPouchDefinitions.vyre.rogueBonusCoins)
    }

    @Test
    fun GameTestState.`citizen pouch and feedback assets match cache 233`() = runBasicGameTest {
        val pouch = cacheTypes.objs[ThievingObjs.citizenPouch]
        assertEquals(22521, pouch.id)
        assertEquals("Coin pouch", pouch.name)
        assertTrue(pouch.isStackable)
        assertFalse(pouch.tradeable)
        assertEquals("Open-all", pouch.iop[0])
        assertEquals("Open", pouch.iop[1])
        assertEquals(881, seqs.human_pickpocket.id)
        assertEquals(1054, seqs.stunned_thieving.id)
        assertEquals(245, spotanims.stunned_thieving.id)
        assertEquals(2114, synths.coins_jingle_1.id)
        assertEquals(4398, synths.thieving.id)
        assertEquals(4458, varbits.ardougne_diary_easy_complete.id)
        assertEquals(4459, varbits.ardougne_diary_medium_complete.id)
        assertEquals(4460, varbits.ardougne_diary_hard_complete.id)
        assertEquals(4461, varbits.ardougne_diary_elite_complete.id)
    }
}
