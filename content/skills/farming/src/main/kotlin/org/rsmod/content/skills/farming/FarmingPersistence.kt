package org.rsmod.content.skills.farming

import jakarta.inject.Inject
import org.rsmod.api.account.character.CharacterDataStage
import org.rsmod.api.account.character.CharacterMetadataList
import org.rsmod.api.db.DatabaseConnection
import org.rsmod.game.entity.Player

public data class FarmingPatchData(val patches: List<FarmingPatchState>) :
    CharacterDataStage.Segment

public class FarmingPatchApplier
@Inject
constructor(private val repository: FarmingPatchRepository) :
    CharacterDataStage.Applier<FarmingPatchData> {
    override fun apply(player: Player, data: FarmingPatchData) {
        repository.replace(player, data.patches)
    }
}

public class FarmingPatchPipeline
@Inject
constructor(
    private val applier: FarmingPatchApplier,
    private val repository: FarmingPatchRepository,
) : CharacterDataStage.Pipeline {
    override fun append(connection: DatabaseConnection, metadata: CharacterMetadataList) {
        val patches = mutableListOf<FarmingPatchState>()
        connection
            .prepareStatement(
                """
                SELECT patch_key, patch_type, weeds, crop_key, stage, status, compost, watered,
                       harvest_lives, next_growth_at, resurrection_attempted, health_checked,
                       protected, scarecrow, compost_bin_kind, compost_bin_status,
                       compost_bin_count, compost_ready_at
                FROM farming_patches
                WHERE character_id = ?
            """
                    .trimIndent()
            )
            .use { statement ->
                statement.setInt(1, metadata.characterId)
                statement.executeQuery().use { rows ->
                    while (rows.next()) {
                        patches +=
                            FarmingPatchState(
                                patchKey = rows.getString("patch_key"),
                                patchType = FarmingPatchType.valueOf(rows.getString("patch_type")),
                                weeds = rows.getInt("weeds"),
                                cropKey = rows.getString("crop_key"),
                                stage = rows.getInt("stage"),
                                status = FarmingCropStatus.valueOf(rows.getString("status")),
                                compost = FarmingCompost.valueOf(rows.getString("compost")),
                                watered = rows.getInt("watered") != 0,
                                harvestLives = rows.getInt("harvest_lives"),
                                nextGrowthAt = rows.getLong("next_growth_at"),
                                resurrectionAttempted = rows.getInt("resurrection_attempted") != 0,
                                healthChecked = rows.getInt("health_checked") != 0,
                                protected = rows.getInt("protected") != 0,
                                scarecrow = rows.getInt("scarecrow") != 0,
                                compostBinKind =
                                    CompostBinKind.valueOf(rows.getString("compost_bin_kind")),
                                compostBinStatus =
                                    CompostBinStatus.valueOf(rows.getString("compost_bin_status")),
                                compostBinCount = rows.getInt("compost_bin_count"),
                                compostReadyAt = rows.getLong("compost_ready_at"),
                            )
                    }
                }
            }
        metadata.add(applier, FarmingPatchData(patches))
    }

    override fun save(connection: DatabaseConnection, player: Player, characterId: Int) {
        connection.prepareStatement("DELETE FROM farming_patches WHERE character_id = ?").use {
            it.setInt(1, characterId)
            it.executeUpdate()
        }
        val patches = repository.snapshot(player)
        if (patches.isEmpty()) return
        connection
            .prepareStatement(
                """
                INSERT INTO farming_patches (
                    character_id, patch_key, patch_type, weeds, crop_key, stage, status, compost,
                    watered, harvest_lives, next_growth_at, resurrection_attempted, health_checked,
                    protected, scarecrow, compost_bin_kind, compost_bin_status, compost_bin_count,
                    compost_ready_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """
                    .trimIndent()
            )
            .use { statement ->
                for (patch in patches) {
                    statement.setInt(1, characterId)
                    statement.setString(2, patch.patchKey)
                    statement.setString(3, patch.patchType.name)
                    statement.setInt(4, patch.weeds)
                    statement.setString(5, patch.cropKey)
                    statement.setInt(6, patch.stage)
                    statement.setString(7, patch.status.name)
                    statement.setString(8, patch.compost.name)
                    statement.setInt(9, if (patch.watered) 1 else 0)
                    statement.setInt(10, patch.harvestLives)
                    statement.setLong(11, patch.nextGrowthAt)
                    statement.setInt(12, if (patch.resurrectionAttempted) 1 else 0)
                    statement.setInt(13, if (patch.healthChecked) 1 else 0)
                    statement.setInt(14, if (patch.protected) 1 else 0)
                    statement.setInt(15, if (patch.scarecrow) 1 else 0)
                    statement.setString(16, patch.compostBinKind.name)
                    statement.setString(17, patch.compostBinStatus.name)
                    statement.setInt(18, patch.compostBinCount)
                    statement.setLong(19, patch.compostReadyAt)
                    statement.addBatch()
                }
                statement.executeBatch()
            }
    }
}
