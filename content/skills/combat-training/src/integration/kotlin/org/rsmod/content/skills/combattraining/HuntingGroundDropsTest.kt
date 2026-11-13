package org.rsmod.content.skills.combattraining

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.combat.commons.npc.slayerFamily
import org.rsmod.api.config.refs.*
import org.rsmod.api.death.*
import org.rsmod.api.death.plugin.NpcDeathScript
import org.rsmod.api.hit.plugin.NpcHitScript
import org.rsmod.api.player.diary.*
import org.rsmod.api.random.CoreRandom
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.random.FixedRandom
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.entity.Npc
import org.rsmod.game.hit.HitType
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class HuntingGroundDropsTest {
    @Test
    fun GameTestState
        .`every native hunting ground family has one full main table and real item identities`() =
        game { d ->
            val data = HuntingGroundDropData.rows
            assertEquals(227, data.size)
            for (f in
                listOf(
                    "lizard",
                    "dust",
                    "locust",
                    "mithril",
                    "green",
                    "lava",
                    "mogre",
                    "rare",
                )) assertEquals(128, data.filter { it.family == f }.sumOf { it.weight }, f)
            assertEquals(1008, data.filter { it.family == "general_seed" }.sumOf { it.weight })
            assertEquals(1090, data.filter { it.family == "rare_seed" }.sumOf { it.weight })
            assertEquals(1048, data.filter { it.family == "uncommon_seed" }.sumOf { it.weight })
            for (id in
                listOf(
                    458,
                    459,
                    460,
                    461,
                    462,
                    463,
                    423,
                    795,
                    796,
                    2919,
                    260,
                    261,
                    262,
                    263,
                    264,
                    6593,
                    2592,
                )) assertNotNull(d.tables[cacheTypes.npcs.getValue(id)], "$id")
            for (row in data.filter { it.branch == "item" }) {
                assertTrue(row.obj > 0)
                val t = objTypes.getValue(row.obj)
                assertNotEquals(
                    "null",
                    if (t.isCert) objTypes.getValue(t.certlink).name else t.name,
                    "${row.obj}",
                )
                assertTrue(row.minimum > 0 && row.maximum >= row.minimum)
            }
        }

    @Test
    fun GameTestState
        .`mithril death physically delivers three bars and the selected chewed bone result`() =
        game { d ->
            player.members = true
            player.observerUUID = player.uuid
            val c = CoordGrid(1743, 5332)
            player.placeAt(c.translate(-1, 0))
            val npc = spawnNpc(c, npcTypes.getValue(2919)) { respawns = true }
            assertTrue(npc.hitpoints > 0, "Mithril native HP=${npc.hitpoints}")
            (d.deathRandom as FixedRandom).set(offset("mithril") { it.obj == 11338 })
            npc.queueHit(player, 1, HitType.Melee, npc.hitpoints)
            advance(15)
            val loot = d.ground.findAll(c).toList()
            assertEquals(
                3,
                loot.filter { it.type == 2359 }.sumOf { it.count },
                "hp=${npc.hitpoints} visible=${npc.isVisible} heroes=${npc.heroPoints.toMutableList()} queues=${npc.queueList} loot=$loot",
            )
            assertEquals(1, loot.count { it.type == 11338 })
            assertEquals(1, loot.count { it.type == 536 })
        }

    @Test
    fun GameTestState.`lava death physically delivers exactly one bone hide and scale`() =
        game { d ->
            player.members = true
            player.observerUUID = player.uuid
            val c = CoordGrid(3192, 3831)
            player.placeAt(c.translate(-1, 0))
            val npc = spawnNpc(c, npcTypes.getValue(6593)) { respawns = true }
            assertTrue(npc.hitpoints > 0, "Lava native HP=${npc.hitpoints}")
            (d.deathRandom as FixedRandom).set(1)
            npc.queueHit(player, 1, HitType.Melee, npc.hitpoints)
            advance(15)
            val loot = d.ground.findAll(c).toList()
            for (id in listOf(11943, 1747, 11992)) assertEquals(
                1,
                loot.filter { it.type == id }.sumOf { it.count },
                "$id",
            )
        }

    @Test
    fun GameTestState
        .`green death physically delivers its own hide and dragon bones without duplicate remains`() =
        game { d ->
            player.members = true
            player.observerUUID = player.uuid
            val c = CoordGrid(2980, 3616)
            player.placeAt(c.translate(-1, 0))
            val npc = spawnNpc(c, npcTypes.getValue(260)) { respawns = true }
            assertTrue(npc.hitpoints > 0, "Green native HP=${npc.hitpoints}")
            (d.deathRandom as FixedRandom).set(1)
            npc.queueHit(player, 1, HitType.Melee, npc.hitpoints)
            advance(15)
            val loot = d.ground.findAll(c).toList()
            for (id in listOf(536, 1753)) assertEquals(
                1,
                loot.filter { it.type == id }.sumOf { it.count },
                "$id",
            )
            assertTrue(loot.none { it.type == 1747 || it.type == 11943 })
        }

    @Test
    fun GameTestState.`green bone notes require elite completion and an actual Wilderness kill`() =
        game { d ->
            player.members = true
            val wild = creature(260, CoordGrid(2980, 3616))
            val ordinary = creature(261, CoordGrid(2800, 3300))
            val raw = 536
            val note = objTypes.getValue(raw).certlink
            fun roll(n: Npc): List<NpcDrop> {
                (d.deathRandom as FixedRandom).set(1)
                return d.tables[n.visType]!!.roll(
                    NpcDropContext(n, player, n.coords, d.deathRandom)
                )
            }
            assertTrue(roll(wild).any { it.obj.id == raw })
            assertTrue(roll(wild).none { it.obj.id == note })
            player.setVarBit(DiaryRegion.Wilderness.tier(DiaryDifficulty.Elite).complete, 1)
            assertTrue(roll(wild).any { it.obj.id == note })
            assertTrue(roll(wild).none { it.obj.id == raw })
            assertTrue(roll(ordinary).any { it.obj.id == raw })
            assertTrue(roll(ordinary).none { it.obj.id == note })
        }

    @Test
    fun GameTestState.`lava notes require elite and the saved taskmaster toggle independently`() =
        game { d ->
            player.members = true
            val npc = creature(6593, CoordGrid(3192, 3831))
            val raw = 11943
            val note = objTypes.getValue(raw).certlink
            fun roll(): List<NpcDrop> {
                (d.deathRandom as FixedRandom).set(1)
                return d.tables[npc.visType]!!.roll(
                    NpcDropContext(npc, player, npc.coords, d.deathRandom)
                )
            }
            player.setVarBit(HuntingDropBits.lavaNotes, 1)
            assertTrue(roll().any { it.obj.id == raw })
            player.setVarBit(DiaryRegion.Wilderness.tier(DiaryDifficulty.Elite).complete, 1)
            player.setVarBit(HuntingDropBits.lavaNotes, 0)
            assertTrue(roll().any { it.obj.id == raw })
            player.setVarBit(HuntingDropBits.lavaNotes, 1)
            assertTrue(roll().any { it.obj.id == note })
            assertTrue(roll().none { it.obj.id == raw })
        }

    @Test
    fun GameTestState
        .`Duly noted mithril bars require the unlock and a real active assigned task`() =
        game { d ->
            player.members = true
            val npc = creature(2919, CoordGrid(1743, 5332))
            val noted = objTypes.getValue(2359).certlink
            fun roll(): List<NpcDrop> {
                (d.deathRandom as FixedRandom).set(1)
                return d.tables[npc.visType]!!.roll(
                    NpcDropContext(npc, player, npc.coords, d.deathRandom)
                )
            }
            player.setVarBit(HuntingDropBits.mithrilNotes, 1)
            assertTrue(roll().any { it.obj.id == 2359 && it.obj.count == 3 })
            assertTrue(npc.visType.slayerFamily > 0)
            player.setVarp(varps.slayer_target, npc.visType.slayerFamily)
            player.setVarp(varps.slayer_count, 1)
            assertTrue(roll().any { it.obj.id == noted && it.obj.count == 3 })
            assertTrue(roll().none { it.obj.id == 2359 })
            player.setVarBit(HuntingDropBits.mithrilNotes, 0)
            assertTrue(roll().any { it.obj.id == 2359 })
        }

    @Test
    fun GameTestState.`lizard herb roll includes its forty two fire rune companion`() = game { d ->
        player.members = true
        val npc = creature(462)
        random.next = 1
        random.then = offset("lizard") { it.branch == "herb_fire" }
        random.then = 0
        val loot = d.tables[npc.visType]!!.roll(NpcDropContext(npc, player, npc.coords, d.rng))
        assertTrue(loot.any { it.obj.id == 199 })
        assertEquals(42, loot.single { it.obj.id == 554 }.obj.count)
        assertEquals(1, loot.count { it.obj.id == 526 })
    }

    @Test
    fun GameTestState.`lizard seed roll gives four potato seeds plus both empty waterskins`() =
        game { d ->
            player.members = true
            val npc = creature(459)
            random.next = 1
            random.then = offset("lizard") { it.branch == "seed_water" }
            random.then = 0
            val loot = d.tables[npc.visType]!!.roll(NpcDropContext(npc, player, npc.coords, d.rng))
            assertEquals(4, loot.single { it.obj.id == 5318 }.obj.count)
            assertEquals(2, loot.single { it.obj.id == 1831 }.obj.count)
        }

    @Test
    fun GameTestState.`lizard double gem cells perform two independent gem rolls`() = game { d ->
        player.members = true
        val npc = creature(463)
        random.next = 1
        random.then = offset("lizard") { it.branch == "doublegem" }
        random.then = 63
        random.then = 63
        val loot = d.tables[npc.visType]!!.roll(NpcDropContext(npc, player, npc.coords, d.rng))
        assertEquals(2, loot.count { it.obj.id == 1623 })
    }

    @Test
    fun GameTestState
        .`light mystic gloves preroll preserves guaranteed bones and suppresses the main table`() =
        game { d ->
            val npc = creature(462)
            random.next = 0
            val loot = d.tables[npc.visType]!!.roll(NpcDropContext(npc, player, npc.coords, d.rng))
            assertEquals(setOf(526, 4115), loot.map { it.obj.id }.toSet())
        }

    @Test
    fun GameTestState.`dust devil chainbody is the rare outcome of its one main cell`() =
        game { d ->
            val npc = creature(423)
            random.next = 1
            random.then = offset("dust") { it.branch == "dust_chain" }
            random.then = 0
            val loot = d.tables[npc.visType]!!.roll(NpcDropContext(npc, player, npc.coords, d.rng))
            assertTrue(loot.any { it.obj.id == 3140 })
            assertEquals(1, loot.count { it.obj.id == 526 })
        }

    @Test
    fun GameTestState
        .`Mogre main rolls make native flippers and the water filled fishbowl obtainable`() =
        game { d ->
            player.members = true
            val npc = creature(2592)
            for (id in listOf(6666, 6668)) {
                random.next = offset("mogre") { it.obj == id }
                random.then = 1
                random.then = 1
                val loot =
                    d.tables[npc.visType]!!.roll(NpcDropContext(npc, player, npc.coords, d.rng))
                assertTrue(loot.any { it.obj.id == id }, "$id")
            }
        }

    private fun offset(family: String, match: (HuntingDropRow) -> Boolean) =
        HuntingGroundDropData.rows
            .filter { it.family == family }
            .takeWhile { !match(it) }
            .sumOf { it.weight }

    private fun GameTestScope.creature(id: Int, c: CoordGrid = player.coords.translate(1, 0)) =
        spawnNpc(c, npcTypes.getValue(id))

    private fun GameTestState.game(body: GameTestScope.(Deps) -> Unit) =
        runInjectedGameTest(
            Deps::class,
            null,
            HuntingGroundDrops::class,
            NpcHitScript::class,
            NpcDeathScript::class,
            testBody = body,
        )

    data class Deps
    @Inject
    constructor(
        val tables: NpcDropTableRegistry,
        val ground: ObjRepository,
        val rng: GameRandom,
        @CoreRandom val deathRandom: GameRandom,
    )
}
