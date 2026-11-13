package org.rsmod.content.skills.construction

import jakarta.inject.Singleton
import java.util.WeakHashMap
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

/** Door angles are the mapped west, north, east and south wall angles of the room template. */
public enum class HouseRoomType(
    public val label: String,
    public val level: Int,
    public val fee: Int,
    public val source: CoordGrid,
    public val hotspotPrefix: String,
    private val doors: Set<Int>,
) {
    Garden("Garden", 1, 1000, CoordGrid(1856, 5704), "crude_garden", setOf(0, 1, 2, 3)),
    Parlour("Parlour", 1, 1000, CoordGrid(1856, 5752), "parlour", setOf(0, 2, 3)),
    Kitchen("Kitchen", 5, 5000, CoordGrid(1872, 5752), "kitchen", setOf(0, 3)),
    DiningRoom("Dining room", 10, 5000, CoordGrid(1888, 5752), "dining_room", setOf(0, 2, 3)),
    Workshop("Workshop", 15, 10000, CoordGrid(1856, 5736), "workshop", setOf(1, 3)),
    Bedroom("Bedroom", 20, 10000, CoordGrid(1904, 5752), "bedroom", setOf(0, 3)),
    Study("Study", 40, 50000, CoordGrid(1888, 5736), "study", setOf(0, 2, 3)),
    Chapel("Chapel", 45, 50000, CoordGrid(1872, 5736), "chapel", setOf(0, 3)),
    SuperiorGarden(
        "Superior garden",
        65,
        75000,
        CoordGrid(1896, 5696),
        "superior_garden_hotspot",
        setOf(0, 1, 2, 3),
    ),
    SkillHall("Skill Hall", 25, 15000, CoordGrid(1864, 5744), "hall1", setOf(0, 1, 2, 3)),
    QuestHall("Quest Hall", 35, 25000, CoordGrid(1896, 5744), "hall2", setOf(0, 1, 2, 3)),
    Menagerie("Menagerie", 37, 30000, CoordGrid(1912, 5696), "menagerie", setOf(0, 1, 2, 3)),
    PortalChamber("Portal chamber", 50, 100000, CoordGrid(1864, 5728), "teleroom", setOf(3)),
    CostumeRoom("Costume room", 42, 50000, CoordGrid(1904, 5704), "cos_room", setOf(3)),
    IndoorMenagerie("Indoor menagerie",37,30000,CoordGrid(1912,5712),"menagerie",setOf(0,1,2,3)),
    DungeonStairs("Dungeon stairs",70,7500,CoordGrid(1872,5720),"dungeon_stairs",setOf(0,1,2,3)),
    DungeonJunction("Dungeon junction",70,7500,CoordGrid(1856,5720),"dungeon",setOf(0,1,2,3)),
    DungeonCorridor("Dungeon corridor",70,7500,CoordGrid(1888,5720),"dungeon",setOf(1,3)),
    FormalGarden("Formal garden",55,75000,CoordGrid(1872,5704),"posh_garden",setOf(0,1,2,3)),
    CombatRoom("Combat room",32,25000,CoordGrid(1880,5728),"combat_room",setOf(0,2,3)),
    ThroneRoom("Throne room",60,150000,CoordGrid(1904,5736),"throne_room",setOf(3));

    internal val isMenagerie:Boolean get()=this==Menagerie || this==IndoorMenagerie

    internal val outdoors get() = this == Garden || this == FormalGarden || this == SuperiorGarden || this == Menagerie
    internal val stairHall get() = this == SkillHall || this == QuestHall || this == DungeonStairs

    internal fun permits(floor:Int):Boolean = when(this){
        DungeonStairs,DungeonCorridor,DungeonJunction -> floor == -1
        else -> floor in -1..1 && (!outdoors || floor == 0)
    }

    public fun hasDoor(angle: Int, rotation: Int): Boolean = ((angle - rotation) and 3) in doors
}

