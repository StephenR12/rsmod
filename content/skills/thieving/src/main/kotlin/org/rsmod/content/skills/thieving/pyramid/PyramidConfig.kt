package org.rsmod.content.skills.thieving.pyramid

import org.rsmod.api.config.refs.categories
import org.rsmod.api.config.refs.params
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.api.type.refs.interf.InterfaceReferences
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.timer.TimerReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.game.type.obj.ObjType
import org.rsmod.map.CoordGrid

public object PyramidNpcs : NpcReferences() {
    val charmer = find("feud_snakecharmer_multi")
    val guardian = find("ntk_mummy_guardian")
    val swarm = find("ntk_scarab_swarm")
    val mummies = (1..5).map { find("ntk_mummy_" + it) }
}

public object PyramidLocs : LocReferences() {
    val entrances =
        listOf("north", "east", "south", "west").map { find("ntk_pyramid_door_" + it + "_multi") }
    val exit = find("ntk_tomb_door_exit")
    val lobbyExit = find("ntk_antechamber_exit")
    val urns =
        (1..15).map {
            val style = if (it <= 5) 1 else if (it <= 10) 2 else 3
            find("ntk_urn_type" + style + "_multi_" + it)
        }
    val doors = (1..4).map { find("ntk_tomb_door" + it) }
    val money = find("feud_money_bowl")
    val obelisk = find("necropolis_obelisk")
    val chest = find("ntk_golden_chest_multi")
    val sarcophagus = find("ntk_sarcophagus_multi")
    val spear = find("ntk_speartrap_inmotion")
}

public object PyramidBits : VarBitReferences() {
    val urns = (1..15).map { find("ntk_urn" + it + "_state") }
    val doors = (1..4).map { find("ntk_door" + it + "_state") }
    val chest = find("ntk_golden_chest_state")
    val sarcophagus = find("ntk_sarcophagus_state")
    val push = find("ntk_sarcophagus_push")
    val level = find("ntk_current_room_level")
    val required = find("ntk_thieving_required")
    val room = find("ntk_room_number")
    val trap = find("ntk_trap_active")
    val played = find("ntk_played_before")
    val rightDoor = find("ntk_right_door_opened")
    val timer = find("ntk_player_timer_count")
    val lastTeleport = find("pharaohs_sceptre_last_teleport")
    val necropolis = find("pharaohs_sceptre_necropolis")
}

public object PyramidObjs : ObjReferences() {
    val lockpick = find("lockpick")
    val hairclip = find("kr_hairclip")
    val charm = find("snake_flute")
    val basket = find("basket_for_snake")
    val comb = find("ntk_ivory_comb")
    val potteryScarab = find("ntk_scarab_pottery")
    val potteryStatue = find("ntk_statuette_pottery")
    val stoneSeal = find("ntk_seal_stone")
    val stoneScarab = find("ntk_scarab_stone")
    val stoneStatue = find("ntk_statuette_stone")
    val goldSeal = find("ntk_seal_gold")
    val goldScarab = find("ntk_scarab_gold")
    val goldStatue = find("ntk_statuette_gold")
    val sceptre = find("pharaohs_sceptre")
    val charged = find("pharaohs_sceptre_charged")
    val initial = find("pharaohs_sceptre_charged_initial")
}

public object PyramidSeqs : SeqReferences() {
    val search = find("ntk_tomb_search")
    val push = find("ntk_tomb_push_loop")
    val open = find("ntk_tomb_open")
    val teleport = find("ntk_human_tele")
    val mummyAttack = find("mummy_update_civillian_attack")
    val mummyDefend = find("mummy_update_defend")
    val mummyDeath = find("mummy_update_death")
    val swarm = find("scarab_attack")
    val swarmDefend = find("scarab_parry")
    val swarmDeath = find("scarab_spiral")
}

public object PyramidInterfaces : InterfaceReferences() {
    val overlay = find("ntk_overlay")
}

public object PyramidTimers : TimerReferences() {
    val round = find("pyramid_plunder_round")
}

internal object PyramidPeople : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<PyramidPlunder>("pyramid-plunder.toml")
    }
}

