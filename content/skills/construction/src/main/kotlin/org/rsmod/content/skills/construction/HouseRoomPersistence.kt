package org.rsmod.content.skills.construction

import jakarta.inject.Inject
import org.rsmod.api.account.character.CharacterDataStage
import org.rsmod.api.account.character.CharacterMetadataList
import org.rsmod.api.db.DatabaseConnection
import org.rsmod.game.entity.Player
import org.rsmod.plugin.module.PluginModule

public class ConstructionModule : PluginModule() {
    override fun bind() {
        bindInstance<HouseRoomRepository>()
        addSetBinding<CharacterDataStage.Pipeline>(HouseRoomPipeline::class.java)
    }
}

public data class HouseRoomData(val rooms: Map<Int, HouseRoom>) : CharacterDataStage.Segment

public class HouseRoomApplier @Inject constructor(private val layouts: HouseRoomRepository) :
    CharacterDataStage.Applier<HouseRoomData> {
    override fun apply(player: Player, data: HouseRoomData) {
        layouts.replace(player, data.rooms)
    }
}

public class HouseRoomPipeline
@Inject
constructor(private val applier: HouseRoomApplier, private val layouts: HouseRoomRepository) :
    CharacterDataStage.Pipeline {
    override fun append(connection: DatabaseConnection, metadata: CharacterMetadataList) {
        val layout = mutableMapOf<Int, HouseRoom>()
        connection
            .prepareStatement(
                "SELECT cell, room_type, rotation, furniture, pet_rocks FROM house_rooms WHERE character_id = ?"
            )
            .use { query ->
                query.setInt(1, metadata.characterId)
                query.executeQuery().use { rows ->
                    while (rows.next()) {
                        val cell = rows.getInt(1)
                        layout[cell] =
                            HouseRoom(
                                cell,
                                HouseRoomType.valueOf(rows.getString(2)),
                                rows.getInt(3),
                                rows.getLong(4),
                                rows.getLong(5),
                            )
                    }
                }
            }
        metadata.add(applier, HouseRoomData(layout))
    }

    override fun save(connection: DatabaseConnection, player: Player, characterId: Int) {
        val rooms = layouts.snapshot(player)
        connection.prepareStatement("DELETE FROM house_rooms WHERE character_id = ?").use {
            it.setInt(1, characterId)
            it.executeUpdate()
        }
        if (rooms.isEmpty()) return
        connection
            .prepareStatement(
                "INSERT INTO house_rooms(character_id, cell, room_type, rotation, furniture, pet_rocks) VALUES (?, ?, ?, ?, ?, ?)"
            )
            .use { insert ->
                for (room in rooms.values) {
                    insert.setInt(1, characterId)
                    insert.setInt(2, room.cell)
                    insert.setString(3, room.type.name)
                    insert.setInt(4, room.rotation)
                    insert.setLong(5, room.furniture)
                    insert.setLong(6, room.petRocks)
                    insert.addBatch()
                }
                insert.executeBatch()
            }
    }
}