public data class HouseRoom(
    val cell: Int,
    val type: HouseRoomType,
    val rotation: Int = 0,
    val furniture: Long = 0,
    val petRocks: Long = 0,
) {
    public val x: Int
        get() = cell and 15

    public val z: Int
        get() = (cell shr 4) and 15

    public val floor: Int get() = cell shr 8

    internal val stairDirection: Int get() = if (!type.stairHall || variant(1) == 0) 0 else when(variant(15)){1 -> 1; 2 -> -1; else -> 0}
    internal fun withStairs(tier:Int, direction:Int):HouseRoom = furnish(1,tier).furnish(15,if(tier==0)0 else if(direction>0)1 else 2)
    internal fun stairLanding(rooms:Map<Int,HouseRoom>):HouseRoom? {
        if(stairDirection==0)return null
        return rooms[cell+256*stairDirection]?.takeIf { it.stairDirection == -stairDirection && it.variant(1)==variant(1) && it.rotation==rotation }
    }
    internal val source:CoordGrid get() = if(stairDirection<0 && type != HouseRoomType.DungeonStairs) type.source.translateX(16) else type.source

    public fun withPortal(slot: Int, destination: Int): HouseRoom = furnish(slot + 8, destination)

    public fun portal(slot: Int): Int = variant(slot + 8)

    public fun neighbour(angle: Int): Int =
        when (angle) {
            0 -> cell - 1
            1 -> cell + 16
            2 -> cell + 1
            else -> cell - 16
        }

    public fun variant(slot: Int): Int = ((furniture ushr ((slot - 1) * 4)) and 15).toInt()

    public fun furnish(slot: Int, variant: Int): HouseRoom {
        val shift = (slot - 1) * 4
        return copy(
            furniture = (furniture and (15L shl shift).inv()) or (variant.toLong() shl shift)
        )
    }

    public companion object {
        public const val ENTRANCE_CELL: Int = 2 + 2 * 16

        public fun connected(rooms: Map<Int, HouseRoom>): Boolean {
            if (rooms.values.any { it.floor == 1 && rooms[it.cell-256]?.type?.outdoors != false }) return false
            val entrance = rooms[ENTRANCE_CELL] ?: return false
            val seen = mutableSetOf(entrance.cell)
            val pending = ArrayDeque(listOf(entrance))
            while (pending.isNotEmpty()) {
                val room = pending.removeFirst()
                room.stairLanding(rooms)?.let { if(seen.add(it.cell))pending.add(it) }
                for (angle in 0..3) {
                    val other = rooms[room.neighbour(angle)] ?: continue
                    if (
                        room.type.hasDoor(angle, room.rotation) &&
                            other.type.hasDoor((angle + 2) and 3, other.rotation) &&
                            seen.add(other.cell)
                    ) {
                        pending.add(other)
                    }
                }
            }
            return seen.size == rooms.size
        }
    }
}

@Singleton
public class HouseRoomRepository {
    private val layouts = WeakHashMap<Player, Map<Int, HouseRoom>>()

    @Synchronized
    public fun snapshot(player: Player): Map<Int, HouseRoom> = layouts[player].orEmpty()

    @Synchronized
    public fun replace(player: Player, rooms: Map<Int, HouseRoom>) {
        layouts[player] = rooms.toMap()
    }

    @Synchronized
    public fun layout(player: Player): Map<Int, HouseRoom> {
        val saved = snapshot(player)
        if (saved.isNotEmpty()) return saved
        val garden = HouseRoom(HouseRoom.ENTRANCE_CELL, HouseRoomType.Garden)
        var parlour = HouseRoom(garden.neighbour(1), HouseRoomType.Parlour)
        val original = player.vars[HouseVarps.furniture]
        for (slot in 1..7) parlour = parlour.furnish(slot, (original ushr (3 * (slot - 1))) and 7)
        return listOf(garden, parlour).associateBy { it.cell }.also { layouts[player] = it }
    }
}