internal object PyramidNpcEditor : NpcEditor() {
    init {
        edit(PyramidNpcs.charmer) { defaultMode = NpcMode.None }
        edit(PyramidNpcs.guardian) { defaultMode = NpcMode.None }
        edit(PyramidNpcs.swarm) {
            defaultMode = NpcMode.None
            hitpoints = 25
            attack = 255
            strength = 5
            defence = 30
            magic = 1
            ranged = 1
            param[params.attackrate] = 1
            param[params.attack_anim] = PyramidSeqs.swarm
            param[params.defend_anim] = PyramidSeqs.swarmDefend
            param[params.death_anim] = PyramidSeqs.swarmDeath
            param[params.npc_attack_type] = categories.attacktype_slash
            param[params.defence_stab] = 90
            param[params.defence_slash] = 90
            param[params.defence_crush] = 5
            param[params.defence_magic] = 90
            param[params.defence_ranged] = 90
        }
        for (mummy in PyramidNpcs.mummies) edit(mummy) {
            defaultMode = NpcMode.None
            hitpoints = 90
            attack = 90
            strength = 30
            defence = 90
            magic = 1
            ranged = 1
            param[params.attackrate] = 4
            param[params.attack_anim] = PyramidSeqs.mummyAttack
            param[params.defend_anim] = PyramidSeqs.mummyDefend
            param[params.death_anim] = PyramidSeqs.mummyDeath
            param[params.npc_attack_type] = categories.attacktype_crush
            param[params.melee_strength] = 30
            param[params.defence_stab] = 90
            param[params.defence_slash] = 90
            param[params.defence_crush] = 30
            param[params.defence_magic] = 90
            param[params.defence_ranged] = 0
        }
    }
}

public data class PyramidRoom(
    public val entry: CoordGrid,
    public val pastTrap: CoordGrid,
    public val centre: CoordGrid,
)

public object PyramidRooms {
    public val all: List<PyramidRoom> =
        listOf(
            PyramidRoom(CoordGrid(1927, 4477), CoordGrid(1927, 4471), CoordGrid(1927, 4468)),
            PyramidRoom(CoordGrid(1954, 4477), CoordGrid(1954, 4472), CoordGrid(1953, 4468)),
            PyramidRoom(CoordGrid(1977, 4471), CoordGrid(1976, 4464), CoordGrid(1975, 4458)),
            PyramidRoom(CoordGrid(1928, 4452), CoordGrid(1932, 4452), CoordGrid(1936, 4452)),
            PyramidRoom(CoordGrid(1965, 4444), CoordGrid(1960, 4444), CoordGrid(1955, 4450)),
            PyramidRoom(CoordGrid(1926, 4424), CoordGrid(1926, 4429), CoordGrid(1926, 4434)),
            PyramidRoom(CoordGrid(1944, 4422), CoordGrid(1944, 4426), CoordGrid(1951, 4427)),
            PyramidRoom(CoordGrid(1974, 4421), CoordGrid(1974, 4425), CoordGrid(1972, 4431)),
        )
    public val urnXp: List<Double> = listOf(60.0, 90.0, 150.0, 215.0, 300.0, 450.0, 675.0, 825.0)
    public val chestXp: List<Double> = listOf(40.0, 60.0, 100.0, 140.0, 200.0, 300.0, 450.0, 550.0)
    public val strengthXp: List<Double> = listOf(20.0, 30.0, 50.0, 70.0, 100.0, 150.0, 225.0, 275.0)
    public val sceptreDenominator: List<Int> = listOf(4200, 2800, 1600, 950, 800, 750, 650, 650)
    public val lobby: CoordGrid = CoordGrid(1934, 4428, 3)
    public val outside: CoordGrid = CoordGrid(3289, 2801)
    public val emptyLobbies: List<CoordGrid> =
        listOf(CoordGrid(1934, 4450, 2), CoordGrid(1968, 4420, 2), CoordGrid(1968, 4450, 3))

    public fun inside(c: CoordGrid): Boolean = c.x in 1920..1983 && c.z in 4416..4479

    public fun nearest(c: CoordGrid): Int =
        all.indices.minBy {
            val t = all[it].centre
            (t.x - c.x) * (t.x - c.x) + (t.z - c.z) * (t.z - c.z)
        }

    // Native cache233.4 urn positions and per-player slot numbers.
    public val urns: Map<CoordGrid, Pair<Int, Int>> =
        mapOf(
            CoordGrid(1923, 4468) to (0 to 0),
            CoordGrid(1926, 4439) to (5 to 0),
            CoordGrid(1943, 4430) to (6 to 0),
            CoordGrid(1953, 4471) to (1 to 0),
            CoordGrid(1970, 4434) to (7 to 0),
            CoordGrid(1977, 4455) to (2 to 0),
            CoordGrid(1923, 4471) to (0 to 1),
            CoordGrid(1949, 4470) to (1 to 1),
            CoordGrid(1961, 4450) to (4 to 1),
            CoordGrid(1970, 4461) to (2 to 1),
            CoordGrid(1930, 4431) to (5 to 2),
            CoordGrid(1932, 4469) to (0 to 2),
            CoordGrid(1941, 4452) to (3 to 2),
            CoordGrid(1951, 4423) to (6 to 2),
            CoordGrid(1955, 4444) to (4 to 2),
            CoordGrid(1972, 4428) to (7 to 2),
            CoordGrid(1923, 4436) to (5 to 3),
            CoordGrid(1938, 4450) to (3 to 3),
            CoordGrid(1952, 4451) to (4 to 3),
            CoordGrid(1954, 4428) to (6 to 3),
            CoordGrid(1957, 4470) to (1 to 3),
            CoordGrid(1968, 4430) to (7 to 3),
            CoordGrid(1974, 4456) to (2 to 3),
            CoordGrid(1925, 4467) to (0 to 4),
            CoordGrid(1928, 4435) to (5 to 4),
            CoordGrid(1940, 4455) to (3 to 4),
            CoordGrid(1947, 4428) to (6 to 4),
            CoordGrid(1956, 4469) to (1 to 4),
            CoordGrid(1958, 4446) to (4 to 4),
            CoordGrid(1975, 4457) to (2 to 4),
            CoordGrid(1976, 4429) to (7 to 4),
            CoordGrid(1923, 4469) to (0 to 5),
            CoordGrid(1927, 4439) to (5 to 5),
            CoordGrid(1933, 4455) to (3 to 5),
            CoordGrid(1943, 4428) to (6 to 5),
            CoordGrid(1952, 4471) to (1 to 5),
            CoordGrid(1959, 4453) to (4 to 5),
            CoordGrid(1971, 4434) to (7 to 5),
            CoordGrid(1971, 4462) to (2 to 5),
            CoordGrid(1931, 4435) to (5 to 6),
            CoordGrid(1939, 4457) to (3 to 6),
            CoordGrid(1948, 4424) to (6 to 6),
            CoordGrid(1961, 4449) to (4 to 6),
            CoordGrid(1970, 4459) to (2 to 6),
            CoordGrid(1923, 4434) to (5 to 7),
            CoordGrid(1929, 4465) to (0 to 7),
            CoordGrid(1940, 4450) to (3 to 7),
            CoordGrid(1952, 4423) to (6 to 7),
            CoordGrid(1952, 4449) to (4 to 7),
            CoordGrid(1959, 4470) to (1 to 7),
            CoordGrid(1971, 4428) to (7 to 7),
            CoordGrid(1979, 4457) to (2 to 7),
            CoordGrid(1924, 4438) to (5 to 8),
            CoordGrid(1925, 4465) to (0 to 8),
            CoordGrid(1936, 4450) to (3 to 8),
            CoordGrid(1954, 4429) to (6 to 8),
            CoordGrid(1957, 4447) to (4 to 8),
            CoordGrid(1957, 4469) to (1 to 8),
            CoordGrid(1968, 4431) to (7 to 8),
            CoordGrid(1926, 4468) to (0 to 9),
            CoordGrid(1929, 4434) to (5 to 9),
            CoordGrid(1938, 4453) to (3 to 9),
            CoordGrid(1946, 4428) to (6 to 9),
            CoordGrid(1953, 4452) to (4 to 9),
            CoordGrid(1957, 4465) to (1 to 9),
            CoordGrid(1973, 4464) to (2 to 9),
            CoordGrid(1977, 4429) to (7 to 9),
            CoordGrid(1923, 4470) to (0 to 10),
            CoordGrid(1936, 4457) to (3 to 10),
            CoordGrid(1949, 4471) to (1 to 10),
            CoordGrid(1961, 4451) to (4 to 10),
            CoordGrid(1971, 4457) to (2 to 10),
            CoordGrid(1972, 4434) to (7 to 10),
            CoordGrid(1931, 4434) to (5 to 11),
            CoordGrid(1932, 4470) to (0 to 11),
            CoordGrid(1940, 4457) to (3 to 11),
            CoordGrid(1950, 4423) to (6 to 11),
            CoordGrid(1950, 4466) to (1 to 11),
            CoordGrid(1954, 4453) to (4 to 11),
            CoordGrid(1978, 4431) to (7 to 11),
            CoordGrid(1979, 4459) to (2 to 11),
            CoordGrid(1923, 4435) to (5 to 12),
            CoordGrid(1926, 4465) to (0 to 12),
            CoordGrid(1939, 4450) to (3 to 12),
            CoordGrid(1954, 4427) to (6 to 12),
            CoordGrid(1956, 4470) to (1 to 12),
            CoordGrid(1970, 4428) to (7 to 12),
            CoordGrid(1926, 4467) to (0 to 13),
            CoordGrid(1928, 4434) to (5 to 13),
            CoordGrid(1935, 4450) to (3 to 13),
            CoordGrid(1947, 4429) to (6 to 13),
            CoordGrid(1958, 4447) to (4 to 13),
            CoordGrid(1959, 4469) to (1 to 13),
            CoordGrid(1975, 4456) to (2 to 13),
            CoordGrid(1976, 4428) to (7 to 13),
            CoordGrid(1925, 4468) to (0 to 14),
            CoordGrid(1929, 4435) to (5 to 14),
            CoordGrid(1938, 4452) to (3 to 14),
            CoordGrid(1946, 4429) to (6 to 14),
            CoordGrid(1951, 4471) to (1 to 14),
            CoordGrid(1957, 4446) to (4 to 14),
            CoordGrid(1974, 4435) to (7 to 14),
            CoordGrid(1974, 4457) to (2 to 14),
        )
}

public object PyramidLoot {
    // Four equally likely columns reproduce published chest/sarcophagus loot fractions.
    public val chest: List<List<ObjType>> =
        listOf(
            listOf(
                PyramidObjs.stoneSeal,
                PyramidObjs.stoneScarab,
                PyramidObjs.stoneScarab,
                PyramidObjs.stoneStatue,
            ),
            listOf(
                PyramidObjs.stoneScarab,
                PyramidObjs.stoneScarab,
                PyramidObjs.stoneStatue,
                PyramidObjs.stoneStatue,
            ),
            listOf(
                PyramidObjs.stoneScarab,
                PyramidObjs.stoneStatue,
                PyramidObjs.stoneStatue,
                PyramidObjs.goldSeal,
            ),
            listOf(
                PyramidObjs.stoneStatue,
                PyramidObjs.stoneStatue,
                PyramidObjs.goldSeal,
                PyramidObjs.goldSeal,
            ),
            listOf(
                PyramidObjs.stoneStatue,
                PyramidObjs.goldSeal,
                PyramidObjs.goldSeal,
                PyramidObjs.goldScarab,
            ),
            listOf(
                PyramidObjs.goldSeal,
                PyramidObjs.goldSeal,
                PyramidObjs.goldScarab,
                PyramidObjs.goldScarab,
            ),
            listOf(
                PyramidObjs.goldSeal,
                PyramidObjs.goldScarab,
                PyramidObjs.goldScarab,
                PyramidObjs.goldStatue,
            ),
            listOf(
                PyramidObjs.goldScarab,
                PyramidObjs.goldScarab,
                PyramidObjs.goldStatue,
                PyramidObjs.goldStatue,
            ),
        )
    public val sarcophagus: List<List<ObjType>> =
        listOf(
            listOf(
                PyramidObjs.potteryScarab,
                PyramidObjs.potteryStatue,
                PyramidObjs.potteryStatue,
                PyramidObjs.stoneSeal,
            ),
            listOf(
                PyramidObjs.potteryStatue,
                PyramidObjs.potteryStatue,
                PyramidObjs.stoneSeal,
                PyramidObjs.stoneSeal,
            ),
            listOf(
                PyramidObjs.potteryStatue,
                PyramidObjs.stoneSeal,
                PyramidObjs.stoneSeal,
                PyramidObjs.stoneScarab,
            ),
            listOf(
                PyramidObjs.stoneSeal,
                PyramidObjs.stoneSeal,
                PyramidObjs.stoneScarab,
                PyramidObjs.stoneScarab,
            ),
            listOf(
                PyramidObjs.stoneSeal,
                PyramidObjs.stoneScarab,
                PyramidObjs.stoneScarab,
                PyramidObjs.stoneStatue,
            ),
            listOf(
                PyramidObjs.stoneScarab,
                PyramidObjs.stoneScarab,
                PyramidObjs.stoneStatue,
                PyramidObjs.stoneStatue,
            ),
            listOf(
                PyramidObjs.stoneScarab,
                PyramidObjs.stoneStatue,
                PyramidObjs.stoneStatue,
                PyramidObjs.goldSeal,
            ),
            listOf(
                PyramidObjs.stoneStatue,
                PyramidObjs.stoneStatue,
                PyramidObjs.goldSeal,
                PyramidObjs.goldSeal,
            ),
        )
    // Exact urn weights are unpublished: uniform eligible material is a playable approximation.
    public val urn: List<List<ObjType>> =
        (0..7).map { room ->
            buildList {
                if (room <= 1) add(PyramidObjs.comb)
                if (room == 0) add(PyramidObjs.potteryScarab)
                if (room <= 2) add(PyramidObjs.potteryStatue)
                if (room <= 4) add(PyramidObjs.stoneSeal)
                if (room <= 6) add(PyramidObjs.stoneScarab)
                add(PyramidObjs.stoneStatue)
                if (room >= 2) add(PyramidObjs.goldSeal)
                if (room >= 4) add(PyramidObjs.goldScarab)
                if (room >= 6) add(PyramidObjs.goldStatue)
            }
        }
}
